(ns borg.client
  (:require [borg.transport.core :as transport]))

(def ^:dynamic *client* nil)

(defn set-transporter! [trans-key]
  (transport/set-transporter! trans-key))

(defn connect [host port]
  (transport/client-create host port))

(defn disconnect
  ([]
   (disconnect *client*))
  ([client]
    (transport/client-close client)))

(defn run-handler
  "Run a handler on a borglet.
   Ex: (run-handler :shell {:command \"ls ./\" :dir \"/home/\"})"
  ([handler options]
   (run-handler *client* handler options))
  ([client handler options]
    (transport/run-handler client handler options)))

(defmacro with-client [client & body]
  `(binding [*client* ~client]
     ~@body))
