(ns brawl-haus.css
  (:require [garden.def :refer [defstyles]]))


(defn grid [& strs]
  (let [rows (butlast strs)
        columns (last strs)
        escaped-rows (for [row rows]
                       (let [[areas size] (clojure.string/split row #" (?=[\w\d]+$)")]
                         (format "\"%s\" %s" areas size)))]
    (str (clojure.string/join "\n"  (conj (vec escaped-rows)  (str "/ " columns))))))

(defstyles screen
  [[:.set-nick {:display "flex"
                :flex-direction "row"
                :justify-content "center"
                :align-items "center"}
    [:button {:min-width "100px"}]]
   [:.progress-bar {:transition "1s !important"}]
   [:.badge {:border-radius "4px"}]
   [:span.badge.new {:font-size "1.1rem"
                     :font-weight "500"}]
   [:body {:overflow-y "hidden"}]
   [:.tube-indicator {:position "absolute"
                      :height "15px" :width "15px"
                      :top "10px"
                      :right "10px"
                      :border-radius "50%"
                      :border-style "solid"
                      :border-width "3px"}
    [:&.true {:border-color "green"}]
    [:&.false {:border-color "gray"}]]
   [:.help-btn {:position "fixed"
                :right "15px"
                :top "10px"
                :z-index 1}]
   [:.hiccup-touch {:position "fixed"
                    :right "15px"
                    :top "50px"
                    :z-index 1}]
   [:.hiccup-touch-panel {:display "grid"
                          :height "100vh"
                          :width "100vw"
                          :grid-template
                          (grid "hiccup-editor result 1fr"
                                "garden-editor result 1fr"
                                "1fr 1fr")}
    [:.switch-line {:position "absolute"
                    :bottom "0px"
                    :left "0px"
                    :right "0px"
                    :display "flex"
                    :justify-content "center"}
     [:.switch {:padding "5px 8px"
                :margin-bottom "0px"
                :border-radius "5px 5px 0px 0px"
                :background-color "rgba(0,0,0,0.7)"}
      ]]
    [:.hiccup-editor {:grid-area "hiccup-editor"}
     [:.content {:height "100%"}
      [:.paren-soup {:height "100%"}]]]
    [:.garden-editor {:grid-area "garden-editor"}
     [:.content {:height "100%"}
      [:.paren-soup {:height "100%"}]]]
    [:.render-result {:grid-area "result"}]
    [:.compile-result {:grid-area "result"}
     [:div {:height "50%"}]]]
   [:.help {:position "fixed"
            :min-width "300px"
            :width "25vw"
            :right "-50vw"
            :transition "0.4s"
            :top "0px"
            :overflow "visible"
            :z-index 2}
    [:&.open {:right "0px"}]
    [:.collection-header
     [:.header {:display "inline-block"}]
     [:.description {:display "inline-block"
                     :margin-left "10px"
                     :color "gray"}]]
    [:.collection-item {:cursor "pointer"}]]
   [:.chat {:position "fixed"
            :bottom "-55vh"
            :margin "0px"
            :display "grid"
            :grid-template
            (grid "send-box participants 50px"
                  "messages participants 1fr"
                  "2fr 1fr")
            :height "50vh"
            :width "100vw"
            :transition "0.5s"}
    [:&.open {:bottom "0px"}]
    [:.messages {:grid-area "messages"
                 :min-height "0px"
                 :overflow-y "auto"
                 :width "100%"
                 :padding-left "5%"
                 :padding-right "5%"}
     [:ul {:display "flex"
           :flex-direction "column"}
      [:li {:margin-bottom "3px"
            :padding "10px 10px"
            :width "fit-content"
            :background-color "lightgray"
            :border-radius "8px"
            :margin-left "10px"
            :max-width "80%"}
       [:&.my {:background-color "lightblue"
               :align-self "flex-end"
               :margin-left "0px"
               :margin-right "10px"}]
       [:.from {:display "inline-block"
                :margin-right "10px"}]
       [:.received-at {:display "inline-block"}]]]
     [:.message {}]]
    [:.send-box {:grid-area "send-box"
                 :padding-left "5%"
                 :padding-right "5%"
                 :display "flex"
                 :align-items "center"}
     [:input {:max-width "90%"
              :flex 1}]
     [:div.btn {:transition "0.2s"}]]
    [:ul.participants {:grid-area "participants"
                       :overflow-y "auto"
                       :margin "0px"
                       :height "100%"}
     [:li]
     [:a {:cursor "pointer"}]
     [:.collection-header {:position "sticky"
                           :top "0px"}]
     [:.activity-indicator]]]
   [:.collection.open-races
    {:display "flex"
     :grid-area "open-races"
     :flex-direction "column"}
    [:.race {;:padding "5px 10px"
             ;:margin "3px 0px"
             :cursor "pointer"
             ;:background-color "lightgray"
             :display "flex"
             :flex-direction "row"
             :align-items "center"}]
    [:.countdown {:width "40px"}]]
   [:.races-panel
    {:display "grid"
     :grid-template (grid "controls highscores 50px"
                          "open-races highscores 1fr"
                          "2fr 1fr")}
    [:.new-race-btn {:margin "5px 5px"}]
    [:ul.highscores {:grid-area "highscores"
                     :overflow-y "auto"
                     :margin "0px"
                     :height "100%"}
     [:li]
     [:a {:cursor "pointer"}]
     [:.collection-header {:position "sticky"
                           :top "0px"}]
     [:.activity-indicator]]
    #_[:.highscores {:grid-area "highscores"
                     :overflow-y "auto"
                     :margin "0px"
                     :height "100%"}]]

   ;; Race panel
   [:.race-panel {:padding "5px 10px"
                  :width "90%"
                  :height "90%"}
    [:.countdown {:text-align "center"
                  :font-size "20px"
                  :text-decoration "underline"
                  :margin "5px"}]
    [:.waiting {:vertical-align "center"}]
    [:.text-race
     [:.race-text {:display "flex"
                   :flex-direction "row"
                   :flex-wrap "wrap"}
      [:.char {:display "block"
               :min-width "unset"
               :min-height "unset"
               :font-family "\"Courier New\", Courier, monospace"
               :white-space "pre"
               :border-bottom "2px solid rgba(255,255,255,0)"
               :transition "border 0.4s"}
       [:&.done {:color "gray"}]
       [:&.right-typed {:border-bottom "2px solid blue"}]
       [:&.wrong-typed {:border-bottom "2px solid red"}]]]
     [:input {:width "90%"
              :margin "auto"
              :display "block"}]]
    [:.race-progress {:margin "5px 0px"}
     [:.participant {:position "relative"}
      [:.nick {:display "inline-block"}]
      [:.average-speed {:display "inline-block"
                        :position "absolute"
                        :top 0 :right 0
                        :margin-right "10px"}]
      [:&.quit
       [:.nick {:color "lightgray"}]
       [:.rc-progress-bar {:background-color "lightgray"}]]]]
    [:.to-next-row
     {:display "flex"
      :justify-content "flex-end"}]]
   #_[:.app {:display "grid"
             :grid-template-columns "auto 1fr"}
      [:.navbar {:display "flex"
                 :flex-direction "column"
                 :height "100vh"
                 :width "60px"
                 :border-right "1px solid lightgray"
                 :align-items "center"}
       [:.tab {:padding "0px 10px"
               :margin "3px 0px"
               :cursor "pointer"}
        [:i {:font-size "30px"}]]]
      [:.content {:margin-left "10px"
                  :box-shadow "0 2px 2px 0 rgba(0,0,0,0.14), 0 3px 1px -2px rgba(0,0,0,0.12), 0 1px 5px 0 rgba(0,0,0,0.2)"}]]
   [:.content {:margin-left "10px"}]

   [:.notifications {:position "absolute"
                     :top "10px"
                     :right "10px"}]

   [:.login-panel {:display "flex"
                   :flex-direction "column"
                   :align-items "center"
                   :justify-content "center"
                   :height "100vh"
                   :width "100vw"}
    [:form {:max-width "300px"
            :max-height "700px"}]
    [:.onward {:width "100%"
               :height "42px"}]]])
