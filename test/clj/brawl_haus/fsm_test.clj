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
        (=>evt1 [:sv/attend])
        (expect {:location-id :space-versus}
                (<=sub1 [:location]))

        (expect {:integrity number?}
                (<=sub1 [:sv.ship/integrity]))
        (=>evt2 [:sv/attend])
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

        ;;; Not ready and did not fire
        (=>evt1 [:sv.weapon/hit conn-id2 :weapons :burst-laser-2])
        (expect {:weapons {:damaged 0}}
                (<=sub2 [:sv/systems]))

        ((fn [db] (Thread/sleep 200) db))
        ;;; Ready
        (expect {:is-on true
                 :percentage 100
                 :is-ready true}
                (<=sub1 [:sv.weapon/readiness :burst-laser-2]))

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

        ;;; Enemy powers weapon
        (=>evt2 [:sv.system/power-up :weapons])
        (=>evt2 [:sv.system/power-up :weapons])
        (=>evt2 [:sv.weapon/power-up :burst-laser-2])
        (expect {:is-on true}
                (<=sub2 [:sv.weapon/readiness :burst-laser-2]))

        ;;; System gets damaged and weapon turns off
        (=>evt1 [:sv.weapon/hit conn-id2 :weapons :burst-laser-2])
        (expect {:weapons {:damaged 2
                           :in-use 1}}
                (<=sub2 [:sv/systems]))
        (expect {:is-on false}
                (<=sub2 [:sv.weapon/readiness]))

        ;;; Power down weapon system depletes power from weapons (not smartly)
        (=>evt1 [:sv.system/power-down :weapons])
        (=>evt1 [:sv.system/power-down :weapons])
        (expect {:is-on false}
                (<=sub1 [:sv.weapon/readiness :burst-laser-2]))
        )))
