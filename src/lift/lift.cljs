(ns lift.lift
  (:require
   [quil.core :as q]
   [lift.config :refer [scene-height size margin radius
                        game-width game-height text-size
                        lift-width floor-height]]))

(defn- draw-lift-space []
  (q/fill 200)
  (q/stroke-weight 2)
  (q/rect margin margin
          (+ lift-width margin) (- game-height margin)
          radius))

(defn draw-game-space []
  (q/fill 250)
  (q/stroke 0)
  (q/stroke-weight 2)
  (q/rect size size
          game-width game-height
          radius)
  (draw-lift-space))


(defn draw-lift [pos]
  (q/fill 250)
  (q/stroke 0)
  (q/stroke-weight 4)
  (q/rect (* 3 size)
          (- scene-height (* 1.5 floor-height) (* 4 pos size))
          lift-width
          (* 0.8 floor-height)
          radius))

(defn draw-floors []
  (q/fill 250)
  (q/stroke 0)
  (q/stroke-weight 2)
  (q/text-size (* 2 text-size))
  (q/text-style :bold)
  (q/text-align :left)
  (doseq [i (range 10)]
    (let [y (- game-height size (* i floor-height))]
      (q/text i (* 12.5 size) (- y (/ size 2)))
      (q/line (* 14 size) y
              (* 20 size) y))))
