(ns clj.reindeer.ui
   (:use [clj.reindeer.util])
   (import [com.vaadin.ui UI])
  (:gen-class
    :extends com.vaadin.ui.UI
    :name clj.reindeer.ReindeerUI))

(defn -init
  [this vaadin-request]
  (if-let [init-fn-name (-> this .getSession (get-vaadin-param "initFn" "missing") )]
    (if-let [init-fn (require-and-resolve init-fn-name)]
       (init-fn this vaadin-request)
       (throw (Exception. (str "vaadin init function not resolvable: " init-fn-name))))
    (throw (Exception. " no vaadin init function name defined: please set servlet init parameter 'initFn'" ))))
 

