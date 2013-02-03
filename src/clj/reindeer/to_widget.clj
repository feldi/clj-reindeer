;  Copyright (c) Peter Feldtmann, 2013. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

;  "Borrowed" and adapted from project seesaw.
;  Thanks to Dave Ray for his great work!

(ns clj.reindeer.to-widget
  (:use [clj.reindeer.util :only [try-cast]])
  (:import [com.vaadin.ui AbstractComponent AbstractComponentContainer])
  )

(defprotocol ToWidget 
  (to-widget* [v]))

(defmacro ^{:private true} def-to-widget [t b & forms]
  `(extend-type 
     ~t
     ToWidget 
      (~'to-widget*   ~b ~@forms)))

(def-to-widget Object [c] nil)

(def-to-widget AbstractComponent [c] c)

(def-to-widget AbstractComponentContainer [c] c)

(def-to-widget java.util.EventObject 
  [v] 
  (try-cast AbstractComponent (.getSource v)))

