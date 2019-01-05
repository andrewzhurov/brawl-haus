(ns brawl-haus.fit.sound)

(def fire-assets
  {1 (js/Audio. "/sandbox/ak-47-1.wav")
   2 (js/Audio. "/sandbox/ak-47-2.wav")
   3 (js/Audio. "/sandbox/ak-47-3.wav")
   4 (js/Audio. "/sandbox/ak-47-4.wav")
   5 (js/Audio. "/sandbox/ak-47-5.wav")
   6 (js/Audio. "/sandbox/ak-47-6.wav")
   7 (js/Audio. "/sandbox/ak-47-7.wav")})

(defn play-fire [idx]
  (let [sound (get fire-assets idx)]
    (.pause sound)
    (set! (.-current-time sound) 0)
    (.play sound)))
