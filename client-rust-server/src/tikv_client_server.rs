pub mod raw {
    tonic::include_proto!("tikv.raw"); // The string specified here must match the proto package name
}

pub mod txn {
    tonic::include_proto!("tikv.txn"); // The string specified here must match the proto package name
}
