(load-file "sources/class.clj")
(load-file "solutions/class.clj")

(def apply-message-to
     (fn [class instance message args]
       (let [method (or (method-from-message message class)
                        message)]
       (apply method instance args))))

(def method-from-message
     (fn [message class]
       (message (:__instance_methods__ class))))

(def class-from-instance
     (fn [instance]
       (assert (map? instance))
       (eval (:__class_symbol__ instance))))

(def make
     (fn [class & args]
       (let [seeded {:__class_symbol__ (:__own_symbol__ class)}]
         (apply-message-to class seeded :add-instance-values args))))

(def send-to
     (fn [instance message & args]
       (apply-message-to (class-from-instance instance)
                         instance message args)))

(def Point
{
 :__own_symbol__ 'Point
 :__superclass_symbol__ 'Anything
 :__instance_methods__
 {
  :add-instance-values (fn [this x y]
                         (assoc this :x x :y y))
  :origin (fn [this] (make Point 0 0))
  :class-name :__class_symbol__    
  :class (fn [this] (class-from-instance this))
  :shift (fn [this xinc yinc]
           (make Point (+ (:x this) xinc)
                       (+ (:y this) yinc)))
  :add (fn [this other]
         (send-to this :shift (:x other)
                              (:y other)))
 }
})

(def Anything
{
 :__own_symbol__ 'Anything
 :__instance_methods__
 {
  :add-instance-values identity
  :class-name :__class_symbol__
  :class (fn [this] (class-from-instance this))
 }
})

;; implemented these based on the description in 6.2
(def lineage
  (fn [symbol]
    (let [class (eval symbol)
          super-symbol (:__superclass_symbol__ class)]
      (if (nil? super-symbol)
        (list symbol)
        ;; reverse, since we want to put the super-symbol first
        (reverse (cons symbol (lineage super-symbol)))))))

(def class-instance-methods
  (fn [symbol]
    (:__instance_methods__ (eval symbol))))

(lineage 'Point)

(def maps (map class-instance-methods (lineage 'Point)))
(pprint maps)
(def merged (apply merge maps))
(pprint merged)

((:add-instance-values merged) {} 1 2) ;; it works!

;; Gets all instance methods for the supplied class, recursively grabbing
;; superclass definitions where no class definitions exist.
(def method-cache
  (fn [class]
    (let [class-symbol (:__own_symbol__ class)
          method-maps (map class-instance-methods
                           (lineage class-symbol))]
      (apply merge method-maps))))

;; footnote 5
(["zero" "one"] 1) ;; so weird that the vector is the callable here

;; lineage using accumulator; tail recursive
(def lineage
  (fn [symbol accumulator]
    (if (nil? symbol)
      accumulator
      (let [class (eval symbol)
            super-symbol (:__superclass_symbol__ class)]
        (recur super-symbol (cons symbol accumulator))))))
;; The `recur` keyword stands in for the function being executed, but tells the
;; Clojure compiler to approximate proper tail recursion using some JVM magic.

(lineage 'Point []) ;; there we go

;; exercise 1
(def factorial
  (fn [n]
    (if (or (= n 0) (= n 1))
      1 ;; mathematically, 0! is 1 for some reason
      (* n (factorial (dec n))))))

(factorial 5)
(factorial 1)
(factorial 0)

;; exercise 2
(def factorial
  (fn [n accumulator] ;; accumulator must be called as 1
    (if (or (= n 1) (= n 0))
      accumulator
      (recur (dec n) (* accumulator n)))))

(factorial 5 1)
(factorial 4 1)
(factorial 1 1)
(factorial 0 1)

;; exercise 3
(def add-list
  (fn [list accumulator]
    (if (empty? list)
      accumulator
      (recur (rest list) (+ (first list) accumulator)))))

(add-list [25 100 25] 0)
(add-list [-100 1] 0)
(add-list [] 0)
(add-list [(+ 10 10) 10] 0)

;; exercise 4
(def multiply-list
  (fn [list accumulator]
    (if (empty? list)
      accumulator
      (recur (rest list) (* (first list) accumulator)))))

(multiply-list [2 10 10] 1)

;; Operation must be a commutative mathematical function, most commonly
;; addition and multiplication (embodied in Clojure as + and *).
(def apply-computation
  (fn [operation list accumulator]
    ;; Let's be honest, we could just (apply operation list) here.
    (if (empty? list)
      accumulator
      (recur operation (rest list) (operation (first list) accumulator)))))

(apply-computation + [10 10] 0)
(apply-computation * [10 10] 1)

;; exercise 5
(apply-computation (fn [key hash] (assoc hash key (count hash)))
                   [:a :b :c]
                   {})

(reduce + 0 [1 2 3 4]) ;; ...oh!

;; 6.5
(def apply-message-to
  (fn [class instance message args]
    (apply (message (method-cache class)) instance args)))

(send-to (make Point 1 2) :class-name)
(send-to (make Point 1 2) :shift 3 4)


