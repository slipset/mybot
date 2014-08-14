(ns joint-bot.teamcity
    (:require [clj-http.client :as client]))

(def api-url "http://git.joint.no:8111/app/rest/")

(defn builds-url [build]
  (str api-url "builds?locator=status:success,buildType:" build))

(defn build-url [build-id]
  (str api-url "builds/id:" build-id))

(defn get-url [url]
  (let [response (client/get url
                              {
                               :as :json
                               :accept :json
                               :content-type :json
                               :basic-auth ["erik.assum" "Joint123"]
                               })] (:body response)))




