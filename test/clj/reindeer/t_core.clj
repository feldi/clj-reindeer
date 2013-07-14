(ns clj.reindeer.t_core
  (:use [clj.reindeer.core]
        [midje.sweet]))

(def on-click-called (ref false))

(background (before :facts (dosync (ref-set on-click-called false))))

(defn on-click [e]
  (dosync (ref-set on-click-called true)))

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
