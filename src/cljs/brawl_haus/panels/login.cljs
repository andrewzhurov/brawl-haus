(ns brawl-haus.panels.login
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [brawl-haus.panels :as panels]
            [brawl-haus.utils :refer [l <sub]]))

(defmethod panels/panel :login-panel
  [_ _]
  (r/with-let [form (r/atom {:nick ""
                             :pass ""})]
    [:div.login-panel
     [:h5 "Here your journey begins"]
     [:form {:on-submit #(do (.preventDefault %)
                             (rf/dispatch [:tube/send [:login @form]]))}
      [:input {:type "text"
               :placeholder "How shall we call you?"
               :value (:nick @form)
               :on-change #(swap! form assoc :nick (.-value (.-target %)))}]
      [:input {:type "password"
               :placeholder "Passs they shall not pass"
               :value (:pass @form)
               :on-change #(swap! form assoc :pass (.-value (.-target %)))}]
      [:button.onward.btn {:type "submit"
                           :disabled (when (or (empty? (:nick @form))
                                               (empty? (:pass @form))) "true")}
       "Onward!"
       [:i.material-icons  "accessibility_new"]]]]))
