(ns lift.views)


(defn- intro-view [_state]
  [:div "intro"
   [:div "press SPACE for start"]])

(defn- game-view [{:keys [moves passengers waiters floor]}]
  [:div "game state"
   [:div "Moves: " moves]
   [:div "Floor: " floor]
   [:div "Waiters:" waiters]
   [:div "Passengers: " passengers]])


(defn view [state]
  (case (:screen-type state)
    :intro (intro-view state)
    :game (game-view state)))
