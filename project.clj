(defproject concordia "0.1.0-SNAPSHOT"
  :description "JSON schema validation"
  :url "http://github.com/mcohen01/concordia"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :profiles {:dev {:dependencies [[midje "1.5.1"]
                                  [http-kit "2.1.10"]]}}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [cheshire "5.2.0"]])
