(ns phi-todo.domain
  (:require [clojure.string :as str])
  (:import [goog.ui IdGenerator]))

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
                 {:id (.getNextUniqueId (.getInstance IdGenerator))
                  :text v
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

(defn delete-item [db index]
  (update-in
    db [:todo-items]
    (fn [items]
      (vec (concat (subvec items 0 index)
                   (subvec items (inc index)))))))

(defn start-item-edit [db index]
  (update-in
    db [:todo-items index]
    assoc :editing? true :needs-focus? true))

(defn set-item-focus [db index focus?]
  (update-in
    db [:todo-items index]
    assoc :needs-focus? focus?))

(defn stop-item-edit [db index]
  (update-in
    db [:todo-items]
    (fn [items]
      (let [item (get items index)]
        (if (str/blank? (:text item))
          (remove (partial = item) items)
          (assoc-in items [index :editing?] false))))))

(defn display-item? [state-display item]
  (or (= :all state-display)
      (= (:state item) state-display)))

(defn get-items-to-display [db]
  (let [state (get-item-state-display db)]
    (keep
      (fn [v]
        (when (display-item? state v)
          v))
      (:todo-items db))))

(defn all-items-complete? [db]
  (every? (comp #{:complete} :state) (:todo-items db)))

(defn set-all-states [db state]
  {:pre [(#{:complete :incomplete} state)]}
  (let [display-state (get-item-state-display db)]
    (update-in
      db [:todo-items]
      (fn [items]
        (mapv
          (fn [item]
            (if (display-item? display-state item)
              (assoc item :state state)
              item))
          items)))))

(defn delete-all [db]
  (let [ids (set (get-items-to-display db))]
    (update-in
      db [:todo-items]
      (fn [items]
        (filterv ids items)))))

(defn clear-completed [db]
  (update-in
    db [:todo-items]
    (fn [items]
      (filterv (comp (partial not= :complete) :state) items))))

(defn get-incomplete-count [db]
  (count
    (filter
      (comp (partial = :incomplete) :state)
      (:todo-items db))))

(defn get-complete-count [db]
  (count
    (filter
      (comp (partial = :complete) :state)
      (:todo-items db))))

(defn get-total-item-count [db]
  (count (:todo-items db)))
