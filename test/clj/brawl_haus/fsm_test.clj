(ns brawl-haus.fsm-test
  (:require  [clojure.test :refer [deftest testing is]]
             [brawl-haus.events :refer :all]
             [brawl-haus.subs :refer [derive]]
             [matcho.core :refer [assert match]]
             ))

(def =>evt
  (fn [db evt] (drive db evt)))

(def init-=>evt
  (fn [conn-id]
    (fn [db evt]
      (drive db evt conn-id))))
(def init-<=sub
  (fn [conn-id]
    (fn [sub]
      {:conn-id conn-id
       :sub sub})))

(defn wait [db ms]
  (Thread/sleep ms)
  db)
(defn inspect [db]
  (clojure.pprint/pprint db)
  db)

(defn failed-test-line-number []
  (let [trace (->> (.. Thread currentThread getStackTrace)
                   (map (fn [el] (str (.getFileName el) ":" (.getLineNumber el))))
                   vec)]
    (println (get trace 5))))

(defn expect [db an-assert {:keys [conn-id sub]}]
  (when-not (assert an-assert
                    (derive db sub conn-id))
    (failed-test-line-number))
  db)

(def ship-dummy
  (deep-merge
   ship-basic-power-hub
   {:systems {:engines {:max 1
                        :damaged 0
                        :in-use 0
                        :powered-since []}}}))

(def ship-fast-burst-laser-2
  (deep-merge
   ship-burst-laser-2
   {:systems {:weapons {:stuff {:burst-laser-2 {:charge-time 200
                                                :fire-time 50}}}}}))
(def ship-fast-shields
  (deep-merge
   ship-basic-shields
   {:systems {:shields {:charge-time 300}}}))

(def world-with-station
  {:games {:sv {:locations {:station-location {:station {}}}}}})


