(ns text-tactical.game
  (require [swing-text.ui :as ui]
           [swing-text.events :as events]
           [swing-text.menu :as menu]
           [text-tactical.menu-data :as menu-data]))

(defn exit-game
  []
  (ui/close-all-windows)
  ;;(System/exit 0)
  )

(defn- main-do-start
  [win]
  ;;(events/pop-keymap! win)
  (println "Starting game"))
(defn- main-do-settings
  [win]
  ;;(events/pop-keymap! win)
  (println "Settings"))
(defn- do-exit
  [win]
  (events/reset-keymap-stack! win)
  (println "Exiting...")
  (exit-game))

(defn- main-menu-callback
  [win main-menu]
  (menu/create-menu-callback
   {:before (fn [] 
              (.clear! win)
              (main-menu :draw win)
              (.repaint win))
    :activate {:start (partial main-do-start win)
               :settings (partial main-do-settings win)
               :exit (partial do-exit win)}
    :cancel (fn []
              (println "Main Menu cancel")
              (do-exit win))}))

(def main-menu (apply menu/create-menu menu-data/main-menu-data))

(defn game-start
  [win]
  (.clear! win)
  ;;(main-menu :bind win (main-menu-callback win main-menu))
  (main-menu :push-bindings win (main-menu-callback win main-menu))
  (main-menu :draw win)
  (.repaint win))
  
