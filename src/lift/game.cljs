(ns lift.game
  (:require
   [quil.core :as q]
   [lift.config :refer [text-size game-width margin size
                        game-height floor-height]]
   [lift.lift :as lift]
   [lift.methods :refer [draw update-state key-press reset-state]]))


(defn- draw-stats [moves open-doors]
  (q/text-style :normal)
  (q/text-align :left)
  (q/text-size text-size)
  (q/no-stroke)
  (q/fill 0)
  (q/text (str "Moves: " moves)
          (+ game-width margin)
          margin)
  (q/text (str "Doors open: " open-doors)
          (+ game-width margin)
          (* 2 margin)))



(defn- draw-waiters [waiters]
  (q/text-style :normal)
  (q/text-align :left)
  (q/text-size text-size)
  (q/no-stroke)
  (q/fill 0)
  (doseq [[i wtrs] (group-by :start waiters)]
    (let [y (- game-height size (* i floor-height) (/ size 2))]
      (doseq [[j p] (map-indexed vector wtrs)]
        (q/text (:end p)
                (+ (* 0.8 j size) (* 14 size))
                y)))))

(defn- draw-passengers [passengers floor]
  (q/text-style :normal)
  (q/text-align :left)
  (q/text-size text-size)
  (q/no-stroke)
  (q/fill 0)
  (let [y (- game-height size (* floor floor-height) (/ size 2))]
    (doseq [[i p] (map-indexed vector (sort-by :end passengers))]
      (q/text (:end p)
              (+ (* 0.8 i size) (* 2 margin))
              y))))

(defmethod draw :game
  [{:keys [floor moves waiters passengers open-doors]}]
  (q/background 200)
  (lift/draw-game-space)
  (lift/draw-lift floor)
  (lift/draw-floors)
  (draw-stats moves open-doors)
  (draw-waiters waiters)
  (draw-passengers passengers floor))





(defn- move [{:keys [floor] :as state} f]
  (let [floor' (f floor)]
    (cond-> state
      (<= 0 floor' 9) (-> (assoc :floor floor')
                          (update :moves inc)
                          (assoc :made-move? true)))))

(defn- go-out [passengers floor]
  (remove #(= floor (:end %)) passengers))


(defn- open-doors [{:keys [floor waiters] :as state}]
  (let [{new-passengers true, remaining-waiters false}
        (group-by #(= floor (:start %)) waiters)]
    (-> state
        (update :open-doors inc)
        (update :passengers go-out floor)
        (update :passengers into new-passengers)
        (assoc :waiters remaining-waiters))))


(defmethod key-press :game
  [state]
  (let [k (q/key-as-keyword)]
    (case k
      :q         (reset-state :intro)
      :ArrowUp   (move state inc)
      :ArrowDown (move state dec)
      :space     (open-doors state)
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
