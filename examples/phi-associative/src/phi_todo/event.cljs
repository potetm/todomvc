(ns phi-todo.event
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :as a :refer [<! chan]]
            [datascript :as d]
            [phi.core :as phi
             :refer [conn]
             :refer-macros [add-subscriber]]
            [phi-todo.domain :as domain]))

(add-subscriber new-item-input-change 20 [:new-item-input/input-change]
  (go-loop []
    (when-some [{:keys [subjects]} (<! new-item-input-change)]
      (swap! conn domain/set-new-item-text-facts (:value subjects))
      (recur))))

(add-subscriber save-new-todo 20 [:save-new-todo]
  (go-loop []
    (when-some [_ (<! save-new-todo)]
      (swap! conn (comp #(domain/set-new-item-text-facts % "") domain/save-new-todo-from-input-facts))
      (recur))))

(add-subscriber todo-item-set-text 20 [:todo-item/set-text]
  (go-loop []
    (when-some [{{:keys [id text]} :subjects} (<! todo-item-set-text)]
      (swap! conn domain/set-item-text-facts id text)
      (recur))))

(add-subscriber todo-item-start-edit 20 [:todo-item/start-editing]
  (go-loop []
    (when-some [{{:keys [id]} :subjects} (<! todo-item-start-edit)]
      (swap! conn domain/start-item-edit id)
      (recur))))

(add-subscriber todo-item-stop-edit 20 [:todo-item/stop-editing]
  (go-loop []
    (when-some [{{:keys [id]} :subjects} (<! todo-item-stop-edit)]
      (swap! conn domain/stop-item-edit id)
      (recur))))

(add-subscriber todo-item-toggle-complete 20 [:todo-item/toggle-complete]
  (go-loop []
    (when-some [{{:keys [id]} :subjects} (<! todo-item-toggle-complete)]
      (swap! conn domain/toggle-item-state-facts id)
      (recur))))

(add-subscriber todo-item-delete 20 [:todo-item/delete]
  (go-loop []
    (when-some [{{:keys [id]} :subjects} (<! todo-item-delete)]
      (swap! conn domain/delete-item id)
      (recur))))

(add-subscriber set-all-states 20 [:set-all-states]
  (go-loop []
    (when-some [{{:keys [complete?]} :subjects} (<! set-all-states)]
      (swap! conn domain/set-all-states (if complete? :complete :incomplete))
      (recur))))

(add-subscriber add-todo 100 [:add-todo]
  (go-loop []
    (when-some [{{:keys [text status]} :subjects} (<! add-todo)]
      (swap! conn domain/save-new-todo-facts text status)
      (recur))))

(add-subscriber delete-all 100 [:delete-all]
  (go-loop []
    (when-some [{:keys [db]} (<! delete-all)]
      (swap! conn domain/delete-all)
      (recur))))

(add-subscriber filter 100 [:filter]
  (go-loop []
    (when-some [{{:keys [showing]} :subjects} (<! filter)]
      (when (#{:all :active :completed} showing)
        (swap! conn domain/set-item-state-display
               (condp = showing
                 :active :incomplete
                 :completed :complete
                 showing)))
      (recur))))
