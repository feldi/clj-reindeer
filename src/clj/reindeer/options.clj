;  Copyright (c) Peter Feldtmann, 2013. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

;  "Borrowed" and adapted from projects seesaw and upshot.
;  Thanks to Dave Ray for his great works!

(ns ^{:doc "Functions for dealing with options."
      :author "Peter Feldtmann, Dave Ray"}
  clj.reindeer.options
  (:use [clj.reindeer.util :only [camelize illegal-argument check-args
                                  resource resource-key? to-seq]])
  (:require [clojure.string]))


(defprotocol OptionProvider
  (get-option-maps* [this]))

(defn get-option-map [this]
  (apply merge (get-option-maps* this)))

(defmacro option-provider [class options]
  `(extend-protocol OptionProvider 
     ~class
     (~'get-option-maps* [this#] [~options])))

(defrecord Option [name setter getter examples])

(declare apply-options)

(defn- strip-question-mark
  [^String s] 
  (if (.endsWith s "?")
    (.substring s 0 (dec (count s)))
    s))

(defn- setter-name [property]
  (->> property 
    name 
    strip-question-mark
    (str "set-") 
    camelize 
    symbol))

(defn- getter-name [property]
  (let [property (name property)
        prefix   (if (.endsWith property "?") "is-" "get-")]
    (->> property 
      name 
      strip-question-mark
      (str prefix) 
      camelize 
      symbol)))

(defn- split-bean-option-name [v]
  (cond
    (vector? v) v
    :else [v v]))

(defmacro bean-option 
  [name-arg target-type & [set-conv get-conv examples]]
  (let [[option-name bean-property-name] (split-bean-option-name name-arg)
        target (gensym "target")]
  `(Option. ~option-name 
      (fn [~(with-meta target {:tag target-type}) value#]
        (. ~target ~(setter-name bean-property-name) (~(or set-conv `identity) value#)))
      (fn [~(with-meta target {:tag target-type})]
        (~(or get-conv `identity) (. ~target ~(getter-name bean-property-name))))
      ~examples)))

(defn default-option 
  ([name] (default-option name (fn [_ _] (illegal-argument "No setter defined for option %s" name))))
  ([name setter] (default-option name setter (fn [_] (illegal-argument "No getter defined for option %s" name))))
  ([name setter getter] (default-option name setter getter nil))
  ([name setter getter examples] (Option. name setter getter examples)))

(defn ignore-option
  "Might be used to explicitly ignore the default behaviour of options."
  ([name examples] (default-option name (fn [_ _]) (fn [_ _]) "Internal use."))
  ([name] (ignore-option name nil)))

(defn resource-option 
  "Defines an option that takes a j18n namespace-qualified keyword as a
  value. The keyword is used as a prefix for the set of properties in
  the given key list. This allows subsets of widget options to be configured
  from a resource bundle.
  
  Example:
    ; The :resource property looks in a resource bundle for 
    ; prefix.text, prefix.foreground, etc.
    (resource-option :resource [:text :foreground :background])
  "
  [option-name keys]
  (default-option 
    option-name 
    (fn [target value]
      {:pre [(resource-key? value)]}
      (let [nspace (namespace value)
            prefix (name value)]
            (apply-options
              target (mapcat (fn [k]
                              (let [prop (keyword nspace (str prefix "." k))]
                                (when-let [v (resource prop)]
                                  [(keyword k) v])))
                            (map name keys)))))
    nil
    [(str "A i18n prefix for a resource with keys") 
     (pr-str keys)]))

(defn- apply-option
  [target ^Option opt v]
  (if-let [setter (:setter opt)] 
    (setter target v)
    (illegal-argument "No setter found for option %s" (:name opt))))

(defn- ^Option lookup-option [target handler-maps name]
  ;(println "---------------------------")
  ;(println handler-maps)
  (if-let [opt (some #(if % (% name)) handler-maps)]
    opt
    (illegal-argument "%s does not support the %s option" (class target) name)))

(defn- apply-options*
  [target opts handler-maps]
  (let [pairs (if (map? opts) opts (partition 2 opts))] 
    (doseq [[k v] pairs]
      (let [opt (lookup-option target handler-maps k)]
        (apply-option target opt v))))
  target)

(defn apply-options
  [target opts]
  (check-args (or (map? opts) (even? (count opts))) 
              "opts must be a map or have an even number of entries")
  (apply-options* target opts (get-option-maps* target)))

(defn ignore-options
  "Create a ignore-map for options, which should be ignored. Ready to
  be merged into default option maps."
  [source-options]
  (into {} (for [k (keys source-options)] [k (ignore-option k)])))

(defn around-option
  ([parent-option set-conv get-conv examples]
  (default-option (:name parent-option)
    (fn [target value]
      ((:setter parent-option) target ((or set-conv identity) value)))
    (fn [target]
      ((or get-conv identity) ((:getter parent-option) target)))
    examples))
  ([parent-option set-conv get-conv]
   (around-option parent-option set-conv get-conv nil)))

(defn option-map 
  "Construct an option map from a list of options."
  [& opts]
  (into {} (map (juxt :name identity) opts)))

(defn get-option-value 
  ([target name] (get-option-value target name (get-option-maps* target)))
  ([target name handlers]
    (let [^Option option (lookup-option target handlers name)
          getter (:getter option)]
      (if getter
        (getter target)
        (illegal-argument "Option %s cannot be read from %s" name (class target))))))

(defn set-option-value
  ([target name value] (set-option-value target name (get-option-maps* target)))
  ([target name value handlers]
    (let [^Option option (lookup-option target handlers name)
          setter (:setter option)]
      (if setter
        (setter target value)
        (illegal-argument "Option %s cannot be set on %s" name (class target))))))


(defn- dash-case
  [^String s]
  (let [gsub (fn [s re sub] (.replaceAll (re-matcher re s) sub))]
    (-> s
      (gsub #"([A-Z]+)([A-Z][a-z])" "$1-$2")
      (gsub #"([a-z]+)([A-Z])" "$1-$2")
      (.replace "_" "-")
      (clojure.string/lower-case))))

(defn- get-option-info [m]
  (if (and (= 1 (count (.getParameterTypes m)))
          (.matches (.getName m) "^set[A-Z].*"))
    (let [base-name (.substring (.getName m) 3)
          type      (first (.getParameterTypes m))
          dash-name (dash-case base-name)
          boolean?  (= Boolean/TYPE type)]
      { :setter (symbol  (.getName m))
        :getter (symbol  (str (if boolean? "is" "get") base-name))
        :name   (keyword (if boolean?
                           (str dash-name "?")
                           dash-name))
        :type   type
        :enum   (.getEnumConstants type) })))

(defn get-public-instance-methods [class]
  (->> class
    .getDeclaredMethods
    (remove #(.isSynthetic %))
    (filter #(let [ms (.getModifiers %)]
               (= java.lang.reflect.Modifier/PUBLIC
                  (bit-and ms
                           (bit-or java.lang.reflect.Modifier/PUBLIC
                                   java.lang.reflect.Modifier/STATIC)))))))

(defmacro options-for-class-helper [class]
  `(option-map
     ~@(for [{:keys [setter getter name event type enum paint]}
             (->> (resolve class)
               get-public-instance-methods
               (map get-option-info)
               (filter identity))]
         (cond
           enum `(let [set-conv# ~(into {} (for [e enum]
                                             [(keyword (dash-case (.name e)))
                                              (symbol (.getName type) (.name e)) ]))
                       get-conv# (clojure.set/map-invert set-conv#)
                       ]
                   (default-option
                      ~name
                      (fn [ c# v#]
                        (.. c# (~setter (set-conv# v# v#))))
                      (fn [ c# ]    (get-conv# (.. c# ~getter)))
                     (keys set-conv#)))
           :else (let [ target (gensym "target")
                        value  (gensym "value")
                        ;; Xxx.setHeight needs extra treatment for reflection, since it is overloaded
                        value-meta (if (= setter 'setHeight)
                                       {:tag java.lang.String}
                                       {})]
                    `(default-option
                      ~name
                      (fn [~(with-meta target {:tag (resolve class)}) ~(with-meta value value-meta)] (. ~target  (~setter ~value)))
                      (fn [~(with-meta target {:tag (resolve class)})   ] (. ~target ~getter))
                      [~type]))))))

;; strange : only this lets the with-meta above work to get rid of reflection warnings... 
(defmacro options-for-class [class]
  `(binding [*print-meta* true]
     (eval (read-string (str 
          (macroexpand '(options-for-class-helper ~class))
       )))))

;*******************************************************************************

; A macro to handle most of the boilerplate for each kind of widget
(defmacro defwidget [func-name class-or-construct
                     base-options extra-options]
  (let [opts-name (symbol (str (name func-name) "-options"))
        class (if (symbol? class-or-construct)
                class-or-construct
                (first class-or-construct))
        args  (if (symbol? class-or-construct)
                []
                (rest class-or-construct))]
    `(do
       (def ~opts-name
         (merge
           ~@base-options
           (options-for-class ~class)
           ~@extra-options))

       (option-provider ~class ~opts-name)
      
       (defn ~func-name
         [& opts#]
         (case (count opts#)
           0 (~func-name :caption "")
           1 (~func-name :caption (first opts#))
           (apply-options (new ~class ~@args) opts#))
         ))))


;; These are "borrowed" and adapted from project seesaw/dev

(defn- examples-str [examples]
  (clojure.string/join (format "%n  %24s  " "") (to-seq examples)))

(defn show-options
  "Given an object, print information about the options it supports. These
  are all the options you can legally pass to (clj.reindeer.core/config) and
  friends."
  [v]
  (printf "%s%n" (.getName (class v)))
  (printf "  %24s  Notes/Examples%n" "Option")
  (printf "--%24s  --------------%n" (apply str (repeat 24 \-)))
  (doseq [{:keys [name setter examples]} (sort-by :name (vals (get-option-map v)))]
    (printf "  %24s  %s%n" 
            name
            (if examples (examples-str examples) ""))))
