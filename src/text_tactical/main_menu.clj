(ns text-tactical.main-menu
  (use [text-tactical.window :only [window]])
  (require [swing-text.ui :as ui]
           [swing-text.events :as events]
           [swing-text.menu :as menu]
           [text-tactical.menu-data :as menu-data]))

(defn- main-menu-callback
  [main-menu activate-callbacks cancel-callback]
  (menu/create-menu-callback
   {:before (fn [] 
              (.clear! @window)
              (main-menu :draw @window)
              (.repaint @window))
    :activate activate-callbacks
    :cancel (fn [_] ;;take in an ID, but ignore it
              (println "Main Menu cancel")
              (cancel-callback))}))

(def main-menu (apply menu/create-menu menu-data/main-menu-data))

(defn show-main-menu
  [activate-callbacks cancel-callback]
  (.clear! @window)
  (main-menu :push-bindings @window (main-menu-callback main-menu activate-callbacks cancel-callback))
  (main-menu :draw @window)
  (.repaint @window))
