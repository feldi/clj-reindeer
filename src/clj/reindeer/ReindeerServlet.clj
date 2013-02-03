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
   :extends com.vaadin.terminal.gwt.server.AbstractApplicationServlet))

(defn -getApplicationClass 
  [_] 
  (class com.vaadin.Application))

(defn -getNewApplication
  [this _] 
  (println "Building new Vaadin application through Clojure..." )
  (if-let [build-fn-name (.getApplicationProperty ^com.vaadin.Application this "buildfn")]
    (if-let [build-fn (require-and-resolve build-fn-name)]
       (build-fn)
       (throw (Exception. (str "build function not resolvable: " build-fn-name))))
    (throw (Exception. " no build function name defined: please set servlet init parameter 'buildfn'" )
  )))
