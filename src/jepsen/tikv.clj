(ns jepsen.tikv
  (:require [clojure.string :as str]
            [jepsen
             [cli :as cli]
             [tests :as tests]
             [checker :as checker]
             [generator :as gen]]
            [knossos.model :as model]
            [jepsen.os.centos :as centos]
            [jepsen.tikv
             [db :as db]
             [nemesis :as nemesis]
             [register :as register]
             [set :as set]
             [list-append :as list-append]
             [util :as tu]]))

(def workloads
  "A map of workload names to functions that construct workloads, given opts."
  {:register       register/workload
   :set            set/workload
   :list-append    list-append/workload})

(def all-workloads
  "A collection of workloads we run by default."
  (remove #{:none} (keys workloads)))

(defn tikv-test
  "Given an options map from the command line runner (e.g.: :nodes, :ssh,
  :concurrency, ...), construct a test map.
  
      :workload    Name of the workload to run"
  [opts]
  (let [workload-name (:workload opts)
        workload ((workloads workload-name) opts)
        nemesis  (nemesis/nemesis opts)
        gen      (->> (:generator workload)
                      (gen/nemesis (:generator nemesis))
                      (gen/time-limit (:time-limit opts)))
        gen      (if (:final-generator workload)
                    (gen/phases gen
                                (gen/log "Healing cluster")
                                (gen/nemesis (:final-generator nemesis))
                                (gen/log "Waiting for recovery")
                                (gen/sleep 10)
                                (gen/clients (:final-generator workload))))]
    (merge tests/noop-test
           opts
           {:name (str "tikv " (name workload-name))
            :os centos/os
            :db (db/tikv)
            :pure-generators true
            :client (:client workload)
            :nemesis (:nemesis nemesis)
            :checker (checker/compose
                      {:perf     (checker/perf)
                       :workload (:checker workload)})
            :generator gen})))

(defn parse-nemesis-spec
  "Parses a comma-separated string of nemesis types, and turns it into an
  option map like {:kill-alpha? true ...}"
  [s]
  (if (= s "none")
    {}
    (->> (str/split s #",")
         (map (fn [o] [(keyword o) true]))
         (into {}))))

(def nemesis-specs
  "These are the types of failures that the nemesis can perform."
  #{:partition
    :partition-one
    :partition-pd-leader
    :partition-half
    :partition-ring
    :kill
    :pause
    :kill-pd
    :kill-kv
    :pause-pd
    :pause-kv
    :schedules
    :shuffle-leader
    :shuffle-region
    :random-merge
    :restart-kv-without-pd})

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
    :validate [pos? "Must be a positive integer."]]

   [nil "--nemesis-interval SECONDS"
    "Roughly how long to wait between nemesis operations. Default: 10s."
    :parse-fn tu/parse-long
    :assoc-fn (fn [m k v] (update m :nemesis assoc :interval v))
    :validate [(complement neg?) "should be a non-negative number"]]

   [nil "--nemesis SPEC" "A comma-separated list of nemesis types"
    :default {:interval 10}
    :parse-fn parse-nemesis-spec
    :assoc-fn (fn [m k v] (update m :nemesis merge v))
    :validate [(fn [parsed]
                 (and (map? parsed)
                      (every? nemesis-specs (keys parsed))))
               (str "Should be a comma-separated list of failure types. A failure "
                    (.toLowerCase (cli/one-of nemesis-specs))
                    ". Or, you can use 'none' to indicate no failures.")]]

   [nil "--nemesis-long-recovery" "Every so often, have a long period of no faults, to see whether the cluster recovers."
    :default false
    :assoc-fn (fn [m k v] (update m :nemesis assoc :long-recovery v))]

   [nil "--nemesis-schedule SCHEDULE" "Whether to have randomized delays between nemesis actions, or fixed ones."
    :parse-fn keyword
    :assoc-fn (fn [m k v] (update m :nemesis assoc :schedule v))
    :validate [#{:fixed :random} "Must be either 'fixed' or 'random'"]]])

(defn -main
  "Handles command line arguments. Can either run a test, or a web server for
  browsing results."
  [& args]
  (cli/run! (merge (cli/single-test-cmd {:test-fn  tikv-test
                                         :opt-spec cli-opts})
                   (cli/serve-cmd))
            args))
