;  Copyright (c) Peter Feldtmann, 2013. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns 
  ^{:author "Peter Feldtmann"
    :doc "Reindeer utility functions"}
  clj.reindeer.util
  (:require clojure.string)
  (:use [i18n.core])
  (:import [java.net URL URI MalformedURLException URISyntaxException]
           [javax.servlet ServletContext]
           [com.vaadin.server VaadinSession DeploymentConfiguration]
           [com.vaadin.ui AbstractComponentContainer])
)

;  Partly "borrowed" and adapted from project seesaw.
;  Thanks to Dave Ray for his great work!

(defn illegal-argument 
  "Throw an illegal argument exception formatted as with (clojure.core/format)"
  [fmt & args]
  (throw (IllegalArgumentException. ^String (apply format fmt args))))

(defn check-args 
  [condition message]
  (if-not condition
    (throw (IllegalArgumentException. ^String message))
    true))

(defn root-cause
  [^Throwable e]
  (if-let [cause (.getCause e)]
    (root-cause cause)
    e))

(defmacro cond-doto
  "Spawn of (cond) and (doto). Works like (doto), but each form has a condition
   which controls whether it is executed. Returns x.

  (doto (new java.util.HashMap) 
    true    (.put \"a\" 1) 
    (< 2 1) (.put \"b\" 2))
  
  Here, only (.put \"a\" 1) is executed.
  "
  [x & forms]
    (let [gx (gensym)]
      `(let [~gx ~x]
         ~@(map (fn [[c a]]
                  (if (seq? a)
                    `(when ~c (~(first a) ~gx ~@(next a)))
                    `(when ~c (~a ~gx))))
                (partition 2 forms))
         ~gx)))

(defn to-seq [v]
  "Stupid helper to turn possibly single values into seqs"
  (cond 
    (nil? v) v
    (seq? v)  v
    (coll? v) (seq v)
    (.isArray (class v)) (seq v)
    :else (seq [v])))

(defn- constantize-keyword [k]
  (.. (name k) (toUpperCase) (replace "-" "_")))

(defn constant-map
  "Given a class and a list of keywordized constant names returns the 
   values of those fields in a map. The name mapping upper-cases and replaces
   hyphens with underscore, e.g.
 
    :above-baseline --> ABOVE_BASELINE

   Note that the fields must be static and declared *in* the class, not a 
   supertype.
  "
  [^Class klass & fields]
  (let [[options fields] (if (map? (first fields)) [(first fields) (rest fields)] [{} fields])
        {:keys [suffix] :or {suffix ""}} options]
    (reduce
      (fn [m [k v]] (assoc m k v))
      {}
      (map 
        #(vector %1 (.. klass 
                      (getDeclaredField (str (constantize-keyword %1) suffix)) 
                      (get nil)))
        fields))))
    
  
(defn camelize
  "Convert input string to camelCase from hyphen-case"
  [s]
  (clojure.string/replace s #"-(.)" #(.toUpperCase ^String (%1 1))))

(defn boolean? [b]
  "Return true if b is exactly true or false. Useful for handling optional
   boolean properties where we want to do nothing if the property isn't 
   provided."
  (or (true? b) (false? b)))

(defn atom? [a]
  "Return true if a is an atom"
  (isa? (type a) clojure.lang.Atom))

(defn try-cast [c x]
  "Just like clojure.core/cast, but returns nil on failure rather than throwing ClassCastException"
  (try
    (cast c x)
    (catch ClassCastException e nil)))

(defn ^URL to-url [s]
  "Try to parse (str s) as a URL. Returns new java.net.URL on success, nil 
  otherwise. This is different from clojure.java.io/as-url in that it doesn't
  throw an exception and it uses (str) on the input."
  (if (instance? URL s) s
  (try
    (URL. (str s))
    (catch MalformedURLException e nil))))

(defn ^URI to-uri [s]
  "Try to make a java.net.URI from s"
  (cond
    (instance? URI s) s
    (instance? URL s) (.toURI ^URL s)
    :else (try
            (URI. (str s))
            (catch URISyntaxException e nil))))

(defprotocol Children 
  "A protocol for retrieving the children of a widget as a seq. 
  This takes care of idiosyncracies of frame vs. menus, etc."

  (children [c] "Returns a seq of the children of the given widget"))

(extend-protocol Children
 
  AbstractComponentContainer  (children [this] (iterator-seq (.iterator this)))
  
  )

(defn collect
  "Given a root widget or panel, returns a depth-fist seq of all the widgets
  in the hierarchy. For example to disable everything:
  
    (config! (collect (get-ui-content)) :enabled? false)
  "
  [root]
  (tree-seq 
    (constantly true) 
    children
    root))

(defn ^Integer to-mnemonic-keycode
  "Convert a character to integer to a mnemonic keycode. In the case of char
  input, generates the correct keycode even if it's lower case. Input argument 
  can be:

  * i18n resource keyword - only first char is used
  * string - only first char is used
  * char   - lower or upper case
  * int    - key event code
  
  See:
    java.awt.event.KeyEvent for list of keycodes
    http://download.oracle.com/javase/6/docs/api/java/awt/event/KeyEvent.html"
  [v]
  (cond 
    (i18n-key? v) (to-mnemonic-keycode (i18n v))
    (string? v)       (to-mnemonic-keycode (.charAt v 0))
    (char? v)         (int (Character/toUpperCase ^Character v))
    :else             (int v)))

;; These are "borrowed" and adapted from project org.lpetit.ring/ring-java-servlet
 
(defn require-and-resolve 
  [symbol-str]
  (let [[n h] (map symbol ((juxt namespace name) (symbol symbol-str)))]
    (require n)
    (ns-resolve (the-ns n) h)))

(defn get-context-params 
  "Given a servlet context, it returns a map with all the context parameters. Keys are keywords."
  [^ServletContext ctx]
  (into {} (for [param (enumeration-seq (.getInitParameterNames ctx))
                 :let [value (.getInitParameter ctx param)]]
             [(keyword param) value])))

(defn get-vaadin-param
  "Returns the session parameter from vaadin's web.xml or system property."
  [^VaadinSession session param-name default-value]
  (-> session .getConfiguration 
                           (.getApplicationOrSystemProperty param-name default-value)))

