(ns fluxme-todo.core
  (:require [datascript :as d]
            [fluxme.core :as fluxme
             :refer [conn component publish! event]]
            [fluxme-todo.domain :as domain]))

(enable-console-print!)

(fluxme/init-conn! (d/create-conn domain/schema))
(d/transact conn domain/initial-state)

(def new-item-input
  (component
    (reify
      fluxme/IFlux
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
          {:value item-text
           :on-change
           (fn [e]
             (publish!
               (event
                 :new-item-input/input-change @conn
                 {:value (.. e -target -value)})))}]]))))

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
      fluxme/IFlux
      (query [_ {:keys [item-id]} db]
        (into {} (d/entity db item-id)))
      (render [_ {:keys [item-id]} item]
        (let [complete? (= :complete (:todo-item/state item))
              editing? (:todo-item/editing? item)]
          [:li
           {:class (cond
                     complete? "completed"
                     editing? "editing")}
           (item-edit item-id item)
           (display-item item-id item)]))
      fluxme/IDidUpdate
      (did-update [_ comp props state]
        (let [node (fluxme/get-dom-node (fluxme/get-ref comp "editItemInput"))
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
      fluxme/IFlux
      (query [_ db]
        (domain/get-item-ids-to-display db))
      (render [_ item-ids]
        [:ul#todo-list
         (map #(item {:item-id %}) item-ids)]))))

(def todo-app
  (component
    (reify
      fluxme/IFlux
      (query [_ _db] nil)
      (render [_ _v]
        [:div
         [:header#header]
         (new-item-input)
         [:section
          #_(toggle-all)
          (item-list)]
         #_(footer)]))))

(fluxme/mount-app (todo-app) (js/document.getElementById "todoapp"))
