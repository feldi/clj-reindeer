(ns clj.reindeer.ReindeerUI
   (:use [clj.reindeer.util])
   (import [com.vaadin.ui UI])
  (:gen-class
    :extends com.vaadin.ui.UI)
)

(defn -init
  [this vaadin-request]
  
  ;; (println "ReindeerUI: Building new Vaadin UI application through Clojure..." )
  
  (if-let [init-fn-name (-> this .getSession (get-vaadin-param "initFn" "missing") )]
    (if-let [init-fn (require-and-resolve init-fn-name)]
       (init-fn this vaadin-request)
       (throw (Exception. (str "vaadin init function not resolvable: " init-fn-name))))
    (throw (Exception. " no vaadin init function name defined: please set servlet init parameter 'initFn'" )
  ))
 
  ;; (println "ReindeerUI: ... finished building new Vaadin UI application through Clojure." )
  )

