(ns borg.handler.handlers
  (:require [borg.handler.core :as h]
            [borg.util.io :as io]))

;; Options is a map with keys
;; *required*
;; :command the bash command to run
;; *optional*
;; All the rest of the keys are the same as clojure.java.shell/sh
;; with the exception that all supplied values from the client side must
;; be serializable (no streams).
(h/defhandler shell [options]
  (let [re (io/sh h/*user*
                  (:command options)
                  (dissoc options :command))]
    (if (not= 0 (:exit re))
      (h/make-error "Command exited with code other than zero." re)
      re)))

;; returns the user this command was executed as
(h/defhandler user [options]
  h/*user*)


;; args
;; :repo-url
;; :commit
(h/defhandler update-borglet [options]
  (let [cur-commit (io/sh h/*user* "git git rev-parse HEAD")]
    (io/git-deploy-revision h/*user* (:repo-url options) (:commit options) "../")
    ))
