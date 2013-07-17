;; example project.clj for your project named "clj.vdn.xxx" 

(defproject clj-vdn-xxx "0.1.0-SNAPSHOT"
  :description "Vaadin app in clojure"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [com.vaadin/vaadin-server "7.1.0"]
                 [com.vaadin/vaadin-client "7.1.0"]
                 [com.vaadin/vaadin-client-compiled "7.1.0"]
                 [com.vaadin/vaadin-client-compiler "7.1.0"]
                 [com.vaadin/vaadin-themes "7.1.0"]
                 [com.vaadin/vaadin-theme-compiler "7.1.0"]
                 [clj-reindeer "0.3.1"]
                ]
   :repositories {"vaadin-addons"
                 "http://maven.vaadin.com/vaadin-addons"}
  :plugins [ [lein-localrepo "0.4.1"]]
  )


