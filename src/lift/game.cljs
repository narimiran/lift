(ns lift.game)


(def lift-capacity 10)


(defn reset-state [& [screen-type]]
  {:screen-type (or screen-type :intro)
   :floor 0
   :moves 0
   :open-doors 0
   :waiters []
   :passengers []
   :stats []})


(defn- create-waiter [t]
  (let [start (rand-int 10)
        end (rand-int 10)]
    (if (= start end)
      (create-waiter t)
      {:start start
       :end end
       :created-at t})))

(defn- maybe-create-waiter [waiters t]
  (cond-> waiters
    (< (rand) 0.5) (conj (create-waiter t))))

(defn move [{:keys [floor moves] :as state} dir]
  (let [f (case dir :up inc :down dec)
        floor' (f floor)]
    (cond-> state
      (<= 0 floor' 9) (-> (assoc :floor floor')
                          (dissoc :message)
                          (update :moves inc)
                          (update :waiters maybe-create-waiter moves)))))


(defn- add-passengers [passengers new-passengers t]
  (let [new-psgs (map #(assoc % :trip-start t) new-passengers)]
    (into passengers new-psgs)))

(defn- calc-stats [{:keys [created-at trip-start]} t]
  {:waited (- trip-start created-at)
   :traveled (- t trip-start 1)})

(defn open-doors [{:keys [floor waiters passengers moves] :as state}]
  (let [{new-passengers true, remaining-waiters false}
        (group-by #(= floor (:start %)) waiters)
        {trip-done true, remaining-passengers false}
        (group-by #(= floor (:end %)) passengers)
        passengers' (add-passengers remaining-passengers new-passengers moves)
        trip-done' (map #(calc-stats % moves) trip-done)]
    (if (> (count passengers') lift-capacity)
      (assoc state :message "At full capacity. Can't take new passengers.")
      (-> state
          (update :open-doors inc)
          (update :moves inc)
          (assoc :passengers passengers')
          (update :stats into trip-done')
          (assoc :waiters remaining-waiters)))))
