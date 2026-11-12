(ns lift.methods)


(defmulti update-state :screen-type)
(defmethod update-state :default
  [state]
  state)

(defmulti draw :screen-type)
(defmethod draw :default
  [_state])

(defmulti key-press :screen-type)
(defmethod key-press :default
  [state]
  state)

(defmulti key-release :screen-type)
(defmethod key-release :default
  [state _e]
  state)


(defn reset-state [& [screen-type]]
  {:screen-type (or screen-type :intro)
   :floor 0
   :moves 0
   :open-doors 0
   :waiters []
   :passengers []
   :made-move? false
   :direction 0.03})
