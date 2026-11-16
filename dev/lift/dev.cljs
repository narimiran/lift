(ns lift.dev
  (:require
   [dataspex.core :as dataspex]
   [nexus.action-log :as action-log]
   [lift.core :as lift]
   [lift.game :as game]))


(defn inspect-actions [nex]
  (let [log (action-log/create-log {:max-age {:minutes 15}
                                    :max-entries 25})]
    (action-log/install-logger nex log)))


(defonce store (atom (game/reset-state :intro)))
(def nex (inspect-actions lift/nex))


(defn configure []
  (dataspex/inspect "Game state" store {:track-changes? false
                                        :history-limit 25}))

(defn main []
  (configure)
  (lift/main nex store))
