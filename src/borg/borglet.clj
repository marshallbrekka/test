;; Must be run as a user who has su access.
(ns borg.borglet
  (:require [borg.handler.core :as handler]
            [borg.handler.handlers]
            [borg.transport.core :as transport]
            [clojure.tools.logging :as lg]))

(def alive (atom true))
(def borglet (atom nil))

(defn start [port user]
  (reset! handler/user user)
  (transport/set-transporter! :basic)
  (let [local-borglet (transport/borglet-start handler/call-handler (Integer. port))]
    (reset! borglet local-borglet)
    (lg/info "Borglet online")
    (while @alive)
    (transport/borglet-stop borglet)))

(defn stop []
  (reset! alive false))

(handler/defhandler shutdown [options]
  (reset! alive false))

(handler/defhandler list-clients [options]
  (transport/borglet-clients @borglet))

(handler/defhandler kill-clients [options]
  (transport/borglet-kill-clients @borglet))

(defn -main [& [port user]]
  (start port user))
