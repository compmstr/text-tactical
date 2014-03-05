(ns text-tactical.core
  (use [text-tactical.window :only [window create-window]])
  (require [swing-text.ui :as ui]
           [swing-text.events :as events]
           [swing-text.menu :as menu]
           [text-tactical.window :as window]
           [text-tactical.game :as game]))

(def welcome-msg "Welcome to Text Tactical!")

(defn -main
  [& args]
  (println "Text tactical game")
  (create-window)
  (.cursor-pos! @window [(- 40 (quot (count welcome-msg) 2)) 10])
  (.write! @window welcome-msg)
  (.repaint @window)
  (Thread/sleep 250)
  (events/on-any-key
   (.component @window)
   game/game-start))

(defn test-blit
  [x y w h]
  (when (not @window)
    (create-window))
  (.clear! @window)
  (.blit! @window (repeat (* w h) {:char \.})
         [x y w h])
  (.repaint @window))
