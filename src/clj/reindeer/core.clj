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
    :doc "A Clojure Vaadin bridge.
          This is still work in progress."}
  clj.reindeer.core
  (:use [clojure.set :only [map-invert]]
        [i18n.core   :only [i18n-key? i18n]]
        [clj.reindeer.util :only [illegal-argument to-seq check-args
                                  def-constants-handler
                                  to-url try-cast
                                  cond-doto to-mnemonic-keycode]]
        [clj.reindeer.config  :only [Configurable config* config!*]]
        [clj.reindeer.options :only [ignore-option default-option bean-option
                                     apply-options
                                     option-map option-provider
                                     get-option-value
                                     options-for-class defwidget]]
        [clj.reindeer.to-widget :only [ToWidget to-widget*]]
        [clojure.tools.nrepl.server :only [start-server stop-server]])
   (:import [java.net 
             URL] 
            [javax.servlet.http 
             HttpServletRequest HttpServletResponse
             Cookie]
	          [com.vaadin.ui
             UI JavaScript
             AbstractOrderedLayout AbstractSelect
             Component Component$Listener ComponentContainer
             AbstractComponent AbstractComponentContainer 
             AbstractSingleComponentContainer Panel Alignment
             Window Window$CloseListener
             Label Button Button$ClickListener Button$ClickEvent
             NativeButton OptionGroup
             AbstractOrderedLayout HorizontalLayout VerticalLayout
             AbstractField AbstractTextField TextField TextArea PasswordField RichTextArea
             DateField InlineDateField PopupDateField
             Link Embedded CheckBox
             Notification Notification$Type
             Table Table$ColumnHeaderMode]
            [com.vaadin.server
             VaadinSession WrappedSession VaadinService Page
             VaadinRequest VaadinResponse
             Resource ExternalResource ClassResource ThemeResource
             UserError]
            [com.vaadin.event
             FieldEvents$TextChangeListener FieldEvents$TextChangeEvent
             ItemClickEvent ItemClickEvent$ItemClickListener]
            [com.vaadin.shared.ui.label
             ContentMode]
            [com.vaadin.data
             Property Property$ValueChangeListener
             Item Container]
            [com.vaadin.data.util
             ObjectProperty IndexedContainer]))

