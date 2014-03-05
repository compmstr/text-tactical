(ns swing-text.stack-events
  (import [javax.swing ActionMap InputMap]))

(gen-class
 :name swing-text.StackActionMap
 :extends ActionMap
 :init init
 :constructors {[ActionMap] []}
 :prefix stackAction-)

(defn stackAction-init
  [prev-map]
  ;;Can't seem to refer to this inside this definition
  (if (= (class prev-map) StackActionMap)
    [[] (conj (.state prev-map) (ActionMap.))]
    [[] [(ActionMap.)]]))

(gen-class
 :name swing-text.StackInputMap
 :extends InputMap
 :init init
 :constructors {[StackInputMap] []}
 :prefix stackInput-)