(ns jepsen.tikv
  (:require [jepsen
             [cli :as cli]
             [tests :as tests]
             [generator :as gen]]
            [jepsen.os.centos :as centos]
            [jepsen.tikv
             [db :as db]
             [register :as register]]))

(def workloads
  "A map of workload names to functions that construct workloads, given opts."
  {:register       register/workload})

(def all-workloads
  "A collection of workloads we run by default."
  (remove #{:none} (keys workloads)))

(defn tikv-test
  "Given an options map from the command line runner (e.g.: :nodes, :ssh,
  :concurrency, ...), construct a test map.
  
      :workload    Name of the workload to run"
  [opts]
  (let [workload-name (:workload opts)
        workload ((workloads workload-name) opts)]
    (merge tests/noop-test
           {:name (str "tikv " (name workload-name))
            :os centos/os
            :db (db/tikv)
            :pure-generators true
            :client (:client workload)
            :generator (->> (:generator workload)
                            (gen/stagger 1)
                            (gen/nemesis nil)
                            (gen/time-limit 15))}
           opts)))

(def cli-opts
  "Additional command line options."
  [["-v" "--version VERSION" "The version of TiKV"]
   ["-w" "--workload NAME" "The workload to run"
    :parse-fn keyword
    :validate [workloads (cli/one-of workloads)]]])

(defn -main
  "Handles command line arguments. Can either run a test, or a web server for
  browsing results."
  [& args]
  (cli/run! (merge (cli/single-test-cmd {:test-fn  tikv-test
                                         :opt-spec cli-opts})
                   (cli/serve-cmd))
            args))
