(ns phi-todo.core
  (:require [datascript :as d]
            [phi.core :as phi
             :refer [conn component publish! event]]
            [phi-todo.domain :as domain]
            [goog.events :as events]
            [goog.history.EventType :as EventType]
            [secretary.core :as secretary :refer-macros [defroute]])
  (:import [goog History]))

(enable-console-print!)

(phi/init-conn! (d/create-conn domain/schema))
(d/transact conn domain/initial-state)
#_(phi/start-debug-conn!)
#_(phi/start-debug-events!)

(def history (History.))

(doto history
  (events/listen
    EventType/NAVIGATE
    (fn [e] (secretary/dispatch! (.-token e))))
  (.setEnabled true))

(defroute "/" []
  (publish!
    (event
      :filter {:showing :all})))

(defroute "/:showing" [showing]
  (publish!
    (event
      :filter {:showing (keyword showing)})))

(def new-item-input
  (component
    (reify
      phi/IPhi
      (render [_ db]
        (let [item-text (domain/get-new-item-text db)]
          [:form
           {:on-submit
            (fn [e]
              (.preventDefault e)
              (publish!
                (event :save-new-todo {})))}
           [:input#new-todo
            {:ref "input"
             :placeholder "What needs to be done?"
             :value item-text
             :on-change
             (fn [e]
               (publish!
                 (event
                   :new-item-input/input-change {:value (.. e -target -value)})))}]]))
      phi/IDidMount
      (did-mount [_ c]
        (.focus (phi/get-dom-node (phi/get-ref c "input")))))))

(def toggle-all
  (component
    (reify
      phi/IPhi
      (render [_ db]
        (let [all-complete? (domain/all-items-complete? db)]
          [:input#toggle-all
           {:type :checkbox
            :checked (not all-complete?)
            :on-change
            (fn [_]
              (publish!
                (event
                  :set-all-states {:complete? (not all-complete?)})))}])))))

(defn item-edit [id item]
  [:input.edit
   {:ref :editItemInput
    :value (:todo-item/text item)
    :on-change
    (fn [e]
      (publish!
        (event
          :todo-item/set-text {:id id, :text (.. e -target -value)})))
    :on-blur
    (fn [e]
      (publish!
        (event
          :todo-item/stop-editing {:id id})))
    :on-key-down
    (fn [e]
      (when (#{13 27} (.-which e))
        (publish!
          (event
            :todo-item/stop-editing {:id id}))))}])

(defn display-item [id item]
  [:div.view
   [:input.toggle
    {:type :checkbox
     :checked (when (= :complete (:todo-item/state item)) "checked")
     :on-change #(publish!
                  (event
                    :todo-item/toggle-complete {:id id}))}]
   [:label
    {:on-double-click #(publish!
                        (event
                          :todo-item/start-editing {:id id}))}
    (:todo-item/text item)]
   [:button.destroy
    {:on-click #(publish!
                 (event
                   :todo-item/delete {:id id}))}]])

(def item
  (component
    (reify
      phi/IPhi
      (render [_ {:keys [item-id]} db]
        (let [item (d/entity db item-id)
              complete? (= :complete (:todo-item/state item))
              editing? (:todo-item/editing? item)]
          [:li
           {:class (cond
                     complete? "completed"
                     editing? "editing")}
           (display-item item-id item)
           (item-edit item-id item)]))
      phi/IDidUpdate
      (did-update [_ comp {:keys [item-id]} this-db _prev-props _prev-db]
        (let [node (phi/get-dom-node (phi/get-ref comp "editItemInput"))
              len (.. node -value -length)]
          (when (:todo-item/needs-focus? (d/entity this-db item-id))
            (.focus node)
            (.setSelectionRange node len len)
            (publish!
              (event
                :todo-item/focused {:id item-id}))))))))

(def item-list
  (component
    (reify
      phi/IPhi
      (render [_ db]
        (let [item-ids (domain/get-item-ids-to-display db)]
          [:ul#todo-list
           (map #(item db {:key % :item-id %}) item-ids)])))))

(def footer
  (component
    (reify
      phi/IPhi
      (render [_ db]
        (let [count-incomplete (domain/get-incomplete-count db)
              count-complete (domain/get-complete-count db)
              display-state (domain/get-item-state-display db)
              sel? #(when (= display-state %) "selected")]
          [:footer#footer
           [:span#todo-count
            [:strong count-incomplete]
            " items left"]
           [:ul#filters
            [:li
             [:a {:href "#/", :class (sel? :all)} "All"]]
            [:li
             [:a {:href "#/active", :class (sel? :incomplete)} "Active"]]
            [:li
             [:a {:href "#/completed", :class (sel? :complete)} "Completed"]]]
           (when-not (zero? count-complete)
             [:button#clear-completed
              {:on-click #(publish!
                           (event :clear-completed {}))}
              (str "Clear Completed (" count-complete ")")])])))))

(def todo-app
  (component
    (reify
      phi/IPhi
      (render [_ db]
        (let [count (domain/get-total-item-count db)]
          [:div
           [:header#header
            [:h1 "todos"]
            (new-item-input db)
            [:section#main
             (toggle-all db)
             (item-list db)]
            (when-not (zero? count)
              (footer db))]])))))

(phi/mount-app todo-app (js/document.getElementById "todoapp"))
