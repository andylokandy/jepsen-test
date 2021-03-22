(ns jepsen.tikv.register
  "Linearizable, single-register operations"
  (:require [clojure.tools.logging :refer :all]
            [slingshot.slingshot :refer [try+]]
            [jepsen
             [generator :as gen]
             [client :as client]]
            [jepsen.tikv
             [client :as c]]))

(defn parse-long
  "Parses a string to a Long. Passes through `nil`."
  [s]
  (when s (Long/parseLong s)))

(defrecord Client [conn]
  client/Client
  (open! [this test node]
    (assoc this :conn (c/open node)))

  (setup! [this test])

  (invoke! [_ test op]
    (try+ (case (:f op)
            :read  (let [value (-> conn
                                   (c/get "foo")
                                   parse-long)]
                     (assoc op :type :ok :value value))
            :write (let [value (:value op)]
                     (do (c/put! conn "foo" value)
                         (assoc op :type :ok))))
          (catch [:status 5] e ; gRPC not found error
            (assoc op :type :fail :error :not-found))))

  (teardown! [this test])

  (close! [_ test]
    (c/close! conn)))

(defn r   [_ _] {:type :invoke, :f :read,  :value nil})
(defn w   [_ _] {:type :invoke, :f :write, :value (rand-int 5)})
(defn cas [_ _] {:type :invoke, :f :cas,   :value [(rand-int 5) (rand-int 5)]})

(defn workload
  "Tests linearizable reads, writs, and compare-and-set operations
  on one key."
  [opts]
  {:client (Client. nil)
   :generator (gen/mix [r w])})
