(ns text-tactical.core
  (require [swing-text.ui :as ui]
           [swing-text.events :as events]))

(def welcome-msg "Welcome to Text Tactical!")
(def menu-keys {"Exit" {:key "ESCAPE"
                        :on-press (fn [e]
                                    (println "Exiting")
                                    (ui/close-all-windows)
                                    (System/exit 0))}})

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
     (fn []
       (println "Closing windows")
       (ui/close-all-windows)))))