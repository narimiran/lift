(ns lift.game
  (:require
   [quil.core :as q]
   [lift.config :refer [text-size game-width margin size
                        game-height floor-height]]
   [lift.lift :as lift]
   [lift.methods :refer [draw update-state key-press reset-state]]))


(defn- draw-moves [moves]
  (q/text-style :normal)
  (q/text-align :left)
  (q/text-size text-size)
  (q/no-stroke)
  (q/fill 0)
  (q/text (str "Moves: " moves)
          (+ game-width margin)
          margin))

(defn- draw-waiters [waiters]
  (q/text-style :normal)
  (q/text-align :left)
  (q/text-size text-size)
  (q/no-stroke)
  (q/fill 0)
  (doseq [[i psngs] (group-by :start waiters)]
    (let [y (- game-height size (* i floor-height) (/ size 2))]
      (doseq [[j p] (map-indexed vector psngs)]
        (q/text (:end p)
                (+ (* 0.8 j size) (* 14 size))
                y)))))




(defmethod draw :game
  [{:keys [floor moves waiters]}]
  (q/background 200)
  (lift/draw-game-space)
  (lift/draw-lift floor)
  (lift/draw-floors)
  (draw-moves moves)
  (draw-waiters waiters))


(defn- move [{:keys [floor] :as state} f]
  (let [floor' (f floor)]
    (cond-> state
      (<= 0 floor' 9) (-> (assoc :floor floor')
                          (update :moves inc)
                          (assoc :made-move? true)))))

(defmethod key-press :game
  [state]
  (let [k (q/key-as-keyword)]
    (case k
      :q         (reset-state :intro)
      :ArrowUp   (move state inc)
      :ArrowDown (move state dec)
      state)))


(defn create-waiter []
  (let [start (rand-int 10)
        end (rand-int 10)]
    (if (= start end)
      (create-waiter)
      {:start start
       :end end})))

(defn maybe-create-waiter [waiters]
  (cond-> waiters
    (< (rand) 0.5) (conj (create-waiter))))


(defmethod update-state :game
  [{:keys [made-move?] :as state}]
  (cond-> state
    made-move? (-> (update :waiters maybe-create-waiter)
                   (assoc :made-move? false))))
