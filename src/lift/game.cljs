(ns lift.game)


(def lift-capacity 10)


(defn- create-waiter [t]
  (let [start (rand-int 10)
        end (rand-int 10)
        r (rand)]
    (if (= start end)
      (create-waiter t)
      {:start start
       :end end
       :created-at t
       :impatient? (> r 0.9)
       :relaxed? (< r 0.15)})))

(defn- maybe-create-waiter [waiters t]
  (cond-> waiters
    (< (rand) 0.5) (conj (create-waiter t))))

(defn reset-state [screen-type]
  {:screen-type (or screen-type :intro)
   :high-score (or (.. js/window -localStorage (getItem :lift-high-score)) 0)
   :floor 0
   :moves 0
   :score 0
   :open-doors 0
   :message []
   :waiters (repeatedly 3 #(create-waiter 0))
   :passengers []
   :finished []})

(defn maybe-end [{:keys [moves score high-score] :as state}]
  (if (> moves 100)
    (let [record (max score high-score)]
      (.. js/window -localStorage (setItem :lift-high-score record))
      (-> state
          (assoc :screen-type :over)
          (assoc :high-score record)))
    state))

(defn move [{:keys [floor moves] :as state} dir]
  (let [f (case dir :up inc :down dec)
        floor' (f floor)]
    (cond
      (<= 0 floor' 9) (-> state
                          (assoc :floor floor')
                          (dissoc :message)
                          (update :moves inc)
                          (update :waiters maybe-create-waiter moves))
      :else (assoc state :message [:wrong-floor "Can't move there."]))))


(defn- add-passengers [passengers new-passengers t]
  (let [new-psgs (map #(assoc % :trip-start t) new-passengers)]
    (into passengers new-psgs)))

(defn- calc-stats [{:keys [created-at trip-start impatient? relaxed?
                           start end] :as passengers} t]
  (let [waited (- trip-start created-at)
        traveled (- t trip-start 1)
        multi (cond
                impatient? 1.5
                relaxed? 0.8
                :else 1)
        direct? (= traveled (abs (- end start)))
        score (max 0 (long (- 100 (* 2 multi waited) (* multi traveled))))]
    (-> passengers
        (assoc :waited waited)
        (assoc :traveled traveled)
        (assoc :direct? direct?) ; needed just for debugging/experimenting
        (assoc :score (cond-> score
                        direct? (* 2))))))

(defn open-doors [{:keys [floor waiters passengers moves] :as state}]
  (let [{new-passengers true, remaining-waiters false}
        (group-by #(= floor (:start %)) waiters)
        {trip-done true, remaining-passengers false}
        (group-by #(= floor (:end %)) passengers)
        passengers' (add-passengers remaining-passengers new-passengers moves)
        trip-done' (map #(calc-stats % moves) trip-done)
        score (reduce + (map :score trip-done'))]
    (cond
      (> (count passengers') lift-capacity)
      (-> state
          (assoc :passengers remaining-passengers)
          (update :finished into trip-done')
          (update :score + score)
          (assoc :message [:full-cap "Can't take all new passengers. Taking nobody."]))
      (and (zero? (count new-passengers)) (zero? (count trip-done)))
      (assoc state :message [:door-close "No need to open the door."])
      :else (-> state
                (update :open-doors inc)
                (update :moves inc)
                (dissoc :message)
                (assoc :passengers passengers')
                (update :finished into trip-done')
                (update :score + score)
                (assoc :waiters remaining-waiters)))))
