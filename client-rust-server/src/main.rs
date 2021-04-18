use std::net::TcpListener;

use clap::{App, Arg};
use tonic::transport::Server;

use tikv_client::{RawClient, TransactionClient};

use tikv_client_server::raw::client_server::ClientServer as RawClientServer;
use tikv_client_server::txn::client_server::ClientServer as TxnClientServer;

use raw::ClientProxy as RawClientProxy;
use txn::ClientProxy as TxnClientProxy;

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
            Arg::with_name("port")
                .long("port")
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
    let typ = matches.value_of("type").unwrap();
    let port = matches.value_of("port").unwrap();

    let pd_endpoints = vec![format!("{}:2379", node)];
    match typ {
        "raw" => {
            let client = RawClient::new(pd_endpoints).await?;
            let proxy = RawClientProxy::new(client);
            let server = RawClientServer::new(proxy);
            let addr = format!("127.0.0.1:{}", port).parse()?;
            println!("{}", addr);
            Server::builder().add_service(server).serve(addr).await?;
        }
        "txn" => {
            let client = TransactionClient::new(pd_endpoints).await?;
            let proxy = TxnClientProxy::new(client);
            let server = TxnClientServer::new(proxy);
            let addr = format!("127.0.0.1:{}", port).parse()?;
            println!("{}", addr);
            Server::builder().add_service(server).serve(addr).await?;
        }
        _ => {
            eprintln!("type is not one of \"raw\" and \"txn\"");
            std::process::exit(1);
        }
    };

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

mod tikv_client_server;

mod raw;
mod txn;
