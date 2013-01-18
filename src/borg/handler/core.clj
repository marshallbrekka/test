(ns borg.handler.core)

(def handlers (atom {}))
(def user (atom nil)) ;; a hack until an actual user if specified from the client
(def ^:dynamic *user* nil)

;;****************************
;; create an error or success response
;;****************************

(defrecord Response [status details error])

(defn make-response [success & [details error]]
  (->Response success details error))

(defn make-error
  [msg & [details]]
  (make-response :fail details msg))

(defn make-success [details]
  (make-response :ok details))

(defn result->map [r]
  {:status (:status r)
   :details (:details r)
   :error (:error r)})

(defn ->result [obj]
  (-> (if (= Response (type obj))
        obj
        (make-success obj))
      (result->map)))

;;*****************************
;; register and call handlers
;;*****************************

(defmacro defhandler
  "Creates a handler function.
   Params must be a vector of one arg, whose value will be a map."
  [handle params & body]
  `(swap! handlers assoc ~(keyword handle)
                         (fn ~params ~@body)))

(defn call-handler
  "Ex: (call-handler :shell {:command \"ls ~/\"})"
  [{:keys [handler options]}]
  (binding [*user* @user]
  (-> (try
        (if (contains? @handlers handler)
          ((handler @handlers) options)
          (make-error (str "There is no handler with the name " handler)))
        (catch Exception e (make-error (.getMessage e))))
      (->result))))
