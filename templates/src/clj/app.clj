;; example vaadin application

(ns clj.vdn.xxx.app
  (:use [clj.reindeer.core])
)

(defn init-app 
  [] 
  (main-window :title :clj.xxx.app/windowtitle 
               :items
               [(h-l :spacing true 
                     :items
                     [(label "Hello Vaadin/Clojure user! #11#"
                             :description "Tooltip-desc")
                      (button :clj.xxx.app/buttonCaption1 
                              :click (fn [e] (alert :clj.xxx.app/alertMsg1)))
                      (button :clj.xxx.app/logoutButton 
                              :click (fn [e] (close-app)))
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

(defn on-request-start
  [request response] 
  (println "on-request-start called")
  )

(defn on-requeste-end
  [request response] 
  (println "on-request-end called")
  )

(defn user-changed
  [event]
  (println "user-changed called")
  )

(defn launch-app 
  "This is the 'main entry point' to the Vaadin application.
   It returns an instance of class com.vaadin.Application."
  []
  (build-app :main-theme "reindeer" 
             :init-fn init-app
             :user-changed-fn user-changed
             :on-request-start-fn on-request-start
             :on-request-end-fn on-request-end
             :logout-url "http://localhost:8085")
)
