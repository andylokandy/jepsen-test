use std::net::TcpListener;
use std::str;

use clap::{App, Arg};
use tikv_client::RawClient;
use tikv_client_common::Error;
use tonic::{transport::Server, Request, Response, Status};

use tikv_client_server::raw::client_server::{Client, ClientServer};
use tikv_client_server::raw::{GetReply, GetRequest, PutRequest};

mod tikv_client_server;

pub struct RawClientProxy {
    client: RawClient,
}

impl RawClientProxy {
    fn new(client: RawClient) -> RawClientProxy {
        RawClientProxy { client }
    }
}

#[tonic::async_trait]
impl Client for RawClientProxy {
    async fn get(&self, request: Request<GetRequest>) -> Result<Response<GetReply>, Status> {
        println!("Got a request: {:?}", request);
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
        println!("Got a request: {:?}", request);
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

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    let matches = App::new("client-rust server")
        .version("0.1")
        .author("Ziyi Yan <ziyi.yan@foxmail.com>")
        .about("A server to proxy tikv operations built on client-rust")
        .arg(
            Arg::with_name("node")
                .long("node")
                .required(true)
                .takes_value(true),
        )
        .arg(
            Arg::with_name("type")
                .long("type")
                .required(true)
                .takes_value(true)
                .possible_values(&["raw", "txn"]),
        )
        .get_matches();

    let node = matches.value_of("node").unwrap();
    let _type = matches.value_of("type").unwrap();

    let client = RawClient::new(vec![format!("{}:2379", node)]).await?;
    let proxy = RawClientProxy::new(client);
    let server = ClientServer::new(proxy);

    let port = get_available_port().unwrap();
    let addr = format!("127.0.0.1:{}", port).parse()?;
    println!("{}", addr);

    Server::builder().add_service(server).serve(addr).await?;

    Ok(())
}

fn get_available_port() -> Option<u16> {
    (8000..9000).find(|port| port_is_available(*port))
}

fn port_is_available(port: u16) -> bool {
    match TcpListener::bind(("127.0.0.1", port)) {
        Ok(_) => true,
        Err(_) => false,
    }
}
