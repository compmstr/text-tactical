(ns swing-text.ui
  (use swing-text.util)
  (import [javax.swing JComponent JFrame]
          [java.awt Color Dimension Font Graphics Graphics2D Rectangle GraphicsEnvironment FontMetrics Window]
          java.awt.font.FontRenderContext
          java.awt.geom.Rectangle2D
          [java.io IOException InputStreamReader PipedInputStream PipedOutputStream PrintStream]
          ))

(set! *warn-on-reflection* true)

;;Clojure translation of: 
;;http://code.google.com/p/mikeralib/source/browse/trunk/Mikera/src/main/java/mikera/ui/JConsole.java

(def font-info
  ;;Especialy since we need to pull up a window to do this, memoize this function
  ;;  the font graphics shouldn't change during a single run of this
  (memoize
   (fn font-info
     [^Font font]
     (let [frame (doto (JFrame.) (.setVisible true))
           g (.getGraphics frame)
           metrics ^FontMetrics (.getFontMetrics g font)
           render-ctx (.getFontRenderContext metrics)
           char-bounds (.getStringBounds font "X" render-ctx)]
       (let [info {:font-width (int (.charWidth metrics (char \X)))
                   :font-height (int (.getHeight metrics))
                   :font-y-offset (- (int (.getMinY char-bounds)))}]
         (.dispose frame)
         info)))))

