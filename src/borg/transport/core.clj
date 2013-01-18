(ns borg.transport.core
  (:require [borg.transport.interface :as in]
            [borg.transport.basic :as basic]))



(def transporters {:basic (borg.transport.basic.Basic.)})
(def transporter (atom nil))

(defn set-transporter! [k]
  (reset! transporter (k transporters)))

(defn borglet-start [handler-executer port]
  (in/start-server @transporter handler-executer port))

(defn borglet-stop [borglet]
  (in/stop-server @transporter borglet))

(defn client-create [host port]
  (in/create-client @transporter host port))

(defn client-close [client]
  (in/close-client @transporter client))

(defn run-handler [client handler options]
  (in/send-command @transporter
    client
    {:handler handler
     :options options}))
