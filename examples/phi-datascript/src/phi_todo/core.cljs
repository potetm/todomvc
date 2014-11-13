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

(phi/init-datascript-conn! (d/create-conn domain/schema))
(d/transact conn domain/initial-state)

(def history (History.))

(doto history
  (events/listen
    EventType/NAVIGATE
    (fn [e] (secretary/dispatch! (.-token e))))
  (.setEnabled true))

(defroute "/" []
  (publish!
    (event
      :filter @conn {:showing :all})))

(defroute "/:showing" [showing]
  (publish!
    (event
      :filter @conn {:showing (keyword showing)})))

(def new-item-input
  (component
    (reify
      phi/IUpdateInAnimationFrame
      phi/IUpdateForFactParts
      (fact-parts [_]
        [[1 :new-item/text]])
      phi/IPhi
      (query [_ db]
        (domain/get-new-item-text db))
      (render [_ item-text]
        [:form
         {:on-submit
          (fn [e]
            (.preventDefault e)
            (publish!
              (event :save-new-todo @conn {})))}
         [:input#new-todo
          {:ref "input"
           :placeholder "What needs to be done?"
           :value item-text
           :on-change
           (fn [e]
             (publish!
               (event
                 :new-item-input/input-change @conn
                 {:value (.. e -target -value)})))}]])
      phi/IDidMount
      (did-mount [_ c]
        (.focus (phi/get-dom-node (phi/get-ref c "input")))))))

(def toggle-all
  (component
    (reify
      phi/IUpdateInAnimationFrame
      phi/IUpdateForFactParts
      (fact-parts [_]
        [[nil :todo-item/state]])
      phi/IPhi
      (query [_ db]
        (domain/all-items-complete? db))
      (render [_ all-complete?]
        [:input#toggle-all
         {:type :checkbox
          :checked all-complete?
          :on-change
          (fn [_]
            (publish!
              (event
                :set-all-states @conn {:complete? (not all-complete?)})))}]))))

(defn item-edit [id item]
  [:input.edit
   {:ref :editItemInput
    :value (:todo-item/text item)
    :on-change
    (fn [e]
      (publish!
        (event
          :todo-item/set-text @conn {:id id, :text (.. e -target -value)})))
    :on-blur
    (fn [e]
      (publish!
        (event
          :todo-item/stop-editing @conn {:id id})))
    :on-key-down
    (fn [e]
      (when (#{13 27} (.-which e))
        (publish!
          (event
            :todo-item/stop-editing @conn {:id id}))))}])

(defn display-item [id item]
  [:div.view
   [:input.toggle
    {:type :checkbox
     :checked (when (= :complete (:todo-item/state item)) "checked")
     :on-change #(publish!
                  (event
                    :todo-item/toggle-complete @conn {:id id}))}]
   [:label
    {:on-double-click #(publish!
                        (event
                          :todo-item/start-editing @conn {:id id}))}
    (:todo-item/text item)]
   [:button.destroy
    {:on-click #(publish!
                 (event
                   :todo-item/delete @conn {:id id}))}]])

(def item
  (component
    (reify
      phi/IUpdateInAnimationFrame
      phi/IUpdateForFactParts
      (fact-parts [_ {:keys [item-id]}]
        [[item-id]])
      phi/IPhi
      (query [_ {:keys [item-id]} db]
        (into {} (d/entity db item-id)))
      (render [_ {:keys [item-id]} item]
        (let [complete? (= :complete (:todo-item/state item))
              editing? (:todo-item/editing? item)]
          [:li
           {:class (cond
                     complete? "completed"
                     editing? "editing")}
           (display-item item-id item)
           (item-edit item-id item)]))
      phi/IDidUpdate
      (did-update [_ comp props state]
        (let [node (phi/get-dom-node (phi/get-ref comp "editItemInput"))
              len (.. node -value -length)]
          (when (:todo-item/needs-focus? state)
            (.focus node)
            (.setSelectionRange node len len)
            (publish!
              (event
                :todo-item/focused @conn {:id (:item-id props)}))))))))

(def item-list
  (component
    (reify
      phi/IUpdateInAnimationFrame
      phi/IUpdateForFactParts
      (fact-parts [_]
        [[2 :item-state-display]
         [nil :todo-item/state]])
      phi/IPhi
      (query [_ db]
        (domain/get-item-ids-to-display db))
      (render [_ item-ids]
        [:ul#todo-list
         (map #(item {:key % :item-id %}) item-ids)]))))

(def footer
  (component
    (reify
      phi/IUpdateInAnimationFrame
      phi/IUpdateForFactParts
      (fact-parts [_]
        [[nil :todo-item/state :incomplete]
         [2 :item-state-display]])
      phi/IPhi
      (query [_ db]
        [(domain/get-incomplete-count db)
         (domain/get-item-state-display db)])
      (render [_ [count display-state]]
        (let [sel? #(when (= display-state %) "selected")]
          [:footer#footer
           [:span#todo-count
            [:strong count]
            " items left"]
           [:ul#filters
            [:li
             [:a {:href "#/", :class (sel? :all)} "All"]]
            [:li
             [:a {:href "#/active", :class (sel? :incomplete)} "Active"]]
            [:li
             [:a {:href "#/completed", :class (sel? :complete)} "Completed"]]]])))))

(def todo-app
  (component
    (reify
      phi/IUpdateInAnimationFrame
      phi/IUpdateForFactParts
      (fact-parts [_]
        [[nil :todo-item/text]])
      phi/IPhi
      (query [_ db]
        (domain/get-total-item-count db))
      (render [_ count]
        [:div
         [:header#header
          [:h1 "todos"]
          (new-item-input)
          [:section#main
           (toggle-all)
           (item-list)]
          (when-not (zero? count)
            (footer))]]))))

(phi/mount-app (todo-app) (js/document.getElementById "todoapp"))

(aset js/window "benchmark1"
  (fn [_]
    (dotimes [_ 200]
      (publish!
        (event
          :add-todo nil {:text "foo", :status :incomplete})))))

(aset js/window "benchmark2"
  (fn [_]
    (dotimes [_ 200]
      (publish!
        (event
          :add-todo nil {:text "foo", :status :incomplete})))
    (dotimes [i 5]
      (publish!
        (event
          :set-all-states @conn {:complete? (= 0 (mod i 2))})))
    (publish!
      (event
        :delete-all @conn {}))))
