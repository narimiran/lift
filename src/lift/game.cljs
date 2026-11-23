(ns lift.game
  (:require [cljs.reader :as r]))


(def lift-capacity 10)
(def wait-limit 50)


(defn- create-waiter []
  (let [start (rand-int 10)
        end (rand-int 10)
        r (rand)
        impatient? (> r 0.9)
        relaxed? (< r 0.15)]
    (if (= start end)
      (create-waiter)
      {:start start
       :end end
       :waiting-time 0
       :max-wait (cond
                   impatient? (long (* 0.84 wait-limit))
                   relaxed? (long (* 1.2 wait-limit))
                   :else wait-limit)
       :trip-time -1
       :impatient? impatient?
       :relaxed? relaxed?})))

(defn- maybe-create-waiter [waiters moves]
  (let [h (quot moves 40)]
    (reduce (fn [w i]
              (cond-> w
                (< (rand) (- 0.35 (* 0.05 i))) (conj (create-waiter))))
            (cond-> waiters
              (< (rand) 0.5) (conj (create-waiter)))
            (range h))))

(defn- load [k]
  (try
    (some-> (.. js/window -localStorage (getItem k))
            r/read-string)
    (catch :default _ nil)))

(defn- save! [k data]
  (try
    (some-> (.. js/window -localStorage (setItem k (pr-str data))))
    (catch :default _ nil)))

(defn reset-state [screen-type]
  (let [best-result (load :lift-best-result)]
    {:screen-type (or screen-type :intro)
     :best-result best-result
     :floor 0
     :moves 0
     :delivered 0
     :score 0
     :message []
     :waiters (repeatedly 3 create-waiter)
     :specials {:clean-floor 0
                :current-dest 0}
     :passengers []
     :finished []}))

(defn maybe-end [{:keys [score moves delivered best-result waiters] :as state}]
  (if (some (fn [{:keys [waiting-time max-wait]}]
              (>= waiting-time max-wait))
            waiters)
    (let [record (if (> score (long (get best-result :score 0)))
                   {:score score
                    :moves moves
                    :delivered delivered}
                   best-result)]
      (save! :lift-best-result record)
      (-> state
          (assoc :screen-type :over)
          (assoc :best-result record)))
    state))

(defn- increase-time [state people field]
  (assoc state people (mapv #(update % field inc) (people state))))

(defn tick [state]
  (-> state
      (update :moves inc)
      (dissoc :message)
      (dissoc :finished)
      (increase-time :waiters :waiting-time)
      (increase-time :passengers :trip-time)
      maybe-end))

(defn move [{:keys [floor moves] :as state} dir]
  (let [f (case dir :up inc :down dec)
        floor' (f floor)]
    (cond
      (<= 0 floor' 9) (-> state
                          tick
                          (assoc :floor floor')
                          (update :waiters maybe-create-waiter moves))
      :else (assoc state :message [:wrong-floor "Can't move there."]))))


(defn- calc-score [{:keys [waiting-time trip-time impatient? relaxed? start end]
                    :as passengers}]
  (let [multi (cond
                impatient? 1.5
                relaxed? 0.8
                :else 1)
        dist (abs (- end start))
        direct? (= trip-time dist)
        score (max 10 (long (* (if direct? 2 1)
                               (- (+ 80 (* dist dist))
                                  (* 2 multi waiting-time)
                                  (* multi trip-time)))))]
    (assoc passengers :score score)))

(defn open-doors [{:keys [floor waiters passengers] :as state}]
  (let [{new-passengers true, remaining-waiters false}
        (group-by #(= floor (:start %)) waiters)
        {trip-done true, remaining-passengers false}
        (group-by #(= floor (:end %)) passengers)
        passengers' (into remaining-passengers new-passengers)
        trip-done' (map calc-score trip-done)
        move-score (reduce + (map :score trip-done'))
        full-cap? (> (count passengers') lift-capacity)]
    (if (and (empty? new-passengers)
             (empty? trip-done))
      (assoc state :message [:door-close "No need to open the door."])
      (-> state
          (assoc :passengers (if full-cap? remaining-passengers passengers'))
          (cond-> (not full-cap?) (assoc :waiters remaining-waiters))
          (update :delivered + (count trip-done))
          (update :score + move-score)
          (cond-> (or (not full-cap?) (seq trip-done)) tick)
          (assoc :finished trip-done')
          (cond-> full-cap? (assoc :message [:full-cap "Can't take all new passengers. Taking nobody."]))))))



(def special-freq
  {:clean-floor 60
   :current-dest 75})

(defn- available-special? [special-name special-value moves]
  (< special-value (quot moves (special-freq special-name))))


(defn clean-longest-floor [{:keys [waiters specials moves] :as state}]
  (if (available-special? :clean-floor (:clean-floor specials) moves)
    (let [longest (->> waiters
                       (group-by :start)
                       (apply max-key (comp count val))
                       first)]
      (-> state
          (update-in [:specials :clean-floor] inc)
          (assoc :waiters (remove (fn [{:keys [start]}]
                                    (= longest start))
                                  waiters))))
    (assoc state :message [:no-clean
                           (str "You're not alowed to remove people from the bussiest floor. "
                                "You need to wait for additional "
                                (mod (- moves) (special-freq :clean-floor))
                                " moves before it is available.")])))


(defn current-dest [{:keys [passengers specials moves floor] :as state}]
  (if (available-special? :current-dest (:current-dest specials) moves)
    (-> state
        (update-in [:specials :current-dest] inc)
        (assoc :passengers (map #(assoc % :end floor) passengers)))
    (assoc state :message [:no-lift
                           (str "You're not alowed to change passengers' destination. "
                                "You need to wait for additional "
                                (mod (- moves) (special-freq :current-dest))
                                " moves before it is available.")])))
