(ns mybot.core-test
    (:require [clojure.test :refer :all]
              [midje.sweet  :refer :all]   
              [mybot.core :refer :all]))

(let [issue {:key "CLOJ-1"
             :fields {:summary "summary"
                      :status {:name "status-name"}
                      :assignee {:name "assignee-name"}}}
      url "http://localhost:8080/browse/"
      result (str url "CLOJ-1 summary, status: status-name, assignee: assignee-name")]
  (facts about displaying issues issue         
         (fact "format-issue knows how to format an issue"
               (format-issue issue) => result))

  (fact "it does what it's supposed to do"
        (with-redefs-fn {#'get-issue (fn [id] issue)}
          #(let []
             (show-issue "CLOJ-1") => result))))

(facts about tell-where
       (fact "it formats correctly"
             (let [info {:host "localhost" :port 4711}]
               (tell-where info) => "Oh, I'm running on localhost:4711")))

(facts about remove-nick
       (fact "it removes the nick from the body"
             (let [nick "fisk"
                   body "@fisk rules"]
               (remove-nick body nick) => "rules"))
       (fact "it only removes the nick when in the beginning of body"
             (let [nick "fisk"
                   body "lol @fisk rules"]
               (remove-nick body nick) => "lol @fisk rules")))

(facts about handle-command
       (fact "it replies sort of nice on unknown commands"
             (handle-command {:body "lolfisk"}) => "I'm sorry, I don't know how to do that: lolfisk")
       (fact "it knows how to handle 'where are you running?'"
             (with-redefs-fn {#'tell-where (fn [_] "here")}
               #(let []
                  (handle-command {:body "where are you running"}) => "here"))))

(facts about to-me?
       (fact "returns true if body starts with '@<nick>'"
             (to-me? {:body (str "@" (:nick config) " what's up")}) => true)
       (fact "returns false if not"
             (to-me? {:body (str " " "@" (:nick config) " what's up")}) => false))

(facts about handle-noise
       (fact "if body contains clojure, it returns 'clojure rocks!'"
             (handle-noise {:body "adfafclojureadfadfa"}) => "clojure rocks!")
       (fact "if body matches a jira-issue, it returns info about that issue"
             (with-redefs-fn {#'show-issue (fn [i] i)}
               #(let []
                  (handle-noise {:body "blablaCLOJ-1blabla"}) => "CLOJ-1"))))

(facts about handle-chatter
       (fact "if the message is to me, we treat it as a command"
             (with-redefs-fn {#'handle-command (fn [m] :command)
                              #'handle-noise (fn [m] :noise)}
               #(let []
                 (handle-chatter {:body (str "@"  (:nick config) " do something")}) => :command)))
       (fact "if the message is not to me, we treat it as noise"
             (with-redefs-fn {#'handle-command (fn [m] :command)
                              #'handle-noise (fn [m] :noise)}
               #(let []
                 (handle-chatter {:body (str  (:nick config) " do something")}) => :noise))))
                                 
                            
             

