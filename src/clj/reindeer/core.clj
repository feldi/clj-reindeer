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
    :doc "A Clojure Vaadin Bridge.
          This is still work in progress."}
  clj.reindeer.core
  (:use [i18n.core :only [i18n-key? i18n]]
         [clj.reindeer.util :only [illegal-argument to-seq check-args
                                   constant-map
                                   to-dimension to-insets to-url try-cast
                                   cond-doto to-mnemonic-keycode]]
         [clj.reindeer.config :only [Configurable config* config!*]]
         [clj.reindeer.options :only [ignore-option default-option bean-option
                                      ;;; resource-option around-option
                                      apply-options
                                      option-map option-provider
                                      get-option-value
                                      options-for-class defwidget]]
          [clj.reindeer.to-widget :only [ToWidget to-widget*]]
         )
   (:import [java.net URL] 
            [javax.servlet.http HttpServletRequest HttpServletResponse]
            [com.vaadin Application Application$UserChangeListener Application$UserChangeEvent]
	          [com.vaadin.ui AbstractOrderedLayout 
                           Component Component$Listener
                           AbstractComponent AbstractComponentContainer 
                           Window Window$CloseListener Window$Notification
                           Label Button Button$ClickListener Button$ClickEvent
                           NativeButton OptionGroup
                           HorizontalLayout VerticalLayout
                           AbstractField TextField TextArea PasswordField RichTextArea
                           DateField InlineDateField PopupDateField
                           Link Embedded CheckBox
                           ]
            [com.vaadin.terminal Terminal Terminal$ErrorEvent 
             Resource ExternalResource ClassResource ThemeResource]
            [com.vaadin.event FieldEvents$TextChangeListener FieldEvents$TextChangeEvent]
            [com.vaadin.terminal.gwt.server HttpServletRequestListener]
            [com.vaadin.data Property$ValueChangeListener]
   )
)

