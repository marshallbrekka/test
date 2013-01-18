;; Must be run as a user who has su access.
(ns borg.borglet
  (:require [borg.handler.core :as handler]
            [borg.handler.handlers]
            [borg.transport.core :as transport]
            [clojure.tools.logging :as lg]))

(def alive (atom true))

(defn start [port user]
  (reset! handler/user user)
  (transport/set-transporter! :basic)
  (let [borglet (transport/borglet-start handler/call-handler (Integer. port))]
    (lg/info "Borglet online")
    (while @alive)
    (transport/borglet-stop borglet)))

(defn stop []
  (reset! alive false))

(handler/defhandler shutdown [options]
  (reset! alive false))

(defn -main [& [port user]]
  (start port user))
