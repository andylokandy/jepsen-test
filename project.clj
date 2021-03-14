(defproject jepsen.tikv "0.1.0-SNAPSHOT"
  :description "Jepsen test for TiKV"
  :url "https://tikv.org"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :main jepsen.tikv
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [jepsen "0.2.3"]]
  :repl-options {:init-ns jepsen.tikv})
