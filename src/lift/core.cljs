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
                          " " [[:dom/prevent-default] [:game/start]]
                          "h" [[:game/help]])
                 :over (case k
                         "n" [[:game/start]]
                         "h" [[:game/help]])
                 :help (case k
                         ("h" "q" " ") [[:dom/prevent-default] [:game/back]])
                 :game (case k
                         " " [[:game/tic [:game/doors]]]
                         ("w" "ArrowUp")   [[:game/tic [:game/move :up]]]
                         ("s" "ArrowDown") [[:game/tic [:game/move :down]]]
                         "h" [[:game/help]]
                         "q" [[:game/quit]])))
   :game/tic   (fn [_ action] [[:dom/prevent-default] action [:game/over?]])
   :game/start (fn [_] [[:store/reset (game/reset-state :game)]])
   :game/quit  (fn [_] [[:store/reset (game/reset-state :intro)]])
   :game/help  (fn [state] [[:store/reset (-> state
                                              (assoc :old-screen (:screen-type state))
                                              (assoc :screen-type :help))]])
   :game/back  (fn [state] [[:store/reset (assoc state :screen-type (:old-screen state))]])
   :game/doors (fn [state] [[:store/reset (game/open-doors state)]])
   :game/move  (fn [state dir] [[:store/reset (game/move state dir)]])
   :game/over? (fn [state] [[:store/reset (game/maybe-end state)]])})


(def effects
  {:dom/prevent-default (fn [{:keys [dispatch-data]} _]
                          (some-> (:replicant/dom-event dispatch-data)
                                  .preventDefault))
   :store/reset (fn [_ store new-state]
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
