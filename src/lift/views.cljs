(ns lift.views)

(def colors
  [:slate :orange :amber :lime :emerald :cyan :blue :violet :fuchsia :rose])

(defn bg-color [c & [h]]
  (str "bg-" (name (colors c)) "-" (or h 300)))

(defn text-color [c & [h]]
  (str "text-" (name (colors c)) "-" (or h 400)))

(defn- intro-view [_state]
  [:div.flex.flex-col.h-168.w-200.m-2.p-8.items-center.justify-center.bg-orange-100
   [:h1.text-5xl.p-16.font-mono.font-black "Lift"]
   [:button.btn.p-8.font-mono.text-xl.border-1.border-black.rounded-lg.cursor-pointer
    {:on {:click [[:game/start]]}}
    "press SPACE for start"]
   [:span.font-mono.text-sm.p-16 "press H for help"]])



(defn- people-view [people & [floor]]
  (for [{:keys [end impatient? relaxed? x]} people]
    [:span.font-mono.border-1.border-black.rounded.text-xl.leading-5.bottom-0
     {:class (cond-> [(bg-color end) "px-[3px]" "mb-[2px]"]
              (= floor end) (conj :font-bold (bg-color end 400))
              impatient? (conj :animate-impatient)
              relaxed? (conj :italic :text-gray-600))
      :style (when x
               {:position :absolute
                :left (str x "px")})}
     end]))

(defn- lift-view [floor passengers message]
  [:div.absolute.w-32.h-16.ml-6.mb-5.bg-white.border-4.border-black.rounded-lg.transition-all.duration-400
   {:class (case (first message)
             (:full-cap :door-close) [:animate-shake]
             :wrong-floor [:animate-bounce]
             nil)
    :style {:bottom (str "calc(" floor " * 4rem)")}}
   [:div.flex.flex-wrap.h-full.gap-1.items-end
    {:class ["px-[1px]"]}
    (people-view passengers floor)]])

(defn- waiters-positions [waiters]
  (reduce (fn [acc {:keys [created-at] :as w}]
            (if-let [prev-x (:x (peek acc))]
              (let [d (- created-at (:created-at (peek acc)))]
                (conj acc (assoc w :x (+ prev-x d 20))))
              (conj acc (assoc w :x 0))))
          []
          waiters))

(defn- floor-row [i waiters]
  [:div.flex.h-16.ml-42.mr-2.items-end
   [:div.flex.flex-1.h-16.items-end.border-b-2.border-black
    [:div.flex.gap-2.items-end
     [:div.text-3xl.font-bold.px-1
      {:class [(text-color i)]}
      i]
     [:div.flex.relative
      (people-view waiters)]]]])


(defn- floor-view [waiters]
  (let [by-floor (group-by :start waiters)]
    [:div.flex.flex-col-reverse.absolute.inset-0.mb-6
     (for [i (range 10)]
       (floor-row i (->> (get by-floor i)
                         (sort-by :created-at)
                         (waiters-positions))))]))

(defn- board-view [{:keys [floor waiters passengers message]}]
  [:div.w-100.bg-white.border-2.border-black.rounded-lg
   [:div.relative.h-170
    [:div.absolute.inset-4.w-36.bg-gray-500.border-2.border-black.rounded-lg]
    (lift-view floor passengers message)
    (floor-view waiters)]])

(defn- stats-view [{:keys [score moves doors finished message]}]
  [:div
   [:p "Score: " score]
   [:p "Moves: " moves]
   [:p "Doors open: " doors]
   [:p.italic (second message)]
   [:div.mt-4
    ; debugging/experimenting:
    (for [{:keys [waited traveled impatient? relaxed? direct? score]} finished]
      [:div.flex.gap-1
       [:span "Waited: " waited]
       [:span "Traveled: " traveled]
       (when direct? [:span "direct!"])
       (when impatient? [:span "impatient!"])
       (when relaxed? [:span "relaxed..."])
       [:span "Score: " score]])]])


(defn- button [text on-click]
  [:button.flex.flex-wrap.btn.h-12.bg-white.hover:bg-gray-300.px-8.border-4.border-black.rounded-lg.font-bold.text-2xl.content-center.cursor-pointer
   {:on {:click on-click}}
   text])

(defn button-view []
  [:div.flex.h-12.gap-4.justify-between.items-stretch
   (button "⇧" [[:game/move :up]])
   (button "⇦ ⇨" [[:game/doors]])
   (button "⇩" [[:game/move :down]])])



(defn game-view [state]
  [:div.flex.h-192.w-200.bg-gray-300.m-2.p-4.gap-8.items-start
   [:div.flex.flex-col.gap-2
    (board-view state)
    (button-view)]
   (stats-view state)])



(defn end-view [{:keys [score high-score]}]
  [:div.flex.flex-col.h-168.w-200.m-2.p-8.items-center.justify-center.bg-orange-100
   [:h1.text-5xl.p-16.font-black "Game over!"]
   [:div "Current score: " score]
   [:div.mb-8 "Your best score so far: " high-score]
   [:button.btn.p-8.font-mono.border-1.border-black.rounded-lg.cursor-pointer
    {:on {:click [[:game/start]]}}
    "press SPACE to play again"]])


(defn help-view []
  [:div.flex.flex-col.h-168.w-200.m-2.p-8.gap-2.bg-orange-100
   [:h1.text-3xl "Lift: how to play?"]
   [:p "You are a lift moving up and down, taking passengers.
        You have a limited space for 10 persons at time.
        If you want to open your doors to take and drop passengers, press SPACE.
        There are different kinds of passengers:"]
   [:div.flex.gap-2
    (people-view [{:end 3}])
    [:span "Regular passenger. Wants to go to the 3rd floor."]]
   [:div.flex.gap-2
    (people-view [{:end 5 :impatient? true}])
    [:span "Impatient passenger. Hates waiting and wasting time."]]
   [:div.flex.gap-2
    (people-view [{:end 8 :relaxed? true}])
    [:span "Relaxed passenger. Doesn't mind waiting longer."]]
   [:p "If you take a passenger directly (no going in wrong direction nor opening
        the doors along the way) to their destination, you receive a bonus.
        The longer the passenger waits and travels, the lower the score.
        The longer the distance between the starting and ending floor, the higher the score."]
   [:p "The game ends after 100 moves. Opening the doors also counts as a move.
        Time doesn't matter, just moves. Take your time."]])

(defn view [state]
  (case (:screen-type state)
    :intro (intro-view state)
    :game (game-view state)
    :over (end-view state)
    :help (help-view)))
