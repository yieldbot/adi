(defproject im.chit/adi "0.3.2-SNAPSHOT"
  :description "adi (a datomic interface)"
  :url "https://www.github.com/zcaudate/adi"
  :license {:name "The MIT License"
            :url "http://http://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [im.chit/hara.common    "2.2.0-SNAPSHOT"]
                 [im.chit/hara.string    "2.2.0-SNAPSHOT"]
                 [im.chit/hara.data      "2.2.0-SNAPSHOT"]
                 [im.chit/hara.function  "2.2.0-SNAPSHOT"]
                 [im.chit/hara.component "2.2.0-SNAPSHOT"]
                 [im.chit/ribol "0.4.1"]
                 [inflections "0.9.14"]]

  :documentation {:files {"docs/index"
                       {:input "test/midje_doc/adi_guide.clj"
                        :title "adi"
                        :sub-title "a datomic interface"
                        :author "Chris Zheng"
                        :email  "z@caudate.me"}}}

  :profiles {:dev {:plugins [[lein-midje "3.1.3"]
                             [lein-midje-doc "0.0.24"]]
                   ;;:injections [(require 'spyscope.core)]
                   :dependencies [[com.datomic/datomic-free "0.9.5173" :exclusions [joda-time]]
                                  [midje "1.6.3"]
                                  ;;[spyscope "0.1.4"]
                                  [clj-time "0.6.0"]
                                  [me.raynes/fs "1.4.5"]
                                  [cheshire "5.2.0"]]}})
