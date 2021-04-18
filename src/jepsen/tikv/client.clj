(ns jepsen.tikv.client
  "Usage:
    (use 'jepsen.tikv.client)
    (def client (open \"0.0.0.0\"))
    (get client \"1\")
    (put! client \"1\" \"one\")
    (close! client)"
  (:require [clojure.tools.logging :refer :all]
            [popen]
            [tikv.raw.Client.client :as rawkv]
            [protojure.grpc.client.providers.http2 :as grpc.http2]
            [protojure.grpc.client.api :as grpc.api]))

(def global-counter (atom 8000))

(defn next-port
  []
  (swap! global-counter inc))

(defn open
  "Create a TiKV client."
  ([node]
   (open node {}))
  ([node opts]
   (let [port    (str (next-port))
         process (popen/popen ["./rpc-server" "--node" node "--port" port "--type" (:type opts "raw")] :redirect false :dir nil :env {})
         uri     (str "127.0.0.1:" port)]
     (do (info "rpc server uri:" uri)
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
