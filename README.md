# mybot

This is a little xmpp-bot which knows how to respond to messages containing jira-issues
     ;; you need to update the config if you want to use this your self

     (def config {:host "localhost"
                  :port 5222
                  :username "the username of the bot"
                  :domain "??"
                  :password "the bots password"
                  :nick "the nick the bot should have in the room"
				  :room "the room id"})

        (def out *out*)
        (def chat (xmpp/connect config))
        (def clojure-room (xmpp/join chat (:room config) (:nick config)))

        (.sendMessage clojure-room "Hello! Clojutre!!")
        (.sendMessage clojure-room "clojure rocks")

        (xmpp/add-listener clojure-room (xmpp/default-processor
                                              #'message-listener
                                                 (xmpp/create-sender :response)
                                                    (xmpp/wrap-errors out)))


## License

Copyright © 2014 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
