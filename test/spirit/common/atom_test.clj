(ns spirit.common.atom-test
  (:use hara.test)
  (:require [spirit.common.atom :refer :all]
            [hara.io.file :as fs]))

^{:refer spirit.common.atom/write-value :added "0.5"}
(fact "write a value to file"
  
  (write-value {:account {"a@a.com" {:id 1 :value "a"}
                          "b@b.com" {:id 2 :value "b"}}
                :info {3 {:name "Cain"}
                       1 {:name "Chris"}
                       2 {:name "David"}}}
               {:path "test.db"
                :format :table
                :suffix "txt"
                :levels 1
                :headers  {:account [:id :email :value]
                           :info    [:id :name]}
                :sort-key {:info    :name}
                :id-key   {:account :email}}))

^{:refer spirit.common.atom/read-value :added "0.5"}
(fact "reads a value from a file"
  
  (read-value {:path "test.db"
               :format :table})
  => {:account {"a@a.com" {:id 1, :value "a"},
                "b@b.com" {:id 2, :value "b"}},
      :info {3 {:name "Cain"},
             1 {:name "Chris"},
             2 {:name "David"}}})

^{:refer spirit.common.atom/file-out :added "0.5"}
(fact "adds watch to atom, saving its contents to file on every change"

  (def out-file (str (fs/create-tmpdir) "/test.txt"))
  
  (swap! (file-out (atom 1) {:path out-file})
         inc)
  
  (read-string (slurp out-file))
  => 2)

^{:refer spirit.common.atom/log-out :added "0.5"}
(comment "adds watch to atom, logging the contents on every change"
  
  (with-out-str
    (swap! (log-out (atom 1) {})
           inc)))

^{:refer spirit.common.atom/attach-state :added "0.5"}
(comment "used with component, adds watch on record that incorporates state")

^{:refer spirit.common.atom/detach-state :added "0.5"}
(comment "used with component, remove watch on record that incorporates state")

^{:refer spirit.common.atom/cursor :added "0.5"}
(fact "adds a cursor to the atom to update on any change"

  (def a (atom {:a {:b 1}}))
  
  (def ca (cursor a [:a :b]))

  (do (swap! ca + 10)
      (swap! a update-in [:a :b] + 100)
      [(deref a) (deref ca)])
  => [{:a {:b 111}} 111])

^{:refer spirit.common.atom/derived :added "0.5"}
(fact "constructs an atom derived from other atoms"

  (def a (atom 1))
  (def b (atom 10))
  (def c (derived [a b] +))

  (do (swap! a + 1)
      (swap! b + 10)
      [@a @b @c])
  => [2 20 22])
