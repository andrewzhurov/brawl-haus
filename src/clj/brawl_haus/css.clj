(ns brawl-haus.css
  (:require [garden.def :refer [defstyles]]))

(defstyles screen
  [[:.tube-indicator {:position "absolute"
                      :height "15px" :width "15px"
                      :top "10px"
                      :right "10px"
                      :border-radius "50%"
                      :border-style "solid"
                      :border-width "3px"}
    [:&.true {:border-color "green"}]
    [:&.false {:border-color "gray"}]]
   [:.chat {:max-height "100px"}
    [:.messages {:display "flex"
                 :flex-direction "column"
                 :min-height "0px"
                 :overflow-y "scroll"}
     [:.message {:padding "5px 10px"}]]
    [:input]]
   [:.race-game
    [:.race-text {:display "flex"
                  :flex-direction "row"}
     [:.char {:display "block"
              :min-width "5px"}
      [:&.done {
                :color "gray"}]
      [:&.right-typed {:border-bottom "2px solid blue"}]
      [:&.wrong-typed {:border-bottom "2px solid red"}]]]]])
