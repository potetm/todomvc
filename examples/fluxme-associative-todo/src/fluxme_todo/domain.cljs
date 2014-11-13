(ns fluxme-todo.domain
  (:require [clojure.string :as str]
            [datascript :as d]))

(def initial-state
  {:new-item/text ""
   :item-state-display :all
   :todo-items []})

(defn set-item-state-display [db state]
  {:pre [(#{:all :incomplete :complete} state)]}
  (assoc db :item-state-display state))

(defn get-item-state-display [db]
  (:item-state-display db))

(defn get-new-item-text [db]
  (:new-item/text db))

(defn set-new-item-text-facts [db v]
  (assoc db :new-item/text v))

(defn save-new-todo-facts [db v state]
  {:pre [(#{:complete :incomplete} state)]}
  (let [v (str/trim v)]
    (when-not (str/blank? v)
      (update-in db [:todo-items]
                 conj
                 {:text v
                  :state state
                  :editing? false
                  :needs-focus? false}))))

(defn save-new-todo-from-input-facts [db]
  (save-new-todo-facts db (get-new-item-text db) :incomplete))

(defn set-item-text-facts [db id text]
  (assoc-in db [:todo-items id :text] text))

(defn toggle-item-state-facts [db id]
  (update-in db [:todo-items id :state] {:incomplete :complete
                                         :complete :incomplete}))

(defn get-item-by-id [db id]
  (get-in db [:todo-items id]))

(defn delete-item [db id]
  (update-in
    db [:todo-items]
    (fn [items]
      (vec (concat (subvec items 0 id)
                   (subvec items (inc id)))))))

(defn start-item-edit [db id]
  (update-in
    db [:todo-items id]
    assoc :editing? true :needs-focus? true))

(defn stop-item-edit [db id]
  (update-in
    db [:todo-items]
    (fn [items]
      (let [item (get items id)]
        (if (str/blank? (:text item))
          (dissoc items id)
          (update-in items [id :editing] false))))))

(defn get-item-ids-to-display [db]
  (let [state (get-item-state-display db)]
    (remove
      nil?
      (map-indexed
        (fn [id v]
          (when (or (= :all state)
                    (= (:state v) state))
            id))
        (:todo-items db)))))

(defn all-items-complete? [db]
  (every? (comp #{:complete} :state) (:todo-items db)))

(defn set-all-states [db state]
  {:pre [(#{:complete :incomplete} state)]}
  (let [ids (set (get-item-ids-to-display db))]
    (update-in
      db [:todo-items]
      (fn [items]
        (vec
          (map-indexed
            (fn [id item]
              (if-not (ids id)
                item
                (assoc item :state state)))
            items))))))

(defn delete-all [db]
  (let [ids (set (get-item-ids-to-display db))]
    (update-in
      db [:todo-items]
      (fn [items]
        (filterv ids items)))))

(defn get-incomplete-count [db]
  (count
    (filter
      (comp (partial = :incomplete) :state)
      (:todo-items db))))

(defn get-total-item-count [db]
  (count (:todo-item db)))
