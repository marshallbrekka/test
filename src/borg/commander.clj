(ns borg.commander
  (:require [clojure.java.shell :as sh]))

(def ^:dynamic *user* "marshall")
(defrecord Response [status details error])

(defn make-response [success & [details error]]
  (->Response success details error))

(defn make-error [msg & [details]]
  (make-response :fail details msg))

(defn make-success [details]
  (make-response :ok details))

(defn sh [cmd & [opts]]
  (let [re (->> (into [] opts)
                (apply concat [cmd])
                (apply sh/sh "su" *user* "-c"))]
    (if (not= 0 (:exit re))
      (make-error "Command exited with code other than zero."
                  re)
      re)))

(defn shell [options]
  (sh (:command options)
      (dissoc options :command)))

(defn result->map [r]
  {:status (:status r)
   :details (:details r)
   :error (:error r)})

(defn ->result [obj]
  (-> (if (instance? Response obj)
        obj
        (make-success obj))
      (result->map)))

(defn exec [command]
  (-> (try
        (if (= :shell (:command command))
          (shell (:options command))
          (make-error "That is not a valid command"))
        (catch Exception e (make-error (.getMessage e))))
      (->result)))
