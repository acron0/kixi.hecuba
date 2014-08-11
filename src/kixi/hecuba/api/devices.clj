(ns kixi.hecuba.api.devices
  (:require
   [clojure.core.match :refer (match)]
   [cheshire.core :as json]
   [clojure.tools.logging :as log]
   [kixi.hecuba.security :as sec]
   [kixi.hecuba.webutil :as util]
   [kixi.hecuba.data.misc :as m]
   [kixi.hecuba.webutil :refer (decode-body authorized? uuid stringify-values sha1-regex)]
   [liberator.core :refer (defresource)]
   [liberator.representation :refer (ring-response)]
   [qbits.hayt :as hayt]
   [kixi.hecuba.storage.db :as db]
   [kixi.hecuba.storage.sha1 :as sha1]
   [kixi.hecuba.web-paths :as p]
   [kixi.hecuba.data.users :as users]
   [kixi.hecuba.data.projects :as projects]
   [kixi.hecuba.data.entities :as entities]
   [kixi.hecuba.data.devices :as devices]
   [kixi.hecuba.data.sensors :as sensors]
   [kixi.hecuba.data.entities.search :as search]
   [schema.core :as s]
   [kixi.amon-schema :as schema]))

(def ^:private device-resource (p/resource-path-string :entity-device-resource))

