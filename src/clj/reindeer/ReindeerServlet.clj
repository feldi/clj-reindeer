;  Copyright (c) Peter Feldtmann, 2013. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns 
  ^{:doc "Generic servlet for the clojure vaadin bridge."
    :author "Peter Feldtmann"}
  clj.reindeer.ReindeerServlet
  (:use [clj.reindeer.util])
  (:gen-class
   :extends com.vaadin.server.VaadinServlet
   ))

(defn- create-reindeer-ui-provider 
  "Helper: creates an UIProvider with the ability to override the theme name and widget set."
  [^com.vaadin.server.SessionInitEvent siEvent]
   (proxy [com.vaadin.server.DefaultUIProvider] [] 
        (getTheme 
          [^com.vaadin.server.UICreateEvent crtEvent]
          (get-vaadin-param (.getSession siEvent) "themeName" "reindeer")) ;; reindeer is default theme
         (getWidgetset 
          [^com.vaadin.server.UICreateEvent crtEvent]
          (get-vaadin-param (.getSession siEvent) "widgetset" nil)) 
        ))

(defn- create-session-init-listener
  "Helper: creates a vaadin session init listener with the new ui provider."
  []
  (reify com.vaadin.server.SessionInitListener
     (^void sessionInit
            [this ^com.vaadin.server.SessionInitEvent siEvent]
            (-> siEvent .getSession (.addUIProvider (create-reindeer-ui-provider siEvent))))))

(defn -servletInitialized
  "Called automatically by Vaadin when the servlet is ready."
  [this]
  ; (println "ReindeerServlet: servletInitialized entered." )
  (-> this .getService (.addSessionInitListener (create-session-init-listener)))
  )

