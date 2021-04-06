(ns jepsen.tikv
  (:require [jepsen
             [cli :as cli]
             [tests :as tests]
             [checker :as checker]
             [nemesis :as nemesis]
             [generator :as gen]]
            [knossos.model :as model]
            [jepsen.os.centos :as centos]
            [jepsen.tikv
             [db :as db]
             [register :as register]
             [set :as set]
             [util :as tu]]))

(def workloads
  "A map of workload names to functions that construct workloads, given opts."
  {:register       register/workload
   :set            set/workload})

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
            :nemesis (nemesis/partition-random-halves)
            :checker (checker/compose
                      {:perf     (checker/perf)
                       :workload (:checker workload)})
            :generator (->> (:generator workload)
                            (gen/nemesis
                             (cycle [(gen/sleep 5)
                                     {:type :info, :f :start}
                                     (gen/sleep 5)
                                     {:type :info, :f :stop}]))
                            (gen/time-limit (:time-limit opts)))}
           opts)))

(def cli-opts
  "Additional command line options."
  [["-v" "--version VERSION" "The version of TiKV"]
   ["-w" "--workload NAME" "The workload to run"
    :parse-fn keyword
    :validate [workloads (cli/one-of workloads)]]
   ["-r" "--rate HZ" "Approximate number of requests per second, per thread."
    :default  10
    :parse-fn read-string
    :validate [#(and (number? %) (pos? %)) "Must be a positive number"]]
   [nil "--ops-per-key NUM" "Maximum number of operations on any given key."
    :default  100
    :parse-fn tu/parse-long
    :validate [pos? "Must be a positive integer."]]])

(defn -main
  "Handles command line arguments. Can either run a test, or a web server for
  browsing results."
  [& args]
  (cli/run! (merge (cli/single-test-cmd {:test-fn  tikv-test
                                         :opt-spec cli-opts})
                   (cli/serve-cmd))
            args))
