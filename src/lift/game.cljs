(ns lift.game
  (:require
   [quil.core :as q]
   [lift.config :refer [text-size game-width margin]]
   [lift.lift :as lift]
   [lift.methods :refer [draw key-press]]))


(defn- draw-moves [moves]
  (q/text-style :normal)
  (q/text-align :left)
  (q/text-size text-size)
  (q/no-stroke)
  (q/fill 0)
  (q/text (str "Moves: " moves)
          (+ game-width margin)
          margin))

(defmethod draw :game
  [{:keys [floor moves]}]
  (q/background 200)
  (lift/draw-game-space)
  (lift/draw-lift floor)
  (lift/draw-floors)
  (draw-moves moves))


(defn- move [{:keys [floor] :as state} f]
  (let [floor' (f floor)]
    (cond-> state
      (<= 0 floor' 9) (-> (assoc :floor floor')
                          (update :moves inc)))))

(defmethod key-press :game
  [state]
  (let [k (q/key-as-keyword)]
    (case k
      :q (assoc state :screen-type :intro)
      :ArrowUp   (move state inc)
      :ArrowDown (move state dec)
      state)))
