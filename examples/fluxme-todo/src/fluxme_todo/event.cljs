(ns fluxme-todo.event
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :as a :refer [<! chan]]
            [datascript :as d]
            [fluxme.core :as fluxme
             :refer [conn]
             :refer-macros [add-subscriber]]
            [fluxme-todo.domain :as domain]))

(add-subscriber new-item-input-change 20 [:new-item-input/input-change]
  (go-loop []
    (when-some [{:keys [subjects]} (<! new-item-input-change)]
      (d/transact conn
                  (domain/set-new-item-text-facts (:value subjects)))
      (recur))))

(add-subscriber save-new-todo 20 [:save-new-todo]
  (go-loop []
    (when-some [{:keys [db]} (<! save-new-todo)]
      (d/transact conn
                  (concat (domain/save-new-todo-facts db)
                          (domain/set-new-item-text-facts "")))
      (recur))))

(add-subscriber todo-item-set-text 20 [:todo-item/set-text]
  (go-loop []
    (when-some [{{:keys [id text]} :subjects} (<! todo-item-set-text)]
      (d/transact conn (domain/set-item-text-facts id text))
      (recur))))

(add-subscriber todo-item-start-edit 20 [:todo-item/start-editing]
  (go-loop []
    (when-some [{{:keys [id]} :subjects
                 db :db} (<! todo-item-start-edit)]
      (d/transact conn (domain/start-item-edit id))
      (recur))))

(add-subscriber todo-item-stop-edit 20 [:todo-item/stop-editing]
  (go-loop []
    (when-some [{{:keys [id]} :subjects
                 db :db} (<! todo-item-stop-edit)]
      (d/transact conn (domain/stop-item-edit db id))
      (recur))))

(add-subscriber todo-item-toggle-complete 20 [:todo-item/toggle-complete]
  (go-loop []
    (when-some [{{:keys [id]} :subjects
                 db :db} (<! todo-item-toggle-complete)]
      (d/transact conn (domain/toggle-item-state-facts db id))
      (recur))))

(add-subscriber todo-item-delete 20 [:todo-item/delete]
  (go-loop []
    (when-some [{{:keys [id]} :subjects} (<! todo-item-delete)]
      (d/transact conn (domain/delete-item id))
      (recur))))
