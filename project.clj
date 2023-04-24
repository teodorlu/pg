(defproject com.github.igrishaev/pg "0.1.0-SNAPSHOT"

  :description
  "FIXME: write description"

  :url
  "http://example.com/FIXME"

  :license
  {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
   :url "https://www.eclipse.org/legal/epl-2.0/"}

  :plugins
  [[lein-sub "0.3.0"]
   [exoscale/lein-replace "0.1.1"]]

  :dependencies
  [[com.github.igrishaev/pg-oid :version]
   [com.github.igrishaev/pg-encode :version]
   [com.github.igrishaev/pg-copy :version]
   [com.github.igrishaev/pg-joda-time :version]]

  :sub
  ["pg-oid"
   "pg-encode"
   "pg-copy"
   "pg-joda-time"]

  :managed-dependencies
  [[com.github.igrishaev/pg-oid :version]
   [com.github.igrishaev/pg-encode :version]
   [joda-time/joda-time "2.12.5"]]

  :profiles
  {:dev
   {:dependencies
    [[org.clojure/clojure "1.11.1"]]}})
