use super::tikv_client_server::raw::client_server::Client;
use super::tikv_client_server::raw::{GetReply, GetRequest, PutRequest};
use log::info;
use std::str;
use tikv_client::RawClient;
use tikv_client_common::Error;
use tonic::{Request, Response, Status};

pub struct ClientProxy {
    client: RawClient,
}

impl ClientProxy {
    pub fn new(client: RawClient) -> ClientProxy {
        ClientProxy { client }
    }
}

#[tonic::async_trait]
impl Client for ClientProxy {
    async fn get(&self, request: Request<GetRequest>) -> Result<Response<GetReply>, Status> {
        info!("Got a request: {:?}", request);
        let response = match self.client.get(request.into_inner().key).await {
            Ok(response) => response,
            Err(err) => {
                match err {
                    Error::Io(_)
                    | Error::Grpc(_)
                    | Error::UndeterminedError(_)
                    | Error::MultipleErrors(_)
                    | Error::InternalError { message: _ } => {
                        return Err(Status::unknown(format!(
                            "tikv client get() failed: {:?}",
                            err
                        )))
                    }
                    _ => {
                        return Err(Status::aborted(format!(
                            "tikv client get() aborted: {:?}",
                            err
                        )))
                    }
                };
            }
        };
        match response {
            Some(value) => Ok(Response::new(GetReply {
                value: str::from_utf8(value.as_ref()).unwrap().into(),
            })),
            None => Err(Status::not_found("key is not found")),
        }
    }

    async fn put(&self, request: Request<PutRequest>) -> Result<Response<()>, Status> {
        info!("Got a request: {:?}", request);
        let message = request.into_inner();
        match self.client.put(message.key, message.value).await {
            Ok(()) => Ok(Response::new(())),
            Err(err) => match err {
                Error::Io(_)
                | Error::Grpc(_)
                | Error::UndeterminedError(_)
                | Error::MultipleErrors(_)
                | Error::InternalError { message: _ } => {
                    return Err(Status::unknown(format!(
                        "tikv client put() failed: {:?}",
                        err
                    )));
                }
                _ => {
                    return Err(Status::aborted(format!(
                        "tikv client put() aborted: {:?}",
                        err
                    )))
                }
            },
        }
    }
}
