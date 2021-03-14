# jepsen.tikv

Jepsen test for TiKV.

## Usage

1. Set up tester using `scripts/setup-tester.sh`.

2. Set up nodes via LXC using `scripts/setup-nodes.sh`, `scripts/prepare-nodes.sh`.

3. Set up virtual network using `scripts/setup-network.sh`.

4. Run the test on nodes:
```bash
lein run test --ssh-private-key ~/.ssh/id_rsa
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
