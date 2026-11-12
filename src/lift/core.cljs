(ns lift.core
  (:require
   [quil.core :as q]
   [quil.middleware :as m]
   [lift.drawings :refer [scene-width scene-height text-size]]
   [lift.methods :refer [update-state draw key-press key-release reset-state]]))


(defn- prevent-defaults []
  (.addEventListener
   js/document.body
   "keydown"
   (fn [e]
     (when (#{9 32         ; tab, space
              37 38 39 40  ; arrows
              87 65 83 68} ; wasd
            (.-keyCode e))
       (.preventDefault e)))))

(defn- settings []
  (q/smooth 8))

(defn- setup []
  (prevent-defaults)
  (q/frame-rate 60)
  (q/no-stroke)
  (q/ellipse-mode :corner)
  (q/text-font "monospace" text-size)
  (reset-state))


(defn -main []
  (q/sketch
   :host "app"
   :size [scene-width scene-height]
   :settings #'settings
   :setup #'setup
   :update #'update-state
   :draw #'draw
   :key-pressed #'key-press
   :key-released #'key-release
   :middleware [m/fun-mode]))
