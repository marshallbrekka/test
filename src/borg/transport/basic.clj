(ns borg.transport.basic
  (:import [java.io ObjectInputStream ObjectOutputStream]
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

****************************************
;; A decent amount of the server code was
;; taken directly from server-socket.
****************************************

(defn accept-fn [^Socket s connections fun]
  (let [ins (ObjectInputStream. (.getInputStream s))
        outs (ObjectOutputStream. (.getOutputStream s))]
    (-> #(do (dosync (commute connections conj s))
          (try
            (fun s ins outs)
            (catch SocketException e (println (.getMessage e))))
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
    (while (.isConnected *socket*)
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


;; Server functions

(defn start-server [exec-fn port]
  (reset! executer exec-fn)
  (create-server-aux init port))

(defn stop-server [server]
  (doseq [s @(:connections server)]
    (close-socket s))
  (dosync (ref-set (:connections server) #{}))
  (.close ^ServerSocket (:server-socket server)))


;; Client functions

(defn create-client [host port]
  (let [client (Socket. host port)
        out-os (ObjectOutputStream. (.getOutputStream client))
        in-os (ObjectInputStream. (.getInputStream client))]
    {:client client
     :in in-os
     :out out-os}))

(defn close-client [client]
  (.close (:client client)))

(defn send-command [client cmd]
  (-> (:out client)
      (.writeObject cmd))
  (get-result client))