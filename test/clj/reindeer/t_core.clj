(ns clj.reindeer.t_core
  (:use [clj.reindeer.core]
        [midje.sweet]))

(facts "about labels"
  (let [l (label "Caption")]
    (.getCaption l) => "Caption"))
