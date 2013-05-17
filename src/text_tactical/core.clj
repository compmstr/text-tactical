(ns text-tactical.core
  (require [swing-text.ui :as ui]
           [swing-text.events :as events]
           [swing-text.menu :as menu]))

(def welcome-msg "Welcome to Text Tactical!")
(def menu-keys {"Exit" {:key "ESCAPE"
                        :on-press (fn [e]
                                    (println "Exiting")
                                    (ui/close-all-windows)
                                    (System/exit 0))}})

(defn exit-game
  []
  (ui/close-all-windows)
  ;;(System/exit 0)
  )
(def main-menu-data ["Main Menu"
                     [{:name "start"
                       :label "Start Game"
                       :callback (fn [] (println "Start the game"))}
                      {:name "settings"
                       :label "Settings"
                       :callback (fn [] (println "Settings"))}
                      {:name "exit"
                       :label "Exit"
                       :callback exit-game}]])

(defn- main-menu-callback
  [win main-menu]
  (fn [{:keys [action selected]}]
    (.clear! win)
    (main-menu :draw win)
    (.repaint win)
    (case action
      :activate
      (println selected "activated")
      :cancel
      (do
        (println "Main menu cancel")
        (exit-game))
      nil)))

(defn -main
  [& args]
  (println "Text tactical game")
  (let [win (ui/text-console {:title "Text Tactical"})
        main-menu (apply menu/create-menu main-menu-data)]
    (.cursor-pos! win [(- 40 (quot (count welcome-msg) 2)) 10])
    (.write! win welcome-msg)
    (.repaint win)
    (Thread/sleep 250)
    (events/on-any-key
     (.component win)
     (fn []
       (.clear! win)
       (main-menu :bind win (main-menu-callback win main-menu))
       (main-menu :draw win)
       (.repaint win)))))