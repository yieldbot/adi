(defproject adi "0.1.0-SNAPSHOT"
  :description "adi (a datomic interface) simplifies usage of datomic by providing
                query, insertion and maniputlation interfaces that produce
                structured data, yet at the same time keeping with the 'more is less'
                philosophy of clojure."
  :url "http://www.github.com/zcaudate/adi"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [com.datomic/datomic-free "0.8.3789"]]
  :profiles {:dev {:dependencies [[midje "1.5-beta2"]
                                  [clj-time "0.4.4"]]}})
