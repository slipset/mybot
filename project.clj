(defproject joint-bot "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
   :repl-options {:host "0.0.0.0"
                  :init-ns mybot.core}
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [clj-http "0.7.8"]
                 [net.assum/xmpp-clj "0.0.1"]]
   :profiles {:dev {:dependencies [[midje "1.6.3"]]}})
