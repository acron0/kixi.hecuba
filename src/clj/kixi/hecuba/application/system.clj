(ns kixi.hecuba.application.system
  (:require
   [com.stuartsierra.component :as component]
   [clojure.tools.logging :as log]

   ;; Hecuba custom components

   [kixi.hecuba.controller.pipeline :refer (new-pipeline)]
   [kixipipe.scheduler]
   [kixipipe.storage.s3 :as s3]
   [kixi.hecuba.routes :refer (new-web-app)]
   [kixi.hecuba.storage.db :as db]
   [kixi.hecuba.storage.search :as search]
   [kixi.hecuba.email :refer (new-email)]

   ;; Misc
   [clojure.edn :as edn]
   [clojure.java.io :as io]))

(defn combine
  "Merge maps, recursively merging nested maps whose keys collide."
  ([] {})
  ([m] m)
  ([m1 m2]
    (reduce (fn [m1 [k2 v2]]
              (if-let [v1 (get m1 k2)]
                (if (and (map? v1) (map? v2))
                  (assoc m1 k2 (combine v1 v2))
                  (assoc m1 k2 v2))
                (assoc m1 k2 v2)))
            m1 m2))
  ([m1 m2 & more]
    (apply combine (combine m1 m2) more)))

(defn- pushback-reader [x]
  (java.io.PushbackReader. (io/reader x)))

(defn config []
  (let [f (io/file (System/getProperty "user.home") ".hecuba.edn")]
    (combine (edn/read (pushback-reader (io/resource "default.hecuba.edn")))
             (when (.exists f)
               (edn/read (pushback-reader (io/input-stream f)))))))

(defn new-system []
  (let [cfg (config)]
    (-> (component/system-map
         :cluster (db/new-cluster (:cassandra-cluster cfg))
         :hecuba-session (db/new-session (:hecuba-session cfg))
         :search-session (search/new-search-session (:search-session cfg))
         :s3 (s3/mk-session (:s3 cfg))
         :store (db/new-store)
         :pipeline (new-pipeline)
         :scheduler (kixipipe.scheduler/mk-session cfg)
         :web-app (new-web-app cfg)
         :e-mail (new-email (:e-mail cfg)))
        (component/system-using
         {:web-app [:store :s3 :pipeline :e-mail]
          :store [:hecuba-session :s3 :search-session :e-mail]
          :pipeline [:store]
          :scheduler [:pipeline]
          :hecuba-session [:cluster]}))))
