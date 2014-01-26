(defproject clj-reindeer "0.3.1"
  :description "Clojure Vaadin bridge"
  :url "https://github.com/feldi/clj-reindeer"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.5.1"] 
                 [com.vaadin/vaadin-server "7.1.10"]
                 [com.vaadin/vaadin-client-compiled "7.1.10"]
                 [com.vaadin/vaadin-themes "7.1.10"]
                 [org.clojure/tools.nrepl "0.2.3"]]
  :profiles {:dev 
             {:dependencies
              [[javax.servlet/javax.servlet-api "3.1.0"]
               [midje "1.5.1"]]}}
  :aot [clj.reindeer.servlet
        clj.reindeer.ui])