(deftest fsm-test
  (let [conn-id1 "test-conn-id1"
        conn-id2 "test-conn-id2"
        =>evt1 (init-=>evt conn-id1)
        =>evt2 (init-=>evt conn-id2)
        <=sub1 (init-<=sub conn-id1)
        <=sub2 (init-<=sub conn-id2)]

    "Power is a crucial ship's resource.
     It is used to power up systems
     Powered up systems produce some desired helpful effects"
    (testing "Power up/down"
      (-> {}
          (=>evt1 [:sv/attend {:with-ship (deep-merge
                                           ship-basic-power-hub
                                           ship-basic-shields)}])
          ;;; Attended, init power
          (expect {:left 5}
                  (<=sub1 [:sv.power/info]))

          ;; Power up, no overflow
          (=>evt1 [:sv.system/power-up :shields])
          (=>evt1 [:sv.system/power-up :shields])
          (=>evt1 [:sv.system/power-up :shields])
          (=>evt1 [:sv.system/power-up :shields])
          (=>evt1 [:sv.system/power-up :shields])
          (expect {:shields {:in-use 4
                             :max 4}}
                  (<=sub1 [:sv/systems]))
          (expect {:left 1}
                  (<=sub1 [:sv.power/info]))
          ;; Power down, no underflow
          (=>evt1 [:sv.system/power-down :shields])
          (=>evt1 [:sv.system/power-down :shields])
          (=>evt1 [:sv.system/power-down :shields])
          (=>evt1 [:sv.system/power-down :shields])
          (=>evt1 [:sv.system/power-down :shields])
          (expect {:shields {:in-use 0}}
                  (<=sub1 [:sv/systems]))
          (expect {:left 5}
                  (<=sub1 [:sv.power/info]))))

    (-> {}
        (=>evt1 [:sv/attend {:with-ship (deep-merge
                                         ship-basic-power-hub
                                         ship-basic-weapons
                                         ship-fast-burst-laser-2)}])
        (expect {:location-id :space-versus}
                (<=sub1 [:location]))

        (=>evt2 [:sv/attend {:with-ship (deep-merge
                                         ship-basic-power-hub
                                         ship-basic-weapons
                                         ship-fast-burst-laser-2)}])
        (expect {:weapons {:in-use 0}}
                (<=sub1 [:sv/systems]))


        ;; Power up weapon, not enough power
        (=>evt1 [:sv.system/power-up :weapons])
        (=>evt1 [:sv.weapon/power-up :burst-laser-2])
        (expect {:status :idle}
                (<=sub1 [:sv.weapon/readiness :burst-laser-2]))

        ;;; Enough power
        (=>evt1 [:sv.system/power-up :weapons])
        (=>evt1 [:sv.weapon/power-up :burst-laser-2])
        (expect {:status :charging
                 :percentage number?}
                (<=sub1 [:sv.weapon/readiness :burst-laser-2]))

        ;;; Not ready, not selected and did not fire
        (=>evt1 [:sv.weapon/hit conn-id2 :weapons])
        (expect {:weapons {:damaged 0}}
                (<=sub2 [:sv/systems]))

        (wait 200)

        ;;; Ready
        (expect {:status :ready}
                (<=sub1 [:sv.weapon/readiness :burst-laser-2]))

        ;;; Not selected and did not fire
        (=>evt1 [:sv.weapon/hit conn-id2 :weapons])
        (expect {:weapons {:damaged 0}}
                (<=sub2 [:sv/systems]))

        ;;; Select
        (=>evt1 [:sv.weapon/select :burst-laser-2])

        ;; On weapon power off discharge continually
        ;(=>evt1 [:sv.weapon/power-down :burst-laser-2])
        ;((fn [db] (Thread/sleep 150) db))
        ;(expect {:is-on false
        ;         :percentage #(and (> % 10) (< % 50) )
        ;         :is-ready false}
        ;        (<=sub1 [:sv.weapon/readiness :burst-laser-2]))

        ;; On weapon power up continue charging from previous charge level
        ;(=>evt1 [:sv.weapon/power-down :burst-laser-2])
        ;((fn [db] (Thread/sleep 100) db))
        ;(expect {:is-on true
        ;         :percentage #(and (> % 50) (< % 90))
        ;         :is-ready false}
        ;        (<=sub1 [:sv.weapon/readiness :burst-laser-2]))

        ;;; Enemy powers, selects weapon
        (=>evt2 [:sv.system/power-up :weapons])
        (=>evt2 [:sv.system/power-up :weapons])
        (=>evt2 [:sv.weapon/power-up :burst-laser-2])
        (wait 200)
        (=>evt2 [:sv.weapon/select :burst-laser-2])
        (expect {:status :selected}
                (<=sub2 [:sv.weapon/readiness :burst-laser-2]))

        ;;; FIRE!
        (=>evt1 [:sv.weapon/hit conn-id2 :weapons])

        ;;; It's firing for some time
        (expect {:status :firing}
                (<=sub1 [:sv.weapon/readiness :burst-laser-2]))
        (wait 50)
        ;;; Charges again
        (expect {:status :charging
                 :percentage #(and % (< % 80))}
                (<=sub1 [:sv.weapon/readiness :burst-laser-2]))

        ;;; System gets damaged
        (expect {:weapons {:damaged 2
                           :in-use 1}}
                (<=sub2 [:sv/systems]))
        ;;; Doesn't power up damaged cells
        (=>evt2 [:sv.system/power-up :weapons])
        (expect {:weapons {:damaged 2
                           :in-use 1}}
                (<=sub2 [:sv/systems]))

        ;;; Weapons turns off, selection drops
        (expect {:status :idle}
                (<=sub2 [:sv.weapon/readiness :burst-laser-2]))


        ;;; Damage does not go over max system integrity
        (wait 200)

        (=>evt1 [:sv.weapon/select :burst-laser-2])
        (=>evt1 [:sv.weapon/hit conn-id2 :weapons])
        (expect {:weapons {:max 3
                           :damaged 3
                           :in-use 0}}
                (<=sub2 [:sv/systems]))


        ;;; Power down weapon system depletes power from weapons (not smartly)
        (=>evt1 [:sv.system/power-down :weapons])
        (=>evt1 [:sv.system/power-down :weapons])
        (expect {:status :idle}
                (<=sub1 [:sv.weapon/readiness :burst-laser-2]))
        )

    ;;; Preserve ship state
    (testing "Preserve ship state"
      (-> {}
          (=>evt1 [:sv/attend {:with-ship (deep-merge
                                           ship-basic-power-hub
                                           ship-basic-weapons
                                           ship-fast-burst-laser-2)}])
          (=>evt2 [:sv/attend {:with-ship ship-basic-weapons}])
          (=>evt1 [:sv.system/power-up :weapons])
          (=>evt1 [:sv.system/power-up :weapons])
          (=>evt1 [:sv.weapon/power-up :burst-laser-2])
          (wait 200)
          (=>evt1 [:sv.weapon/select :burst-laser-2])
          (=>evt1 [:sv.weapon/hit conn-id2 :weapons])
          (expect {:weapons {:damaged 2}}
                  (<=sub2 [:sv/systems]))

          ;;; Player2 re-attends
          (=>evt2 [:sv/attend])
          (expect {:weapons {:damaged 2}}
                  (<=sub2 [:sv/systems]))
          ))


    (testing "Same location visit"
      (-> {}
          (=>evt1 [:sv/attend {:with-ship (deep-merge
                                           ship-basic-power-hub
                                           ship-basic-weapons
                                           ship-fast-burst-laser-2)}])
          (=>evt1 [:sv/jump :test-location])
          (=>evt2 [:sv/attend {:with-ship (deep-merge
                                           ship-basic-power-hub
                                           ship-fast-shields
                                           ship-basic-engines)}])
          (=>evt2 [:sv/jump :test-location])
          (expect #(= 1 (count %))
                  (<=sub1 [:sv/locations]))

          (=>evt1 [:sv/jump])
          (expect #(= 2 (count %))
                  (<=sub1 [:sv/locations]))

          ;;; Hey!
          (=>evt1 [:sv/jump :test-location])
          (expect #{:test-location}
                  (<=sub1 [:sv/locations]))
          (expect {:ships #{conn-id1 conn-id2}}
                  (<=sub1 [:sv.location/details :test-location]))

          ;;; Wanna trade?;) (powers weapons)
          (=>evt1 [:sv.system/power-up :weapons])
          (=>evt1 [:sv.system/power-up :weapons])
          (=>evt1 [:sv.weapon/power-up :burst-laser-2])

          ;;; Fleeing, fast!
          (=>evt2 [:sv.system/power-up :shields])
          (=>evt2 [:sv.system/power-up :shields])


          (wait 400)
          ;;; Weapon is ready
          (expect {:status :ready}
                  (<=sub1 [:sv.weapon/readiness :burst-laser-2]))
          ;;; Shield is ready
          (expect {:status :ready}
                  (<=sub2 [:sv.shield/readiness]))

          ;;; Flee!
          (=>evt2 [:sv/jump :flee-location])
          (expect #(= 2 (count %))
                  (<=sub1 [:sv/locations]))

          ;;; Systems' power is preserved
          (expect {:shields {:in-use 2}}
                  (<=sub2 [:sv/systems]))
          ;;; Shield readiness is preserved
          (expect {:status :ready}
                  (<=sub2 [:sv.shield/readiness]))

          ;;; In pursuit!
          (=>evt1 [:sv/jump :flee-location])
          (expect #(= 1 (count %))
                  (<=sub1 [:sv/locations]))

          ;;; Weapons' readiness dropped
          (expect {:status :idle}
                  (<=sub1 [:sv.weapon/readiness :burst-laser-2]))


          ;;; Shield absorbs
          (=>evt1 [:sv.weapon/power-up :burst-laser-2])
          (wait 200)
          (=>evt1 [:sv.weapon/select :burst-laser-2])
          (=>evt1 [:sv.weapon/hit conn-id2 :engines])
          (expect {:status :firing}
                  (<=sub1 [:sv.weapon/readiness :burst-laser-2]))
          (expect {:status :charging}
                  (<=sub2 [:sv.shield/readiness]))
          (expect {:engines {:damaged 0}}
                  (<=sub2 [:sv/systems]))

          ;; Weapon does not charge again until it finished firing
          (wait 20)
          (expect {:status :firing}
                  (<=sub1 [:sv.weapon/readiness :burst-laser-2]))

          (wait 30)
          (expect {:status :charging
                   :percentage (comp not zero?)}
                  (<=sub1 [:sv.weapon/readiness :burst-laser-2]))))

    "There are 'locations' - dots/places/points where some game objects could be.
     They are represented by simple ids, no spacial allocation or whatsoever.
     In such locations can be game objects.
     There will be three types of game objects: ships, garbage (from destoyed ships) and stations (for upgrade)
     At the beginning of a game session they are generated:
     - for every ship
     - for every station
     Then they are populated by objects.
     Ships can jump to a different location.
     From a game point of view we are not interested in empty locations, so we present to a user only those with something of interest"
    (testing "Locations"
      (-> world-with-station

          (expect #{:station-location}
                  (<=sub1 [:sv/locations]))
          (expect {:station {}}
                  (<=sub1 [:sv.location/details :station-location]))

          (=>evt1 [:sv/attend {:with-ship (deep-merge
                                           ship-basic-power-hub
                                           ship-basic-weapons
                                           ship-fast-burst-laser-2)}])
          (expect #(= 2 (count %))
                  (<=sub1 [:sv/locations]))
          (=>evt1 [:sv/jump :station-location])
          (expect {:station {}
                   :ships #{conn-id1}}
                  (<=sub1 [:sv.location/details :station-location]))

          (=>evt2 [:sv/attend {:with-ship (deep-merge
                                           ship-dummy
                                           {:cargo {:scrap 10}})}])

          (=>evt1 [:sv/jump :a-location])
          (=>evt2 [:sv/jump :a-location])
          (expect {:ships #{conn-id1 conn-id2}}
                  (<=sub2 [:sv.location/details :a-location]))

          (=>evt1 [:sv.system/power-up :weapons])
          (=>evt1 [:sv.system/power-up :weapons])
          (=>evt1 [:sv.weapon/power-up :burst-laser-2])
          (wait 200)
          (=>evt1 [:sv.weapon/select :burst-laser-2])
          (=>evt1 [:sv.weapon/hit conn-id2 :engines])
          (expect {:engines {:damaged 1}}
                  (<=sub2 [:sv/systems]))

          (expect true
                  (<=sub1 [:sv.ship/wrecked? conn-id2]))

          (=>evt1 [:sv.ship/loot conn-id2])
          (expect 0
                  (<=sub2 [:sv.cargo/scrap]))
          (expect 10
                  (<=sub1 [:sv.cargo/scrap]))
          ))

    "Station with Store"
    "There may be 'store' service in a station
     It presents a ship with ability to purchase second 'basic-laser'
     Price is 30 'scrap', fixed
     One purchase per ship
     Weapon appears at ship slot after purchase"
    (testing "Store weapon purchase"
      (-> world-with-station
          (=>evt1 [:sv/attend {:with-ship (deep-merge
                                           ship-basic-power-hub
                                           ship-basic-weapons
                                           ship-fast-burst-laser-2
                                           {:cargo {:scrap 30}})}])
          (=>evt1 [:sv/jump :station-location])
          (=>evt1 [:sv.store/purchase :basic-laser])
          (expect 0
                  (<=sub1 [:sv.cargo/scrap]))
          (expect #(= 2 (count %))
                  (<=sub1 [:sv.weapons.stuff/view]))
          ))

    ))

