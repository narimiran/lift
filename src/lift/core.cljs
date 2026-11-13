(ns lift.core
  (:require
   [nexus.core :as nexus]
   [replicant.dom :as r]
   [lift.game :as game]
   [lift.views :as views]))


(def actions
  {:game/key (fn [state k]
               (case (:screen-type state)
                 :intro (case k
                          " " [[:dom/prevent-default] [:game/start]]
                          nil)
                 :game (case k
                         " " [[:dom/prevent-default] [:game/doors]]
                         ("w" "ArrowUp")   [[:dom/prevent-default] [:game/move :up]]
                         ("s" "ArrowDown") [[:dom/prevent-default] [:game/move :down]]
                         "q" [[:dom/prevent-default] [:game/quit]]
                         nil)
                 nil))
   :game/start (fn [_] [[:store/reset (game/reset-state :game)]])
   :game/quit  (fn [_] [[:store/reset (game/reset-state :intro)]])
   :game/doors (fn [state] [[:store/reset (game/open-doors state)]])
   :game/move  (fn [state dir] [[:store/reset (game/move state dir)]])})


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
