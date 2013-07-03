(ns text-tactical.game
  (use [text-tactical.window :only [window]])
  (require [swing-text.ui :as ui]
           [swing-text.events :as events]
           [swing-text.menu :as menu]
           [text-tactical.main-menu :as main-menu]))

(def ^:private game-state (atom nil))

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


;;===Test code===
(def tileset
  {:floor \.
   :empty \space
   :wall (:white ui/box-glyphs)})
(def base-tile {:type :floor :objects nil :entities nil})
(defn gen-base-map
  [w h]
  {:w w :h h
   :map (repeat (* w h) base-tile)})
(defn walls-around-map
  [map]
  (update-in map [:map] #(map-indexed
                          (fn [i cell]
                            (if (or (< i (:w map))
                                    (> i (- (* (:w map) (:h map)) (:w map)))
                                    (= 0 (mod i (:w map)))
                                    (= (dec (:w map)) (mod i (:w map))))
                              (assoc cell :type :wall)
                              cell))
                          %)))
(def test-map (gen-base-map 5 5))

(defn map->ui-data
  [m]
  (map (comp #(hash-map :char %) tileset :type) (:map m)))

(defn dump-map
  [m]
  (let [lines (map #(apply str %)
                   (partition (:w m)
                              (map (comp tileset :type)
                                   (:map m))))]
    (doseq [line lines]
      (println line))))
