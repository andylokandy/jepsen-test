;;;----------------------------------------------------------------------------------
;;; Generated by protoc-gen-clojure.  DO NOT EDIT
;;;
;;; GRPC tikv.raw.Client Client Implementation
;;;----------------------------------------------------------------------------------
(ns tikv.raw.Client.client
  (:require [tikv.raw :refer :all]
            [tikv.error :as tikv.error]
            [clojure.core.async :as async]
            [protojure.grpc.client.utils :refer [send-unary-params invoke-unary]]
            [promesa.core :as p]
            [protojure.grpc.client.api :as grpc]))

;-----------------------------------------------------------------------------
; GRPC Client Implementation
;-----------------------------------------------------------------------------

(defn Get
  ([client params] (Get client {} params))
  ([client metadata params]
  (let [input (async/chan 1)
        output (async/chan 1)
        desc {:service "tikv.raw.Client"
              :method  "Get"
              :input   {:f tikv.raw/new-GetRequest :ch input}
              :output  {:f tikv.raw/pb->GetReply :ch output}
              :metadata metadata}]
    (-> (send-unary-params input params)
        (p/then (fn [_] (invoke-unary client desc output)))))))

(defn Put
  ([client params] (Put client {} params))
  ([client metadata params]
  (let [input (async/chan 1)
        output (async/chan 1)
        desc {:service "tikv.raw.Client"
              :method  "Put"
              :input   {:f tikv.raw/new-PutRequest :ch input}
              :output  {:f tikv.raw/pb->PutReply :ch output}
              :metadata metadata}]
    (-> (send-unary-params input params)
        (p/then (fn [_] (invoke-unary client desc output)))))))

