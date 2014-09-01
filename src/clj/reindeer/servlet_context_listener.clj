;  Copyright (c) Peter Feldtmann, 2014. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns 
  ^{:doc "Servlet context listener for the clojure vaadin bridge."
    :author "Peter Feldtmann"}
clj.reindeer.servlet-context-listener
  (:use [clj.reindeer.util])
  (:gen-class
    :extends javax.servlet.ServletContextListener
    :name ^{javax.servlet.annotation.WebListener {}} 
clj.reindeer.ReindeerServletContextListener)
  (:import [javax.servlet
            ServletContext
            ServletContextEvent
            ServletContextListener
            ]
           [javax.servlet.annotation 
            WebListener
            ]
           ))

(defn -contextInitialized
  "Called automatically when the servlet context is ready."
  [^ServletContextEvent sce]
  (println "ReindeerServletContextListener: contextInitialized entered.")
  
;	(let [servlet-context (.getServletContext sce)
;		  config-fn (require-and-resolve "clj.reindeer.config-servlet-context!")
;	      config-parm-map (config-fn)]
;		(doseq [[k v] config-parm-map] 
;			(.setInitParameter servlet-context k v))
;			)
 )
   
(defn -contextDestroyed
  "Called automatically when the servlet context has been destroyed."
  [^ServletContextEvent sce]
  (println "ReindeerServletContextListener: contextDestroyed entered." )
  )