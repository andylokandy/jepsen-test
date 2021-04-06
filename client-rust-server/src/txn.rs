use std::collections::BTreeMap;
use std::str;
use std::sync::atomic::{AtomicU32, Ordering};

use super::tikv_client_server::txn::begin_txn_request::Type;
use super::tikv_client_server::txn::client_server::Client;
use super::tikv_client_server::txn::{
    BeginTxnReply, BeginTxnRequest, CommitRequest, GetReply, GetRequest, PutRequest,
    RollbackRequest,
};
use tikv_client::{Transaction, TransactionClient};
use tokio::sync::Mutex;
use tonic::{Response, Status};

pub struct ClientProxy {
    client: TransactionClient,
    txns: Mutex<BTreeMap<u32, Transaction>>,
    next_txn_id: AtomicU32,
}

impl ClientProxy {
    pub fn new(client: TransactionClient) -> ClientProxy {
        ClientProxy {
            client,
            txns: Mutex::new(BTreeMap::new()),
            next_txn_id: AtomicU32::new(1),
        }
    }
}

#[tonic::async_trait]
impl Client for ClientProxy {
    async fn begin_txn(
        &self,
        request: tonic::Request<BeginTxnRequest>,
    ) -> Result<tonic::Response<BeginTxnReply>, tonic::Status> {
        let res = match request.into_inner().r#type() {
            Type::Optimistic => self.client.begin_optimistic().await,
            Type::Pessimistic => self.client.begin_pessimistic().await,
        };
        let txn = match res {
            Ok(txn) => txn,
            Err(err) => {
                return Err(Status::unknown(format!(
                    "begin transaction failed: {:?}",
                    err
                )))
            }
        };
        let txn_id = self.next_txn_id.load(Ordering::Relaxed);
        self.txns.lock().await.insert(txn_id, txn);
        let res = self.next_txn_id.compare_exchange(
            txn_id,
            txn_id + 1,
            Ordering::Relaxed,
            Ordering::Relaxed,
        );
        match res {
            Ok(_) => Ok(Response::new(BeginTxnReply { txn_id })),
            Err(err) => Err(Status::unknown(format!(
                "increment next_txn_id failed: {:?}",
                err
            ))),
        }
    }

    async fn get(
        &self,
        request: tonic::Request<GetRequest>,
    ) -> Result<tonic::Response<GetReply>, tonic::Status> {
        let GetRequest { key, txn_id } = request.into_inner();
        let txns = self.txns.lock().await;
        let txn = txns.get(&txn_id);
        match txn.unwrap().get(key).await.unwrap() {
            Some(value) => Ok(Response::new(GetReply {
                value: str::from_utf8(value.as_ref()).unwrap().into(),
            })),
            None => Err(Status::not_found("key is not found")),
        }
    }

    async fn put(
        &self,
        request: tonic::Request<PutRequest>,
    ) -> Result<tonic::Response<()>, tonic::Status> {
        let PutRequest { key, value, txn_id } = request.into_inner();
        let mut txns = self.txns.lock().await;
        let txn = txns.get_mut(&txn_id);
        match txn.unwrap().put(key, value).await {
            Ok(()) => Ok(Response::new(())),
            Err(err) => Err(Status::unknown(format!(
                "tikv transaction put() failed: {:?}",
                err
            ))),
        }
    }

    async fn commit(
        &self,
        request: tonic::Request<CommitRequest>,
    ) -> Result<tonic::Response<()>, tonic::Status> {
        let CommitRequest { txn_id } = request.into_inner();
        let mut txns = self.txns.lock().await;
        let txn = txns.get_mut(&txn_id).unwrap();
        match txn.commit().await {
            Ok(_) => Ok(Response::new(())),
            Err(err) => Err(Status::unknown(format!(
                "tikv transaction commit failed: {:?}",
                err
            ))),
        }
    }

    async fn rollback(
        &self,
        request: tonic::Request<RollbackRequest>,
    ) -> Result<tonic::Response<()>, tonic::Status> {
        let RollbackRequest { txn_id } = request.into_inner();
        let mut txns = self.txns.lock().await;
        let txn = txns.get_mut(&txn_id).unwrap();
        match txn.rollback().await {
            Ok(_) => Ok(Response::new(())),
            Err(err) => Err(Status::unknown(format!(
                "tikv transaction rollback failed: {:?}",
                err
            ))),
        }
    }
}
