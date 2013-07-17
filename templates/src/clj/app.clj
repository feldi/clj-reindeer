;; example vaadin application

(ns clj.vdn.xxx.app
  (:use [clj.reindeer.core])
)


(defn create-content 
  [] 
  (v-l :spacing true 
       :items
               [(h-l :spacing true 
                     :items
                     [(label "Hello Vaadin/Clojure user!"
                             :description "Tooltip-desc")
                      (button :clj.xxx.app/buttonCaption1 
                              :click (fn [e] (alert :clj.xxx.app/alertMsg1)))
                      (button :clj.xxx.app/logoutButton 
                              :click (fn [e] (close-session "http://example.com/bye.html")))
                      ])
                (v-l :spacing true 
                     :items
                     [(label :caption "1. Label")
                      (text-field "1. " :width "30%" :description :clj.xxx.app/desc1
                                  :init-value "1111")
                      (label :caption "2. Label")
                      (text-field "2. " :width "20%" :description "Tooltip for 2. input"
                                  :init-value "2222")
                      ])
                ] ))

(defn init-app 
  [] 
 )

(defn launch-app 
  "This is the 'main entry point' to the Vaadin application."
  []
   (ui :title "Vaadin7 Wrapper Test"
       :init-fn init-app
       :content (create-content))
)
