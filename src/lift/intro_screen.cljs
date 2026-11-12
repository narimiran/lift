(ns lift.intro-screen
  (:require
   [quil.core :as q]
   [lift.lift :as lift]
   [lift.methods :refer [draw update-state]]
   [lift.config :refer [scene-width scene-height
                        game-width
                        title-size text-size]]
   [lift.lift :as lift]
   [lift.methods :refer [draw update-state]]))


(defn- draw-title []
  (q/text-size title-size)
  (q/text-align :center)
  (q/text-style :bold)
  (q/stroke 0)
  (q/fill 255)
  (q/text "Lift"
          (+ game-width (/ (- scene-width game-width) 2))
          (* 0.3 scene-height)))

(defn- draw-text []
  (q/text-style :normal)
  (q/text-align :center)
  (q/text-size (* 1.3 text-size))
  (q/stroke 0)
  (q/fill 255)
  (q/text "Press   SPACE   to play"
           (+ game-width (/ (- scene-width game-width) 2))
           (* 0.7 scene-height)))


(defmethod draw :intro
  [{:keys [floor]}]
  (q/background 80)
  (lift/draw-game-space)
  (lift/draw-lift floor)
  (lift/draw-floors)
  (draw-title)
  (draw-text))

(defmethod update-state :intro
  [{:keys [floor direction] :as state}]
  (let [dir (cond
              (<= 0 floor 9) direction
              :else (- direction))]
    (-> state
        (assoc :direction dir)
        (update :floor #(+ dir %)))))
