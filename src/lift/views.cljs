(ns lift.views
  (:require
   [lift.game :refer [wait-limit]]))


(defn- intro-view []
  [:div.flex.flex-col.w-full.max-w-128.p-8.items-center.justify-center.bg-orange-100.mx-auto
   [:h1.text-5xl.p-16.font-mono.font-black "Lift"]
   [:button.p-8.font-mono.text-xl.border-1.border-black.rounded-lg.cursor-pointer
    {:on {:click [[:game/start]]}}
    "press SPACE for start"]
   [:span.font-mono.text-sm.p-8 "press H for help"]])





(def colors
  [:slate :orange :amber :lime :emerald :cyan :blue :violet :fuchsia :rose])

(defn- color [t c h]
  (str t (name (colors c)) "-" h))

(defn- bg-color [c & [h]]
  (color "bg-" c (or h 300)))

(defn- text-color [c & [h]]
  (color "text-" c (or h 400)))

(defn- people-view [people {:keys [floor]}]
  (for [{:keys [end impatient? relaxed? x waiting-time]} people]
    [:span.font-mono.border-1.border-black.rounded.text-xl.leading-4.bottom-0
     {:class (cond-> [(bg-color end) "px-[3px]" "mb-[1px]" "pt-[2px]"]
              (= floor end) (conj :font-bold (bg-color end 400))
              impatient? (conj :animate-impatient)
              relaxed? (conj :italic :text-slate-600)
              (and (nil? floor)
                   (> waiting-time (- wait-limit 15))) (conj :animate-pulse))
      :style (when x
               {:position :absolute
                :left (str x "px")})}
     end]))

(defn- lift-view [floor passengers message]
  [:div.absolute.w-32.h-14.ml-4.mb-3.bg-white.border-4.border-black.rounded-lg.transition-all.duration-400
   {:class (case (first message)
             (:full-cap :door-close) [:animate-shake]
             :wrong-floor [:animate-bounce]
             nil)
    :style {:bottom (str "calc(" floor " * 56px)")}}
   [:div.flex.flex-wrap.h-full.gap-1.items-end
    {:class ["px-[1px]"]}
    (people-view passengers {:floor floor})]])

(defn- waiters-positions [waiters]
  (reduce (fn [acc {:keys [waiting-time] :as w}]
            (if-let [prev-x (:x (peek acc))]
              (let [d (- waiting-time (:waiting-time (peek acc)))]
                (conj acc (assoc w :x
                                 #_(+ prev-x (max waiting-time 20))
                                 #_(max waiting-time (+ prev-x 20))
                                 (+ prev-x d 22))))
              (conj acc (assoc w :x waiting-time))))
          []
          waiters))

(defn- floor-row [i waiters]
  [:div.flex.h-14.ml-42.mr-2.items-end
   [:div.flex.flex-1.h-14.items-end.border-b-2.border-black
    [:div.flex.gap-2.items-end
     [:div.text-3xl.font-bold.px-1
      {:class [(text-color i)]}
      i]
     [:div.flex.relative
      (people-view waiters {})]]]])

(defn- floor-view [waiters]
  (let [by-floor (group-by :start waiters)]
    [:div.flex.flex-col-reverse.absolute.inset-0.mb-4
     (for [i (range 10)]
       (floor-row i (->> (get by-floor i)
                         (sort-by :waiting-time)
                         (waiters-positions))))]))

(defn- board-view [{:keys [floor waiters passengers message]}]
  [:div.w-full.max-w-128.bg-white.border-2.border-black.rounded-lg
   [:div.relative.h-146
    [:div.absolute.inset-2.w-36.bg-slate-500.border-2.border-black.rounded-lg]
    (lift-view floor passengers message)
    (floor-view waiters)]])



(defn- stats-view [{:keys [score moves finished message]}]
  [:div
   [:p "Score: " score]
   [:p "Moves: " moves]
   [:p.italic (second message)]
   [:div.mt-4
    ; debugging/experimenting:
    (for [{:keys [waiting-time trip-time impatient? relaxed? score]} finished]
      [:div.flex.gap-1
       [:span "Waited: " waiting-time]
       [:span "Traveled: " trip-time]
       (when impatient? [:span "impatient!"])
       (when relaxed? [:span "relaxed..."])
       [:span "Score: " score]])]])



(defn- button [text on-click]
  [:button.touch-manipulation.cursor-pointer.flex.flex-wrap.h-14.bg-white.hover:bg-slate-300.px-4.border-4.border-black.rounded-lg.font-bold.text-3xl.content-center
   {:on {:click on-click}}
   text])

(defn- button-view []
  [:div.flex.w-full.justify-between
   [:div.flex.gap-3
    (button "↑︎" [[:game/move :up]])
    (button "↓" [[:game/move :down]])]
   [:div.flex.gap-3
    (button "H" [[:game/help]])
    (button "⇄" [[:game/doors]])]])


(defn- game-view [state]
  [:div.flex.flex-col.w-full.max-w-128.bg-slate-300.p-2.gap-2.items-start.mx-auto
   (board-view state)
   (button-view)
   (stats-view state)])





(defn- end-msgs [title l1 l2]
  [:div.gap-8
   [:h1.text-5xl.py-8.justify-self-center.font-black title]
   [:div l1]
   [:div.mb-8 l2]])

(defn- end-view [{:keys [score high-score message]}]
  [:div.flex.flex-col.w-full.max-w-128.p-8.gap-8.justify-center.bg-orange-100.mx-auto
   (if (seq message)
     (end-msgs "Game over!" "You lost." (second message))
     (end-msgs "You win!"
               (list "Current score: " score)
               (list "Your best score so far: " high-score)))
   [:button.p-8.font-mono.border-1.border-black.rounded-lg.cursor-pointer
    {:on {:click [[:game/start]]}}
    "press N for a new game"]])





(defn- psgs [p m]
  [:div.flex.gap-2.items-center
    (people-view [p] {})
    [:span m]])

(defn- help-view []
  [:div.flex.flex-col.w-full.max-w-128.p-4.gap-2.bg-orange-100.mx-auto.items-start
   [:h1.text-3xl.font-bold "Lift: how to play?"]
   [:p "You are a lift moving up and down, taking passengers.
        You have a limited space for 10 persons at time.
        If you want to open your doors to take and drop passengers, press SPACE.
        There are different kinds of passengers:"]
   (psgs {:end 3}  "Regular passenger. Wants to go to the 3rd floor.")
   (psgs {:end 5 :impatient? true} "Impatient passenger. Hates waiting and wasting time.")
   (psgs {:end 8 :relaxed? true} "Relaxed passenger. Doesn't mind waiting longer.")
   (psgs {:end 1 :waiting-time 100} "Waiting for a long time. Pick it up soon (or lose the game).")
   [:p "If you take a passenger directly (no going in wrong direction nor opening
        the doors along the way) to their destination, you receive a bonus.
        The longer the passenger waits and travels, the lower the score.
        The longer the distance between the starting and ending floor, the higher the score."]
   [:p "The game ends after 100 moves. Opening the doors also counts as a move.
        Time doesn't matter, just moves. Take your time."]
   (button "↶" [[:game/back]])])




(defn view [state]
  (case (:screen-type state)
    :intro (intro-view)
    :game (game-view state)
    :over (end-view state)
    :help (help-view)))
