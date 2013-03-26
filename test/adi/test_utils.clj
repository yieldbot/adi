(ns adi.test-utils
  (:use midje.sweet)
  (:require [adi.utils :as ut]))

(fact "funcmap creates a hashmap using as key the function applied to each
       element of the collection."
  (ut/funcmap identity [1 2 3]) => {1 1 2 2 3 3}
  (ut/funcmap #(* 2 %) [1 2 3]) => {2 1 4 2 6 3}
  (ut/funcmap #(* 2 %) [1 1 1]) => {2 1}
  (ut/funcmap :id [{:id 1 :val 1} {:id 2 :val 2}])
  => {1 {:id 1 :val 1} 2 {:id 2 :val 2}}
  (ut/funcmap :id [{:id 1 :val 1} {:id 1 :val 2}])
  => {1 {:id 1 :val 2}})


(fact "clean-name *** FIX change to 'slugify-name' *****"
  (ut/clean-name "h&t") => "h-and-t"
  (ut/clean-name "h & t") => "h-and-t"
  (ut/clean-name "H & T ") => "h-and-t"
  (ut/clean-name "Elephant's walk") => "elephants-walk")

(fact "dissoc-in"
  (ut/dissoc-in {:a 2 :b 2} [:a]) => {:b 2}
  (ut/dissoc-in {:a {:b 2 :c 3}} [:a :b]) => {:a {:c 3}}
  (ut/dissoc-in {:a {:c 3}} [:a :c]) => {}
  (ut/dissoc-in {:a {:b {:c 3}}} [:a :b :c]) => {})

(fact "dissoc-in-keepempty
       *** FIX change to 'dissoc-in-keep' "
 (ut/dissoc-in-keepempty {:a 2 :b 2} [:a]) => {:b 2}
 (ut/dissoc-in-keepempty {:a {:b 2 :c 3}} [:a :b]) => {:a {:c 3}}
 (ut/dissoc-in-keepempty {:a {:c 3}} [:a :c]) => {:a {}}
 (ut/dissoc-in-keepempty {:a {:b {:c 3}}} [:a :b :c]) => {:a {:b {}}})

(fact "?sym generates a unique symbol that starts with ?"
  (ut/?sym) => (every-checker
                    symbol?
                    (fn [x] (= \? (first (name x))))
                    (fn [x] (not= (ut/?sym) x))))

(fact "no-repeats outputs a filtered list of values
       *** FIX to remove-repeated"
  (ut/no-repeats [1 1 2 2 3 3 4 5 6]) => [1 2 3 4 5 6]
  (ut/no-repeats :n [{:n 1} {:n 1} {:n 1} {:n 2} {:n 2}]) => [{:n 1} {:n 2}]
  (ut/no-repeats even? [2 4 6 1 3 5]) => [2 1])

(fact "key-str returns the string representation with the colon"
  (ut/key-str nil) => ""
  (ut/key-str :hello) => "hello"
  (ut/key-str :hello/there) => "hello/there"
  (ut/key-str :hello/there/man) => "hello/there/man")

(fact "key-merge merges a sequence of keys together to form a keyword"
  (ut/key-merge []) => nil
  (ut/key-merge [:hello]) => :hello
  (ut/key-merge [:hello :there]) => :hello/there
  (ut/key-merge [:a :b :c :d]) => :a/b/c/d)

(fact "key-unmerge does the inverse of key-merge.
       It takes a keyword and turns it into a vector"
  (ut/key-unmerge nil) => []
  (ut/key-unmerge :hello) => [:hello]
  (ut/key-unmerge :hello/there) => [:hello :there]
  (ut/key-unmerge :a/b/c/d) => [:a :b :c :d])

(fact "key-nsvec will output the namespace in vector form"
  (ut/key-nsvec nil) => []
  (ut/key-nsvec :hello) => []
  (ut/key-nsvec :hello/there) => [:hello]
  (ut/key-nsvec :hello/there/again) => [:hello :there])

(fact "key-nsvec? will check is the output of key namespace is the that specified"
  (ut/key-nsvec? nil []) => true
  (ut/key-nsvec? :hello []) => true
  (ut/key-nsvec? :hello/there [:hello]) => true
  (ut/key-nsvec? :hello/there/again [:hello :there]) => true
  (ut/key-nsvec? :hello/there/again [:hello]) => false)

(fact "key-ns will output the namespace of a key"
  (ut/key-ns nil) => nil
  (ut/key-ns :hello) => nil
  (ut/key-ns :hello/there) => :hello
  (ut/key-ns :hello/there/again) => :hello/there)

(fact "key-ns? will check if the key contains a namespace"
  (ut/key-ns? nil) => false
  (ut/key-ns? nil nil) => true
  (ut/key-ns? :hello) => false
  (ut/key-ns? :hello nil) => true
  (ut/key-ns? :hello/there) => true
  (ut/key-ns? :hello/there :hello) => true
  (ut/key-ns? :hello/there/again) => true
  (ut/key-ns? :hello/there/again :hello/there) => true)

(fact "key-val will output the last "
  (ut/key-val nil) => nil
  (ut/key-val :hello) => :hello
  (ut/key-val :hello/there) => :there
  (ut/key-val :hello/there/again) => :again)

(fact "key-val will check if the output is that specified"
  (ut/key-val? nil nil) => true
  (ut/key-val? :hello :hello) => true
  (ut/key-val? :hello/there :there) => true
  (ut/key-val? :hello/there/again :again) => true)


(fact "list-key-ns will output all unique kns namespace"
  (ut/list-key-ns {:hello/a 1 :hello/b 2 :a 3 :b 4}) => #{nil :hello}
  (ut/list-key-ns {:hello/a 1 :hello/b 2 :there/a 3 :there/b 4}) => #{:hello :there})

(fact "list-keys will output all keys"
  (ut/list-keys {:hello/a 1 :hello/b 2
               :there/a 3 :there/b 4} :hello)
  => #{:hello/a :hello/b})

(fact "list-all-keys will output all keys"
  (ut/list-all-keys
    {:a {:b 1 :c 2}})
  => #{:a :b :c})

(fact "flatten-all-keys will take a map of maps and make it into a single map"
  (ut/flatten-all-keys {}) => {}
  (ut/flatten-all-keys {:a 1 :b 2}) => {:a 1 :b 2}
  (ut/flatten-all-keys {:a {:b 2 :c 3}}) => {:a/b 2 :a/c 3}
  (ut/flatten-all-keys {:a {:b {:c 3 :d 4}
                        :e {:f 5 :g 6}}
                    :h {:i 7} })
  => {:a/b/c 3 :a/b/d 4 :a/e/f 5 :a/e/g 6 :h/i 7})

(fact "flatten-keys will take a map of maps and make it into a single map"
  (ut/flatten-keys {}) => {}
  (ut/flatten-keys {:a 1 :b 2}) => {:a 1 :b 2}
  (ut/flatten-keys {:a {:b 2 :c 3}}) => {:a/b 2 :a/c 3}
  (ut/flatten-keys {:a {:b {:c 3 :d 4}
                             :e {:f 5 :g 6}}
                         :h {:i 7} })
  => {:a/b {:c 3 :d 4} :a/e {:f 5 :g 6} :h/i 7})

(fact "treeify-keys will take a single map of compound keys and make it into a tree"
  (ut/treeify-keys {}) => {}
  (ut/treeify-keys {:a 1 :b 2}) => {:a 1 :b 2}
  (ut/treeify-keys {:a/b 2 :a/c 3}) => {:a {:b 2 :c 3}}
  (ut/treeify-keys {:a {:b/c 2} :a/d 3}) => {:a {:b/c 2 :d 3}}
  (ut/treeify-keys {:a/b/c 3 :a/b/d 4 :a/e/f 5 :a/e/g 6 :h/i 7})
  => {:a {:b {:c 3 :d 4}
          :e {:f 5 :g 6}}
      :h {:i 7}})

(fact "treeify-keys will take a map of nested compound keys and shape it into a non-compound tree"
   (ut/treeify-all-keys nil) => {}
   (ut/treeify-all-keys {}) => {}
   (ut/treeify-all-keys {:a 1 :b 3}) => {:a 1 :b 3}
   (ut/treeify-all-keys {:a/b 2 :a {:c 3}}) => {:a {:b 2 :c 3}}
   (ut/treeify-all-keys {:a {:b/c 2} :a/d 3}) => {:a {:b {:c 2} :d 3}})

(fact "tree-diff will take two maps and compare what in the first is different to that in the second"
  (ut/tree-diff {} {}) => {}
  (ut/tree-diff {:a 1} {}) => {:a 1}
  (ut/tree-diff {:a {:b 1}} {})=> {:a {:b 1}}
  (ut/tree-diff {:a {:b 1}} {:a {:b 1}}) => {}
  (ut/tree-diff {:a {:b 1}} {:a {:b 1 :c 1}}) => {}
  (ut/tree-diff {:a {:b 1 :c 1}} {:a {:b 1}}) => {:a {:c 1}}
  (ut/tree-diff {:a 1 :b {:c {:d {:e 1}}}}
                {:a 1 :b {:c {:d {:e 1}}}})
  => {}
  (ut/tree-diff {:a 1 :b {:c {:d {:e 1}}}}
                {:a 1 :b {:c 1}})
  => {:b {:c {:d {:e 1}}}})


(fact "tree-merge will take two maps and merge them recursively"
  (ut/tree-merge {} {}) => {}
  (ut/tree-merge {:a 1} {}) => {:a 1}
  (ut/tree-merge {} {:a 1}) => {:a 1}
  (ut/tree-merge {:a {:b 1}} {:a {:c 2}}) => {:a {:b 1 :c 2}}
  (ut/tree-merge {:a {:b {:c 1}}} {:a {:b {:c 2}}}) => {:a {:b {:c 2}}}
  (ut/tree-merge {:a {:b 3}} {:a {:b {:c 3}}}) => {:a {:b {:c 3}}}
  (ut/tree-merge {:a {:b {:c 3}}} {:a {:b 3}}) => {:a {:b 3}}
  (ut/tree-merge {:a {:b {:c 1 :d 2}}} {:a {:b {:c 3}}}) => {:a {:b {:c 3 :d 2}}})





(fact "extend-keys will extend a treeified map with given namespace keys"
  (ut/extend-keys {:a 1 :b 2} [:hello] [])
  => {:hello {:a 1 :b 2}}

  (ut/extend-keys {:a 1 :b 2} [:hello :there] [])
  => {:hello {:there {:a 1 :b 2}}}

  (ut/extend-keys {:a 1 :b 2} [:hello] [:a])
  => {:hello {:b 2} :a 1}

  (ut/extend-keys {:a 1 :b 2} [:hello] [:a :b])
  => {:hello {} :a 1 :b 2})


(fact "contract-keys will make a treefied map
       *** FIX abbreviate-keys"
  (ut/contract-keys {:hello/a 1
                     :hello/b 2
                     :there/a 3
                     :there/b 4} [:hello] [] )
  => {:a 1 :b 2 :there {:a 3 :b 4}}

  (ut/contract-keys {:hello {:a 1 :b 2}
                     :there {:a 3 :b 4}} [:hello] [] )
  => {:a 1 :b 2 :there {:a 3 :b 4}}

  (ut/contract-keys {:hello/there/a 1
                     :hello/there/b 2
                     :again/there/a 3
                     :again/there/b 4} [:hello :there] [] )
  => {:a 1 :b 2 :again {:there {:a 3 :b 4}}}


  (ut/contract-keys {:hello {:there {:a 1 :b 2}}
                     :again {:there {:a 3 :b 4}}} [:hello :there] [] )
  => {:a 1 :b 2 :again {:there {:a 3 :b 4}}}

  (ut/contract-keys {:hello/a 1
                     :hello/b 2
                     :there/a 3
                     :there/b 4} [:hello] [:+])
  => {:a 1 :b 2 :+ {:there {:a 3 :b 4}}})