;;Generates a java interface: swing-text.Console
;;  can't reference using just Console for type hints or proxy
(defprotocol Console
  "Console protocol, for grid-based text output"
  (component [this])
  (cursor-pos [this])
  (cursor-pos! [this loc])
  (colors [this])
  (colors! [this colors])
  (write-char! [this ^Character char] [this ^Character char pos])
  (write! [this data] [this data colors])
  (fill-area! [this ^Character c colors loc size])
  (fill! [this ^Character c colors])
  (clear! [this])
  (repaint [this])
  (grid-size [this])
  (draw-size [this])
  (mass-copy! [this data]
    "Fill buffer with data, provided as seqable list of maps with keys:
     :char :fg :bg")
  (blit! [this data rect]
    "Fill buffer area with data, provided as seqable list of maps with keys:
       :char :fg :bg
     rect is specified as [x y w h]")
  )

(defn capture-std-out
  [^swing_text.ui.Console console]
  (let [input (PipedInputStream.)
        output (PipedOutputStream. input)
        printer (PrintStream. output)
        input-stream (InputStreamReader. input)
        stop-capture (atom false)]
    (System/setOut printer)
    (.run (proxy [Thread] []
            (run []
              (while (not @stop-capture)
                (doto console
                  (.write! (char (.read input-stream)))
                  (.repaint))))))
    stop-capture))

(def font-name "Courier New")
;;(def font-name Font/MONOSPACED)
;;(def font-name "Andale Mono")

(defn jconsole
  ([] (jconsole {}))
  ([{:keys [rows cols bg fg font-name] :or {rows 24 cols 80 bg Color/BLACK fg Color/LIGHT_GRAY font-name font-name}}]
     (let [font (Font. font-name Font/PLAIN 14)
           buf-size (* rows cols)
           buf-inc (partial looped-inc buf-size)
           buf-dec (partial looped-inc buf-size)
           cur-pos (atom 0)
           cur-fg (atom fg)
           cur-bg (atom bg)
           cur-font (atom font)
           chars (char-array buf-size)
           bg-colors (object-array (take buf-size (repeat @cur-bg)))
           fg-colors (object-array (take buf-size (repeat @cur-fg)))
           {:keys [font-width font-height font-y-offset]} (font-info @cur-font)]
       (let [paint-fn (fn paint [^Graphics graphics]
                        (let [^Graphics2D g graphics
                              clip-rect (.getClipBounds g)
                              font-info (font-info @cur-font)
                              max-col (inc (/ (.getMaxX clip-rect) (:font-width font-info)))
                              min-col (/ (.getMinX clip-rect) (:font-width font-info))
                              num-cols (-  max-col min-col)
                              max-row (inc (/ (.getMaxY clip-rect) (:font-height font-info)))
                              min-row (/ (.getMinY clip-rect) (:font-height font-info))
                              num-rows (- max-row min-row)]
                          (.setFont g @cur-font)
                          (doseq [cur-y (range (max 0 min-row) (min rows max-row))]
                            (let [offset (* cur-y cols)
                                  end (min cols max-col)]
                              (loop [start (max 0 min-col)]
                                (when (< start end)
                                  (let [start-cell (+ offset start)
                                        cur-fg (aget fg-colors start-cell)
                                        cur-bg (aget bg-colors start-cell)
                                        run-end (loop [cur-col (inc start)]
                                                  (when (= 1920 (+ cur-col offset))
                                                    (println "Error, 1920 index")
                                                    (println
                                                     (format "cur-col: %d - end: %d - offset: %d" cur-col end offset)))
                                                  (if (and (< cur-col end)
                                                           (= cur-fg (aget fg-colors (+ cur-col offset)))
                                                           (= cur-bg (aget bg-colors (+ cur-col offset))))
                                                    (recur (inc cur-col))
                                                    cur-col))]
                                    ;;draw
                                    (doto g
                                      (.setBackground cur-bg)
                                      (.clearRect (* (:font-width font-info) start)
                                                  (* cur-y (:font-height font-info))
                                                  (* (- run-end start) (:font-width font-info))
                                                  (:font-height font-info))
                                      (.setColor cur-fg)
                                      (.drawChars chars
                                                  start-cell
                                                  (- run-end start)
                                                  (* start (:font-width font-info))
                                                  (+ (* cur-y (:font-height font-info))
                                                     font-y-offset)))
                                    (recur run-end)
                                    )))))))
             console-component (proxy [JComponent] [] (paint [^Graphics g] (paint-fn g)))]
         (reify
           Console
           (component [this]
             console-component)
           (cursor-pos [this]
             (let [pos @cur-pos]
               {:x (rem pos cols)
                :y (quot pos cols)}))
           (cursor-pos! [this [x y]]
             (reset! cur-pos (loop-num buf-size (+ x (* y cols)))))
           (colors [this]
             {:fg @cur-fg
              :bg @cur-bg})
           (colors!
             [this {:keys [fg bg] :or {fg @cur-fg bg @cur-bg}}]
             (when fg
               (reset! cur-fg fg))
             (when bg
               (reset! cur-bg bg)))
           (write-char!
             [this char]
             ;;(println (format "write-char! -- %c@(%d)" char @cur-pos))
             (let [pos @cur-pos]
               (aset-char chars pos char)
               (aset bg-colors pos @cur-bg)
               (aset fg-colors pos @cur-fg)
               (if (= char \newline)
                 (swap! cur-pos (fn [pos] (loop-num buf-size (+ pos (- cols (mod pos cols))))))
                 (swap! cur-pos buf-inc))))
           (write-char!
             [this char loc]
             ;;(println (format "write-char! -- %c@(%d, %d)" char (first loc) (second loc)))
             (.cursor-pos! this loc)
             (.write-char! this char))
           (write!
             [this data]
             ;;(println (format "write! -- %s" data))
             (cond
              (string? data)
              (do
                ;;(println "String")
                (doseq [c data] (.write! this c)))
              (char? data)
              (do
                (.write-char! this (char data)))
              true
              (do
                ;;(println "Unknown")
                nil)))
           (write!
             [this data colors]
             ;;(println (format "write! -- %s %s" data colors))
             (.colors! this colors)
             (.write! this data))
           (fill-area!
             [this c colors [x y] [w h]]
             ;;(println (format "fill-area! -- %c fg: %s bg: %s (%d,%d)-%dx%d" c fg bg x y w h))
             (.colors! this colors)
             (let [row-step (dec (+ x w))
                   end-y (+ y h)]
               (loop [cur-x x
                      cur-y y
                      pos (+ x (* cols y))]
                 ;;(Thread/sleep 50)
                 (when (not= cur-y end-y)
                   ;;(println (format "cur-x: %d cur-y: %d pos: %d" cur-x cur-y pos))
                   (aset-char chars pos c)
                   (if (= row-step cur-x)
                     (recur x
                            (inc cur-y)
                            (+ x (* cols (inc cur-y))))
                     (recur (inc cur-x)
                            cur-y
                            (inc pos)))))))
           (fill!
             [this c {:keys [fg bg] :or {fg @cur-fg bg @cur-bg}}]
             ;;(println (format "fill! -- %c fg: %s bg: %s" c fg bg))
             ;;(fill-area! this c colors [(long 0) (long 0)] [cols rows])
             (doseq [i (range buf-size)]
               (aset-char chars i c)
               (aset fg-colors i fg)
               (aset bg-colors i bg)))
           (clear!
             [this]
             ;;(println "Clear!")
             (.fill! this \space nil))
           (repaint
             [this]
             (.repaint console-component))
           (draw-size
             [this]
             {:w (* cols (:font-width (font-info @cur-font)))
              :h (* rows (:font-height (font-info @cur-font)))})
           (grid-size
             [this]
             [cols rows])
            (mass-copy! [this data]
              "Fill buffer with data, provided as seqable list of maps with keys:
               :char :fg :bg"
              (loop [data data
                     pos 0
                     elt (first data)]
                (when (not (empty? data))
                  (aset-char chars pos (or (:char elt) \space))
                  (aset bg-colors pos (or (:bg elt) @cur-bg))
                  (aset fg-colors pos (or (:fg elt) @cur-fg))
                  (recur (rest data)
                         (inc pos)
                         (second data)))))
            (blit! [this data [x y w h :as rect]]
              "Fill buffer area with data, provided as seqable list of maps with keys:
                 :char :fg :bg
               rect is specified as [x y w h]"
              (let [[[x y w h :as rect] diff] (clamp-rect (.grid-size this) rect)
                    data (clamp-data data w h diff)
                    [disp-w disp-h] (.grid-size this)
                    start-pos (+ x (* y disp-w))
                    line-end (dec (+ x w))
                    line-skip (- disp-w (dec w))]
                (loop [data data
                       dest-pos start-pos
                       elt (first data)]
                  (when (not (empty? data))
                    (aset-char chars dest-pos (or (:char elt) \space))
                    (aset bg-colors dest-pos (or (:bg elt) @cur-bg))
                    (aset fg-colors dest-pos (or (:fg elt) @cur-fg))
                    (recur (rest data)
                           (if (= line-end (mod dest-pos disp-w))
                             (+ dest-pos line-skip)
                             (inc dest-pos))
                           (second data))))))
           )))))

;;(load "swing-text")(in-ns 'swing-text)(def tmp (jconsole))
(defn text-console
  ([]
     (text-console nil nil))
  ([frame-config]
     (text-console frame-config nil))
  ([{:keys [title resizable] :or {title "swing-text" resizable false}} console-config]
     (let [frame (JFrame.)
           console ^swing_text.ui.Console (jconsole console-config)
           console-size (.draw-size console)]
       (.setPreferredSize ^JComponent (.component console) (Dimension. (:w console-size) (:h console-size)))
       (doto frame
         (.setTitle title)
         (.add ^java.awt.Component (.component console))
         (.pack)
         (.setResizable resizable)
         (.setVisible true))
       (.requestFocusInWindow ^java.awt.Component (component console))
       (doto console
         (.clear!)
         (.repaint)))))

;;Line drawing unicode chars are between 0x2500 and 0x2579
(def box-glyphs
  {:white (char 0x2588)
   :light (char 0x2593)
   :med (char 0x2592)
   :dark (char 0x2591)
   :black \space
   })
(def box-chars
  {:horiz (char 0x2500)
   :vert (char 0x2502)
   :top-left (char 0x250C)
   :top-right (char 0x2510)
   :bottom-left (char 0x2514)
   :bottom-right (char 0x2518)
   :t-left (char 0x251C)
   :t-right (char 0x2524)
   :t-top (char 0x252C)
   :t-bottom (char 0x2534)
   :cross (char 0x253C)})

(def double-box-chars
  {:horiz (char 0x2550)
   :vert (char 0x2551)
   :top-left (char 0x2554)
   :top-right (char 0x2557)
   :bottom-left (char 0x255A)
   :bottom-right (char 0x255D)
   :t-left (char 0x2560)
   :t-right (char 0x2563)
   :t-top (char 0x2566)
   :t-bottom (char 0x2569)
   :cross (char 0x256C)})

(defn draw-box
  ([^swing_text.ui.Console con loc size]
     (draw-box con loc size box-chars))
  ([^swing_text.ui.Console con [x y :as loc] [w h] {:keys [horiz vert top-left top-right bottom-left bottom-right]}]
     (if (or (< w 2) (< h 2))
       (println "Box must be at least 2 wide/high")
       (do
         (doto con
           (.cursor-pos! loc)
           (.write! (str top-left (apply str (repeat (- w 2) horiz)) top-right))
           (.cursor-pos! [x (dec (+ y h))])
           (.write! (str bottom-left (apply str (repeat (- w 2) horiz)) bottom-right)))
         (let [mid-line (str vert (apply str (repeat (- w 2) \space)) vert)]
           (doseq [y (range (inc y) (dec (+ y h)))]
             (doto con
               (.cursor-pos! [x y])
               (.write! mid-line))))))))

(defn draw-box-with-label
  "Draw a box with a label, same args as draw-box, with the addition
   of label and where
   label - the label to draw
   where - offset from side where to draw text
     positive numbers are from the left
     negative numbers are from the right
     nil is centered"
  ([^swing_text.ui.Console con loc size label where]
     (draw-box-with-label con loc size box-chars label where))
  ([^swing_text.ui.Console con [x y :as loc] [w h :as size] chars label where]
     (draw-box con loc size chars)
     (let [label-x (+ x
                      (cond
                       (nil? where)
                       (- (quot w 2) (quot (count label) 2))
                       (neg? where)
                       (- w (count label) (- where))
                       true
                       where))]
       (.cursor-pos! con [label-x y])
       (.write! con label))))

(defn close-all-windows
  []
  (doseq [^java.awt.Window win (Window/getWindows)]
    (.dispose win)))
