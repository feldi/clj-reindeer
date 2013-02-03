(ns clj.vdn.xxx.ringhandler)

;; dummy needed for "lein ring uberwar"  

(defn dummy-handler [request]
  {:status 200
   :headers {"Content-Type" "text/plain"}
   :body "Dummy ring handler" })