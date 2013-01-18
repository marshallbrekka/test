(ns app.handler.handlers
  (:require [borg.handler.core :as h]
            [clojure.java.shell :as sh]))

(defn sh [cmd & [opts]]
  (let [re (->> (into [] opts)
                (apply concat [cmd])
                (apply sh/sh "su" h/*user* "-c"))]
    (if (not= 0 (:exit re))
      (h/make-error "Command exited with code other than zero."
                  re)
      re)))

;; Options is a map with keys
;; *required*
;; :command the bash command to run
;; *optional*
;; All the rest of the keys are the same as clojure.java.shell/sh
;; with the exception that all supplied values from the client side must
;; be serializable (no streams).
(h/defhandler shell [options]
  (sh (:command options)
      (dissoc options :command)))

;; returns the user this command was executed as
(h/defhandler user [options]
  h/*user*)