(defn reload []
  "Helper for easy development at the REPL."
   (use 'clj.reindeer.core :reload-all))

; this is NOT a pure Clojure project, but heavy Java related,
; so it should run as fast as Java if possible! 
(set! *warn-on-reflection* true) 

(declare add-components! set-expand-ratio!)

; embedded nREPL server
(def reindeer-nrepl-server (atom nil))

(defn start-nrepl-server
  "start up embedded nREPL server."
  []
  (reset! reindeer-nrepl-server (start-server :port 7889)) )

(defn stop-nrepl 
  "end the embedded nREPL server."
  []
  (stop-server reindeer-nrepl-server)
  (reset! reindeer-nrepl-server nil))

;; some useful getters for Vaadin application lifecycle stuff

(defn ^UI get-ui []
 (UI/getCurrent))

(defn ^Page get-page []
  (Page/getCurrent))

(defn ^VaadinSession get-session []
  (VaadinSession/getCurrent))

(defn ^WrappedSession get-wrapped-session []
  (.getSession (get-session)))

(defn ^VaadinService get-service []
  (VaadinService/getCurrent))

(defn ^VaadinRequest get-request []
  (VaadinService/getCurrentRequest))

(defn ^VaadinResponse get-response []
  (VaadinService/getCurrentResponse))

;; Cookies

(defn cookie 
  "create a cookie"
  [name value 
  & {:keys [
            max-age
            path
            version
            domain
            http-only
            secure
            comment
            ]}]
  (let [cookie (Cookie. name value)]
    (when path      (.setPath cookie path))
    (when max-age   (.setMaxAge cookie max-age))
    (when version   (.setVersion cookie version)) 
    (when domain    (.setDomain cookie domain))
    (when http-only (.setHttpOnly cookie http-only))
    (when secure    (.setSecure cookie secure))
    (when comment   (.setComment cookie comment))
    cookie))

(defn set-cookie 
  "set a cookie in the current response."
  [cookie]
  (.addCookie (get-response) cookie))

(defn get-cookies 
  "get all cookies from the current request."
  []
  (vec (.getCookies (get-request))))

(defn ^Cookie get-cookie
  "get cookie by name from the current request"
  [name]
;;;  (println (map #(.getName %) (get-cookies)))
  (first (filter #(= (.getName ^Cookie %) name) (get-cookies))))

(defn get-cookie-value
  "get the value of a named cookie from the current request"
  [name]
  (when-let [cookie (get-cookie name)]
    (.getValue cookie))) 
   
     
 ;; JavaScript

(defn ^JavaScript get-js []
  (JavaScript/getCurrent))

(defn execute-js [js-code]
  (.execute (get-js) js-code))


;; Vaadin UI

(defn set-ui-content!
  [^Component c]
  (.setContent ^UI (get-ui) c))

(defn get-ui-content
  []
  (.getContent (get-ui)))

(defn config-ui!
  [& {:keys [
             title
             content
             error-handler
            ]}]
   
    (when title
     (.setTitle (get-page) title))
   
   (when content
     (set-ui-content! content))
   
   (when error-handler
     (.setErrorHandler (get-ui) error-handler)))

(defn access-ui
  "Provide exclusive access to the UI from outside a request handling thread."
  [runnable]
  (.access (get-ui) runnable))

(defn close-session
  "Close the session, destroy UIs."
  [^String redirect-url]
  (.setLocation (get-page) redirect-url)
  ;; needed ?? (.close (get-ui))
  (.close (get-session))
  ;; needed ?? (.invalidate (get-wrapped-session))
  )


;; TODO def-ui 

;(defn defapplication
;  [& {:keys [main-theme
;             init-fn
;             close-fn
;             user-changed-fn
;             terminal-error-fn
;             error-handler
;             on-request-start-fn
;             on-request-end-fn
;             logout-url
;             main-window
;             app-windows] } ]
;  (proxy [Application
;          Application$UserChangeListener
;          HttpServletRequestListener] []
;    (init 
;      []
;      (reset! vaadin-app this) 
;      (when main-theme
;         (.setTheme ^Application this main-theme))
;      (when logout-url
;         (.setLogoutURL ^Application this logout-url))
;      (when error-handler
;         (.setErrorHandler ^Application this error-handler))
;      (when init-fn
;        (init-fn))
;      (when main-window
;        )
;      (when app-windows
;        )
;     )
;    (close 
;      []
;      (when close-fn
;        (close-fn))
;        (proxy-super close))
;    (onRequestStart 
;      [^HttpServletRequest request,	^HttpServletResponse response]
;       (when on-request-start-fn
;        (on-request-start-fn request response)))
;    (onRequestEnd 
;      [^HttpServletRequest request,	^HttpServletResponse response] 
;        (when on-request-end-fn
;          (on-request-end-fn request response)))
;    (applicationUserChanged
;      [^Application$UserChangeEvent event]
;          (when user-changed-fn
;            (user-changed-fn event)))
;    (terminalError 
;      [^Terminal$ErrorEvent event]
;      (if terminal-error-fn
;        (terminal-error-fn event)
;        (proxy-super terminalError event)))
;    ))
;

(defn get-locale []
  (.getLocale (get-ui)))

(defn ^Window window
  [& {:keys [title closable? draggable? modal? resizable? resizeLazy? 
             height width position-x position-y 
             items] } ] 
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
    (add-components! win items)
    win))

(defn add-window!
  "Adds and opens a sub window." 
  [^Window sub-win]
  (.addWindow (get-ui) sub-win))

(defn remove-window!
  "Removes and closes a sub window." 
  [^Window sub-win]
  (.removeWindow (get-ui) sub-win))

(defn open-url 
  "open a url on a new HTML target browser window or tab."
  [^URL url & target]
  (.open (get-page)
      (ExternalResource. url)
      (or target "_blank")))

(defn add!
  [^AbstractComponentContainer component-container component]
  (.addComponent component-container  component)
  component-container)

(defn set-content!
  "Set content of a panel (window)."
  [^Panel panel component]
  (.setContent panel component)
  panel)


;; Resource handling

(defn ^ThemeResource theme-res [path]
  (ThemeResource. path))

(defn ^ClassResource class-res 
  [path & {:keys [buffer-size cache-time] } ] 
  (let [res (ClassResource. path (get-ui))]
    (when buffer-size (.setBufferSize res buffer-size))
    (when cache-time (.setCacheTime   res cache-time))
  res))

(defn ^ExternalResource external-resource 
  [^String path]
  (ExternalResource. path))

(defn ^ExternalResource external-resource-by-url 
  [^URL url]
  (ExternalResource. url))

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
  (set-height!* [this v])
  (get-height* [this])
  (set-icon!* [this r])
  (get-icon* [this])
  (set-immediate!* [this v])
  (is-immediate?* [this]))

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
    (set-height!* [this v] (.setHeight this ^String v))
    (get-height* [this] (.getHeight this))
    (set-icon!* [this r] (.setIcon this ^Resource r))
    (get-icon* [this] (.getIcon this))
    (set-immediate!* [this v] (.setImmediate this v))
    (is-immediate?* [this] (.isImmediate this))  )

(defprotocol ConfigAbstractField
  "Protocol to hook into AbstractField"
  (set-value!* [this v])
  (get-value* [this]))

(extend-protocol ConfigAbstractField
 
  com.vaadin.ui.AbstractField
    (set-value!* [this v] (.setValue this v))
    (get-value* [this] (.getValue this)))


(defn- ^String convert-text-value [v]
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

(defn- set-height!
  "Internal use only"
  [this v]
  (set-height!* this v))

(defn- get-height
  "Internal use only"
  [this]
  (get-height* this))

(defn- set-icon!
  "Internal use only"
  [this r]
  (set-icon!* this (theme-res r)))

(defn- get-icon
  "Internal use only"
  [this]
  (get-icon* this))

(defn- set-immediate!
  "Internal use only"
  [this v]
  (set-immediate!* this v))

(defn- is-immediate?
  "Internal use only"
  [this]
  (is-immediate?* this))

(defn- set-value!
  "Internal use only"
  [this v]
  (set-value!* this v))

(defn- get-value
  "Internal use only"
  [this]
  (get-value* this))

(defn listen!
  [^AbstractComponent c 
   ^Component$Listener l]
  (.addListener c l))

(def default-options
  (option-map
    ;(default-option :id seesaw.selector/id-of! seesaw.selector/id-of ["A keyword id for the widget"])
    ;(default-option :class seesaw.selector/class-of! seesaw.selector/class-of [:class-name, #{:multiple, :class-names}])
    (default-option :caption set-caption! get-caption ["A string" "Anything accepted by (clojure.core/slurp)"])
    (default-option :icon set-icon! get-icon ["An icon" "Icon from resource"])
    (default-option :description set-description! get-description ["A string" "Anything accepted by (clojure.core/slurp)"])
    (default-option :data set-data! get-data ["Anything." "Associate arbitrary user-data with a widget."])
    (default-option :width set-width! get-width ["A string" "A width as string, e.g in percent"])
    (default-option :height set-height! get-height ["A string" "A height as string, e.g in percent"])
    (default-option :immediate? set-immediate! is-immediate? ["immediate" ""])))

(def default-field-options
  (option-map
    (default-option :value set-value! get-value ["value of field" ""])))

;; options of Vaadin super classes

(def abstract-component-options
  (options-for-class AbstractComponent))
(def abstract-component-container-options
  (options-for-class AbstractComponentContainer))
(def abstract-field-options
  (options-for-class AbstractField))
(def abstract-text-field-options
  (options-for-class AbstractTextField))
(def abstract-ordered-layout-options
  (options-for-class AbstractOrderedLayout))
(def abstract-single-component-container-options
  (options-for-class AbstractSingleComponentContainer))
(def abstract-select-options
  (options-for-class AbstractSelect))
(def panel-options
  (options-for-class Panel))
(def window-options
  (options-for-class Window))

;interfaces
;;; Focusable etc

;; Label

; content modes 
(def-constants-handler content-modes {
   :text         ContentMode/TEXT
   :preformatted ContentMode/PREFORMATTED
   :html         ContentMode/HTML
   })

(defn- set-content-mode!
   "Internal use only"
  [^Label lbl mode]
  (.setContentMode lbl (get-constant-from-content-modes mode)))

(defn- get-content-mode
  "Internal use only"
  [^Label lbl]
  (get-key-from-content-modes (.getContentMode lbl)))

;; Label isnt an AbstractField!, so this specialty is needed 
(defn- set-label-value!
   "Internal use only"
  [^Label lbl ^String v]
  (.setValue lbl v))

(defn- get-label-value
  "Internal use only"
  [^Label lbl]
  (.getValue lbl))

(def label-options
  (merge
    default-options
    (option-map
        (default-option :content-mode set-content-mode! get-content-mode ["content mode" ""])
        (default-option :value set-label-value! get-label-value ["value" ""]))))

(option-provider Label label-options)

(defn ^Label label
  [& args]
  (case (count args)
    0 (label :value "")
    1 (label :value (first args))
    (apply-options (Label. "") args)))

(defn v-gap 
  "Vertical spacer with optional height and width."
  [& {:keys [height width] } ]
  (let [^Label gap (Label. "&nbsp;" ContentMode/HTML )]
    (when height (.setHeight gap ^String height))
    (when width  (.setWidth  gap ^String width))
    gap))

(defn h-gap 
  "Horizontal, expanding spacer."
  [^AbstractOrderedLayout layout & {:keys [height] } ]
  (let [^Label gap (Label. "&nbsp;" ContentMode/HTML)]
    (.setWidth  gap "100%")
    (when height (.setHeight gap ^String height))
    (add! layout gap)
    (set-expand-ratio! layout gap 1.0)
    gap))

;; Button

(defn- set-button-click-listener!
  [^Button btn func]
  (.addListener btn
    (reify Button$ClickListener
	    (buttonClick [this event]
        (func event)))))

(def button-options
  (merge
    default-options
    (option-map
      (default-option :on-click set-button-click-listener!
        nil ["A button click listener." ""]))))

(option-provider Button button-options)

(defn ^Button button
  [& args ]
   (case (count args)
    0 (button :caption "")
    1 (button :caption (first args))
    (apply-options (Button. "") args)))

(defn ^NativeButton native-button
  [& args ]
   (case (count args)
    0 (native-button :caption "")
    1 (native-button :caption (first args))
    (apply-options (NativeButton. "") args)))

;; Layouts

(defn- add-components!
  [^AbstractComponentContainer container items]
   (doseq [item items] 
     (cond 
       (nil? item) nil
       ;; make strings to labels 'on the fly'
       (string? item) (add! container (label :value item))
       :else (add! container item))))

(defn ^HorizontalLayout h-l
  [& {:keys [items spacing margin-all width height style-name] } ]
   (let [h (HorizontalLayout.)]
     (when width (.setWidth h width))
     (when height (.setHeight h height))
     (when spacing (.setSpacing h spacing))
     (when margin-all (.setMargin h ^boolean margin-all))
     (when style-name (.addStyleName h style-name))
     (add-components! h items) 
     h))

(defn ^VerticalLayout v-l
  [& {:keys [items spacing margin-all width height style-name] } ]
   (let [v (VerticalLayout.)]
     (when width (.setWidth v width))
     (when height (.setHeight v height))
     (when spacing (.setSpacing v spacing))
     (when margin-all (.setMargin v ^boolean margin-all))
     (when style-name (.addStyleName v style-name))
     (add-components! v items) 
     v))

(defn get-component
  "Gets the component at the given index position"
  [^AbstractOrderedLayout layout index]
  (.getComponent layout index))

(defn set-expand-ratio!
  [^AbstractOrderedLayout layout component ratio]
  (.setExpandRatio layout component (float ratio))
  layout)
 
(defn set-expand-ratio-at-index!
  [^AbstractOrderedLayout layout index ratio]
  (.setExpandRatio layout (get-component layout index) (float ratio))
  layout)

;; Alignment

(defonce ALIGNMENT_TOP_RIGHT Alignment/TOP_RIGHT)
(defonce ALIGNMENT_TOP_LEFT Alignment/TOP_LEFT)
(defonce ALIGNMENT_TOP_CENTER Alignment/TOP_CENTER)
(defonce ALIGNMENT_MIDDLE_RIGHT Alignment/MIDDLE_RIGHT)
(defonce ALIGNMENT_MIDDLE_LEFT Alignment/MIDDLE_LEFT)
(defonce ALIGNMENT_MIDDLE_CENTER Alignment/MIDDLE_CENTER)
(defonce ALIGNMENT_BOTTOM_RIGHT Alignment/BOTTOM_RIGHT)
(defonce ALIGNMENT_BOTTOM_LEFT Alignment/BOTTOM_LEFT)
(defonce ALIGNMENT_BOTTOM_CENTER Alignment/BOTTOM_CENTER)

(defn align!
  [^AbstractOrderedLayout layout component alignment]
  (.setComponentAlignment layout component alignment)
  layout)

(defn align-at-index!
  [^AbstractOrderedLayout layout index alignment]
  (.setComponentAlignment layout (get-component layout index) alignment)
  layout)

;; Fields

(defn set-input-prompt!
  [^AbstractTextField tf ^String prompt]
  (.setInputPrompt tf prompt))

(defn- set-value-change-listener!
  [^AbstractField af func]
  (.addListener af
    (reify Property$ValueChangeListener
	    (valueChange [this event]
        (func event)))))

(defn- set-text-change-listener!
  [^TextField tf func]
  (.addListener tf
    (reify FieldEvents$TextChangeListener
	    (textChange [this event]
        (func event)))))

(defwidget text-field TextField
  [default-options default-field-options]
  [(option-map
     (default-option :input-prompt set-input-prompt! nil ["input prompt" ""])
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

(defn alert [msg]
  (Notification/show (convert-text-value msg)))

(defn show-notification
  "Generic show method."
  [caption msg type]
  (Notification/show caption msg type))

(defn show
  "Shows humanized message."
  [caption msg]
  (show-notification caption msg
                     Notification$Type/HUMANIZED_MESSAGE))
  
(defn show-warning
  "Shows warning message."
  [caption msg]
  (show-notification caption msg
         Notification$Type/WARNING_MESSAGE))
 
(defn show-error
  "Shows error message."
  [caption msg]
    (show-notification caption msg
                       Notification$Type/ERROR_MESSAGE))

(defn show-tray
  "Shows message in system tray area."
  [caption msg]
    (show-notification caption msg
                       Notification$Type/TRAY_NOTIFICATION))

; TODO implement Notification with options
(defn create-notification
  [caption msg type]
  (Notification. caption msg type))

;; Link 

(defn- set-resource!
  "internal use only."
  [^Link this ^Resource r]
  (.setResource this r))

(defn- set-target-name!
  "internal use only."
  [^Link this ^String n]
  (.setTargetName this n))

(def link-options
  (merge
    default-options
    (option-map
      (default-option :resource set-resource! nil ["resource" ""])
      (default-option :target-name set-target-name! nil ["target name" ""]))))

(option-provider Link link-options)

(defn ^Link link
  [& args]
  (case (count args)
    0 (link :caption "")
    1 (link :caption (first args))
    (apply-options (Link.) args)))

(option-provider Embedded link-options)

(defn ^Embedded embedded
  [^Resource res & args]
  (case (count args)
    0 (embedded res :caption "")
    1 (embedded res :caption (first args))
    (apply-options (Embedded. nil res)
                   args)))

(defn ^Embedded embedded-theme-res
  [^String res & args]
  (apply embedded (theme-res res) args))

(defn ^Embedded embedded-class-res
  [^String res & args]
  (apply embedded (class-res res) args))

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

 ;; Window is an AbstractComponent!
 ;;   (config* [target name]  (get-option-value target name))
 ;;   (config!* [target args] (apply-options target args))
    
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
  c)

(defn remove-style-name
  "Removes a named css style from a component." 
  [^Component c 
   ^String n]
  (.removeStyleName c n)
  c)

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

;; Misc.

(defn print-current-page
  "Print the current web page."
  []
  (execute-js "print();"))

(defn print-page-button
  "A convenient 'print-this-page' button."
  [caption]
  (button :caption  (convert-text-value (or caption "Print"))
          :on-click (fn [_] (print-current-page))))

(defn user-error 
  "create an error message."
  [msg]
  (UserError. msg))

;; Properties

(defn object-property 
  "create a new object property."
  [value]
  (ObjectProperty. value))

(defn set-property-value
  [^Property prop value]
  (.setValue prop value))

(defn get-property-value
  [^Property prop]
  (.getValue prop))

;; Items

(defn item-click-listener
  [func]
  (reify ItemClickEvent$ItemClickListener
    (itemClick [this event]
      (func event))))

(defn get-item-from-item-click-event
  [^ItemClickEvent evt]
  (.getItem evt))

(defn get-item-id-from-item-click-event
  [^ItemClickEvent evt]
  (.getItemId evt))

(defn get-property-id-from-item-click-event
  [^ItemClickEvent evt]
  (.getPropertyId evt))

(defn get-item-property
  [^Item item pid]
  (.getItemProperty item pid))

(defn get-item-property-value
  [^Item item pid]
  (when-let [prop (get-item-property item pid)]
    (get-property-value prop)))

(defn set-item-property-value
  [^Item item pid value]
  (when-let [prop (get-item-property item pid)]
    (set-property-value prop value)))

(defn set-item-property-values
  [^Item item pid-and-values]
  (doseq [[pid value] pid-and-values]
    (set-item-property-value item pid value)))

;; Containers

(defn add-container-property!
  [^Container container property-id type default-value]
  (.addContainerProperty container property-id type default-value))

(defn container-property
    [& {:keys [pid type default] } ]
  [pid type default])

(defn add-container-properties 
  [^Container container props]
  (doseq [prop props]
    (apply add-container-property! (list* container prop))))

(defn get-container-item-ids
  "Gets the ID's of all visible (after filtering and sorting) Items."
  [^Container container]
  (.getItemIds container))

(defn ^Item add-container-item
  [^Container container & item-id]
  (if item-id
    (.addItem container item-id)
    (.addItem container (Object.))))

(defn indexed-container
  "create an indexed container."
   [& {:keys [item-ids properties] } ]
   (let [container (if item-ids
                     (IndexedContainer. item-ids)
                     (IndexedContainer.))]
     (when properties
       (add-container-properties container properties))
     container))


;; Binding


;; Table

; Column Header Modes 
(def-constants-handler column-header-modes {
   :hidden      Table$ColumnHeaderMode/HIDDEN
   :id          Table$ColumnHeaderMode/ID
   :explicit    Table$ColumnHeaderMode/EXPLICIT
   :defaults-id Table$ColumnHeaderMode/EXPLICIT_DEFAULTS_ID
   }) 

(defn- set-column-header-mode!
   "Internal use only"
  [^Table tbl mode]
  (.setColumnHeaderMode tbl (get-constant-from-column-header-modes mode)))

(defn- get-column-header-mode
   "Internal use only"
  [^Table tbl]
  (get-key-from-column-header-modes (.getColumnHeaderMode tbl)))

(defn- set-item-click-listener!
   "Internal use only"
  [^Table tbl func]
  (.addItemClickListener tbl (item-click-listener func)))

(defn- set-selectable!
   "Internal use only"
  [^Table tbl state]
  (.setSelectable tbl state))

(defn- is-selectable?
   "Internal use only"
  [^Table tbl]
  (.isSelectable tbl))

(defn- set-container-datasource!
  [^Table tbl ^Container container]
  (.setContainerDataSource tbl container))

(defn- set-visible-columns!
  [^Table tbl visibleCols]
  (.setVisibleColumns tbl (to-array visibleCols)))

(defn- set-striped!
  [^Table tbl striped?]
  (if striped?
    (.addStyleName tbl "striped")
    (.removeStyleName tbl "striped")))

(def table-options
  (merge
    default-options
    (option-map
        (default-option :on-item-click set-item-click-listener! nil ["" ""])
        (default-option :column-header-mode set-column-header-mode! get-column-header-mode ["" ""])
        (default-option :striped? set-striped! nil ["" ""])
        (default-option :selectable? set-selectable! is-selectable? ["" ""])
        (default-option :container-datasource set-container-datasource! nil ["" ""])
        (default-option :visible-columns set-visible-columns! nil ["" ""]))))

(option-provider Table table-options)

(defn ^Table table
  [& args]
  (case (count args)
    0 (table :caption "")
    1 (table :caption (first args))
    (apply-options (Table. "") args)))


