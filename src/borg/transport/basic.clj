(ns borg.transport.basic
  (:use borg.transport.interface)
  (:import [java.io EOFException ObjectInputStream ObjectOutputStream]
           [java.net  ServerSocket Socket SocketException]))

(def executer (atom nil))
(def ^:dynamic *socket* nil)
(def ^:dynamic *input* nil)
(def ^:dynamic *output* nil)

;; helpers
(defn on-thread [f]
  (doto (Thread. ^Runnable f)
    (.start)))

(defn close-socket [^Socket s]
  (when-not (.isClosed s)
    (doto s
      (.shutdownInput)
      (.shutdownOutput)
      (.close))))

;;****************************************
;; A decent amount of the server code was
;; taken directly from server-socket.
;;****************************************

(defn accept-fn [^Socket s connections fun]
  (let [ins (ObjectInputStream. (.getInputStream s))
        outs (ObjectOutputStream. (.getOutputStream s))]
    (-> #(do (dosync (commute connections conj s))
          (try
            (println "Client connected @ " (.getInetAddress s))
            (fun s ins outs)
            (catch EOFException e (println "Got EOF, client disconnected"))
            (catch SocketException e (println "ACCEPT_FN" (.getMessage e))))
          (close-socket s)
          (dosync (commute connections disj s)))
       (on-thread))))

(defstruct server-def :server-socket :connections)

(defn- create-server-aux [fun port]
  (let [connections (ref #{})
        ss (ServerSocket. port)]
    (on-thread #(when-not (.isClosed ss)
      (try
        (accept-fn (.accept ss) connections fun)
        (catch SocketException e (println "EXCEPTION" (.getMessage e))))
      (recur)))
    (struct-map server-def :server-socket ss :connections connections)))

(defn exec [^clojure.lang.PersistentArrayMap command]
  (@executer command))

(defn init [socket in out]
  (binding [*socket* socket
            *output* out
            *input* in]
    (while (and (.isConnected *socket*) (not (.isClosed *socket*)))
      (->> (.readObject *input*)
           (exec)
           (.writeObject *output*)))))

;; Client helpers

(defn parse-result [^clojure.lang.PersistentArrayMap result]
  result)

(defn get-result [client]
  (->> (:in client)
       (.readObject)
       (parse-result)))

(defrecord Basic []
  ITransport
  (start-server [_ exec-fn port]
    (reset! executer exec-fn)
    (create-server-aux init port))

  (stop-server [_ server]
    (doseq [s @(:connections server)]
      (close-socket s))
    (dosync (ref-set (:connections server) #{}))
    (.close ^ServerSocket (:server-socket server)))

  (connected-clients [_ server]
    (let [clients (map #(.toString (.getInetAddress %)) @(:connections server))]
      (println "CLIENTS: " clients)
      clients))

  (sever-clients [_ server]
    (doseq [s @(:connections server)]
      (close-socket s))
    (dosync (ref-set (:connections server) #{})))

  ;; Client functions
  (create-client [_ host port]
    (let [client (Socket. host port)
          out-os (ObjectOutputStream. (.getOutputStream client))
          in-os (ObjectInputStream. (.getInputStream client))]
      {:client client
       :in in-os
       :out out-os}))

  (close-client [_ client]
    (.close (:client client)))

  (send-command [_ client cmd]
    (-> (:out client)
        (.writeObject cmd))
    (get-result client)))
