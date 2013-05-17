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
  []
  (println "Starting game"))
(defn- main-do-settings
  []
  (println "Settings"))
(defn- do-exit
  []
  (println "Exiting...")
  (exit-game))

(defn- main-menu-callback
  [win main-menu]
  (menu/create-menu-callback
   {:before (fn [] 
              (.clear! win)
              (main-menu :draw win)
              (.repaint win))
    :activate {:start main-do-start
               :settings main-do-settings
               :exit do-exit}
    :cancel (fn []
              (println "Main Menu cancel")
              (do-exit))}))

(def main-menu (apply menu/create-menu menu-data/main-menu-data))

(defn game-start
  [win]
  (.clear! win)
  (main-menu :bind win (main-menu-callback win main-menu))
  (main-menu :draw win)
  (.repaint win))
  
