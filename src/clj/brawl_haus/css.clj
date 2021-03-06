(ns brawl-haus.css
  (:require [garden.def :refer [defstyles defkeyframes]]))

(def active-color "rgb(125, 139, 198)" #_"#7D8BC6")
(def focused-active-color "rgb(134, 151, 221)")

(defn grid [& strs]
  (let [rows (butlast strs)
        columns (last strs)
        escaped-rows (for [row rows]
                       (let [[areas size] (clojure.string/split row #" (?=[\w\d]+$)")]
                         (format "\"%s\" %s" areas size)))]
    (str (clojure.string/join "\n"  (conj (vec escaped-rows)  (str "/ " columns))))))

(defkeyframes single-fire
  ["0%" {:margin-left "0px"}]
  ["33%" {:margin-left "-10px"}]
  ["100%" {:margin-left "0px"}])

(defkeyframes charge-out
  ["0%" {:opacity 0}]
  ["5%" {:opacity 1}]
  ["100%" {:margin-right "-300px"}])

(defkeyframes flash
  ["33%" {:opacity 0.1}]
  ["50%" {:opacity 0.6}])

(defstyles screen
  [single-fire
   flash
   charge-out
   [:div.top-hud {:background-color "gray"
                  :border "2px solid lightgray"
                  :display "flex"
                  :align-items "center"
                  :padding "5px"}]
   [:.scrap-icon
    {:width "32px"
     :height "30px"
     :background-image "url(/ftl-assets/img/ui_icons/icon_scrap.png)"}]
   [:.scrap-amount
    {:font-size "20px"
     :color "white"
     :font-weight "600"}]
   [:.main-layout {:display "grid"
                   :height "100vh"
                   :grid-template
                   (grid "nav-section auto"
                         "content-section 1fr"
                         "1fr")}
    [:.nav-section {:grid-area "nav-section"}
     [:.brand-logo {:padding "0px 20px"
                    :cursor "pointer"}]
     [:li.controls {:width "50px"}]]
    [:.content-section {:grid-area "content-section"}]]

   [:.default-panel
    {:padding-top "10%"
     :display "flex"
     :height "100%"
     :flex-direction "column"
     :align-items "center"}
    [:.thoughts {:margin-bottom "-10px"
                 :display "flex"
                 :align-items "center"}
     [:.dot {:border-radius "50%"
             :border "2px solid gray"
             :height "fit-content"
             :margin "2px"
             :transition "0.8s"}
      [:&.active {:border "8px solid gray"}]]]
    [:.face {:margin-top "0px"}]]


   [:.home-panel
    [:.top-section {:grid-area "top-section"
                    :border-bottom "1px solid gray"
                    :display "flex"
                    :align-items "center"}]
    [:.content-section {:grid-area "content-section"
                        :margin "15px 10px 0px 10px"}]
    [:.logo {:font-size "24px"
             :margin "4px"
             :cursor "pointer"}]
    [:ul.shelf {:display "flex"}
     [:li {:max-width "272px"
           :margin-right "20px"}
      [:img {:max-height "200px"
             :object-fit "cover"}]
      [:.card-title {:text-shadow "1px 0 0 #000, 0 -1px 0 #000, 0 1px 0 #000, -1px 0 0 #000"}]
      [:a {:cursor "pointer"}]]]]


   [:.ccc-panel {:margin "20px 30px"
                 :display "flex"
                 :flex-direction "column"}
    ]


   [:.set-nick {:display "flex"
                :flex-direction "row"
                :justify-content "center"
                :align-items "center"}
    [:button {:min-width "100px"}]]
   [:.progress-bar {:transition "1s !important"}]
   [:.badge {:border-radius "4px"}]
   [:span.badge.new {:font-size "1.1rem"
                     :font-weight "500"}]
   [:body {:overflow-y "hidden"}]

   [:.hiccup-touch-panel {:display "grid"
                          :height "100%"
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
            :width "300px"
            :right "-300px"
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

   #_[:.collection.open-races
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

   #_[:.races-panel
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
    [:.highscores {:grid-area "highscores"
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

   [:.content {:margin-left "10px"}]

   [:.notifications {:position "absolute"
                     :top "10px"
                     :right "10px"}]

   [:.space-versus {:position "relative"
                    :display "grid"
                    :grid-template
                    (grid "ship locations environment 1fr"
                          "3fr 2fr 4fr")
                    :height "100%"
                    :background-color "dimgray"}
    [:.me {:background "darkgray"
            :margin "5px"
            :width "65%"}]
    [:.outside {:background "darkgray"
                 :margin "5px"
                 :width "35%"}]
    [:.top-hud {:position "absolute"
                :top "10px"
                :left "10px"}]
    [:.bottom-hud {:position "absolute"
                   :bottom "10px" :left "10px"
                   :display "flex"
                   :align-items "flex-end"}]
    [:.energy-bar {:display "flex"
                   :flex-direction "column-reverse"
                   :width "35px"
                   :margin-right "15px"}
     [:.cell {:height "10px"
              :margin-top "4px"
              :transition "0.5s"}
      [:&.with-power {:background-color active-color}]
      [:&.without-power {:background-opacity "1"
                         :border "1px solid lightgray"}]]]

    [:.module {:display "flex"
               :flex-direction "column"
               :align-items "center"
               :margin-right "10px"}
     [:.icon {:height "30px" :width "30px"
              :border-radius "50%"
              :margin-top "5px"
              :background-size "contain"
              :border "2px solid white"
              :cursor "pointer"
              :transition "0.3s"}
      [:&.with-power {:background-color active-color}]
      [:&:hover {:background-color focused-active-color}]
      [:&.without-power {:border "1px solid lightgray"}]
      [:&.shields {:background-image "url(/image/ShieldsSymbol.png)"}]
      [:&.engines {:background-image "url(/image/EnginesSymbol.png)"}]
      [:&.weapons {:background-image "url(/image/WeaponControlSymbol.png)"}]
      ]
     [:.cell {:width "20px"
              :height "8px"
              :margin-top "2px"
              :transition "0.5s"}
      [:&.damaged {:border "1px solid red"}]
      [:&.with-power {:background-color active-color}]
      [:&.without-power {:border "1px solid lightgray"}]]]

    [:.weapons-panel {:border "2px solid white"
                      :display "flex"}
     [:.weapon {:position "relative"
                :margin "5px 5px"
                :display "flex"
                :align-items "flex-end"
                :color "white"
                :cursor "pointer"}
      [:.cell {:background-color "white"}]
      [:.box {:background-color "gainsboro"}]
      [:&.idle
       [:.cell {:background-color "transparent"}]
       [:.box {:background-color "transparent"}]]
      [:&.ready
       [:.bar {:background-color (str active-color " !important")}]]
      [:&.selected
       [:.bar {:background-color (str active-color " !important")}]
       [:.box {:border-color "gold"}]]
      [:.box:hover
       {:background-color "gainsboro"}]

      [:.readiness {:position "relative"
                    :border "2px solid white"
                    :border-radius "7px 0px 0px 0px"
                    :display "inline-block"
                    :height "60%"
                    :width "10px"}
       [:.bar {:position "absolute"
               :bottom "0px"
               :background-color "white"
               :margin "1px"
               :width "4px"
               :border-radius "7px 0px 0px 0px"}]]
      [:.box {:display "flex"
              :align-items "flex-end"
              :min-height "60px"
              :border "2px solid white"
              :transition "0.3s"}
       [:.power-require {:display "flex"
                         :flex-direction "column"
                         :width "24px"
                         :padding "2px"}
        [:.cell {:border "2px solid white"
                 :weight "14px"
                 :height "8px"
                 :margin-top "2px"}
         ]]]
      [:.name {:margin "3px"}]]
     ]
    [:.ship {:position "relative"
             :grid-area "ship"
             :width "min-content"
             :padding "10px"
             :margin "5px"}
     [:.btn.loot {:visibility "hidden"
                  :position "absolute"
                  :top "95px"
                  :left "70px"
                  :z-index 4}]
     [:&.wrecked
      {:animation [[flash "0.6s"]]}
      [:.btn.loot {:visibility "visible"}]]
     [:.name {:position "absolute"
              :top "148px"
              :left "106px"
              :color "rgb(208, 143, 113)"
              :font-weight "800"
              :text-shadow "1px 0 0 white, 0 -1px 0 white, 0 1px 0 white, -1px 0 0 white"
              }]
     [:.shield {:position "absolute"
                :top "0px" :right "0px" :bottom "0px" :left "0px"
                :border-radius "70%"
                :background-color "rgba(75, 156, 196, 0.20)"
                :transition "0.3s"}
      [:&.ready {:background-color "rgba(75, 156, 196, 0.40)"}]]

     [:.ship-backdrop {:height "210px"}]
     [:.ship-schema
      {:position "absolute"
       :top "26px"
       :display "grid"
       :grid (grid
              ". . . . . . . . . . . . 30px"
              ". . . . . . . w1 w1 . . . 30px"
              ". e e s s . . w w . . . 30px"
              ". e e s s . . w w . . . 30px"
              ". . . . . . . w2 w2 . . . 30px"
              ". . . . . . . . . . . . 30px"
              "30px 30px 30px 30px 30px 30px 30px 30px 30px 30px 30px 30px")
       :z-index 2
       }
      [:.hardware-weapon {:position "relative"
                          :max-height "30px"
                          :max-width "60px"
                          :z-index 1
                          :transition "1.25s"}
       [:img {:width "100%"}]
       [:&.w1 {:grid-area "w1"
               :align-self "end"
               :margin-bottom "-5px" }
        [:&.idle {:margin-bottom "-15px"}]]
       [:&.w2 {:grid-area "w2"
               :align-self "start"
               :transform "scaleY(-1)"
               :margin-top "-5px"}
        [:&.idle {:margin-top "-15px"}]]
       [:.ready-indicator
        {:position "absolute"
         :top "21%"
         :right "21px"
         :height "6px"
         :width "12px"
         :border-radius "4px"
         :z-index 4
         :background-color "darkred"}]
       [:&.ready
        [:.ready-indicator {:background-color "green"}]]
       [:&.selected
        [:.ready-indicator {:background-color "green"}]]
       [:.charge
        {:position "absolute"
         :top "10%"
         :right "-8px"
         :height "10px"
         :width "18px"
         :border-radius "30%"
         :background-color "orange"
         :z-index 4
         :opacity 0}]
       [:&.firing
        {:animation [[single-fire "0.6s"]]}
        [:.charge
         {:animation [[charge-out "0.4s"]]}]
        ]]

      [:.system {:background-color "lightgray"
                 :background-position "center"
                 :background-repeat "no-repeat"
                                        ;:background-size "contain"
                 :border "1px solid gray"
                 :transition "0.3s"
                 :cursor "pointer"
                 :z-index 2
                 :animation [[flash "0.4s"]]}
       [:&:hover {:background-color "gainsboro"}]
       [:&.with-power {:background-color active-color}]
       [:&:hover {:background-color focused-active-color}]
       [:&.without-power {:border "1px solid lightgray"}]
       [:&.shields {:grid-area "s"
                    :background-image "url(/image/ShieldsSymbol.png)"}]
       [:&.engines {:grid-area "e"
                    :background-image "url(/image/EnginesSymbol.png)"}]
       [:&.weapons {:grid-area "w"
                    :background-image "url(/image/WeaponControlSymbol.png)"}]
       [:&.integrity-full {:border-color "gray"}]
       [:&.integrity-damaged {:border-color "yellow"}]
       [:&.integrity-wrecked {:border-color "red"}]
       ]
      ]]
    [:.locations {:grid-area "locations"
                  :width "100%"
                  :overflow-y "auto"}]
    [:.environment {:grid-area "environment"}]
    [:.station
     {:color "white"
      :font-size "18px"
      :font-weight "600"
      :background-color "gray"
      :padding "5px"}]
    [:.store
     [:.weapon
      {:position "relative"
       :width "110px"
       :height "110px"
       :display "grid"
       :font-size "initial"
       :background-image "url(/ftl-assets/img/storeUI/store_buy_weapons_on.png)"
       :cursor "pointer"}
      [:&:hover {:background-image "url(/ftl-assets/img/storeUI/store_buy_weapons_select2.png)"}]
      [:.name {:position "absolute"
               :top "30px"
               :left "20px"
               :font-weight "600"
               :color "white"}]
      [:.price {:position "absolute"
                :top "82px"
                :left "40px"
                :font-weight "600"
                :color "white"}]]]

    ]])
