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
            [jepsen.tikv.util :as tu]
            [protojure.grpc.client.providers.http2 :as grpc.http2]
            [protojure.grpc.client.api :as grpc.api]))

(defn open
  "Create a TiKV client."
  ([node]
   (open node {}))
  ([node opts]
   (let [uri     (str "127.0.0.1:" (+ 8000 (tu/num-suffix node)))]
     (do (info "rpc server uri:" uri)
         {:conn @(grpc.http2/connect {:uri (str "http://" uri)})}))))

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
  (grpc.api/disconnect (:conn conn)))
