(ns spirit.common.table-test
  (:use hara.test)
  (:require [spirit.common.table :refer :all]
            [clojure.string :as string]
            [hara.string.prose :as prose]))

(defn ascii
  [vs]
  (string/join "\n" vs))

^{:refer spirit.common.table/generate-basic-table :added "0.5"}
(fact "generates a table for output"

  (generate-basic-table [:id :value]
                        [{:id 1 :value "a"}
                         {:id 2 :value "b"}])
  
  => (ascii ["| :id | :value |"
             "|-----+--------|"
             "|   1 |    \"a\" |"
             "|   2 |    \"b\" |"]))

^{:refer spirit.common.table/parse-basic-table :added "0.5"}
(fact "reads a table from a string"

  (parse-basic-table (ascii
                      ["| :id | :value |"
                       "|-----+--------|"
                       "|   1 |    \"a\" |"
                       "|   2 |    \"b\" |"]))
  => {:headers [:id :value]
      :data [{:id 1 :value "a"}
             {:id 2 :value "b"}]})

^{:refer spirit.common.table/generate-single-table :added "0.5"}
(fact "generates a single table"

  (generate-single-table {"a@a.com" {:id 1 :value "a"}
                          "b@b.com" {:id 2 :value "b"}}
                         {:headers [:id :email :value]
                          :sort-key :email
                          :id-key :email})
  => (ascii ["| :id |    :email | :value |"
             "|-----+-----------+--------|"
             "|   1 | \"a@a.com\" |    \"a\" |"
             "|   2 | \"b@b.com\" |    \"b\" |"]))


^{:refer spirit.common.table/parse-single-table :added "0.5"}
(fact "generates a single table"

  (parse-single-table
   (ascii ["| :id |    :email | :value |"
           "|-----+-----------+--------|"
           "|   1 | \"a@a.com\" |    \"a\" |"
           "|   2 | \"b@b.com\" |    \"b\" |"])
   
   {:headers [:id :email :value]
    :sort-key :email
    :id-key :email})
  => {"a@a.com" {:id 1 :value "a"}
      "b@b.com" {:id 2 :value "b"}})

^{:refer spirit.common.table/write-table :added "0.5"}
(fact "generates a single table"

  (write-table
   {:account {"a@a.com" {:id 1 :value "a"}
              "b@b.com" {:id 2 :value "b"}}
    :info {1 {:name "Chris"}
           2 {:name "David"}
           3 {:name "Cain"}}}
   {:path   "test.db"
    :suffix "txt"
    :levels 1
    :headers {:account [:id :email :value]
              :info    [:id :name]}
    :sort-key {:info :name}
    :id-key {:account :email}})
  => {:account (ascii
                ["| :id |    :email | :value |"
                 "|-----+-----------+--------|"
                 "|   1 | \"a@a.com\" |    \"a\" |"
                 "|   2 | \"b@b.com\" |    \"b\" |"])

      :info (ascii
             ["| :id |   :name |"
              "|-----+---------|"
              "|   3 |  \"Cain\" |"
              "|   1 | \"Chris\" |"
              "|   2 | \"David\" |"])})
  
^{:refer spirit.common.table/read-table :added "0.5"}
(fact "generates a single table"

  (read-table
   {:path  "test.db"
    :suffix "txt"
    :levels 1
    :headers {:account [:id :email :value]
              :info    [:id :name]}
    :sort-key {:info :name}
    :id-key {:account :email}})
  => {:account {"a@a.com" {:id 1 :value "a"}
                "b@b.com" {:id 2 :value "b"}}
      :info {1 {:name "Chris"}
             2 {:name "David"}
             3 {:name "Cain"}}})
