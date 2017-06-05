(ns spirit.common.pipeline.allow
  (:require [hara.common.checks :refer [hash-map?]]
            [hara.string.path :as path]
            [hara.event :refer [raise]]))

(defn wrap-branch-model-allow
  "Works together with wrap-attr-model-allow to control access to data
  (pipeline/normalise {:account/name \"Chris\"}
                       {:schema (schema/schema examples/account-name-age-sex)
                        :pipeline {:allow {}}}
                       *wrappers*)
  => (throws-info {:data {:name \"Chris\"}
                    :key-path [:account]
                    :normalise true
                    :not-allowed true
                    :nsv [:account]})

  (pipeline/normalise {:account/name \"Chris\"}
                       {:schema (schema/schema examples/account-name-age-sex)
                        :pipeline {:allow {:account {:name :checked}}}}
                       *wrappers*)
  => {:account {:name \"Chris\"}}
  "
  {:added "0.3"}
  [f]
  (fn [subdata subsch nsv interim fns datasource]
    (let [suballow (:allow interim)
          nsubdata  (if (nil? suballow)
                        (raise [:normalise :not-allowed
                                  {:data subdata :nsv nsv :key-path (:key-path interim)}]
                               (str "WRAP_BRANCH_MODEL_ALLOW: key " nsv " is not accessible."))
                        subdata)]
      (f nsubdata subsch nsv interim fns datasource))))

(defn wrap-attr-model-allow [f]
  (fn [subdata [attr] nsv interim fns datasource]
    (let [suballow (:allow interim)]
      (cond (= (:type attr) :ref)
            (cond (= suballow :yield)
                  (let [ynsv   (path/split (-> attr :ref :ns))
                        tmodel (get-in datasource (concat [:pipeline :allow] ynsv))]
                    (f subdata [attr] ynsv (assoc interim :allow tmodel) fns datasource))

                  (or (= suballow :id) (hash-map? suballow))
                  (f subdata [attr] nsv interim fns datasource)

                  :else 
                  (raise [:normalise :not-allowed
                            {:data subdata :nsv nsv :key-path (:key-path interim)}]
                         (str "WRAP_ATTR_MODEL_ALLOW: " nsv " is not accessible")))

            (or (nil? suballow) (not= suballow :checked))
            (let [nsubdata 
                  (raise [:normalise :not-allowed
                            {:data subdata :nsv nsv :key-path (:key-path interim)}]
                         (str "WRAP_ATTR_MODEL_ALLOW: " nsv " is not accessible"))]
               (f nsubdata [attr] nsv interim fns datasource))

            :else
            (f subdata [attr] nsv interim fns datasource)))))
