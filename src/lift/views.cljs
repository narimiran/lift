(ns lift.views)


(defn- intro-view [_state]
  [:div.flex.flex-col.h-168.w-200.m-2.p-8.items-center.justify-center.bg-orange-100
   [:h1.text-5xl.p-16.font-black "Lift"]
   [:div.p-16.font-mono "press SPACE for start"]])




(defn- lift-view [floor passengers message]
  [:div.absolute.w-40.h-14.ml-6.mb-5.bg-white.border-4.border-black.rounded-lg.transition-all.duration-400
   {:class (case (first message)
             (:full-cap :door-close) [:animate-shake]
             :wrong-floor [:animate-bounce]
             nil)
    :style {:bottom (str "calc(" floor " * 4rem)")}}
   [:div.flex.h-full.px-1.gap-1.items-end
    (for [end (map :end passengers)]
      [:span.font-mono
       {:class (when (= floor end) [:font-bold])}
       end])]])

(defn- floor-row [i waiters]
  [:div.flex.h-16.ml-50.mr-2.items-end
   [:div.flex.flex-1.h-16.items-end.border-b-2.border-black
    [:div.flex.gap-3.items-end
     [:div.text-3xl.font-bold i]
     [:div.flex.gap-1
      (for [end (map :end waiters)]
        [:span.font-mono end])]]]])


(defn- floor-view [waiters]
  (let [by-floor (group-by :start waiters)]
    [:div.flex.flex-col-reverse.absolute.inset-0.mb-6
     (for [i (range 10)]
       (floor-row i (get by-floor i)))]))

(defn- board-view [{:keys [floor waiters passengers message]}]
  [:div.w-100.bg-white.border-2.border-black.rounded-lg
   [:div.relative.h-168
    [:div.absolute.inset-4.w-44.bg-gray-500.border-2.border-black.rounded-lg]
    (lift-view floor passengers message)
    (floor-view waiters)]])

(defn- stats-view [{:keys [moves open-doors stats message]}]
  [:div.p-4
   [:p "Moves: " moves]
   [:p "Doors open: " open-doors]
   [:p.italic (second message)]
   [:div.mt-4
    (for [{:keys [waited traveled]} stats]
      [:p "Waited: " waited "; Traveled: " traveled])]])

(defn game-view [state]
  [:div.flex.h-178.w-200.bg-gray-300.m-2.p-4.gap-4.items-start
   (board-view state)
   (stats-view state)])


(defn view [state]
  (case (:screen-type state)
    :intro (intro-view state)
    :game (game-view state)))
