(ns clj.reindeer.t_core
  (:use [clj.reindeer.core]
        [midje.sweet])
  (:import [com.vaadin.ui
            VerticalLayout
            Button
            Panel
            TextField]
           [com.vaadin.server
            ExternalResource]))

(def listener-called (ref false))

(background (before :facts (dosync (ref-set listener-called false))))

(defn test-listen! [e]
  (dosync (ref-set listener-called true)))

(facts "about containers"
  (fact "it is parent after add"
    (let [p (VerticalLayout.)
          b (Button.)]
      (add! p b) => irrelevant
      (.getParent b) => p))
  (fact "it stores the expand ratio of a subcomponent"
    (let [p (VerticalLayout.)
          b (Button.)]
      (add! p b) => irrelevant
      (set-expand-ratio! p b 0.5) => irrelevant
      (.getExpandRatio p b) => (roughly 0.5)))
  (fact "it stores its content"
    (let [p (Panel.)
          c (VerticalLayout.)]
      (set-content! p c) => irrelevant
      (.getContent p) => c)))

(facts "about v-gaps"
  (fact "it has a width"
    (let [g (v-gap :width "30")]
      (.getWidth g) => 30.0))
  (fact "it has a height"
    (let [g (v-gap :height "40")]
      (.getHeight g) => 40.0)))

(facts "about h-gaps"
  (fact "it has a height"
    (let [c (VerticalLayout.)
          g (h-gap c :height "10")]
      (.getHeight g) => 10.0)))
  
(facts "about labels"
  (fact "it has the right caption"
    (let [l (label :caption "Caption")]
      (.getCaption l) => "Caption")))

(facts "about buttons"
  (let [b (button :caption "Caption" :on-click test-listen!)]
    (fact "it has the right caption"
      (.getCaption b) => "Caption")
    (fact "it calls the listener function"
      (.click b) => irrelevant 
      (deref listener-called) => true)))

(facts "about native buttons"
  (let [b (native-button :caption "Caption" :on-click test-listen!)]
    (fact "it has the right caption"
      (.getCaption b) => "Caption")
    (fact "it calls the listener function"
      (.click b) => irrelevant
      (deref listener-called) => true)))

(facts "about horizontal layouts"
  (let [b1 (button :caption "b1")
        b2 (button :caption "b2")
        h (h-l :items [b1 b2] :spacing true :style-name "foo")]
    (fact "it has items"
      (.getComponentCount h) => 2)
    (fact "it has spacing"
      (.isSpacing h) => true)
    (fact "it has a style"
      (.getStyleName h) => "foo")))

(facts "about vertical layouts"
  (let [b1 (button :caption "b1")
        b2 (button :caption "b2")
        v (v-l :items [b1 b2] :spacing true :style-name "foo")]
    (fact "it has items"
      (.getComponentCount v) => 2)
    (fact "it has spacing"
      (.isSpacing v) => true)
    (fact "it has a style"
      (.getStyleName v) => "foo")))

(facts "about fields"
  (fact "it sets the value"
    (let [f (TextField.)]
      (config! f :value "foo") => irrelevant
      (config f :value) => "foo"))
  (fact "it listens to value changes"
    (let [f (text-field :change-listener test-listen!)]
      (config! f :value "bar") => irrelevant
      (deref listener-called) => true)))

(facts "about links"
  (let [l (link :target-name "http://www.foobar.com" :caption "foo")]
     (fact "it has a target name"
      (.getTargetName l) => "http://www.foobar.com")
    (fact "it has a caption"
      (.getCaption l) => "foo")))

(facts "about embeddeds"
  (let [e (embedded (ExternalResource. "http://www.foobar.com") :caption "foo")]
    (fact "it has a caption"
      (.getCaption e) => "foo")))

(facts "about widgets"
  (let [b (doto (Button.) 
            (.setHeight "100") 
            (.setWidth "200"))]
    (fact "it knows its height"
      (height b) => (roughly 100))
    (fact "it knows its width"
      (width b) => (roughly 200)) 
    (fact "it can add a style name"
      (add-style-name b "bar") => irrelevant
      (.getStyleName b) => "bar")))
