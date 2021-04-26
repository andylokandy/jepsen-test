(ns jepsen.tikv.client.txn
  (:require [tikv.txn.Client.client :as txnkv]
            [protojure.grpc.client.providers.http2 :as grpc.http2]
            [protojure.grpc.client.api :as grpc.api]))

(def ^:dynamic *txn-id* 0)

(defmacro with-txn
  [conn & body]
  `(binding [*txn-id* (begin-txn ~conn)]
     (try (let [ret# (do ~@body)]
            (do (commit! ~conn)
                ret#))
          (catch Exception e#
            (do (rollback! ~conn)
                (throw e#))))))

(defn begin-txn
  [conn]
  (:txn-id @(txnkv/BeginTxn (:conn conn) {:type 0}))) ; TODO(ziyi) hard-coded 0, which means using begin_optimistic

(defn commit!
  [conn]
  @(txnkv/Commit (:conn conn) {:txn-id *txn-id*}))

(defn rollback!
  [conn]
  @(txnkv/Rollback (:conn conn) {:txn-id *txn-id*}))

(defn get
  [conn key]
  (let [key (str key)]
    (:value @(txnkv/Get (:conn conn) {:txn-id *txn-id* :key key}))))

(defn put!
  [conn key value]
  (let [key   (str key)
        value (str value)]
    @(txnkv/Put (:conn conn) {:txn-id *txn-id* :key key :value value})))
