(ns clj.reindeer.t_core
  (:use [clj.reindeer.core]
        [midje.sweet])
  (:import [com.vaadin.ui
            VerticalLayout
            Button
            Panel]))

(def on-click-called (ref false))

(background (before :facts (dosync (ref-set on-click-called false))))

(defn on-click [e]
  (dosync (ref-set on-click-called true)))

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

(facts "about labels"
  (fact "it has the right caption"
    (let [l (label "Caption")]
      (.getCaption l) => "Caption")))

(facts "about buttons"
  (let [b (button :caption "Caption" :on-click on-click)]
    (fact "it has the right caption"
      (.getCaption b) => "Caption")
    (fact "it calls the listener function"
      (.click b) => irrelevant 
      (deref on-click-called) => true)))
