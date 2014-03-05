(ns text-tactical.core
  (use [text-tactical.window :only [window create-window]])
  (require [swing-text.ui :as ui]
           [swing-text.events :as events]
           [swing-text.menu :as menu]
           [swing-text.util :as util]
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
  [[x y w h :as rect]]
  (when (not @window)
    (create-window))
  (let [data (repeat (* w h) {:char \.})]
    (.clear! @window)
    (.blit! @window data rect)
    (.repaint @window)))
