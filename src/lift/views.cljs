(ns lift.views)


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
  (for [{:keys [end impatient? relaxed? waiting-time max-wait]} people]
    (let [waiter? (nil? floor)
          time-remaining (- max-wait waiting-time)
          short-time? (and waiter? (<= time-remaining 20))
          urgent? (and waiter? (<= time-remaining 5))]
      [:div.flex.flex-col-reverse.items-end
       [:span.font-mono.border-1.border-black.rounded.text-xl.leading-4.bottom-0
        {:class (cond-> [(bg-color end) "px-[3px]" "mb-[1px]" "pt-[2px]"]
                  (= floor end) (conj :font-black (bg-color end 400))
                  impatient? (conj :animate-impatient)
                  relaxed? (conj :italic :text-slate-600)
                  urgent? (conj :animate-urgent))}
        end]
       (when short-time?
         [:span.flex.font-mono.text-xs.leading-2.italic time-remaining])])))


(defn- lift-view [floor passengers message finished]
  [:div.absolute.w-30.h-13.ml-2.mb-3.bg-white.border-4.border-black.rounded-lg.transition-all.duration-400
   {:class (case (first message)
             (:full-cap :door-close) [:animate-shake]
             :wrong-floor [:animate-bounce]
             nil)
    :style {:bottom (str "calc(" (* 52 floor) "px)")}}
   [:div.flex.flex-wrap.h-full.items-end
    {:class ["pl-[2px]" "pt-[1px]" "gap-[2px]"]}
    (people-view passengers {:floor floor})]
   (for [[i {:keys [score]}] (map-indexed vector finished)]
     [:span.absolute.-top-6.font-mono.font-bold.animate-float-up
      {:class [(text-color floor)]
       :style {:left (str "calc(5% + " (* 40 i) "px)")}}
      (str "+" score)])])

(defn- floor-row [i waiters]
  [:div.flex.h-13.ml-34.mr-2.items-end
   [:div.flex.flex-1.h-14.items-end.border-b-2.border-black
    [:div.flex.gap-2.items-end
     [:div.text-3xl.font-mono.font-black.leading-6
      {:class [(text-color i)]}
      i]
     [:div.flex.relative.gap-1
      (people-view waiters {})]]]])

(defn- floor-view [waiters]
  (let [by-floor (group-by :start waiters)]
    [:div.flex.flex-col-reverse.absolute.inset-0.mb-4
     (for [i (range 10)]
       (floor-row i (->> (get by-floor i)
                         (sort-by :waiting-time >))))]))

(defn- board-view [{:keys [floor waiters passengers message finished]}]
  [:div.w-full.max-w-128.bg-white.border-1.border-black.rounded-lg
   [:div.relative.h-136
    [:div.absolute.inset-1.w-32.bg-slate-500.border-2.border-black.rounded-lg]
    (lift-view floor passengers message finished)
    (floor-view waiters)]])



(defn- stats-view [{:keys [score moves message]}]
  [:div
   [:p "Score: " score]
   [:p "Moves: " moves]
   [:p.italic (second message)]])


(def icons
  {:up    "M6 15l6-6 6 6"
   :down  "M6 9l6 6 6-6"
   :doors "M9 7l-5 5 5 5M15 7l5 5-5 5"})

(defn icon [kind]
  [:svg {:viewBox "0 0 24 24"
         :width "48"
         :height "48"
         :fill "none"
         :stroke "currentColor"
         :stroke-width "2"
         :stroke-linecap "round"
         :stroke-linejoin "round"}
   [:path {:d (icons kind)}]])

(defn- button [text on-click]
  [:button.touch-manipulation.cursor-pointer.flex.flex-wrap.h-12.aspect-square.bg-white.hover:bg-slate-300.border-2.border-black.rounded-lg.font-mono.justify-center.text-2xl.content-center
   {:on {:click on-click}}
   (if (= (type text) Keyword)
     (icon text)
     text)])

(defn- button-view []
  [:div.flex.w-full.justify-between
   [:div.flex.gap-2
    (button :up [[:game/move :up]])
    (button :down [[:game/move :down]])]
   [:div.flex.gap-2
    (button :doors [[:game/doors]])]])


(defn- game-view [state]
  [:div.flex.flex-col.w-full.max-w-128.bg-slate-300.p-1.gap-1.items-start.mx-auto
   (board-view state)
   (button-view)
   [:div.flex.w-full.justify-between
    (stats-view state)
    [:div.flex.gap-2
     (button "C" [[:game/clean-floor]])
     (button "L" [[:game/current-dest]])
     (button "H" [[:game/screen :help]])]]])



(defn- end-view [{:keys [score moves high-score]}]
  [:div.flex.flex-col.w-full.max-w-128.p-4.gap-8.justify-center.bg-orange-100.mx-auto
   [:h1.text-5xl.py-8.justify-self-center.font-mono.font-black "Game over"]
   [:span.font-mono "Current score: " score " (" moves " moves)."]
   [:span.font-mono.mb-8  "Your best score so far: " high-score]
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
   (psgs {:end 3 :max-wait 100}  "Regular passenger. Wants to go to the 3rd floor.")
   (psgs {:end 5 :max-wait 100 :impatient? true} "Impatient passenger. Hates waiting and wasting time.")
   (psgs {:end 8 :max-wait 100 :relaxed? true} "Relaxed passenger. Doesn't mind waiting longer.")
   (psgs {:end 1 :waiting-time 30 :max-wait 39} "Waiting for a long time. Pick it up soon (here you have 9 more moves or you'll lose the game). Once in the lift, you're safe.")
   [:p "If you take a passenger directly (no going in wrong direction nor opening
        the doors along the way) to their destination, you receive a bonus.
        The longer the passenger waits and travels, the lower the score.
        The longer the distance between the starting and ending floor, the higher the score."]
   [:p "The game ends when a passenger waits for too long. Opening the doors also counts as a move."]
   [:p "Time doesn't matter, just moves. Take your time, choose passengers and your paths carefuly."]

   [:h2.text-xl.font-bold.mt-4 "Specials"]
   [:div.flex.gap-2
    [:h3.font-bold "Clean floor (C)"]
    [:p "Remove all passengers from the bussiest floor. Available every 100 moves."]]
   [:div.flex.gap-2
    [:h3.font-bold "Current destination (L)"]
    [:p "All lift passengers go to the current floor. Available every 60 moves."]]

   [:div.mt-4 (button "↶" [[:game/back]])]])




(defn view [state]
  (case (:screen-type state)
    :intro (intro-view)
    :game (game-view state)
    :over (end-view state)
    :help (help-view)))
