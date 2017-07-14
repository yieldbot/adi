(ns spirit.jetty.server)

(defrecord JettyServer)

(defmethod base/create :default
  [{:keys [url] :as m}])