(ns jepsen.tikv
  (:require [jepsen
             [cli :as cli]
             [tests :as tests]]
            [jepsen.os.centos :as centos]
            [jepsen.tikv
             [db :as db]]))

(defn tikv-test
  "Given an options map from the command line runner (e.g.: :nodes, :ssh,
  :concurrency, ...), construct a test map."
  [opts]
  (merge tests/noop-test
         {:name "tikv"
          :os centos/os
          :db (db/tikv)
          :pure-generators true}
         opts))

(def cli-opts
  "Additional command line options."
  [["-v" "--version VERSION" "The version of TiKV"]])

(defn -main
  "Handles command line arguments. Can either run a test, or a web server for
  browsing results."
  [& args]
  (cli/run! (merge (cli/single-test-cmd {:test-fn  tikv-test
                                         :opt-spec cli-opts})
                   (cli/serve-cmd))
            args))
