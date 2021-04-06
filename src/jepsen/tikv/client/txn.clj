(ns jepsen.tikv.client.txn
  (:require [tikv.txn.Client.client :as txnkv]
            [protojure.grpc.client.providers.http2 :as grpc.http2]
            [protojure.grpc.client.api :as grpc.api]))

(def ^:dynamic *txn-id* 0)

(defmacro with-txn
  [conn & body]
  `(binding [*txn-id* (begin-txn ~conn)]
     (try ~@body
          (commit! ~conn)
          (catch Exception e#
            (rollback! ~conn)))))

(defn begin-txn
  [conn]
  (:txn-id @(txnkv/BeginTxn (:conn conn) {}))) ; TODO(ziyi) hard-coded 0, which means using begin_optimistic

(defn commit!
  [conn]
  @(txnkv/Commit (:conn conn) {:txn-id *txn-id*}))

(defn rollback!
  [conn]
  @(txnkv/Rollback (:conn conn) {:txn-id *txn-id*}))

(defn get
  [conn key]
  (:value @(txnkv/Get (:conn conn) {:txn-id *txn-id* :key key})))

(defn put!
  [conn key value]
  @(txnkv/Put (:conn conn) {:txn-id *txn-id* :key key :value value}))
