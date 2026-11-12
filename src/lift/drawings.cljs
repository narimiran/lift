(ns lift.drawings
  (:require
   [quil.core :as q]))


(def scene-width 880)
(def scene-height 720)
(def size 16)
(def margin (* 2 size))
(def radius (/ size 2))

(def game-width (* 20 size))
(def game-height (- scene-height (* 2 size)))


(def lift-width (* 8 size))
(def floor-height (* 4 size))

(def title-size 48)
(def text-size size)




;; ---------------------- DEFAULTS ----------------------

(defn- text-defaults [& {:keys [color size align style]
                         :or {color :black
                              size text-size
                              align :left
                              style :normal}}]
  (q/text-size size)
  (q/text-align align)
  (q/text-style style)
  (q/no-stroke)
  (q/fill (case color
            :white 250
            :black 0)))


(defn- rect-defaults [& {:keys [fill weight]
                         :or {fill 250
                              weight 2}}]
  (q/fill fill)
  (q/stroke 0)
  (q/stroke-weight weight))


;; ---------------------- INTRO ----------------------

(defn intro-title []
  (text-defaults {:size title-size
                  :align :center
                  :style :bold
                  :color :white})
  (q/text "Lift"
          (+ game-width (/ (- scene-width game-width) 2))
          (* 0.3 scene-height)))

(defn intro-text []
  (text-defaults {:size (* 1.3 text-size)
                  :align :center
                  :style :bold
                  :color :white})
  (q/text "Press   SPACE   to play"
           (+ game-width (/ (- scene-width game-width) 2))
           (* 0.7 scene-height)))






;; ---------------------- GENERAL ----------------------

(defn lift-space []
  (rect-defaults {:fill 200})
  (q/rect margin margin
          (+ lift-width margin) (- game-height margin)
          radius))

(defn game-space []
  (rect-defaults)
  (q/rect size size
          game-width game-height
          radius)
  (lift-space))


(defn lift [pos]
  (rect-defaults {:weight 4})
  (q/rect (* 3 size)
          (- scene-height (* 1.5 floor-height) (* 4 pos size))
          lift-width
          (* 0.8 floor-height)
          radius))

(defn floors []
  (text-defaults {:size (* 2 text-size)
                  :style :bold
                  :color :white})
  (rect-defaults)
  (doseq [i (range 10)]
    (let [y (- game-height size (* i floor-height))]
      (q/text i (* 12.5 size) (- y (/ size 2)))
      (q/line (* 14 size) y
              (* 20 size) y))))







;; ---------------------- GAME ----------------------

(defn stats [moves open-doors]
  (text-defaults)
  (q/text (str "Moves: " moves)
          (+ game-width margin)
          margin)
  (q/text (str "Doors open: " open-doors)
          (+ game-width margin)
          (* 2 margin)))

(defn waiters [waiters]
  (text-defaults)
  (doseq [[i wtrs] (group-by :start waiters)]
    (let [y (- game-height size (* i floor-height) (/ size 2))]
      (doseq [[j p] (map-indexed vector wtrs)]
        (q/text (:end p)
                (+ (* 0.8 j size) (* 14 size))
                y)))))

(defn passengers [passengers floor]
  (text-defaults)
  (let [y (- game-height size (* floor floor-height) (/ size 2))]
    (doseq [[i p] (map-indexed vector (sort-by :end passengers))]
      (q/text (:end p)
              (+ (* 0.8 i size) (* 2 margin))
              y))))
