(ns text-tactical.game
  (use [text-tactical.window :only [window]])
  (require [swing-text.ui :as ui]
           [swing-text.events :as events]
           [swing-text.menu :as menu]
           [text-tactical.main-menu :as main-menu]))

(defn exit-game
  []
  (ui/close-all-windows)
  ;;(System/exit 0)
  )

(defn- on-main-start
  []
  ;;(events/pop-keymap! @window)
  (println "Starting game"))
(defn- on-main-settings
  []
  ;;(events/pop-keymap! @window)
  (println "Settings"))
(defn- on-main-exit
  []
  (events/reset-keymap-stack! @window)
  (println "Exiting...")
  (exit-game))
(def main-callbacks
  {:start on-main-start 
   :settings on-main-settings
   :exit on-main-exit})


(defn game-start
  []
  ;;(main-menu :bind win (main-menu-callback win main-menu))
  (main-menu/show-main-menu main-callbacks on-main-exit))
