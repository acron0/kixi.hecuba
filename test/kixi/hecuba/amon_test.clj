(ns kixi.hecuba.amon-test
  (:use clojure.test)
  (:require
   [kixi.hecuba.dev :refer (create-ref-store ->CassandraDirectCommander ->CassandraQuerier sha1-keyfn)]
   [kixi.hecuba.web.amon :as amon]
   [kixi.hecuba.hash :refer (sha1)]
   [kixi.hecuba.protocols :refer (item items)]
   [cheshire.core :refer (generate-string parse-string)]
   [bidi.bidi :as bidi]
   [clojure.edn :as edn]
   [jig.bidi :refer (wrap-routes)]
   [ring.mock.request :refer (request header)]
   [camel-snake-kebab :refer (->camelCaseString)]
   [clojure.walk :refer (postwalk)])
  (:import (java.util UUID)))

(defn uuid [] (java.util.UUID/randomUUID))

(defn make-entity
  ([] {}))

(defn add-devices [entity & uuids]
  (update-in entity [:device-ids] concat uuids))

(defn add-metering-points [entity & uuids]
  (update-in entity [:metering-point-ids] concat uuids))

(defn add-content-type [req mime-type]
  (update-in req [:headers] conj ["Content-Type" mime-type]))

(defn ->camelCaseKeywords
  "Turn keywords into camelCase (the AMON API convention)"
  [body]
  (postwalk #(if (keyword? %) (->camelCaseString %) %) body))

(defn serialize-body [body]
  (-> body ->camelCaseKeywords generate-string))

(defn as-json-body-request [body path]
  (-> (request :post path)
      (add-content-type "application/json")
      (assoc :body (serialize-body body))))

(def example-programme {:name "America" :leaders "Bush"})

(defn create-programme [store]
  (let [handlers (-> store amon/make-handlers)
        routes (-> handlers amon/make-routes)
        _ (is (not (nil? (:programmes handlers))))
        path (-> routes (bidi/path-for (:programmes handlers)))
        _ (is (not (nil? path)))
        handler (-> routes bidi/make-handler (wrap-routes routes))
        _ (is (not (nil? handler)))
        response (-> (request :post path)
                     (add-content-type "application/edn")
                     (assoc :body (pr-str example-programme))
                     handler)
        location (get-in response [:headers "Location"])
        _ (is (not (nil? location)))
        {handler :handler {:keys [programme-id]} :params} (bidi/match-route routes location)]
    (is (= "/programmes/9913ca29c6bde71f5e1e7c2e6ec4da88c2a3ed19" location))
    (is (= "9913ca29c6bde71f5e1e7c2e6ec4da88c2a3ed19" programme-id))
      ;; Return the programme-id, might be useful
    programme-id))

(defn get-programme [{:keys [commander querier] :as store} id]
  (let [handlers (-> store amon/make-handlers)
        routes (-> handlers amon/make-routes)
        path (-> routes (bidi/path-for (:programme handlers) :programme-id (str id)))
        handler (-> routes bidi/make-handler (wrap-routes routes))]
    (is (= (count (items querier :programme)) 1))
    (let [response (-> (request :get path)
                       (header :accept "application/edn")
                       handler)]
      (is (not (nil? response)))
      (is (= (:status response) 200))
      (is (re-matches #"application/edn.*" (get-in response [:headers "Content-Type"])))
      (when (= (:status response) 200)
        (let [body (edn/read-string (:body response))]
          (is (= example-programme (dissoc body :id :projects)))
          body)))))

(def example-project {:name "Green Manhattan"})

(defn create-project [store programme-id]
  (let [handlers (-> store amon/make-handlers)
        routes (-> handlers amon/make-routes)
        _ (is (not (nil? (:projects handlers))))
        path (-> routes (bidi/path-for (:projects handlers) :programme-id programme-id))
        _ (is (not (nil? path)))
        handler (-> routes bidi/make-handler (wrap-routes routes))
        _ (is (not (nil? handler)))
        response (-> (request :post path)
                     (add-content-type "application/edn")
                     (assoc :body (pr-str example-project))
                     handler)
        location (get-in response [:headers "Location"])
        _ (is (not (nil? location)))
        {handler :handler {:keys [project-id]} :params} (bidi/match-route routes location)]
    (is (= "/projects/ce9801586b97977016b77ead6de53e8ece6d6f9d" location))
    (is (= "ce9801586b97977016b77ead6de53e8ece6d6f9d" project-id))
      ;; Return the project-id, might be useful
    project-id))

;; TODO: Can this be simplified with Prismatic graph?
(defn create-entity [{:keys [commander querier] :as store}]
  ;; There a good reason to run directly against Liberator handlers
  ;; rather than via http-kit, which is that when things go wrong,
  ;; there's a single stacktrace across the whole stack.
  (let [handlers (-> store amon/make-handlers)
        routes (-> handlers amon/make-routes)
        path (-> routes (bidi/path-for (:entities handlers)))
        handler (-> routes bidi/make-handler (wrap-routes routes))
        response (-> (make-entity)
                     (add-devices (uuid))
                     (as-json-body-request path)
                     handler)]
    (is (not (nil? response)))
    (is (= (:status response) 201))
    (is (= (count (items querier :entity)) 1))
    (let [{handler :handler {entity-id :entity-id} :params}
          (bidi/match-route routes (get-in response [:headers "Location"]))]
      (is (= handler (:entity handlers)))
      (is (not (nil? (item querier :entity entity-id))))
      ;; Return the entity-id, might be useful
      entity-id)))

(defn get-entity [{:keys [commander querier] :as store} id]
  (let [handlers (-> store amon/make-handlers)
        routes (-> handlers amon/make-routes)
        path (-> routes (bidi/path-for (:entity handlers) :entity-id (str id)))
        handler (-> routes bidi/make-handler (wrap-routes routes))]
    (is (= (count (items querier :entity)) 1))
    (let [response (-> (request :get path) handler)]
      (is (not (nil? response)))
      (is (= (:status response) 200))
      (is (re-matches #"application/json.*" (get-in response [:headers "Content-Type"])))
      (when (= (:status response) 200)
        (let [json (parse-string (:body response))]
          (is (contains? json "entityId")))))))

(defn delete-entity [{:keys [commander querier] :as store} id]
  (let [handlers (-> store amon/make-handlers)
        routes (-> handlers amon/make-routes)
        path (-> routes (bidi/path-for (:entity handlers) :entity-id (str id)))
        handler (-> routes bidi/make-handler (wrap-routes routes))]
    (is (= (count (items querier :entity)) 1))
    (let [response (-> (request :delete path) handler)]
      (is (not (nil? response)))
      (is (= (:status response) 204))
      (is (= (count (items querier :entity)) 0)))))

;; TODO Try deleting an entity that doesn't exist! (should get a 404)

(defn create-device [{:keys [commander querier] :as store} entity-id]
  (let [handlers (-> store amon/make-handlers)
        routes (-> handlers amon/make-routes)
        path (-> routes (bidi/path-for (:devices handlers) :entity-id (str entity-id)))
        _ (is (= path (str "/entities/" entity-id "/devices")))
        handler (-> routes bidi/make-handler (wrap-routes routes))
        response (-> {"entityId" entity-id}
                     (as-json-body-request path)
                     handler)]
    (is (not (nil? response)))
    (is (= (:status response) 201))
    (let [location (get-in response [:headers "Location"])
          {handler :handler {entity-id :entity-id
                             device-id :device-id} :params}
          (bidi/match-route routes location)
          device-id (java.util.UUID/fromString device-id)]
      (is (not (nil? (java.util.UUID/fromString entity-id))))
      ;; Return the device-id to the caller, might be useful
      device-id
      )))

(defn create-device-with-bad-entity-in-body [{:keys [commander querier] :as store} entity-id]
  (let [handlers (-> store amon/make-handlers)
        routes (-> handlers amon/make-routes)
        path (-> routes (bidi/path-for (:devices handlers) :entity-id (str entity-id)))
        _ (is (= path (str "/entities/" entity-id "/devices")))
        handler (-> routes bidi/make-handler (wrap-routes routes))
        response (-> {"entityId" (str entity-id "BAD_DATA")}
                     (as-json-body-request path)
                     handler)]
    (is (not (nil? response)))
    (is (= (:status response) 400))))

(defn send-measurements [{:keys [commander querier] :as store} entity-id device-id measurements]
  (let [handlers (-> store amon/make-handlers)
        routes (-> handlers amon/make-routes)
        path (-> routes (bidi/path-for (:measurements handlers)
                                       :entity-id (str entity-id)
                                       :device-id (str device-id)))
        handler (-> routes bidi/make-handler (wrap-routes routes))
        response (-> {"measurements" measurements}
                     (as-json-body-request path)
                     handler)]
    (is (not (nil? response)))
    (is (= (:status response) 202))))

(defn get-programmes [{:keys [commander querier] :as store}]
  (let [handlers (-> store amon/make-handlers)
        routes (-> handlers amon/make-routes)
        path (-> routes (bidi/path-for (:programmes handlers)))
        handler (-> routes bidi/make-handler (wrap-routes routes))

        response (-> (request :get "/programmes/")
                     (header :accept "application/edn")
                     handler)]
    (is (not (nil? response)))
    (is (= (:status response) 200))
    (is (re-matches #"application/edn.*" (get-in response [:headers "Content-Type"])))
    (edn/read-string (:body response))))

(defn get-projects [store programme]
  (let [handlers (-> store amon/make-handlers)
        routes (-> handlers amon/make-routes)
        handler (-> routes bidi/make-handler (wrap-routes routes))
        response (-> (request :get (:projects programme))
                     (header :accept "application/edn")
                     handler)]
    (is (not (nil? response)))
    (is (= (:status response) 200))
    (is (re-matches #"application/edn.*" (get-in response [:headers "Content-Type"])))
    (let [body (edn/read-string (:body response))]
      (is (coll? body))
      body)))

(defn amon-api-tests [store]

  (is (= (count (get-programmes store)) 0))

  (let [programme-id (create-programme store)]
    (let [programme (get-programme store programme-id)]
      (is (= (select-keys programme [:leaders :name])
             example-programme))
      (is (not (nil? (:projects programme))))
      (is (= (count (get-projects store programme)) 0))
      (create-project store (:id programme))
      (is (= (count (get-projects store programme)) 1))))

  ;; Now we have some programmes added, let's test the listing
  (is (= (count (get-programmes store)) 1))

  (let [id (create-entity store)]
    (get-entity store id)
    (delete-entity store id))
  (let [entity-id (create-entity store)]
    (create-device-with-bad-entity-in-body store entity-id)
    (let [device-id (create-device store entity-id)]
      (send-measurements store entity-id device-id
                       [{:type :temperature :value 50}
                        {:type :temperature :value 60}
                        {:type :temperature :value 55}]))))

(deftest amon-api-tests-local-ref
  (let [r (ref {})]
    (amon-api-tests
     (create-ref-store r (fn [typ payload]
                           (case typ
                             :programme ((sha1-keyfn :name) payload)
                             :project ((sha1-keyfn :name) payload)
                             :entity (:id payload)
                             :device (:id payload)
                             (throw (ex-info (format "TODO: make key for %s" typ) {}))))))))

;; We can use user/system because we're in test code
(deftest amon-api-tests-real-db
  (amon-api-tests
   {:commander (->CassandraDirectCommander (get-in user/system [:cassandra :session]))
    :querier (->CassandraQuerier (get-in user/system [:cassandra :session]))})
  ;; TODO: clear down the Cassandra database
  )