(defn allowed?* [programme-id project-id allowed-programmes allowed-projects roles request-method]
  (match [(some #(isa? % :kixi.hecuba.security/admin) roles)
          (some #(isa? % :kixi.hecuba.security/programme-manager) roles)
          (some #(= % programme-id) allowed-programmes)
          (some #(isa? % :kixi.hecuba.security/project-manager) roles)
          (some #(= % project-id) allowed-projects)
          (some #(isa? % :kixi.hecuba.security/user) roles)
          request-method]
         ;; super-admin - do everything
         [true _ _ _ _ _ _] true
         ;; programme-manager for this programme - do everything
         [_ true true _ _ _ _] true
         ;; project-manager for this project - do everything
         [_ _ _ true true _ _] true
         ;; user with this programme - get allowed
         [_ _ true _ _ true :get] true
         ;; user with this project - get allowed
         [_ _ _ _ true true :get] true
         :else false))

(defn index-allowed? [store]
  (fn [ctx]
    (let [{:keys [body request-method session params]} (:request ctx)
          {:keys [projects programmes roles]} (sec/current-authentication session)
          project_id (:project_id body)
          programme_id (when project_id (:programme_id (projects/get-by-id (:hecuba-session store) project_id)))]
      (if (and project_id programme_id)
        (allowed?* programme_id project_id programmes projects roles request-method)
        true))))

(defn resource-allowed? [store]
  (fn [ctx]
    (let [{:keys [request-method session params]} (:request ctx)
          {:keys [projects programmes roles]}     (sec/current-authentication session)
          entity_id (:entity_id params)       
          project_id (when entity_id 
                       (:project_id (search/get-by-id entity_id (:search-session store))))
          programme_id (when project_id (:programme_id (projects/get-by-id (:hecuba-session store) project_id)))]
      (if (and project_id programme_id)
        (allowed?* programme_id project_id programmes projects roles request-method)
        true))))

(defn index-exists? [store ctx]
  (db/with-session [session (:hecuba-session store)]
    (let [request        (:request ctx)
          method         (:request-method request)
          route-params   (:route-params request)
          entity_id      (:entity_id route-params)
          entity         (search/get-by-id entity_id (:search-session store))]
      (case method
        :post (not (nil? entity))
        :get (let [items (devices/get-devices session entity_id)]
               {::items items})))))

(defn should-calculate-fields? [sensors]
  (not (some #(and (= (:period %) "CUMULATIVE")
                   (or (= (:actual_annual_calculation %) true)
                       (= (:normalised_annual_calculation %) true))) sensors)))

(def user-editable-keys [:device_id :type :accuracy :alias :actual_annual
                         :corrected_unit :correction
                         :correction_factor :correction_factor_breakdown
                         :frequency :max :min :period
                         :resolution :unit :user_metadata])

(defn index-malformed? [ctx]
  (let [request (:request ctx)
        {:keys [route-params request-method]} request
        entity_id (:entity_id route-params)]
    (case request-method
      :post (let [body (update-in (decode-body request) [:readings]
                                  (fn [readings] (map #(select-keys % user-editable-keys)
                                                      readings)))]
              (if (or
                   (s/check schema/BaseDevice body)
                   (not= (:entity_id body) entity_id)
                   (not (should-calculate-fields? (:readings body))))
                true
                [false {:body body}]))
      false)))
(defn- ext-type [sensor type-ext]
  (-> sensor
      (update-in [:type] #(str % "_" type-ext))
      (assoc :period "PULSE" :synthetic true)))

(defmulti calculated-sensor (fn [sensor] (.toUpperCase (:unit sensor))))

(defmethod calculated-sensor "KWH" [sensor]
  (-> sensor
      (assoc :unit "co2" :synthetic true)
      (update-in [:type] #(m/output-type-for % "KWH2CO2"))))

(defmethod calculated-sensor "M^3" [sensor]
  (let [kwh-sensor (-> sensor
                       (assoc :unit "kWh" :synthetic true)
                       (update-in [:type] #(m/output-type-for % "VOL2KWH")))]
    [kwh-sensor (calculated-sensor kwh-sensor)]))

(defmethod calculated-sensor "FT^3" [sensor]
  (let [kwh-sensor (-> sensor
                       (assoc :unit "kWh" :synthetic true)
                       (update-in [:type] #(m/output-type-for % "VOL2KWH")))]
    [kwh-sensor (calculated-sensor kwh-sensor)]))

(defmethod calculated-sensor :default [sensor])

(defn create-default-sensors
  "Creates default sensors whenever new device is added: *_differenceSeries for CUMULATIVE,
   and *_co2 for kwh PULSE, etc."
  [body]
  (let [sensors        (:readings body)
        new-sensors    (map #(case (:period %)
                               "CUMULATIVE" (ext-type % "differenceSeries")
                               "PULSE"      (calculated-sensor %)
                               "INSTANT"    nil
                               nil) sensors)]
    (update-in body [:readings] (fn [readings] (into [] (remove nil? (flatten (concat readings new-sensors))))))))

(defn index-post! [store ctx]
  (db/with-session [session (:hecuba-session store)]
    (let [{:keys [request body]} ctx
          entity_id              (-> request :route-params :entity_id)
          username               (sec/session-username (-> ctx :request :session))
          user_id                (:id (users/get-by-username session username))]

      (let [device_id    (sha1/gen-key :device body)
            new-body     (create-default-sensors body)]
        (devices/insert session entity_id (assoc new-body :device_id device_id :user_id user_id))
        (doseq [reading (:readings new-body)]
          (sensors/insert session (assoc reading :device_id device_id :user_id user_id)))
        (-> (search/searchable-entity-by-id entity_id session)
            (search/->elasticsearch (:search-session store)))
        {:device_id device_id :entity_id entity_id}))))

(defn index-handle-ok [ctx]
  (util/render-items ctx (::items ctx)))

(defn index-handle-created [ctx]
  (let [entity_id (-> ctx :entity_id)
        device_id (-> ctx :device_id)
        location  (format device-resource entity_id device_id)]
    (ring-response {:headers {"Location" location}
                    :body (json/encode {:location location
                                        :status "OK"
                                        :version "4"})})))

(defn resource-exists? [store ctx]
  (db/with-session [session (:hecuba-session store)]
    (let [device_id (-> ctx :request :route-params :device_id)
          item      (devices/get-by-id session device_id)]
      (if-not (empty? item)
        {::item item}
        false))))

;; Should be device-response etc and it should do the delete in delete!,
;; that should put something in the context which is then checked here.
(defn resource-delete-enacted? [store ctx]
  (db/with-session [session (:hecuba-session store)]
    (let [{item ::item} ctx
          device_id (:device_id item)
          entity_id (:entity_id item)
          response  (devices/delete device_id false session)]
      "Delete Accepted")))

(defn resource-put! [store ctx]
  (db/with-session [session (:hecuba-session store)]
    (let [{request :request} ctx]
      (if-let [item (::item ctx)]
        (let [body          (decode-body request)
              entity_id     (-> item :entity_id)
              username      (sec/session-username (-> ctx :request :session))
              user_id       (:id (users/get-by-username session username))
              device_id     (-> item :device_id)
              new-body      (create-default-sensors body)]
          (devices/update session entity_id device_id (assoc new-body :user_id user_id))
          ;; TODO when new sensors are created they do not necessarilly overwrite old sensors (unless their type is the same)
          ;; We should probably allow to delete sensors through the API/UI
          (doseq [reading (:readings new-body)]
            ;; cassandra's insert only updates specified keys so no need to use update
            (sensors/insert session (assoc reading :device_id device_id
                                           :user_id user_id)))
          (-> (search/searchable-entity-by-id entity_id session)
              (search/->elasticsearch (:search-session store)))
          (ring-response {:status 404 :body "Please provide valid entity_id and device_id"}))))))

(defn resource-handle-ok [ctx]
  (let [req (:request ctx)]
    (util/render-item ctx (::item ctx))))

(defn resource-respond-with-entity [ctx]
  (let [request (:request ctx)
        method  (:request-method request)]
    (cond
     (= method :delete) false
      :else true)))

(defresource index [store]
  :allowed-methods #{:get :post}
  :available-media-types #{"application/json" "application/edn"}
  :known-content-type? #{"application/json"}
  :authorized? (authorized? store)
  :allowed? (index-allowed? store)
  :exists? (partial index-exists? store)
  :malformed? index-malformed?
  :can-post-to-missing? (constantly false)
  :post! (partial index-post! store)
  :handle-ok index-handle-ok
  :handle-created index-handle-created)

(defresource resource [store]
  :allowed-methods #{:get :delete :put}
  :available-media-types #{"application/json" "application/edn"}
  :authorized? (authorized? store)
  :allowed? (resource-allowed? store)
  :exists? (partial resource-exists? store)
  :delete-enacted? (partial resource-delete-enacted? store)
  :respond-with-entity? resource-respond-with-entity
  :new? (constantly false)
  :can-put-to-missing? (constantly false)
  :put! (partial resource-put! store)
  :handle-ok resource-handle-ok)
