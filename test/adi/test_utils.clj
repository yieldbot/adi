(ns adi.test-utils
  (:use midje.sweet
        adi.utils))

(fact "type-checker"
  (type-checker :string) => (exactly #'clojure.core/string?)
  (type-checker :bytes) =>  (exactly #'adi.utils/bytes?)
  (type-checker :other) =>  nil)

(fact "funcmap creates a hashmap using as key the function applied to each
       element of the collection."
  (funcmap identity [1 2 3]) => {1 1 2 2 3 3}
  (funcmap #(* 2 %) [1 2 3]) => {2 1 4 2 6 3}
  (funcmap #(* 2 %) [1 1 1]) => {2 1}
  (funcmap :id [{:id 1 :val 1} {:id 2 :val 2}])
  => {1 {:id 1 :val 1} 2 {:id 2 :val 2}}
  (funcmap :id [{:id 1 :val 1} {:id 1 :val 2}])
  => {1 {:id 1 :val 2}})


(fact "slugify-name"
  (slugify-name "h&t") => "h-and-t"
  (slugify-name "h & t") => "h-and-t"
  (slugify-name "H & T ") => "h-and-t"
  (slugify-name "Elephant's walk") => "elephants-walk")

(fact "dissoc-in"
  (dissoc-in {:a 2 :b 2} [:a]) => {:b 2}
  (dissoc-in {:a {:b 2 :c 3}} [:a :b]) => {:a {:c 3}}
  (dissoc-in {:a {:c 3}} [:a :c]) => {}
  (dissoc-in {:a {:b {:c 3}}} [:a :b :c]) => {})

(fact "dissoc-in-keep"
 (dissoc-in-keep {:a 2 :b 2} [:a]) => {:b 2}
 (dissoc-in-keep {:a {:b 2 :c 3}} [:a :b]) => {:a {:c 3}}
 (dissoc-in-keep {:a {:c 3}} [:a :c]) => {:a {}}
 (dissoc-in-keep {:a {:b {:c 3}}} [:a :b :c]) => {:a {:b {}}})

(fact "?sym generates a unique symbol that starts with ?"
  (?sym) => (every-checker
                    symbol?
                    (fn [x] (= \? (first (name x))))
                    (fn [x] (not= (?sym) x))))

(fact "remove-repeated outputs a filtered list of values"
  (remove-repeated [1 1 2 2 3 3 4 5 6]) => [1 2 3 4 5 6]
  (remove-repeated :n [{:n 1} {:n 1} {:n 1} {:n 2} {:n 2}]) => [{:n 1} {:n 2}]
  (remove-repeated even? [2 4 6 1 3 5]) => [2 1])

(fact "flatten-to-vecs"
  (flatten-to-vecs [[1 2 3] [[1 2 3] [4 5 6]]])
  => [[1 2 3] [1 2 3] [4 5 6]])

(fact "keyword-str returns the string representation with the colon"
  (keyword-str nil) => ""
  (keyword-str :hello) => "hello"
  (keyword-str :hello/there) => "hello/there"
  (keyword-str :hello/there/man) => "hello/there/man")

(fact "keyword-join merges a sequence of keys together to form a keyword"
  (keyword-join []) => nil
  (keyword-join [:hello]) => :hello
  (keyword-join [:hello :there]) => :hello/there
  (keyword-join [:a :b :c :d]) => :a/b/c/d)

(fact "keyword-split does the inverse of keyword-join.
       It takes a keyword and turns it into a vector"
  (keyword-split nil) => []
  (keyword-split :hello) => [:hello]
  (keyword-split :hello/there) => [:hello :there]
  (keyword-split :a/b/c/d) => [:a :b :c :d])

(fact "keyword-contains?"
  (keyword-contains? nil nil) => true
  (keyword-contains? nil :nil) => false
  (keyword-contains? (keyword "") nil) => false
  (keyword-contains? :hello :hello) => true
  (keyword-contains? :hello/there :hello) => true
  (keyword-contains? :hellothere :hello) => false
  (keyword-contains? :hello/there/again :hello) => true
  (keyword-contains? :hello/there/again :hello/there) => true)

(fact "keyword-nsvec will output the namespace in vector form"
  (keyword-nsvec nil) => []
  (keyword-nsvec :hello) => []
  (keyword-nsvec :hello/there) => [:hello]
  (keyword-nsvec :hello/there/again) => [:hello :there])

(fact "keyword-nsvec? will check is the output of key namespace is the that specified"
  (keyword-nsvec? nil []) => true
  (keyword-nsvec? :hello []) => true
  (keyword-nsvec? :hello/there [:hello]) => true
  (keyword-nsvec? :hello/there/again [:hello :there]) => true
  (keyword-nsvec? :hello/there/again [:hello]) => false)

(fact "keyword-nsroot will output the keyword root"
  (keyword-nsroot nil) => nil
  (keyword-nsroot :hello) => nil
  (keyword-nsroot :hello/there) => :hello
  (keyword-nsroot :hello/there/again) => :hello)

(fact "keyword-nsroot? will check is the output of key namespace is the that specified"
  (keyword-nsroot? nil nil) => true
  (keyword-nsroot? :hello nil) => true
  (keyword-nsroot? :hello/there :hello) => true
  (keyword-nsroot? :hello/there/again :hello) => true)

(fact "keyword-stemvec will output the namespace in vector form"
  (keyword-stemvec nil) => []
  (keyword-stemvec :hello) => []
  (keyword-stemvec :hello/there) => [:there]
  (keyword-stemvec :hello/there/again) => [:there :again])

(fact "keyword-stemvec? will check is the output of key namespace is the that specified"
  (keyword-stemvec? nil []) => true
  (keyword-stemvec? :hello []) => true
  (keyword-stemvec? :hello/there [:there]) => true
  (keyword-stemvec? :hello/there/again [:there :again]) => true)

(fact "keyword-stem will output the namespace in tor form"
  (keyword-stem nil) => nil
  (keyword-stem :hello) => nil
  (keyword-stem :hello/there) => :there
  (keyword-stem :hello/there/again) => :there/again)

(fact "keyword-stem? will check is the output of key namespace is the that specified"
  (keyword-stem? nil nil) => true
  (keyword-stem? :hello nil) => true
  (keyword-stem? :hello/there :there) => true
  (keyword-stem? :hello/there/again :there/again) => true)

(fact "keyword-ns will output the namespace of a key"
  (keyword-ns nil) => nil
  (keyword-ns :hello) => nil
  (keyword-ns :hello/there) => :hello
  (keyword-ns :hello/there/again) => :hello/there)

(fact "keyword-ns? will check if the key contains a namespace"
  (keyword-ns? nil) => false
  (keyword-ns? nil nil) => true
  (keyword-ns? :hello) => false
  (keyword-ns? :hello nil) => true
  (keyword-ns? :hello/there) => true
  (keyword-ns? :hello/there :hello) => true
  (keyword-ns? :hello/there/again) => true
  (keyword-ns? :hello/there/again :hello/there) => true)

(fact "keyword-val will output the last "
  (keyword-val nil) => nil
  (keyword-val :hello) => :hello
  (keyword-val :hello/there) => :there
  (keyword-val :hello/there/again) => :again)

(fact "keyword-val will check if the output is that specified"
  (keyword-val? nil nil) => true
  (keyword-val? :hello :hello) => true
  (keyword-val? :hello/there :there) => true
  (keyword-val? :hello/there/again :again) => true)


(fact "list-keyword-ns will output all unique kns namespace"
  (list-keyword-ns {:hello/a 1 :hello/b 2 :a 3 :b 4}) => #{nil :hello}
  (list-keyword-ns {:hello/a 1 :hello/b 2 :there/a 3 :there/b 4}) => #{:hello :there})

(fact "list-ns-keys will output all keys"
  (list-ns-keys {:hello/a 1 :hello/b 2
               :there/a 3 :there/b 4} :hello)
  => #{:hello/a :hello/b})

(fact "contain-ns-keys?"
  (contain-ns-keys? {:hello/a 1 :hello/b 2
                     :there/a 3 :there/b 4} :not-there)
  => nil

  (contain-ns-keys? {:hello/a 1 :hello/b 2
                     :there/a 3 :there/b 4} :hello)
  => true)

(fact "list-keys-in will output all keys"
  (list-keys-in
    {:a {:b 1 :c 2}})
  => #{:a :b :c})

(fact "dissoc-keys-in will output all keys"
  (dissoc-keys-in {:a {:b 1 :c {:b 1}}} [:b])
  => {:a {:c {}}})

(fact "flatten-keys-in will take a map of maps and make it into a single map"
  (flatten-keys-in {}) => {}
  (flatten-keys-in {:a 1 :b 2}) => {:a 1 :b 2}
  (flatten-keys-in {:a {:b 2 :c 3}}) => {:a/b 2 :a/c 3}
  (flatten-keys-in {:a {:b {:c 3 :d 4}
                        :e {:f 5 :g 6}}
                    :h {:i 7} })
  => {:a/b/c 3 :a/b/d 4 :a/e/f 5 :a/e/g 6 :h/i 7})

(fact "flatten-keys will take a map of maps and make it into a single map"
  (flatten-keys {}) => {}
  (flatten-keys {:a 1 :b 2}) => {:a 1 :b 2}
  (flatten-keys {:a {:b 2 :c 3}}) => {:a/b 2 :a/c 3}
  (flatten-keys {:a {:b {:c 3 :d 4}
                             :e {:f 5 :g 6}}
                         :h {:i 7} })
  => {:a/b {:c 3 :d 4} :a/e {:f 5 :g 6} :h/i 7})

(fact "treeify-keys will take a single map of compound keys and make it into a tree"
  (treeify-keys {}) => {}
  (treeify-keys {:a 1 :b 2}) => {:a 1 :b 2}
  (treeify-keys {:a/b 2 :a/c 3}) => {:a {:b 2 :c 3}}
  (treeify-keys {:a {:b/c 2} :a/d 3}) => {:a {:b/c 2 :d 3}}
  (treeify-keys {:a/b {:e/f 1} :a/c {:g/h 1}})
  => {:a {:b {:e/f 1} :c {:g/h 1}}}
  (treeify-keys {:a/b/c 3 :a/b/d 4 :a/e/f 5 :a/e/g 6 :h/i 7})
  => {:a {:b {:c 3 :d 4}
          :e {:f 5 :g 6}}
      :h {:i 7}})

(fact "treeify-keys will take a map of nested compound keys and shape it into a non-compound tree"
   (treeify-keys-in nil) => {}
   (treeify-keys-in {}) => {}
   (treeify-keys-in {:a 1 :b 3}) => {:a 1 :b 3}
   (treeify-keys-in {:a/b 2 :a {:c 3}}) => {:a {:b 2 :c 3}}
   (treeify-keys-in {:a/b {:e/f 1} :a/c {:g/h 1}})
   => {:a {:b {:e {:f 1}}
           :c {:g {:h 1}}}}
   (treeify-keys-in {:a {:b/c 2} :a/d 3}) => {:a {:b {:c 2} :d 3}})

(fact "diff-in will take two maps and compare what in the first is different to that in the second"
  (diff-in {} {}) => {}
  (diff-in {:a 1} {}) => {:a 1}
  (diff-in {:a {:b 1}} {})=> {:a {:b 1}}
  (diff-in {:a {:b 1}} {:a {:b 1}}) => {}
  (diff-in {:a {:b 1}} {:a {:b 1 :c 1}}) => {}
  (diff-in {:a {:b 1 :c 1}} {:a {:b 1}}) => {:a {:c 1}}
  (diff-in {:a 1 :b {:c {:d {:e 1}}}}
                {:a 1 :b {:c {:d {:e 1}}}})
  => {}
  (diff-in {:a 1 :b {:c {:d {:e 1}}}}
                {:a 1 :b {:c 1}})
  => {:b {:c {:d {:e 1}}}})


(fact "merge-in will take two maps and merge them recursively"
  (merge-in {} {}) => {}
  (merge-in {:a 1} {}) => {:a 1}
  (merge-in {} {:a 1}) => {:a 1}
  (merge-in {:a {:b 1}} {:a {:c 2}}) => {:a {:b 1 :c 2}}
  (merge-in {:a {:b {:c 1}}} {:a {:b {:c 2}}}) => {:a {:b {:c 2}}}
  (merge-in {:a {:b 3}} {:a {:b {:c 3}}}) => {:a {:b {:c 3}}}
  (merge-in {:a {:b {:c 3}}} {:a {:b 3}}) => {:a {:b 3}}
  (merge-in {:a {:b {:c 1 :d 2}}} {:a {:b {:c 3}}}) => {:a {:b {:c 3 :d 2}}})


(fact "remove-empty-in"
  (remove-empty-in {}) => {}
  (remove-empty-in {:a {}}) => {}
  (remove-empty-in {:a {} :b 1}) => {:b 1}
  (remove-empty-in {:a {:b {:c 1}}}) => {:a {:b {:c 1}}}
  (remove-empty-in {:a {:b {:c {}}}}) => {}
  (remove-empty-in {:a {:b {:c {} :d 1}}}) => {:a {:b {:d 1}}})

(fact "nest-keys-in will extend a treeified map with given namespace keys"
  (nest-keys-in {:a 1 :b 2} [:hello] [])
  => {:hello {:a 1 :b 2}}

  (nest-keys-in {:a 1 :b 2} [:hello :there] [])
  => {:hello {:there {:a 1 :b 2}}}

  (nest-keys-in {:a 1 :b 2} [:hello] [:a])
  => {:hello {:b 2} :a 1}

  (nest-keys-in {:a 1 :b 2} [:hello] [:a :b])
  => {:hello {} :a 1 :b 2})


(fact "unnest-keys-in will make a treefied map"
  (unnest-keys-in {:hello/a 1
                   :hello/b 2
                   :there/a 3
                   :there/b 4} [:hello])
  => {:a 1 :b 2 :there {:a 3 :b 4}}

  (unnest-keys-in {:hello/a 1
                     :hello/b 2
                     :there/a 3
                     :there/b 4} [:hello] [] )
  => {:a 1 :b 2 :there {:a 3 :b 4}}

  (unnest-keys-in {:hello {:a 1 :b 2}
                     :there {:a 3 :b 4}} [:hello] [] )
  => {:a 1 :b 2 :there {:a 3 :b 4}}

  (unnest-keys-in {:hello/there/a 1
                     :hello/there/b 2
                     :again/there/a 3
                     :again/there/b 4} [:hello :there] [] )
  => {:a 1 :b 2 :again {:there {:a 3 :b 4}}}


  (unnest-keys-in {:hello {:there {:a 1 :b 2}}
                     :again {:there {:a 3 :b 4}}} [:hello :there] [] )
  => {:a 1 :b 2 :again {:there {:a 3 :b 4}}}

  (unnest-keys-in {:hello/a 1
                     :hello/b 2
                     :there/a 3
                     :there/b 4} [:hello] [:+])
  => {:a 1 :b 2 :+ {:there {:a 3 :b 4}}})

(fact "keyword-ns-map"
  (keyword-ns-map {:a {:b {:c 1}
                       :d 1}
                   :e {:f 1
                       :g 1}
                   :h 1})
  => {:a/b #{:a/b/c}, :a #{:a/d}, :e #{:e/g :e/f}})
