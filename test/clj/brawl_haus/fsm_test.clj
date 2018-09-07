(ns brawl-haus.fsm-test
  (:require  [clojure.test :refer [deftest testing is]]
             [brawl-haus.events :refer [drive]]
             [brawl-haus.subs :refer [derive]]
             [matcho.core :refer [assert match]]
             ))

(defn l [desc expr] (println desc expr) expr)

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

#_(defmacro expect [db an-assert info]
  `(do (assert ~an-assert
               (~derive ~db (get ~info :sub) (get ~info :conn-id)))
       ~db))

#_(println (macroexpand '(expect {} {:one 1} {:conn-id :ID :sub [:a-sub]})))

(deftest fsm-test
  (let [conn-id1 "test-conn-id1"
        conn-id2 "test-conn-id2"
        =>evt1 (init-=>evt conn-id1)
        =>evt2 (init-=>evt conn-id2)
        <=sub1 (init-<=sub conn-id1)
        <=sub2 (init-<=sub conn-id2)]
    (-> {}
        (=>evt1 [:sv/attend {:with-test-equipment? true}])
        (expect {:location-id :space-versus}
                (<=sub1 [:location]))

        (expect {:integrity number?}
                (<=sub1 [:sv.ship/integrity]))
        (=>evt2 [:sv/attend {:with-test-equipment? true}])
        (expect {:shields {:in-use 0}
                 :engines {:in-use 0}
                 :weapons {:in-use 0}}
                (<=sub2 [:sv/systems]))

        ;;; Attended, init power
        (expect {:left 5}
                (<=sub2 [:sv.power/info]))

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
                (<=sub1 [:sv.power/info]))

        ;; Power up weapon, not enough power
        (=>evt1 [:sv.system/power-up :weapons])
        (=>evt1 [:sv.weapon/power-up :burst-laser-2])
        (expect {:is-on false}
                (<=sub1 [:sv.weapon/readiness :burst-laser-2]))

        ;;; Enough power
        (=>evt1 [:sv.system/power-up :weapons])
        (=>evt1 [:sv.weapon/power-up :burst-laser-2])
        (expect {:is-on true
                 :percentage number?
                 :is-ready false}
                (<=sub1 [:sv.weapon/readiness :burst-laser-2]))

        ;;; Not ready, not selected and did not fire
        (=>evt1 [:sv.weapon/hit conn-id2 :weapons])
        (expect {:weapons {:damaged 0}}
                (<=sub2 [:sv/systems]))

        (wait 200)

        ;;; Ready
        (expect {:is-on true
                 :percentage 100
                 :is-ready true}
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
        (=>evt2 [:sv.weapon/select :burst-laser-2])
        (expect {:is-on true
                 :is-selected true}
                (<=sub2 [:sv.weapon/readiness :burst-laser-2]))

        ;;; System gets damaged
        (=>evt1 [:sv.weapon/hit conn-id2 :weapons])
        (expect {:weapons {:damaged 2
                           :in-use 1}}
                (<=sub2 [:sv/systems]))
        ;;; Doesn't power up damaged cells
        (=>evt2 [:sv.system/power-up :weapons])
        (expect {:weapons {:damaged 2
                           :in-use 1}}
                (<=sub2 [:sv/systems]))
        ;;; Weapons turns off, selection drops
        (expect {:is-on false
                 :is-selected false}
                (<=sub2 [:sv.weapon/readiness :burst-laser-2]))
        ;;; My selection drops, charges again
        (expect {:is-on true
                 :is-selected false
                 :percentage #(< % 30)}
                (<=sub1 [:sv.weapon/readiness :burst-laser-2]))

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
        (expect {:is-on false}
                (<=sub1 [:sv.weapon/readiness :burst-laser-2]))
        )

    ;;; Preserve ship state
    (testing "Preserve ship state"
      (-> {}
          (=>evt1 [:sv/attend {:with-test-equipment? true}])
          (=>evt2 [:sv/attend {:with-test-equipment? true}])
          (=>evt1 [:sv.system/power-up :weapons])
          (=>evt1 [:sv.system/power-up :weapons])
          (=>evt1 [:sv.weapon/power-up :burst-laser-2])
          (=>evt1 [:sv.weapon/select :burst-laser-2])
          (wait 200)
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
          (=>evt1 [:sv/attend {:with-test-equipment? true}])
          (=>evt2 [:sv/attend {:with-test-equipment? true
                               :location :test-location}])
          (expect #(= 2 (count %))
                  (<=sub1 [:sv/locations]))

          ;;; Hey!
          (=>evt1 [:sv/attend {:location :test-location}])
          (expect #(= 1 (count %))
                  (<=sub1 [:sv/locations]))
          (expect #{conn-id1 conn-id2}
                  (<=sub1 [:sv.location/ships :test-location]))

          ;;; Wanna trade?;) (powers weapons)
          (=>evt1 [:sv.system/power-up :weapons])
          (=>evt1 [:sv.system/power-up :weapons])
          (=>evt1 [:sv.weapon/power-up :burst-laser-2])

          ;;; Fleeing, fast!
          (=>evt2 [:sv.system/power-up :shields])
          (=>evt2 [:sv.system/power-up :shields])


          (wait 400)
          ;;; Weapon is ready
          (expect {:is-on true
                   :percentage 100
                   :is-ready true}
                  (<=sub1 [:sv.weapon/readiness :burst-laser-2]))
          ;;; Shield is ready
          (expect {:is-ready true}
                  (<=sub2 [:sv.shield/readiness]))

          ;;; Flee!
          (=>evt2 [:sv/attend {:location :flee-location}])
          (expect #(= 2 (count %))
                  (<=sub1 [:sv/locations]))

          ;;; Systems' power is preserved
          (expect {:shields {:in-use 2}}
                  (<=sub2 [:sv/systems]))
          ;;; Shield readiness is preserved
          (expect {:is-ready true}
                  (<=sub2 [:sv.shield/readiness]))

          ;;; In pursuit!
          (=>evt1 [:sv/attend {:location :flee-location}])
          (expect #(= 1 (count %))
                  (<=sub1 [:sv/locations]))

          ;;; Weapons' readiness dropped
          (expect {:is-on false}
                  (<=sub1 [:sv.weapon/readiness :burst-laser-2]))
          ))))

