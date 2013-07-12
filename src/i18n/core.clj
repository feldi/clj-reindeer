;;  Copyright (c) Peter Feldtmann All rights reserved.  The use and
;;  distribution terms for this software are covered by the Eclipse Public
;;  License 1.0 (http://opensource.org/licenses/eclipse-1.0.php), the same
;;  as clojure, which can be found in the file epl-v10.html at the root of 
;;  this distribution.  By using this software in any fashion, you are 
;;  agreeing to be bound by the terms of this license. You must not remove
;;  this notice, or any other, from this software.
;;
;;  i18n.core
;;
;;  Internationalization for Clojure
;;  using Java resource bundles and message formatting
;;
;;  Peter Feldtmann
;;  Started September 1 2012
;;
;;  based on the projects:
;;   j18n     :author "Meikel Brandmeyer <m@kotka.de>"
;;   clji18n: :author "Sebastián Galkin"

(ns
  ^{:author "Peter Feldtmann"
    :doc "Iternationalization for Clojure"}
  i18n.core
  (:require [clojure.string :as s])
  (:import java.text.MessageFormat
           java.util.ResourceBundle
           java.util.Set
           java.util.Locale))

(defn locale
  "Create a locale passing a language, optional country, and optional variant.
  Use two lowercase letters for the language, as defined by ISO-639.
  Use two uppercase letters for the country, asd defined by ISO-3166.
  Variant is vendor specific."
  ([]     (Locale/getDefault))
  ([lang] (locale lang nil nil))
  ([lang country] (locale lang country nil))
  ([lang country variant] {:pre [(not (s/blank? lang))]}
   (Locale. lang (or country "") (or variant ""))))

(def ^{:dynamic true
       :doc "The current locale for the application. You can rebind this var"}
  *i18n-locale* (locale))

(defmacro with-locale
  "Rebind *current-locale* var and evaluate body in that dynamic context."
  [locale & body]
  `(binding [~'*i18n-locale* ~locale]
     ~@body))

(defn format-message [msg args]
  "Replace message format placeholders {0}, {1} etc."
  (when (empty? args)
    msg)
  (let [mf (doto (MessageFormat. "")
             ;for some reason it won't work if I create the MessageFormat with the
             ;pattern, maybe I need to assign the locale before the pattern
             ;;(.setLocale (java-locale locale))
             (.setLocale *i18n-locale*)
             (.applyPattern msg))]
      (.format mf (to-array args))))

(def ^{:private true :tag Set} bundle-keys
  (memoize #(.keySet ^ResourceBundle %)))

(defn- i18n*
  "Internal helper for i18n"
  ([key]
     (i18n* key []))
   ([key args]
   {:pre [(keyword? key) (namespace key)]}
   (let [bundle (-> key namespace munge (ResourceBundle/getBundle *i18n-locale*))]
     (i18n* bundle key args )))
  ([^ResourceBundle bundle key args ]
   {:pre [(or (keyword? key) (string? key))]}
   (let [key (name key)]
     (when (.contains (bundle-keys bundle) key)
        (format-message (.getString bundle key) args)))))

(defn i18n-key?
  "Returns true if k is an i18n resource key, i.e. a namespaced keyword"
  [k]
  (and (keyword? k) (namespace k)))

(defn i18n
   "Look up the given key in a given bundle. A key is a fully qualified
  keyword. The namespace part of the keyword specifies the bundle. The name
  part of the keyword is finally looked up as the key in the bundle.

  Optionally the bundle might be given explicitly as first argument. In
  that case the key may also be an unqualified keyword or a string.
    
  The rest of the arguments are replacements for message format 
  placeholders {0}, {1} etc."
  [bundleOrKey & args]
  (cond (instance? ResourceBundle bundleOrKey) 
           (i18n* bundleOrKey (first args) (rest args))
        (i18n-key? bundleOrKey)
           (i18n* bundleOrKey args)
        :else (format-message bundleOrKey args)))


