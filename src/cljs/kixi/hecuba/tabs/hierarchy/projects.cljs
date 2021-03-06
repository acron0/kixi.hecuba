(ns kixi.hecuba.tabs.hierarchy.projects
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require
   [om.core :as om :include-macros true]
   [cljs.core.async :refer [<! >! chan put!]]
   [clojure.string :as str]
   [kixi.hecuba.history :as history]
   [kixi.hecuba.tabs.slugs :as slugs]
   [kixi.hecuba.bootstrap :as bs]
   [kixi.hecuba.common :refer (log) :as common]
   [kixi.hecuba.tabs.hierarchy.data :refer (fetch-projects)]
   [sablono.core :as html :refer-macros [html]]
   [kixi.hecuba.model :refer (app-model)]))

(defn programmes []
  (om/ref-cursor (-> (om/root-cursor app-model) :programmes :data)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; projects

(defn error-handler [owner]
  (fn [{:keys [status status-text]}]
    (om/set-state! owner :alert {:status true
                                 :class "alert alert-danger"
                                 :text status-text})))

(defn valid-project? [project projects]
  (let [project-name (:name project)]
    (and (seq project-name) (empty? (filter #(= (:name %) project-name) projects)))))

(defn post-new-project [projects-data refresh-chan owner project programme_id]
  (let [url  (str "/4/programmes/" programme_id "/projects/")]
    (common/post-resource url
                          (assoc project :created_at (common/now->str)
                                 :programme_id programme_id)
                          (fn [_]
                            (put! refresh-chan {:event :projects})
                            (om/update! projects-data :project project)
                            (om/update! projects-data :adding-project false))
                          (error-handler projects-data))))

(defn put-edited-project [history projects-data refresh-chan owner project programme_id project_id]
  (let [url             (str "/4/programmes/" programme_id  "/projects/" project_id)
        updated-project (-> project
                            (assoc :updated_at (common/now->str))
                            (dissoc :properties :slug :selected :editable :href))]
    (common/put-resource url
                         updated-project
                         (fn [_]
                           (history/update-token-ids! history :programmes (:programme_id updated-project))
                           (history/update-token-ids! history :projects project_id)
                           (put! refresh-chan {:event :projects})
                           (om/update! projects-data :alert {:status false})
                           (om/update! projects-data :editing false))
                         (error-handler owner))))

(defn delete-project [projects-data programme_id project_id history refresh-chan]
  (common/delete-resource (str "/4/programmes/" programme_id  "/projects/" project_id)
                          (fn []
                            (put! refresh-chan {:event :projects})
                            (history/update-token-ids! history :projects nil)
                            (om/update! projects-data :editing false))
                          (error-handler projects-data)))

(defn project-add-form [projects programme_id]
  (fn [cursor owner]
    (reify
      om/IInitState
      (init-state [_]
        {:project {}
         :alert {}})
      om/IRenderState
      (render-state [_ state]
        (let [refresh-chan (om/get-shared owner :refresh)]
          (html
           [:div
            [:h3 "Add new project"]
            [:form.form-horizontal {:role "form"}
             [:div.col-md-6
              [:div.form-group
               [:div.btn-toolbar
                [:button {:class "btn btn-success"
                          :type "button"
                          :onClick (fn [_] (let [project (om/get-state owner [:project])]

                                             (if (valid-project? project (:data @projects))
                                               (post-new-project projects refresh-chan owner project programme_id)
                                               (om/set-state! owner :alert {:status true
                                                                            :class "alert alert-danger"
                                                                            :text "Please enter unique name of the project."}))))}
                 "Save"]
                [:button {:type "button"
                          :class "btn btn-danger"
                          :onClick (fn [_]
                                     (om/update! projects :adding-project false))}
                 "Cancel"]]]
              [:div {:id "project-add-alert"} (bs/alert owner)]
              (bs/text-input-control owner [:project :name] "Project Name" true)
              (bs/text-input-control owner [:project :description] "Description")
              (bs/text-input-control owner [:project :organisation] "Organisation")
              (bs/text-input-control owner [:project :project_code] "Project Code")
              (bs/text-input-control owner [:project :project_type] "Project Type")
              (bs/text-input-control owner [:project :type_of] "Type Of")]]]))))))

(defn project-edit-form [projects-data]
  (fn [cursor owner]
    (reify
      om/IInitState
      (init-state [_]
        {:project (:project cursor)
         :alert {}})
      om/IRenderState
      (render-state [_ state]
        (let [{:keys [project_id
                      programme_id]} (:project cursor)
                      history                (om/get-shared owner :history)
                      refresh-chan           (om/get-shared owner :refresh)
                      all-programmes         (om/observe owner (programmes))
                      available-programmes   (sort-by :display (keep (fn [p]
                                                                       (when (:editable p)
                                                                         (hash-map :display (:name p)
                                                                                   :value (:programme_id p)))) all-programmes))]
          (html
           [:div
            [:h3 (:name cursor)]
            [:form.form-horizontal {:role "form"}
             [:div.col-md-12
              [:div.form-group
               [:div.btn-toolbar
                [:button {:type "button"
                          :class "btn btn-success"
                          :onClick (fn [_]
                                     (let [project (om/get-state owner [:project])]
                                       (put-edited-project history projects-data refresh-chan owner project
                                                           programme_id project_id)))} "Save"]
                [:button {:type "button"
                          :class "btn btn-danger"
                          :onClick (fn [_] (om/update! projects-data :editing false))} "Cancel"]
                [:button {:type "button"
                          :class "btn btn-danger pull-right"
                          :onClick (fn [_]
                                     (delete-project projects-data programme_id project_id  history refresh-chan))}
                 "Delete Project"]]]
              [:div {:id "project-edit-alert"} (bs/alert owner)]
              [:div.col-md-4
               (bs/dropdown cursor owner [:project :programme_id] available-programmes programme_id "Programme")
               (bs/text-input-control owner [:project :name] "Project Name")
               (bs/text-input-control owner [:project :organisation] "Organisation")
               (bs/text-input-control owner [:project :project_code] "Project Code")
               (bs/text-input-control owner [:project :project_type] "Project Type")
               (bs/text-input-control owner [:project :type_of] "Type Of")
               (bs/static-text cursor [:project :created_at] "Created At")]
              [:div.col-md-8
               (bs/text-area-control owner [:project :description] "Description")
               (bs/static-text cursor [:project :project_id] "API Project ID")]]]]))))))

(defn project-detail [projects-data editing-chan]
  (fn [cursor owner]
    (om/component
     (let [{:keys [project_id programme_id]} cursor]
       (html
        [:div.col-md-12
         [:h1 (:name cursor)
          (when (:editable cursor)
            [:button {:type "button"
                      :title "Edit"
                      :class "btn btn-primary pull-right fa fa-pencil-square-o"
                      :onClick (fn [_] (put! editing-chan cursor))}])]
         [:div.row
          [:div.col-md-4
           (bs/static-text cursor [:organisation] "Organisation")
           (bs/static-text cursor [:project_code] "Project Code")
           (bs/static-text cursor [:project_type] "Project Type")
           (bs/static-text cursor [:type_of] "Type Of")
           (bs/static-text cursor [:created_at] "Created At")]
          [:div.col-md-8
           (bs/static-text cursor [:description] "Description")
           (bs/static-text cursor [:project_id] "API Project ID")]]])))))

(defn project-row [project owner {:keys [table-id editing-chan]}]
  (reify
    om/IRender
    (render [_]
      (html
       (let [{:keys [project_id name type_of description
                     created_at organisation project_code editable selected]} project
                     history   (om/get-shared owner :history)]
         [:tr {:onClick (fn [e]
                          (history/update-token-ids! history :projects project_id)
                          (common/fixed-scroll-to-element "project-detail-div"))
               :class (when selected "success")
               :id (str table-id "-selected")}
          [:td name]
          [:td type_of]
          [:td organisation]
          [:td project_code]])))))

(defmulti projects-table  (fn [projects owner opts] (:fetching projects)))

(defmethod projects-table :fetching [projects owner opts]
  (om/component
   (html
    (bs/fetching-row projects))))

(defmethod projects-table :no-data [projects owner opts]
  (om/component
   (html
    (bs/no-data-row projects))))

(defmethod projects-table :error [projects owner opts]
  (om/component
   (html
    (bs/no-data-row projects))))

(defmethod projects-table :has-data [projects owner {:keys [editing-chan]}]
  (reify
    om/IInitState
    (init-state [_]
      {:th-chan (chan)})
    om/IWillMount
    (will-mount [_]
      (go-loop []
        (let [{:keys [th-chan]}           (om/get-state owner)
              sort-spec                   (:sort-spec @projects)
              {:keys [sort-key sort-asc]} sort-spec
              th-click                    (<! th-chan)]
          (if (= th-click sort-key)
            (om/update! projects :sort-spec {:sort-key th-click
                                             :sort-asc (not sort-asc)})
            (om/update! projects :sort-spec {:sort-key th-click
                                             :sort-asc true})))
        (recur)))
    om/IRenderState
    (render-state [_ state]
      (html
       (let [table-id   "projects-table"
             history    (om/get-shared owner :history)
             th-chan    (om/get-state owner :th-chan)
             sort-spec  (:sort-spec projects)
             {:keys [sort-key sort-asc]} sort-spec]
         [:div.row
          [:div.col-md-12
           [:table {:className "table table-hover"}
            [:thead
             [:tr
              (bs/sorting-th sort-spec th-chan "Name" :name)
              (bs/sorting-th sort-spec th-chan "Type" :type)
              (bs/sorting-th sort-spec th-chan "Organisation" :organisation)
              (bs/sorting-th sort-spec th-chan "Project Code" :project_code)]]
            [:tbody
             (om/build-all project-row (if sort-asc
                                         (sort-by sort-key (:data projects))
                                         (reverse (sort-by sort-key (:data projects))))
                           {:opts {:table-id table-id
                                   :editing-chan editing-chan}
                            :key :project_id})]]]])))))

(defmethod projects-table :default [_]
  (fn [projects owner opts]
    (om/component
     (html
      [:div.row [:div.col-md-12]]))))

(defn projects-div [projects owner]
  (reify
    om/IInitState
    (init-state [_]
      {:editing-chan (chan)})
    om/IWillMount
    (will-mount [_]
      (go-loop []
        (let [{:keys [editing-chan]} (om/get-state owner)
              edited-row             (<! editing-chan)]
          (om/update! projects :editing true)
          (om/update! projects :edited-row {:project edited-row})
          (common/fixed-scroll-to-element "projects-div"))
        (recur)))
    om/IRenderState
    (render-state [_ {:keys [editing-chan]}]
      (let [editing          (-> projects :editing)
            adding-project   (-> projects :adding-project)
            programme_id     (-> projects :programme_id)
            can-add-projects (-> projects :can-add-projects)
            refresh-chan     (om/get-shared owner :refresh)
            selected         (:selected projects)
            selected-project (first (filter #(= (:project_id %) selected) (:data projects)))]
        (html
         [:div.row#projects-div
          [:div {:class (str "col-md-12 " (if programme_id "" "hidden"))}
           [:h1 "Projects"
            (when (and
                  (not editing)
                  (not adding-project)
                  can-add-projects) ;; programme is editable so allow to add new projects
             [:button.btn.pull-right.fa.fa-plus
              {:type "button"
               :title "Add new"
               :class (str "btn btn-primary " (if editing "hidden" ""))
               :onClick (fn [_]
                          (om/update! projects :adding-project true))}])]
           [:div#projects-add-div
            (when adding-project
              (om/build (project-add-form projects programme_id) (:new-project projects)))]
           [:div#projects-edit-div
            (when editing
              (om/build (project-edit-form projects) (-> projects :edited-row)))]
           [:div#projects-div
            (when-not (or editing adding-project)
              (om/build projects-table projects {:opts {:editing-chan editing-chan}}))]
           [:div#project-detail-div
            (when (and selected (not (or adding-project editing)))
              (om/build (project-detail projects editing-chan) selected-project))]]])))))
