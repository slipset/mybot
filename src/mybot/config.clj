(ns mybot.config)

(def env
  {:jira {:api "http://localhost:8080/rest/api/latest/"
          :url "http://localhost:8080/browse/"
          :basic-auth ["sausagebot" "sausage"]
          :issue-matcher #".*(CLOJ-\d+).*"}
   
   :xmpp {:host "localhost"
          :port 5222
          :username "sausagebot"
          :password "sausage"
          :resource "Mr. Sausage"
          :name "@sausagebot"
          :room "clojure@conference.clojutre" }
   
   :teamcity {:url "http://teamcity.foo.com/app/rest/"
              :artifact-url "http://teamcity.foo.com/repository/download/bt10/"
              :host-mapping {"h1" "host1.foo.com"}
              :basic-auth ["foo@foo.com" "mypasswd"]
              :artifact-matcher #".*installation_package-public-.*.zip"}
   })

(defn getenv [path]
  (get-in env path))
