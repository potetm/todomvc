(ns phi-todo.event
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :as a :refer [<! chan]]
            [phi.core :as phi :refer [conn]]
            [phi-todo.domain :as domain]))

(defn new-item-input-changed [{{:keys [value]} :message}]
  (swap! conn domain/set-new-item-text-facts value))

(defn save-todo [_]
  (swap! conn (comp #(domain/set-new-item-text-facts % "") domain/save-new-todo-from-input-facts)))

(defn todo-item-set-text [{{:keys [index text]} :message}]
  (swap! conn domain/set-item-text-facts index text))

(defn todo-item-start-edit [{{:keys [index]} :message}]
  (swap! conn domain/start-item-edit index))

(defn todo-item-stop-editing [{{:keys [index]} :message}]
  (swap! conn domain/stop-item-edit index))

(defn todo-item-toggle-complete [{{:keys [index]} :message}]
  (swap! conn domain/toggle-item-state-facts index))

(defn todo-item-delete [{{:keys [index]} :message}]
  (swap! conn domain/delete-item index))

(defn set-all-states [{{:keys [complete?]} :message}]
  (swap! conn domain/set-all-states (if complete? :complete :incomplete)))

(defn todo-item-focused [{{:keys [index]} :message}]
  (swap! conn domain/set-item-focus index false))

(defn add-todo [{{:keys [text status]} :message}]
  (swap! conn domain/save-new-todo-facts text status))

(defn delete-all [_]
  (swap! conn domain/delete-all))

(defn filter-todos [{{:keys [showing]} :message}]
  (when (#{:all :active :completed} showing)
    (swap! conn domain/set-item-state-display
           (condp = showing
             :active :incomplete
             :completed :complete
             showing))))

(phi/routing-table
  (a/sliding-buffer 10)
  [[::add-todo] add-todo
   [::delete-all] delete-all
   [::filter] filter-todos
   [::new-item-input-changed] new-item-input-changed
   [::save-new-todo] save-todo
   [::set-all-states] set-all-states
   [:todo-item/delete] todo-item-delete
   [:todo-item/focused] todo-item-focused
   [:todo-item/set-text] todo-item-set-text
   [:todo-item/start-editing] todo-item-start-edit
   [:todo-item/stop-editing] todo-item-stop-editing
   [:todo-item/toggle-complete] todo-item-toggle-complete])
