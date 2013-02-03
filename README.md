# clj-reindeer

Clojure Vaadin bridge library. 

Get the best of two worlds together!

This is just the start. Many interesting things are still missing:
tables, trees, tree-tables, ...
But watch out, as it will be added!


## Usage

In your own Clojure-Vaadin-project, and if you use Leiningen(2),
your project.clj should include settings from this example, to be found in dir 'templates':

(defproject clj-vdn-app-xxx "0.1.0"
  :description "Vaadin app in clojure"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.4.0"]
  				 [com.vaadin/vaadin "6.8.4"]
                 [clj-reindeer "0.3.0"]
                 [i18n "1.0.3"]
                 [ring "1.1.6"]
                ]
  :repositories {"vaadin-addons"
                 "http://maven.vaadin.com/vaadin-addons"}
  :plugins [[lein-ring "0.7.5"]
            [lein-localrepo "0.4.1"]]
  ;; dummy needed for "lein ring uberwar"  
  :ring {:handler clj.vdn.xxx.ringhandler/dummy-handler,
         :adapter {:port 3000},
         :web-xml "src/resources/web.xml",
         :war-exclusions [#"javax.servlet-2.5.0.v201103041518.jar"]}
  :uberjar-name "clj#vdn#xxx.war"
  :war-resources-path "war-resources"
  )

'i18n' is my own Clojure library for - guess? - internationalization, to be found also on GitHub.

'ring' and 'lein-ring' are only needed if you want to use, like I do,  
"lein ring uberwar" for building the war file, which I recommend.
For this to work, you have to include a dummy ring handler. An example is
provided in the file 'templates/src/clj/ringhandler.clj'


For the following, please study the examples in directory 'templates':

You have to provide a web.xml, which has to sit in dir 'src/resources/'.
The servlet class has to be 'clj.reindeer.ReindeerServlet', and you need an init-param
named 'buildfn' with a value of your vaadins app "main" function, e.g. 'your-application/launch-app'.
See file '/templates/resources/web.xml' for an example web.xml, and
file 'templates/src/clj/app.clj' for an example of a launchable app.

If you use Vaadin plugins, dont forget to provide another init-param named 'widgetset' with 
a reference to your compiled custom widgetsets.

Before building the war with 'lein ring uberwar', you have to fill the dir 'war-resources' with your 
custom widgetsets, your custom Vaadin themes etc., which normally sit in folder 'WebContent' 
in an eclipse managed web project.
For Windows users, see 'templates/windows-dev/uberwar.bat' for an example of a copy job.

Finally, copy the war to the /webapps folder of your preferred web server.
For Windows and Apache Tomcat users, see 'templates/windows-dev/copywar.bat' for an example of a copy job.



## License

Copyright © 2013 Peter Feldtmann

Distributed under the Eclipse Public License, the same as Clojure.

Based in parts on "seesaw", a clojure library for Java Swing,
and "upshot", a clojure library for Java FX.
Thanks to Dave Ray for his great work!

