(ns lift.core
  (:require
   [nexus.core :as nexus]
   [replicant.dom :as r]
   [lift.game :as game]
   [lift.views :as views]))


(def actions
  {:game/key (fn [{:keys [screen-type]} k]
               (case screen-type
                 :intro (case k
                          " " [[:game/start]]
                          "h" [[:game/screen :help]])
                 :over (case k
                         "n" [[:game/start]]
                         "h" [[:game/screen :help]])
                 :help (case k
                         ("h" "q" " ") [[:game/back]])
                 :game (case k
                         " " [[:game/doors]]
                         ("w" "ArrowUp")   [[:game/move :up]]
                         ("s" "ArrowDown") [[:game/move :down]]
                         "c" [[:game/clean-floor]]
                         "l" [[:game/random-lift]]
                         "h" [[:game/screen :help]]
                         "q" [[:game/screen :intro]])))

   :game/start (fn [_] [[:store/reset (game/reset-state :game)]])
   :game/screen (fn [state screen] [[:store/reset (-> state
                                                      (assoc :old-screen (:screen-type state))
                                                      (assoc :screen-type screen))]])
   :game/back  (fn [state] [[:store/reset (assoc state :screen-type (:old-screen state))]])
   :game/doors (fn [state] [[:store/reset (game/open-doors state)]])
   :game/move  (fn [state dir] [[:store/reset (game/move state dir)]])

   :game/clean-floor (fn [_] [[:game/specials game/clean-longest-floor]])
   :game/random-lift (fn [_] [[:game/specials game/random-lift]])
   :game/specials (fn [state f] [[:store/reset (f state)] #_[:game/screen :game]])})


(def effects
  {:store/reset (fn [{:keys [dispatch-data]} store new-state]
                  (some-> (:replicant/dom-event dispatch-data)
                          .preventDefault)
                  (reset! store new-state))})


(def nex
  {:nexus/system->state deref
   :nexus/actions actions
   :nexus/effects effects})


(defn main [nex store]
  (let [element (js/document.getElementById "app")]
    (r/set-dispatch!
     (fn [replicant-data action-data]
       (nexus/dispatch nex store replicant-data action-data)))
    (js/document.addEventListener
     "keydown"
     (fn [e]
       (nexus/dispatch nex store
                       {:replicant/dom-event e}
                       [[:game/key (.-key e)]])))
    (add-watch store
               ::render
               (fn [_ _ _ new-state]
                 (r/render element (views/view new-state))))
    (r/render element (views/view @store))))
