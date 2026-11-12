(ns lift.intro-screen
  (:require
   [quil.core :as q]
   [lift.drawings :as draw]
   [lift.methods :refer [draw update-state key-press reset-state]]))


(defmethod draw :intro
  [{:keys [floor]}]
  (q/background 80)
  (draw/game-space)
  (draw/lift floor)
  (draw/floors)
  (draw/intro-title)
  (draw/intro-text))


(defmethod update-state :intro
  [{:keys [floor direction] :as state}]
  (let [dir (cond
              (<= 0 floor 9) direction
              :else (- direction))]
    (-> state
        (assoc :direction dir)
        (update :floor #(+ dir %)))))


(defmethod key-press :intro
  [state]
  (let [k (q/key-as-keyword)]
    (case k
      :space (reset-state :game)
      state)))
