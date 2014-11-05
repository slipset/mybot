(ns mybot.util)

(defn starts-with [s p]
  (= 0 (.indexOf s p)))

(defn contains [s p]
  (not= -1 (.indexOf s p)))

(defn matches [s p]
  (second (re-matches p s)))

(defn parse-date [s]
  (.format (java.text.SimpleDateFormat. "dd/MM HH:mm")
           (.parse (java.text.SimpleDateFormat. "yyyyMMdd'T'HHmmssZ") s)))
    
(defn remove-str [s p]
  (let [pattern (re-pattern p)]
    (clojure.string/replace s pattern "")))

(defn runtime-info []
  {:host (.getCanonicalHostName (java.net.InetAddress/getLocalHost))
   :port  (slurp "target/repl-port")})
