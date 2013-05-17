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
