(ns adi-test.schemas
  (:require [adi.schema :refer [make-xm]]))

(def account-name-age-sex
  {:account {:name [{}]
             :age  [{:type :long}]
             :sex  [{:type :enum
                     :enum {:ns :account.sex
                            :values #{:m :f}}}]}})

(def account-name-age-sex-xm
  (make-xm account-name-age-sex))


(def account-orders-items-image
  {:account {:user     [{}]
             :tags     [{:cardinality :many}]}
   :order   {:account [{:type :ref
                          :ref {:ns :account}}]
               :number [{:type :long}]}
   :item    {:name   [{}]
             :order  [{:type :ref
                       :ref {:ns :order}}]}
   :image   {:item  [{:type :ref
                       :ref {:ns :item}}]
             :url   [{}]}})

(def account-orders-items-image-xm
  (make-xm account-orders-items-image))

(defn generate-alpha []
  (let [c (atom (int \A))]
    (fn []
      (let [res (str (char @c))]
        (swap! c inc)
        res))))

(def link-value-next
  {:link {:value [{:default (generate-alpha)}]
          :next  [{:type :ref
                   :ref {:ns :link
                         :rval :prev}}]}})

(def link-value-next-xm
  (make-xm link-value-next))


(def person-pointer-teacher-student
  {:person {:id   [{}]
            :pointer [{:type :ref}]}
   :teacher {:name [{}]
             :students [{:type :ref
                         :ref {:ns :student}}]}
   :student {:name [{}]}})

(def person-pointer-teacher-student-xm
  (make-xm person-pointer-teacher-student))


(def family-links
  {:person {:name    [{}]
            :age     [{:type :long}]
            :gender  [{:type :enum
                       :enum {:ns :person.gender
                              :values #{:m :f}}}]
            :parent  [{:type :ref
                        :cardinality :many
                        :ref {:ns   :person
                              :rval :child}}]
            :sibling [{:type :ref
                        :cardinality :many
                        :ref {:ns :person
                              :mutual true}}]
            :grandson [{:type :alias
                        :alias {:ns :child/child
                                :template {:child {:son {}}}}}]
            :granddaughter [{:type :alias
                             :alias {:ns :child/child
                                     :template {:child {:daughter {}}}}}]
            :grandma [{:type :alias
                       :alias {:ns :parent/parent
                               :template {:parent {:mother {}}}}}]
            :grandpa [{:type :alias
                       :alias {:ns :parent/parent
                               :template {:parent {:father {}}}}}]
            :nephew  [{:type :alias
                       :alias {:ns :sibling/child
                              :template {:sibling {:child {:gender :m}}}}}]
            :niece   [{:type :alias
                       :alias {:ns :sibling/child
                              :template {:sibling {:child {:gender :m}}}}}]
            :uncle  [{:type :alias
                       :alias {:ns :parent/sibling
                               :template {:parent {:brother {}}}}}]
            :aunt   [{:type :alias
                      :alias {:ns :parent/sibling
                              :template {:parent {:sister {}}}}}]
            :son     [{:type :alias
                       :alias {:ns :child
                               :template {:child {:gender :m}}}}]
            :daughter [{:type :alias
                        :alias {:ns :child
                               :template {:child {:gender :f}}}}]
            :father  [{:type :alias
                       :alias {:ns :parent
                               :template {:parent {:gender :m}}}}]
            :mother  [{:type :alias
                       :alias {:ns :parent
                               :template {:parent {:gender :f}}}}]
            :brother  [{:type :alias
                        :alias {:ns :sibling
                                :template {:sibling {:gender :m}}}}]
            :sister  [{:type :alias
                       :alias {:ns :sibling
                               :template {:sibling {:gender :f}}}}]
            :elder-brother [{:type :alias
                             :alias {:ns :sibling
                                     :template {:age '?x
                                                :sibling {:age '(< ?x)
                                                          :gender :m}}}}]
            :full-sibling  [{:type :alias
                             :alias {:ns :sibling
                                     :template {:parent #{{:gender :m :+ {:db {:id '?x}}}
                                                          {:gender :f :+ {:db {:id '?y}}}}
                                                :sibling {:parent #{{:gender :m :+ {:db {:id '?x}}}
                                                                    {:gender :f :+ {:db {:id '?y}}}}}}}}]}
   :male   [{:type :alias
             :alias {:ns :person
                     :template {:person {:gender :m}}}}]

   :female [{:type :alias
             :alias {:ns :person
                     :template {:person {:gender :f}}}}]})

(def family-links-xm
  (make-xm family-links))
