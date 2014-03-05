(ns swing-text.util)

(defn looped-inc
  "Generates an inc function that always returns between 0 and high"
  [high x]
  (let [new-x (inc x)]
    (if (< new-x high)
      new-x
      (rem new-x high))))
(defn looped-dec
  "Generates a dec function that always returns between 0 and high"
  [high x]
  (let [new-x (dec x)]
    (if (< new-x 0)
      (+ high new-x)
      new-x)))
(defn loop-num
  "Loops a number to between 0 and high"
  [high x]
  (if (< x 0)
    (recur high (+ high x))
    (if (>= x high)
      (rem x high)
      x)))

(defn chunks
  "Take <chunk> elements every <stride> after <offset>"
  [lst offset chunk-size stride]
  (when (seq lst)
    (lazy-seq
     (concat (take chunk-size (nthrest lst offset))
             (chunks (nthrest lst (+ offset chunk-size stride)) 0 chunk-size stride)))))

;;TODO - need to refactor the clamps to work properly... seem to have issues with negative x/y
(defn clamp-rect
  "Takes in a rectangle and clamps it to the viewport of con
   returns [new-rect <differences between old/new>"
  [[width height] [x y w h :as rect]]
  (let [new-rect [(max x 0)
                  (max y 0)
                  (min width w (- width x)(+ x w))
                  (min height h (- height y) (+ y h))]]
    [new-rect (map (comp #(Math/abs ^Integer %) -) rect new-rect)]))

(defn clamp-data
  [data w h [x y w-diff h-diff]]
  (let [offset (+ x (* y w))
        chunk (- w w-diff)
        stride w-diff
        total (* chunk (- h h-diff))]
    (take total (chunks data offset chunk stride))))
