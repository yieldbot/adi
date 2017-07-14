(ns spirit.http.util)

(defn open-browser [{:keys [protocol host port]
                     :or {host "localhost"
                          protocol "http"}}]
  (let [uri (format "%s://%s:%s" protocol host (str port))]
    (try
      (.browse (java.awt.Desktop/getDesktop) (java.net.URI. uri))
      (catch java.awt.HeadlessException _))))