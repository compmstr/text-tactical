(ns text-tactical.core
  (require [swing-text.ui :as ui]
           [swing-text.events :as events]
           [swing-text.menu :as menu]
           [text-tactical.game :as game]))

(def welcome-msg "Welcome to Text Tactical!")

(defn -main
  [& args]
  (println "Text tactical game")
  (let [win (ui/text-console {:title "Text Tactical"})]
    (.cursor-pos! win [(- 40 (quot (count welcome-msg) 2)) 10])
    (.write! win welcome-msg)
    (.repaint win)
    (Thread/sleep 250)
    (events/on-any-key
     (.component win)
     #(game/game-start win))))