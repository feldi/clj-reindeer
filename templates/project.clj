(defproject clj-vdn-zodmap "0.1.0-SNAPSHOT"
  :description "Vaadin ZodMap app in clojure"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [clj-reindeer "0.2.0"]
                 [ring "1.1.6"]
                 [org.vaadin.addons/googlemapwidget "0.9.13"]
                 [org.vaadin.vol/openlayers-wrapper "1.2.0"]
				 [com.google.maps/gwt-maps "1.1.1"]
                 ]
   :repositories {"vaadin-addons"
                 "http://maven.vaadin.com/vaadin-addons"}
  :plugins [[lein-ring "0.7.5"]
            [lein-localrepo "0.4.1"]]
  :aot [clj.zodmap.VaadinServlet]
  :ring {:handler clj.zodmap.ringhandler/dummy-handler,
         :adapter {:port 3000},
         :web-xml "src/resources/web.xml",
         :war-exclusions [#"javax.servlet-2.5.0.v201103041518.jar"]}
  :uberjar-name "clj#vdn#zodmap.war"
  :war-resources-path "war-resources"
  )


