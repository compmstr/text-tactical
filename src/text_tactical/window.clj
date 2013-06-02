(ns text-tactical.window
  (require [swing-text.ui :as ui]))

(def window (atom nil))

(defn create-window
  []
  (reset! window
          (ui/text-console {:title "Text Tactical"})))
