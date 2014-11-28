(ns phi-todo.domain
  (:require [clojure.string :as str]
            [datascript :as d]))

(def schema {})

(def initial-state
  [[:db/add 1 :new-item/text ""]
   [:db/add 2 :item-state-display :all]])

(defn set-item-state-display [state]
  {:pre [(#{:all :incomplete :complete} state)]}
  [[:db/add 2 :item-state-display state]])

(defn get-item-state-display [db]
  (:item-state-display (d/entity db 2)))

(defn get-new-item-text [db]
  (:new-item/text (d/entity db 1)))

(defn set-new-item-text-facts [v]
  [[:db/add 1 :new-item/text v]])

(defn save-new-todo-facts [v state]
  {:pre [(#{:complete :incomplete} state)]}
  (let [v (str/trim v)]
    (when-not (str/blank? v)
      [{:db/id -1
        :todo-item/sort-constant 1
        :todo-item/text v
        :todo-item/state state
        :todo-item/editing? false
        :todo-item/needs-focus? false}])))

(defn save-new-todo-facts* [db state]
  (save-new-todo-facts (get-new-item-text db) state))

(defn save-new-todo-from-input-facts []
  [[:db.fn/call save-new-todo-facts* :incomplete]])

(defn set-item-text-facts [id text]
  [[:db/add id :todo-item/text text]])

(defn toggle-item-state-facts [db id]
  [[:db/add id :todo-item/state (condp = (:todo-item/state (d/entity db id))
                                  :incomplete :complete
                                  :complete :incomplete)]])

(defn delete-item [id]
  [[:db.fn/retractEntity id]])

(defn start-item-edit [id]
  [[:db/add id :todo-item/editing? true]
   [:db/add id :todo-item/needs-focus? true]])

(defn stop-item-edit [db id]
  (concat
    [[:db/add id :todo-item/editing? false]]
    (when (str/blank? (:todo-item/text (d/entity db id)))
      (delete-item id))))

(defn get-item-ids-to-display [db]
  (let [state (get-item-state-display db)]
    (map
      first
      (sort-by
        second
        (if (= :all state)
          (d/q
            '[:find ?i ?tx
              :in $ ?s
              :where
              [?i :todo-item/text ?v]
              [?i :todo-item/sort-constant _ ?tx]]
            db)
          (d/q
            '[:find ?i ?tx
              :in $ ?s
              :where
              [?i :todo-item/text ?v]
              [?i :todo-item/sort-constant _ ?tx]
              [?i :todo-item/state ?s]]
            db
            state))))))

(defn all-items-complete? [db]
  (= #{[:complete]}
     (d/q
       '[:find ?s
         :where
         [_ :todo-item/state ?s]]
       db)))

(defn set-all-states [db state]
  {:pre [(#{:complete :incomplete} state)]}
  (map
    #(vector :db/add % :todo-item/state state)
    (get-item-ids-to-display db)))

(defn delete-all [db]
  (map
    (partial vector :db.fn/retractEntity)
    (get-item-ids-to-display db)))

(defn get-incomplete-count [db]
  (ffirst
    (d/q
      '[:find (count ?i)
        :where
        [?i :todo-item/state :incomplete]]
      db)))

(defn get-total-item-count [db]
  (or
    (ffirst
      (d/q
        '[:find (count ?i)
          :where
          [?i :todo-item/text]]
        db))
    0))
