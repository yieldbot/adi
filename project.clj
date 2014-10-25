(defproject im.chit/adi "0.3.1-SNAPSHOT"
  :description "adi (a datomic interface)"
  :url "http://www.github.com/zcaudate/adi"
  :license {:name "The MIT License"
            :url "http://http://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [im.chit/hara.common    "2.1.6-SNAPSHOT"]
                 [im.chit/hara.string    "2.1.6-SNAPSHOT"]
                 [im.chit/hara.data      "2.1.6-SNAPSHOT"]
                 [im.chit/hara.function  "2.1.6-SNAPSHOT"]
                 [im.chit/ribol "0.4.0"]
                 [inflections "0.9.9"]]

  :profiles {:dev {:plugins [[lein-midje "3.1.1"]
                             [lein-midje-doc "0.0.24"]]
                   :dependencies [[com.datomic/datomic-free "0.9.4899" :exclusions [joda-time]]
                                  [midje "1.6.3"]
                                  [clj-time "0.6.0"]
                                  [me.raynes/fs "1.4.5"]
                                  [cheshire "5.2.0"]]}})
