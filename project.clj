(defproject adi "0.1.2"
  :description "adi (a datomic interface) simplifies usage of datomic by providing
                a common query, insertion and manipulation interface that produce
                structured data, yet at the same time keeping with the 'more is less'
                philosophy of clojure."
  :url "http://www.github.com/zcaudate/adi"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [com.datomic/datomic-free "0.8.3941"]
                 [hara "0.7.1"]
                 [inflections "0.8.0"]]
  :profiles {:dev {:dependencies [[midje "1.5.1"]
                                  [clj-time "0.4.4"]
                                  [fs "1.3.3"]
                                  [cheshire "5.0.1"]]}})
