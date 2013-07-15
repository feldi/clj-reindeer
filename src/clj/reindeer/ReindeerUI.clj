(ns clj.reindeer.ReindeerUI
   (:use [clj.reindeer.util])
   (import [com.vaadin.ui UI VerticalLayout Label]
           [com.vaadin.server VaadinSession DeploymentConfiguration]
           )
  (:gen-class
    :extends com.vaadin.ui.UI)
)

(defn- create-main-layout
  []
  (doto (VerticalLayout.)
	        (.addComponent (Label. "Hello Clojure-Vaadin 7!"))
	       ))

(defn -init
  [this vaadin-request]
  
   (println "ReindeerUI: Building new Vaadin UI application through Clojure..." )
  (if-let [build-fn-name (-> this .getSession .getConfiguration 
                           (.getApplicationOrSystemProperty "buildfn" "missing") )]
    (if-let [build-fn (require-and-resolve build-fn-name)]
       (build-fn this vaadin-request)
       (throw (Exception. (str "build function not resolvable: " build-fn-name))))
    (throw (Exception. " no build function name defined: please set servlet init parameter 'buildfn'" )
  ))

;; (doto this (.setContent (create-main-layout)))
 
  (println "ReindeerUI: ... finished building new Vaadin UI application through Clojure." )

  )

