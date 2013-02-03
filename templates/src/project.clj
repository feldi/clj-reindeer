;; example project.clj for your project named "clj.vdn.xxx" 

(defproject clj-vdn-xxx "0.1.0-SNAPSHOT"
  :description "Vaadin app in clojure"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [com.vaadin/vaadin "6.8.4"]
                 [clj-reindeer "0.3.0"]
                 [ring "1.1.6"]
                ]
   :repositories {"vaadin-addons"
                 "http://maven.vaadin.com/vaadin-addons"}
  :plugins [[lein-ring "0.7.5"]
            [lein-localrepo "0.4.1"]]
  ;; dummy needed for "lein ring uberwar"  
  :ring {:handler clj.vdn.xxx.ringhandler/dummy-handler,
         :web-xml "src/resources/web.xml",
         :war-exclusions [#"javax.servlet-2.5.0.v201103041518.jar"]}
  :uberjar-name "clj#vdn#xxx.war"
  :war-resources-path "war-resources"
  )


