(ns mybot.config)


(def env
  {:jira
   {:url "http://jira.foo.com/rest/api/latest/"
    :basic-auth ["foo@foo.com" "mypasswd"]
    :issue-matcher #"(PROJ)-\d+"
    }
  :xmpp
   {:host "localhost"
    :port 5222
    :username "sausagebot"
    :password "sausage"
    :resource "Mr Sausage"
    :name "@sausage"
    :room "clojure@conference.clojutre"
    }
   :deploy
    {:host "smtp.foo.com"
    :from "deploy-manager@foo.com"
    :cc ["one@foo.com"]
    :message "Please follow\n\n
                  http://wiki.foo.com/display/PROJ/Release+Installation+Guide\n\nand\n\n
                  http://wiki.foo.com/display/PROJ/General+Installation+Guide\n\n
                  Kind regards,\n\n @mybot\n"
     }
   :teamcity
   {:url "http://teamcity.foo.com/app/rest/"
     :artifact-url "http://teamcity.foo.com/repository/download/bt10/"
     :host-mapping {"h1" "host1.foo.com"}
     :basic-auth ["foo@foo.com" "mypasswd"]
     :artifact-matcher #".*installation_package-public-.*.zip"
    }
   })

(defn getenv [path]
  (get-in env path))
  
