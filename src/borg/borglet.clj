;; Must be run as a user who has su access.
(ns borg.borglet
  (:require [borg.handler.core :as handler]
            [borg.handler.handlers]
            [borg.transport.core :as transport]
            [borg.util.io :as util-io]
            [clojure.java.io :as io]
            [clojure.tools.logging :as lg]))

(def alive (atom true))
(def borglet (atom nil))

(defn remove-update-files []
  (lg/info "removing files")
  (for [fname ["updating"
               "old.rev"
               "new.rev"]
        :let [file (io/as-file (str "../../" fname))]]
    (do (println file)
    (when (.exists file)
      (io/delete-file file)))))

(defn start [port user]
  (reset! handler/user user)
  (transport/set-transporter! :basic)
  (let [local-borglet (transport/borglet-start handler/call-handler (Integer. port))]
    (reset! borglet local-borglet)
    (lg/info "Borglet online")
    (doall (remove-update-files))
    (while @alive)
    (lg/info "Borglet shutting down")
    (transport/borglet-stop @borglet)
    ;; hack because some transport threads fail to end
    ;; will remove once transport is more robust
    (System/exit 0)))

(defn stop []
  (reset! alive false))

(handler/defhandler shutdown [options]
  (stop))

(handler/defhandler list-clients [options]
  (transport/borglet-clients @borglet))

(handler/defhandler kill-clients [options]
  (transport/borglet-kill-clients @borglet))

(handler/defhandler current-revision [options]
  (util-io/git-revision handler/*user*))

(handler/defhandler update-borglet [{:keys [repo-url commit]}]
  (let [cur-commit (util-io/git-revision "root")]
    (util-io/git-deploy-revision "root" repo-url commit "../")
    (spit "../../old.rev" cur-commit)
    (spit "../../new.rev" commit))
  (stop))

(defn -main [& [port user]]
  (start port user))
