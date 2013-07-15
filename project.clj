(defproject clj-reindeer "0.3.1"
  :description "Clojure Vaadin bridge"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.5.1"] 
                 [com.vaadin/vaadin "6.8.4"]
                 ;; [j18n "1.0.1"]
                 [i18n "1.0.3"]]
  :profiles {:dev 
             {:dependencies
              [[javax.servlet/servlet-api "2.5"]
               [midje "1.5.1"]]}}
  :aot [clj.reindeer.ReindeerServlet]
)