(defn reload []
  "Helper for easy development at the repl."
   (use 'clj.reindeer.core :reload-all)
  )

(set! *warn-on-reflection* true) 


(declare add!)

(def vaadin-app (atom nil))

(defn ^Application get-app
  []
  @vaadin-app
)

(defn defapplication
  [& {:keys [main-theme
             init-fn
             close-fn
             user-changed-fn
             terminal-error-fn
             error-handler
             on-request-start-fn
             on-request-end-fn
             logout-url
             main-window
             app-windows] } ]
  (proxy [Application
          Application$UserChangeListener
          HttpServletRequestListener] []
    (init 
      []
      (reset! vaadin-app this) 
      (when main-theme
         (.setTheme ^Application this main-theme))
      (when logout-url
         (.setLogoutURL ^Application this logout-url))
      (when error-handler
         (.setErrorHandler ^Application this error-handler))
      (when init-fn
        (init-fn))
      (when main-window
        )
      (when app-windows
        )
     )
    (close 
      []
      (when close-fn
        (close-fn))
        (proxy-super close))
    (onRequestStart 
      [^HttpServletRequest request,	^HttpServletResponse response]
       (when on-request-start-fn
        (on-request-start-fn request response)))
    (onRequestEnd 
      [^HttpServletRequest request,	^HttpServletResponse response] 
        (when on-request-end-fn
          (on-request-end-fn request response)))
    (applicationUserChanged
      [^Application$UserChangeEvent event]
          (when user-changed-fn
            (user-changed-fn event)))
    (terminalError 
      [^Terminal$ErrorEvent event]
      (if terminal-error-fn
        (terminal-error-fn event)
        (proxy-super terminalError event)))
    ))

(defn close-application
  []
  (println "Closing Vaadin Application.")
  (.close ^Application (get-app))
)

(defn get-locale
  []
  (.getLocale (get-app)))


(defn- set-app-close-listener!
  [^Window win]
  (.addListener win 
    (reify Window$CloseListener
	    (windowClose [this event] (close-application))))
)

(defn set-main-window!
  [win] 
  (.setMainWindow (get-app) win)
  (set-app-close-listener! win)
  win
)

(defn ^Window get-main-window 
  []
 (.. (get-app) getMainWindow)
)

(defn remove-main-window!
  []
  (when (get-main-window)
    (.removeWindow (get-app) (get-main-window))))

(defn- set-win-close-listener!
  [^Window win func]
  (.addListener win
    (reify Window$CloseListener
	    (windowClose [this event]
        (func event)
      )))
)

(defn ^Window app-window 
  [& {:keys [title name theme height width position-x position-y 
             close-listener items] } ] 
  (let [win (Window. (or (i18n title) "TODO: set title"))]
    (when name       (.setName      win name))
    (when theme      (.setTheme     win theme))
    (when height     (.setHeight    win ^String height))
    (when width      (.setWidth     win ^String width))
    (when position-x (.setPositionX win position-x))
    (when position-y (.setPositionY win position-y))
    (when close-listener (set-win-close-listener! win close-listener))
	  (doseq [item items] 
	    (add! win item)
    )
  win))

(defmacro main-window
  [& args]
  `(set-main-window! (app-window ~@args))
  )

(defn remove-app-window!   
  [^Window win]
  (.removeWindow (get-app) win))

(defn ^Window sub-window
  [& {:keys [title closable? draggable? modal? resizable? resizeLazy? 
             height width position-x position-y 
             close-listener items] } ] 
  (let [win (Window. (or title "TODO: set title"))]
    (when closable?   (.setClosable    win closable?))  
    (when draggable?  (.setDraggable   win draggable?))
    (when modal?      (.setModal       win modal?))
    (when resizable?  (.setResizable   win resizable?))
    (when resizeLazy? (.setResizeLazy  win resizeLazy?))
    (when height      (.setHeight      win ^String height))
    (when width       (.setWidth       win ^String width))
    (when position-x  (.setPositionX   win position-x))
    (when position-y  (.setPositionY   win position-y))
    (when close-listener (set-win-close-listener! win close-listener))
	  (doseq [item items] 
	    (add! win item)
    )
  win))

(defn remove-sub-window!
  [^Window win ^Window sub-win]
  (.removeWindow win sub-win))

(defn open-url 
  "open a url on a new HTML target browser window or tab."
  [^URL url & target]
  (.open (get-main-window)
      (ExternalResource. url)
      (or target "_blank")))

(defn open-window
  "open a window on a new HTML target browser window or tab."
  [^Window win & target]
  (apply open-url 
         (.getURL win)
         target))

(defn ^AbstractComponentContainer add!
  [^AbstractComponentContainer c1
                    ^Component c2]
  (.addComponent c1 c2)
  c1
  )

(defn set-expand-ratio!
  [^AbstractOrderedLayout layout 
   ^Component c 
   ratio]
  (.setExpandRatio layout c (float ratio)))
 
;; Resource handling

(defn ^ThemeResource theme-res 
  [path]
  (ThemeResource. path)
  )

(defn ^ClassResource class-res 
  [path & {:keys [buffer-size cache-time] } ] 
  (let [res (ClassResource. path (get-app))]
    (when buffer-size (.setBufferSize res buffer-size))
    (when cache-time (.setCacheTime   res cache-time))
  res))

; TODO StreamResource and StreamSource


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; get/setText is a common method on many types, but not in any common interface :(

(defprotocol ConfigAbstractComponent
  "Protocol to hook into AbstractComponent"
  (set-caption!* [this v])
  (get-caption* [this])
  (set-data!* [this v])
  (get-data* [this])
  (set-description!* [this v])
  (get-description* [this])
  (set-width!* [this v])
  (get-width* [this])
  (set-icon!* [this r])
  (get-icon* [this])
)

(extend-protocol ConfigAbstractComponent
 
  com.vaadin.ui.AbstractComponent
    (set-caption!* [this v] (.setCaption this v))
    (get-caption* [this] (.getCaption this))
    (set-data!* [this v] (.setData this v))
    (get-data* [this] (.getData this))
    (set-description!* [this v] (.setDescription this v))
    (get-description* [this] (.getDescription this))
    (set-width!* [this v] (.setWidth this ^String v))
    (get-width* [this] (.getWidth this))
    (set-icon!* [this r] (.setIcon this ^Resource r))
    (get-icon* [this] (.getIcon this))
  )

(defn- ^String convert-text-value
  [v]
  (cond
    (nil? v)      v
    (string? v)   v
    (number? v)   (str v)
    (i18n-key? v) (i18n v)
    (instance? (class v) clojure.java.io/IOFactory) (slurp v)
    :else (str v)))

(defn- set-caption!
  "Internal use only"
  [this v]
  (set-caption!* this (convert-text-value v)))

(defn- get-caption
  "Internal use only"
  [this]
  (get-caption* this))

(defn- set-data!
  "Internal use only"
  [this v]
  (set-data!* this v))

(defn- get-data
  "Internal use only"
  [this]
  (get-data* this))

(defn- set-description!
  "Internal use only"
  [this v]
  (set-description!* this (convert-text-value v)))

(defn- get-description
  "Internal use only"
  [this]
  (get-description* this))

(defn- set-width!
  "Internal use only"
  [this v]
  (set-width!* this v))

(defn- get-width
  "Internal use only"
  [this]
  (get-width* this))

(defn- set-icon!
  "Internal use only"
  [this r]
  (set-icon!* this (theme-res r)))

(defn- get-icon
  "Internal use only"
  [this]
  (get-icon* this))


(defn listen!
  [^AbstractComponent c 
   ^Component$Listener l]
  (.addListener c l)
)

(defn- set-button-click-listener!
  [^Button btn func]
  (.addListener btn
    (reify Button$ClickListener
	    (buttonClick [this event]
        (func event)
      )))
)

(def default-options
  (option-map
    ;(default-option :id seesaw.selector/id-of! seesaw.selector/id-of ["A keyword id for the widget"])
    ;(default-option :class seesaw.selector/class-of! seesaw.selector/class-of [:class-name, #{:multiple, :class-names}])
    ;(default-option
    ;  :meta-data
    ;  (fn [c v] (put-meta c ::user-data v))
    ;  (fn [c]   (get-meta c ::user-data))
    ;  ["Anything."
    ;   "Associate meta-data with a widget."
    ;   ])
    (default-option :caption set-caption! get-caption ["A string" "Anything accepted by (clojure.core/slurp)"])
    (default-option :description set-description! get-description ["A string" "Anything accepted by (clojure.core/slurp)"])
    (default-option :data set-data! get-data ["Anything." "Associate arbitrary user-data with a widget."])
    (default-option :width set-width! get-width ["A string" "A width as string, e.g in percent"])
    (default-option :icon set-icon! get-icon ["An icon" "Icon from theme resource"])
   ))

(def label-options
  (merge
    default-options
    (option-map)))

(option-provider Label label-options)

(defn ^Label label
  [& args]
  (case (count args)
    0 (label :caption "")
    1 (label :caption (first args))
    (apply-options (Label. "") args)))

(defn v-gap 
  "Vertical spacer with optional height and width."
  [& {:keys [height width] } ]
  (let [^Label gap (Label. "&nbsp;" ^int Label/CONTENT_XHTML )]
    (when height (.setHeight gap ^String height))
    (when width  (.setWidth  gap ^String width))
    gap)
  )

(defn h-gap 
  "Horizontal, expanding spacer."
  [^AbstractOrderedLayout layout & {:keys [height] } ]
  (let [^Label gap (Label. "&nbsp;" ^int Label/CONTENT_XHTML)]
    (.setWidth  gap "100%")
    (when height (.setHeight gap ^String height))
    (add! layout gap)
    (set-expand-ratio! layout gap 1.0)
    gap)
  )

(def button-options
  (merge
    default-options
    (option-map
      (default-option :on-click set-button-click-listener! nil ["A button click listener." ""])
      )))

(option-provider Button button-options)

(defn ^Button button
  [& args ]
   (case (count args)
    0 (button :caption "")
    1 (button :caption (first args))
    (apply-options (Button. "") args))
)

(defn ^NativeButton native-button
  [& args ]
   (case (count args)
    0 (button :caption "")
    1 (button :caption (first args))
    (apply-options (NativeButton. "") args))
)

(defn ^HorizontalLayout h-l
  [& {:keys [items spacing] } ]
   (let [h (HorizontalLayout.)]
     (when spacing (.setSpacing h spacing))
     (doseq [item items] 
       (add! h item))
     h)
)

(defn ^VerticalLayout v-l
  [& {:keys [items spacing] } ]
   (let [v (VerticalLayout.)]
     (when spacing (.setSpacing v spacing))
     (doseq [item items] 
       (add! v item)
    )
  v)
)

;; Fields

(defn set-value!
  "Generic field value setter."
  [^AbstractField this ^Object v]
  (.setValue this v))

(defn get-value
  "Generic field value getter."
  [^AbstractField af]
  (.getValue af))

(defn- set-value-change-listener!
  [^AbstractField af func]
  (.addListener af
    (reify Property$ValueChangeListener
	    (valueChange [this event]
        (func event)
      ))))

(defn- set-text-change-listener!
  [^TextField tf func]
  (.addListener tf
    (reify FieldEvents$TextChangeListener
	    (textChange [this event]
        (func event))
     )))

(defwidget text-field TextField
  [default-options]
  [(option-map
      (default-option :init-value set-value! nil ["initial value" ""])
      (default-option :text-change-listener set-text-change-listener! nil ["A text change listener." ""])
      (default-option :change-listener set-value-change-listener! nil ["A value change listener." ""])
      )])
  
(defwidget text-area TextArea
  [default-options]
  [(option-map
      (default-option :init-value set-value! nil ["initial value" ""])
      (default-option :text-change-listener set-text-change-listener! nil ["A text change listener." ""])
      (default-option :change-listener set-value-change-listener! nil ["A value change listener." ""])
      )])

(defwidget password-field PasswordField
  [default-options]
  [(option-map
      (default-option :init-value set-value! nil ["initial value" ""])
      (default-option :text-change-listener set-text-change-listener! nil ["A text change listener." ""])
      (default-option :change-listener set-value-change-listener! nil ["A value change listener." ""])
      )])

(defwidget rich-text-area RichTextArea
  [default-options]
  [(option-map
      (default-option :init-value set-value! nil ["initial value" ""])
      (default-option :text-change-listener set-text-change-listener! nil ["A text change listener." ""])
      (default-option :change-listener set-value-change-listener! nil ["A value change listener." ""])
      )])

(defwidget check-box CheckBox
  [default-options]
  [(option-map
      (default-option :init-value set-value! nil ["initial value" ""])
      (default-option :change-listener set-value-change-listener! nil ["A value change listener." ""])
      )])

;; Date and Time 

(defwidget date-field DateField
  [default-options]
  [(option-map
      (default-option :init-value set-value! nil ["initial value" ""])
      (default-option :change-listener set-value-change-listener! nil ["A value change listener." ""])
      )])

(defwidget popup-date-field PopupDateField
  [default-options]
  [(option-map
      (default-option :init-value set-value! nil ["initial value" ""])
      (default-option :change-listener set-value-change-listener! nil ["A value change listener." ""])
      )])

(defwidget inline-date-field InlineDateField
  [default-options]
  [(option-map
      (default-option :init-value set-value! nil ["initial value" ""])
      (default-option :change-listener set-value-change-listener! nil ["A value change listener." ""])
      )])

;; Notifications

(defn alert
  [msg]
  (.. (get-main-window) (showNotification (convert-text-value msg))))

(defn show-notification
  "Generic show method."
  ([caption msg type]
    (show-notification (get-main-window) caption msg type))
  ([^Window win caption msg type]
    (.. win (showNotification (convert-text-value caption)
                              (convert-text-value msg)
                              type))))

(defn show
  "Shows humanized message."
  ([caption msg]
  (show-notification (get-main-window) caption msg
                     Window$Notification/TYPE_HUMANIZED_MESSAGE))
  ([^Window win caption msg]
  (show-notification win caption msg
                     Window$Notification/TYPE_HUMANIZED_MESSAGE)))

(defn show-warning
  "Shows warning message."
  ([caption msg]
  (show-notification caption msg
         Window$Notification/TYPE_WARNING_MESSAGE))
  ([^Window win caption msg]
    (show-notification win caption msg
         Window$Notification/TYPE_WARNING_MESSAGE)))

(defn show-error
  "Shows error message."
  ([caption msg]
    (show-notification caption msg
                       Window$Notification/TYPE_ERROR_MESSAGE))
  ([^Window win caption msg]
    (show-notification win caption msg
                       Window$Notification/TYPE_ERROR_MESSAGE)))

(defn show-tray
  "Shows message in system tray area."
  ([caption msg]
    (show-notification caption msg
                       Window$Notification/TYPE_TRAY_NOTIFICATION))
  ([^Window win caption msg]
    (show-notification win caption msg
                       Window$Notification/TYPE_TRAY_NOTIFICATION)))

; TODO implement Window$Notification with options
(defn create-notification
  [caption msg type]
  (Window$Notification. caption msg type)
  )

(def link-options
  (merge
    default-options
    (option-map)))

(option-provider Link link-options)

(defn ^Link link
  [^String res & args]
  (case (count args)
    0 (link res :caption "")
    1 (link res :caption (first args))
    (apply-options (Link. nil (ExternalResource. res))
                   args)))

(option-provider Embedded link-options)

(defn ^Embedded embedded
  [^Resource res & args]
  (case (count args)
    0 (embedded res :caption "")
    1 (embedded res :caption (first args))
    (apply-options (Embedded. nil res)
                   args)))

;*******************************************************************************
; Widget configuration stuff

(def ^{:doc (str "Alias of clj.reindeer.config/config:\n" (:doc (meta #'clj.reindeer.config/config)))}
      config clj.reindeer.config/config)

(def ^{:doc (str "Alias of clj.reindeer.config/config!:\n" (:doc (meta #'clj.reindeer.config/config!)))} 
     config! clj.reindeer.config/config!)

(extend-protocol Configurable
 
  AbstractComponent
    (config* [target name]  (get-option-value target name))
    (config!* [target args] (apply-options target args))

  Window
    (config* [target name]  (get-option-value target name))
    (config!* [target args] (apply-options target args))
    
   ;; todo: more?
)

(defn set-text!
  "Set a text value."
  [this v]
  (set-value! this (convert-text-value v)))

(defn- get-text
  "Get a text value."
  [this]
  (get-value this))

(defn text!
  "Set the text of widget(s) or document(s). targets is an object that can be
  turned into a widget or document, or a list of such things. value is the new
  text value to be applied. Returns targets.

  target may be one of:

    A widget
    A widget-able thing like an event
    A Document
    A DocumentEvent

  The resulting text in the widget depends on the type of value:

    A string                               - the string
    A URL, File, or anything \"slurpable\" - the slurped value
    Anythign else                          - (resource value)

  Example:

      user=> (def t (text \"HI\"))
      user=> (text! t \"BYE\")
      user=> (text t)
      \"BYE\"

      ; Put the contents of a URL in editor
      (text! editor (java.net.URL. \"http://google.com\"))

  Notes:

    This applies to the :value property of new text widgets and config! as well.
  "
  [targets value]
  (check-args (not (nil? targets)) "First arg must not be nil")
  (doseq [w (to-seq targets)]
    (set-text! w value))
  targets)

;*******************************************************************************
; Widget coercion prototcol

(defn ^AbstractComponent to-widget
  "Try to convert the input argument to a widget based on the following rules:

    nil -> nil
    AbstractComponent -> return argument unchanged
    java.util.EventObject -> return the event source

  See:
    (clj.reindeer.to-widget)
  "
  ([v] (when v (to-widget* v))))


;*******************************************************************************
; Generic widget stuff

(defprotocol Showable
  (visible! [this])
  (not-visible! [this])
  (visible? [this]))

(extend-protocol Showable
  AbstractComponent
    (visible! [this] (doto this (.setVisible true)))
    (not-visible! [this] (doto this (.setVisible false)))
    (visible? [this] (.isVisible this))
  
  java.util.EventObject
    (visible! [this] (visible! (.getSource this)))
    (not-visible! [this] (not-visible! (.getSource this)))
    (visible? [this] (visible? (.getSource this))))

(defn width
  "Returns the width of the given widget."
  [w]
  (.getWidth (to-widget w)))

(defn height
  "Returns the height of the given widget."
  [w]
  (.getHeight (to-widget w)))

(defn add-style-name
  "Adds a named css style to a component." 
  [^Component c 
   ^String n]
  (.addStyleName c n)
)

(defn remove-style-name
  "Removes a named css style from a component." 
  [^Component c 
   ^String n]
  (.removeStyleName c n)
)

(defn ^OptionGroup option-group
  "Gruped radio buttons." 
  [& {:keys [caption items ] } ]
   (let [^OptionGroup og (OptionGroup. )]
     (when caption (.setCaption og caption))
     (doseq [item items] 
       (.addItem og item))
     og))

(defn ^OptionGroup option-group-multi
  "Grouped check boxes with multiple selections." 
  [& {:keys [caption items ] } ]
   (let [^OptionGroup og (OptionGroup. )]
     (when caption (.setCaption og caption))
     (.setMultiSelect og true) 
     (doseq [item items] 
       (.addItem og item))
     og))

;; aliases
(def radio-buttons option-group)
(def check-boxes option-group-multi)



