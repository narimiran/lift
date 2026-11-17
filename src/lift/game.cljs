(ns lift.game)


(def lift-capacity 10)
(def wait-limit 40)


(defn- create-waiter []
  (let [start (rand-int 10)
        end (rand-int 10)
        r (rand)]
    (if (= start end)
      (create-waiter)
      {:start start
       :end end
       :waiting-time 0
       :impatient? (> r 0.9)
       :relaxed? (< r 0.15)})))

(defn- maybe-create-waiter [waiters]
  (cond-> waiters
    (< (rand) 0.5) (conj (create-waiter))))

(defn reset-state [screen-type]
  {:screen-type (or screen-type :intro)
   :high-score (or (.. js/window -localStorage (getItem :lift-high-score)) 0)
   :floor 0
   :moves 0
   :score 0
   :message []
   :waiters (repeatedly 3 create-waiter)
   :passengers []
   :finished []})

(defn maybe-end [{:keys [moves score high-score waiters] :as state}]
  (cond
    (>= moves 100) (let [record (max score high-score)]
                     (.. js/window -localStorage (setItem :lift-high-score record))
                     (-> state
                         (assoc :screen-type :over)
                         (assoc :high-score record)))
    (>= (reduce max (map :waiting-time waiters)) wait-limit)
    (-> state
        (assoc :screen-type :over)
        (assoc :message [:lose "You left some people waiting for too long."]))
    :else state))

(defn- increase-time [state people field]
  (assoc state people (mapv #(update % field inc) (people state))))

(defn tick [state]
  (-> state
      (update :moves inc)
      (dissoc :message)
      (increase-time :waiters :waiting-time)
      (increase-time :passengers :trip-time)
      maybe-end))

(defn move [{:keys [floor] :as state} dir]
  (let [f (case dir :up inc :down dec)
        floor' (f floor)]
    (cond
      (<= 0 floor' 9) (-> state
                          tick
                          (assoc :floor floor')
                          (update :waiters maybe-create-waiter))
      :else (assoc state :message [:wrong-floor "Can't move there."]))))


(defn- add-passengers [passengers new-passengers]
  (let [new-psgs (map #(assoc % :trip-time -1) new-passengers)] ; `tick` will increment it to 0
    (into passengers new-psgs)))

(defn- calc-stats [{:keys [waiting-time trip-time impatient? relaxed? start end]
                    :as passengers}]
  (let [multi (cond
                impatient? 1.5
                relaxed? 0.8
                :else 1)
        dist (abs (- end start))
        direct? (= trip-time dist)
        score (max 0 (long (- (+ 80 (* dist dist))
                              (* 2 multi waiting-time)
                              (* multi trip-time))))]
    (-> passengers
        (assoc :score (cond-> score
                        direct? (* 2))))))

(defn open-doors [{:keys [floor waiters passengers] :as state}]
  (let [{new-passengers true, remaining-waiters false}
        (group-by #(= floor (:start %)) waiters)
        {trip-done true, remaining-passengers false}
        (group-by #(= floor (:end %)) passengers)
        passengers' (add-passengers remaining-passengers new-passengers)
        trip-done' (map calc-stats trip-done)
        move-score (reduce + (map :score trip-done'))]
    (cond
      (> (count passengers') lift-capacity)
      (-> state
          (assoc :passengers remaining-passengers)
          tick
          (update :finished into trip-done')
          (update :score + move-score)
          (assoc :message [:full-cap "Can't take all new passengers. Taking nobody."]))
      (and (zero? (count new-passengers)) (zero? (count trip-done)))
      (assoc state :message [:door-close "No need to open the door."])
      :else (-> state
                (assoc :passengers passengers')
                (assoc :waiters remaining-waiters)
                tick ; needs to be after changing passengers and waiters
                (update :finished into trip-done')
                (update :score + move-score)))))
