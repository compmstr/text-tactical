(ns swing-text.menu
  (use swing-text.util)
  (require [swing-text.ui :as ui]
           [swing-text.events :as events]))

(defn- draw-menu
  ([console label items selected]
     (draw-menu console nil label items selected))
  ([console [x y] label items selected]
     (let [text-height (count items)
           text-width (apply max (map count (map :label items)))
           box-width (+ text-width 3)
           box-height (+ text-height 2)
           [screen-w screen-h] (.grid-size console)
           x (or x (- (quot screen-w 2)
                      (quot box-width 2)))
           y (or y (- (quot screen-h 2)
                      (quot box-height 2)))
           label-start (inc x)
           items-start (inc y)]
       (ui/draw-box-with-label console [x y] [box-width box-height] label 1)
       (doseq [i (range (count items))]
         (let [item (nth items i)]
           (.cursor-pos! console [label-start (+ items-start i)])
           (.write! console (str (if (= i selected) \> \space) (:label item))))))))

(defn- menu-keymap
  [menu items selected callback]
  (let [call-callback (fn [action]
                        (when callback
                          (callback
                           {:action action
                            :selected (nth items @selected)})))]
    {"Select Next" {:key "DOWN"
                    :on-press (fn [_]
                                (println "select next")
                                (menu :select-next)
                                (call-callback :select-next))}
     "Select Prev" {:key "UP"
                    :on-press (fn [_]
                                (println "select prev")
                                (menu :select-prev)
                                (call-callback :select-prev))}
     "Activate" {:key "ENTER"
                 :on-press (fn [_]
                             (println "activate")
                             (menu :activate)
                             (call-callback :activate))}
     "Cancel" {:key "ESCAPE"
               :on-press (fn [_]
                           (println "cancel")
                           (call-callback :cancel))}}))

(defn create-menu
  "Creates a menu from a list of items
   label is the label at the top of the menu
   items is a list/vector of maps with the following keys
   :id :label (:callback or nil)
    callback for item will be called on activate with that item selected
    you can also handle the actions through the bind callback
   returns a function that has the following spec:
    [cmd & args]
    cmds:
      :select-next - move to next item in menu
        no args
      :select-prev - move to previous item in menu
        no args
      :activate - call the callback for the current selection
        no args
      :bind - binds the menu keys to the provided Console
        args: swing-text.ui.Console to bind to
              callback for when any menu key is pressed (refresh needed) or nil
      :draw - draws the menu centered in the console
        args: swing-text.ui.Console to draw to
      :draw-at - draws the menu at a location on the console
        args: swing-text.ui.Console to draw to
              [x y] of where to draw menu"
  [label items]
  (let [selected (atom 0)]
    (fn menu-cmds [cmd & args]
      (case cmd
        :select-next
        (swap! selected (partial looped-inc (count items)))
        :select-prev
        (swap! selected (partial looped-dec (count items)))
        :activate
        (if-let [callback (:callback (nth items @selected))]
          (callback)
          (println "No callback for menu item"))
        :bind
        (let [[con callback] args]
          (events/bind-keymap (.component con)
                              (menu-keymap menu-cmds items selected callback)
                              true))
        :push-bindings
        (let [[con callback] args]
          (events/push-keymap! (.component con)
                              (menu-keymap menu-cmds items selected callback)))
        :draw
        (let [con (first args)]
          (draw-menu con label items @selected))
        :draw-at
        (let [[con loc] args]
          (draw-menu con loc label items @selected))))))

(defn create-menu-callback
  "Create a menu callback function which takes a map of
   :<action> => {:<selected_name> => fn ...} ...
   special actions :before and :after to trigger on every event"
  [callbacks]
  (fn [{:keys [action selected]}]
    (println "inside created menu callback -- action:" action "selected:" selected)
    (when-let [before-callback (:before callbacks)]
      (before-callback))
    (when-let [action-map (action callbacks)]
      (when-let [action-callback (action-map (:id selected))]
        (action-callback)))
    (when-let [after-callback (:after callbacks)]
      (after-callback))))
