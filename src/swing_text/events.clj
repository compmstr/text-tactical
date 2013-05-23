(ns swing-text.events
  (require [clojure.string :as str])
  (import [javax.swing JTextField JComponent JPanel JFrame JButton KeyStroke AbstractAction Timer
           ActionMap InputMap ComponentInputMap]
          [java.awt BorderLayout]
          [java.awt.event ActionEvent ActionListener KeyListener]))

(defn as-action-listener
  [f]
  (proxy [ActionListener] []
    (actionPerformed [evt]
      (f evt))))

(defn create-stopping-timer
  [timeout callback]
  (doto (Timer. timeout (as-action-listener callback))
    (.setRepeats false)))

(defn- keystroke-press-release
  [key-string action]
  (let [split-str (str/split key-string #" ")
        key (last split-str)
        mods (drop-last split-str)
        mods (if (or (= (last mods)
                        "pressed")
                     (= (last mods)
                        "released"))
               (drop-last mods)
               mods)]
    (apply str (butlast (interleave (concat mods [action key]) (repeat " "))))))
(defn- keystroke-pressed
  [key-string]
  (KeyStroke/getKeyStroke
   (keystroke-press-release key-string "pressed")))
(defn- keystroke-released
  [key-string]
  (KeyStroke/getKeyStroke
   (keystroke-press-release key-string "released")))

;;The release-timer thing is so that when the OS sends a quick
;;  release/press combo, it doesn't count that as a release and a re-press
;;Each of the javax.swing.Timer instances runs on an event queue in a single thread
;;  so it won't spawn a bunch of threads just for these
;;Also, the callback gets called on the UI Thread
(defn stateful-key
  [key-map]
  (let [pressed-atm (atom false)
        old-on-press (:on-press key-map)
        old-on-release (:on-release key-map)
        release-timer (create-stopping-timer 30 (fn [e]
                                                  (reset! pressed-atm false)
                                                  (when old-on-release (old-on-release e))))]
    (merge key-map
           {:on-press (fn [e]
                        ;;Turn off the release timer if it's runing
                        (.stop release-timer)
                        (when (not @pressed-atm)
                          (reset! pressed-atm true)
                          (when old-on-press
                            (old-on-press e))))
            :on-release (fn [e]
                          (.start release-timer))
            :pressed pressed-atm})))

(defn- create-action
  [f]
  (proxy [AbstractAction] []
    (actionPerformed [^ActionEvent e]
      (f e))))

(defn- bind-action
  "Updates a component with an action from the actions/action-keys structures"
  [component action keypress callback]
  ;;WHEN_IN_FOCUSED_WINDOW - anywhere within parent window
  ;;WHEN_FOCUSED - only if this is focused directly
  ;;WHEN_ANCESTOR_OF_FOCUSED_COMPONENT - if this or any child is focused
  (let [input-map (.getInputMap component JComponent/WHEN_IN_FOCUSED_WINDOW)
        action-map (.getActionMap component)]
    (.put input-map keypress action)
    (.put action-map action (create-action callback))))

(defn bind-keymap
  "Binds an entire keymap to a swing component
   Clearing out any existing keymaps if true is sent after the keymap"
  ([component keymap clear?]
     (when clear?
       (.clear (.getInputMap component JComponent/WHEN_IN_FOCUSED_WINDOW))
       (.clear (.getActionMap component)))
     (bind-keymap component keymap))
  ([component keymap]
     (doseq [action (keys keymap)]
       (let [entry (keymap action)
             key (:key entry)]
         (when-let [on-press (:on-press entry)]
           (bind-action component
                        (str action "-press")
                        (keystroke-pressed key)
                        on-press))
         (when-let [on-release (:on-release entry)]
           (bind-action component
                        (str action "-release")
                        (keystroke-released key)
                        on-release))))))

(defn on-any-key
  [component callback]
  (.addKeyListener component
                   (proxy [KeyListener] []
                     (keyPressed [e])
                     (keyTyped [e])
                     (keyReleased [e]
                       (.removeKeyListener component this)
                       (callback)))))

(def component-binding-stacks
  "Stacks of action/input maps keyed by components"
  (atom {}))
(defn push-keymap
  [component keymap]
  (let [existing-stack (or (@component-binding-stacks component) '())
        cur-maps {:input-map (.getInputMap component JComponent/WHEN_IN_FOCUSED_WINDOW)
                  :action-map (.getActionMap component)}
        new-stack (conj existing-stack cur-maps)]
    (doto component
      (.setInputMap JComponent/WHEN_IN_FOCUSED_WINDOW (ComponentInputMap. component))
      (.setActionMap (ActionMap.))
      (bind-keymap keymap))
    (swap! component-binding-stacks
           assoc component new-stack)))

(defn pop-keymap
  [component]
  (let [existing-stack (or (@component-binding-stacks component) '())
        to-restore (first existing-stack)
        new-stack (rest existing-stack)]
    (when to-restore
      (doto component
        (.setInputMap JComponent/WHEN_IN_FOCUSED_WINDOW (:input-map to-restore))
        (.setActionMap (:action-map to-restore))))
    (swap! component-binding-stacks
           assoc component new-stack)))

;;-----------Example code----------
(def test-map1 {"Action 1" {:key "SPACE"
                            :on-press (fn [e] (println "Action 1 map 1"))}})
(def test-map2 {"Action 1" {:key "SPACE"
                            :on-press (fn [e] (println "Action 1 map 2"))}})
(def example-key-map {"Action 1" (stateful-key
                                  {:key "SPACE"})
                      "Action 2" (stateful-key
                                  {:key "X"})
                      "Action 3" (stateful-key
                                  {:key "ctrl SPACE"})
                      "Left" (stateful-key
                              {:key "LEFT"})
                      "Right" (stateful-key
                               {:key "RIGHT"})
                      "Up" (stateful-key
                            {:key "UP"})
                      "Down" (stateful-key
                              {:key "DOWN"})
                      "Escape Press" {:key "ESCAPE"
                                      :on-press (fn [e]
                                                  (println "Escape pressed"))}
                      })
