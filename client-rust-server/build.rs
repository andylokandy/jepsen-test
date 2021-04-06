fn main() -> Result<(), Box<dyn std::error::Error>> {
    tonic_build::compile_protos("../proto/rawkv.proto")?;
    tonic_build::compile_protos("../proto/txnkv.proto")?;
    Ok(())
}
