(ns borg.util.io
  (:require [clojure.java.shell :as sh])
  (:import [java.io File]))

(defn sh [user cmd & [opts]]
  (->> (into [] opts)
       (apply concat [cmd])
       (apply sh/sh "su" user "-c")))

(defn git-clone [user repo-url destination-dir]
  (let [re (sh user (str "git clone " repo-url " " destination-dir))]
    (when (not= 0 (:exit re))
      (:err re))))

(defn git-checkout [user repo-dir commit]
  (let [re (sh user (str "git checkout " commit) {:dir repo-dir})]
    (when (not= 0 (:exit re))
      (:err re))))

(defn git-revision [user]
  (-> (sh user "git rev-parse HEAD")
      (:out)))

(defn git-deploy-revision [user repo-url commit revisions-dir]
  (let [commit-dir (str revisions-dir "/" commit)]
    (when (->> (File. commit-dir)
               (.exists)
               (not))
      (git-clone user repo-url commit-dir)
      (git-checkout user commit-dir commit))))


