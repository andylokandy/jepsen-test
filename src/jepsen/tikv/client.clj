(ns jepsen.tikv.client
  "Usage:
    (use 'jepsen.tikv.client)
    (def client (open \"0.0.0.0\"))
    (get client \"1\")
    (put! client \"1\" \"one\")
    (close! client)"
  (:require [clojure.tools.logging :refer :all]
            [clojure.core.async :as async]
            [popen]
            [tikv.raw.Client.client :as rawkv]
            [protojure.grpc.client.providers.http2 :as grpc.http2]
            [protojure.grpc.client.api :as grpc.api]))

(defn open
  "Create a TiKV client."
  ([node]
   (open node {}))
  ([node opts]
   (let [process (popen/popen ["./rpc-server" "--node" node "--type" (:type opts "raw")] :redirect false :dir nil :env {})
         output  (line-seq (popen/stdout process))
         uri     (->> output
                      (take 1)
                      first)]
     (do (info "rpc server uri:" uri)
         (async/go (doseq [line output]
                     (info "rpc server log:" line)))
         {:conn @(grpc.http2/connect {:uri (str "http://" uri)})
          :process process}))))

(defn get
  "Get a value by key."
  [conn key]
  (let [key (str key)]
    (:value @(rawkv/Get (:conn conn) {:key key}))))

(defn put!
  "Put a value by key."
  [conn key value]
  (let [key (str key)
        value (str value)]
    (let [message {:key key :value value}]
      @(rawkv/Put (:conn conn) message))))

(defn close!
  "Close a TiKV client."
  [conn]
  (do (grpc.api/disconnect (:conn conn))
      (popen/kill (:process conn))))
