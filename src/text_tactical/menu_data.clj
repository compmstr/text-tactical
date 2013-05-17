(ns text-tactical.menu-data
  (require [swing-text.ui :as ui]
           [swing-text.events :as events]
           [swing-text.menu :as menu]))


(def main-menu-data ["Main Menu"
                     [{:id :start
                       :label "Start Game"}
                      {:id :settings
                       :label "Settings"}
                      {:id :exit
                       :label "Exit"}]])
