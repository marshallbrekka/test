(ns borg.transport.core
  (:require [borg.commander :as c]
            [borg.transport.basic :as basic]))

(def server (atom nil))
(def client (atom nil))

(defn server-start [port]
  (->> (basic/start-server c/exec port)
       (reset! server)))

(defn server-stop []
  (basic/stop-server @server)
  (reset! server nil))

(defn client-create [host port]
  (->> (basic/create-client host port)
       (reset! client)))

(defn client-close []
  (basic/close-client @client)
  (reset! client nil))

(defn run-command [name options]
  (basic/send-command
    @client
    {:command name
     :options options}))
