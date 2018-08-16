(ns brawl-haus.events-test
  (:require [brawl-haus.events :as sut]
            [clojure.test :as t]
            [matcho.core :refer [match]]))

(t/deftest events-test
  (t/testing "drive"
    (let [init-state {}
          conn-id "a-connection-id"
          state (-> init-state
                    (sut/drive [:conn/on-create conn-id])
                    )]

      (match (sut/location state conn-id)
             {:location-id :race-panel})

      (match (sut/race-progress state (get-in (sut/location state conn-id) [:params :race-id]))
             [{:nick string?
               :speed nil
               :progress 0
               :did-finish false
               :did-quit false}])
      )))
