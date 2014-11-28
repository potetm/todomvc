(ns phi-todo.core
  (:require [goog.events :as events]
            [goog.history.EventType :as EventType]
            [phi.core :as phi :refer [conn component publish! event]]
            [phi-todo.domain :as d]
            [phi-todo.event :as e]
            [secretary.core :as secretary :refer-macros [defroute]])
  (:import [goog History]))

(enable-console-print!)

(phi/init-conn! (atom d/initial-state))

(def history (History.))

(doto history
  (events/listen
    EventType/NAVIGATE
    (fn [e] (secretary/dispatch! (.-token e))))
  (.setEnabled true))

(defroute "/" []
  (publish!
    (event ::e/filter {:showing :all})))

(defroute "/:showing" [showing]
  (publish!
    (event ::e/filter {:showing (keyword showing)})))

(def new-item-input
  (component
    (reify
      phi/IPhi
      (render [_ db]
        (let [item-text (d/get-new-item-text db)]
          [:form
           {:on-submit
            (fn [e]
              (.preventDefault e)
              (publish!
                (event ::e/save-new-todo {})))}
           [:input#new-todo
            {:ref "input"
             :placeholder "What needs to be done?"
             :value item-text
             :on-change
             (fn [e]
               (publish!
                 (event
                   ::e/new-item-input-changed {:value (.. e -target -value)})))}]]))
      phi/IDidMount
      (did-mount [_ c]
        (.focus (phi/get-child-node c "input"))))))

(def toggle-all
  (component
    (reify
      phi/IPhi
      (render [_ db]
        (let [all-complete? (d/all-items-complete? db)]
          [:input#toggle-all
           {:type :checkbox
            :checked (not all-complete?)
            :on-change
            (fn [_]
              (publish!
                (event
                  ::e/set-all-states {:complete? (not all-complete?)})))}])))))

(defn item-edit [index item]
  [:input.edit
   {:ref :editItemInput
    :value (:text item)
    :on-change
    (fn [e]
      (publish!
        (event
          :todo-item/set-text {:index index, :text (.. e -target -value)})))
    :on-blur
    (fn [e]
      (publish!
        (event
          :todo-item/stop-editing {:index index})))
    :on-key-down
    (fn [e]
      (when (#{13 27} (.-which e))
        (publish!
          (event
            :todo-item/stop-editing {:index index}))))}])

(defn display-item [index item]
  [:div.view
   [:input.toggle
    {:type :checkbox
     :checked (when (= :complete (:state item)) "checked")
     :on-change #(publish!
                  (event
                    :todo-item/toggle-complete {:index index}))}]
   [:label
    {:on-double-click #(publish!
                        (event
                          :todo-item/start-editing {:index index}))}
    (:text item)]
   [:button.destroy
    {:on-click #(publish!
                 (event
                   :todo-item/delete {:index index}))}]])

(def item
  (component
    (reify
      phi/IPhiProps
      (render-props [_ {:keys [index item]}]
        (let [complete? (= :complete (:state item))
              editing? (:editing? item)]
          [:li
           {:class (cond
                     complete? "completed"
                     editing? "editing")}
           (display-item index item)
           (item-edit index item)]))
      phi/IDidUpdateProps
      (did-update-props [_ comp {:keys [index item]} _prev-props]
        (let [node (phi/get-child-node comp "editItemInput")
              len (.. node -value -length)]
          (when (:needs-focus? item)
            (.focus node)
            (.setSelectionRange node len len)
            (publish!
              (event
                :todo-item/focused {:index index}))))))))

(def item-list
  (component
    (reify
      phi/IPhi
      (render [_ db]
        (let [items (d/get-items-to-display db)]
          [:ul#todo-list
           (map-indexed #(item {:index %1, :key (:id %2), :item %2}) items)])))))

(def footer
  (component
    (reify
      phi/IPhi
      (render [_ db]
        (let [count-incomplete (d/get-incomplete-count db)
              count-complete (d/get-complete-count db)
              display-state (d/get-item-state-display db)
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
                           (event ::e/clear-completed {}))}
              (str "Clear Complted (" count-complete ")")])])))))

(def todo-app
  (component
    (reify
      phi/IPhi
      (render [_ db]
        (let [count (d/get-total-item-count db)]
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
