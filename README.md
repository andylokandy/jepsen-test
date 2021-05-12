# jepsen.tikv

Jepsen test for TiKV.

## Build

```bash
# generate clojure client from protobuf
make gen-proto-clojure-client
# build rust-client-server which will be referenced by `./rpc-server`
make build-rust-client-server
```

## Usage

1. Install LXC using `scripts/install-lxc.h`.

2. Set up tester using `scripts/setup-tester.sh`.

3. Set up virtual network using `scripts/setup-network.sh`.

4. Set up nodes via LXC using `scripts/setup-nodes.sh`, `scripts/prepare-nodes.sh`.

5. Run the test on nodes:
```bash
xvfb-run lein run test --ssh-private-key ~/.ssh/id_rsa --version v4.0.0 --workload register --concurrency 10 --leave-db-running --time-limit 30
```

## License

Copyright © 2021 FIXME

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
