(ns phi-todo.event
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :as a :refer [<! chan]]
            [datascript :as d]
            [phi.core :as phi :refer [conn] :refer-macros [add-subscriber]]
            [phi-todo.domain :as domain]))

(defn new-item-input-change [{{:keys [value]} :message}]
  (d/transact conn
              (domain/set-new-item-text-facts value)))

(defn save-new-todo []
  (d/transact conn
              (concat (domain/save-new-todo-from-input-facts)
                      (domain/set-new-item-text-facts ""))))

(defn todo-item-set-text [{{:keys [id text]} :message}]
  (d/transact conn (domain/set-item-text-facts id text)))

(defn todo-item-start-edit [{{:keys [id]} :message}]
  (d/transact conn (domain/start-item-edit id)))

(defn todo-item-stop-edit [{{:keys [id]} :message}]
  (d/transact conn [[:db.fn/call domain/stop-item-edit id]]))

(defn todo-item-toggle-complete [{{:keys [id]} :message}]
  (d/transact conn [[:db.fn/call domain/toggle-item-state-facts id]]))

(defn todo-item-delete [{{:keys [id]} :message}]
  (d/transact conn (domain/delete-item id)))

(defn set-all-states [{{:keys [complete?]} :message}]
  (d/transact conn [[:db.fn/call domain/set-all-states (if complete? :complete :incomplete)]]))

(defn add-todo [{{:keys [text status]} :message}]
  (d/transact conn (domain/save-new-todo-facts text status)))

(defn filter-items [{{:keys [showing]} :message}]
  {:pre [(#{:all :active :completed} showing)]}
  (d/transact conn (domain/set-item-state-display (condp = showing
                                                    :active :incomplete
                                                    :completed :complete
                                                    showing))))

(phi/routing-table
  (a/sliding-buffer 10)
  [[:filter] filter-items
   [:delete-all] #(d/transact conn [[:db.fn/call domain/delete-all]])
   [:add-todo] add-todo
   [:set-all-states] set-all-states
   [:todo-item/delete] todo-item-delete
   [:todo-item/toggle-complete] todo-item-toggle-complete
   [:todo-item/stop-editing] todo-item-stop-edit
   [:todo-item/start-editing] todo-item-start-edit
   [:todo-item/set-text] todo-item-set-text
   [:save-new-todo] save-new-todo
   [:new-item-input/input-change] new-item-input-change])
