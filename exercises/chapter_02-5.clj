;; covers exercises and examples from chapters 2 through 5

;; hash-map generates hashmaps from lists
(apply hash-map (interleave '(:a :b :c) '(1 2 3)))

(def apply-to-map
  (fn [f]
    (f {:a "alpha", :b "beta"})))

(apply-to-map count)
(apply-to-map :a)
;; see how a keyword can act as a function, if its argument is a map

(assoc {:a 1 :b 2} :c 3) ;; add a key-value pair

(assoc {:a 1 :b 2} :c 3 :d 4) ;; or many

(merge {:a 1 :b 2} {:c 3 :d 4}) ;; i.e. _.extend, Python update

((:a {:a (fn [] prn "hi")})) ;; functions are values, natch

(dissoc {:a 1 :b 2 :c 3} :b :c) ;; i.e. delete

(assoc {:a 1} :a "one") ;; overwrites
(merge {:a 1} {:a "one"}) ;; overwrites

(def Point
  (fn [x y]
    {:x x
     :y y}))

(:x (Point 10 20))

(def x (fn [this] (:x this)))
(def y (fn [this] (:y this)))

(x (Point 10 20)) ;; not so useful as-is, but I get the idea

(def Point
  (fn [x y]
    {:x x
     :y y
     :__class_symbol__ 'Point}))

;; basically an alias
(def class-of :__class_symbol__)

(class-of (Point 1 2)) ;; is that homoiconicity I see?

(def shift
  (fn [this dx dy]
    (Point (+ (x this) dx)
           (+ (y this) dy))))

(shift (Point 1 2) 100 100)

(load-file "sources/add-and-make.clj")

;; 3.4 exercises
;; exercise 1
(def add-points
  (fn [addend augend]
    (Point (+ (x addend) (x augend))
           (+ (y addend) (y augend)))))

(def add-points
  (fn [addend augend]
    (shift addend (x augend) (y augend))))

(add-points (Point 100 100) (Point -50 -50))

;; exercise 2
(def make
  (fn [constructor & args]
    (apply constructor args)))

(make Point 1 2)

(make Triangle (make Point 1 2)
               (make Point 1 3)
               (make Point 3 1))

;; exercise 3
(def equal-triangles?
  (fn [a b]
    (= a b)))

(equal-triangles? right-triangle right-triangle) ;; Nice to see deep equality.

;; exercise 4
(def equal-triangles?
  (fn [& triangles]
    (= triangles)))

(equal-triangles? right-triangle equal-right-triangle different-triangle)
(equal-triangles? right-triangle right-triangle right-triangle)
;; "_way_ easier than you might think", indeed.

;; exercise 5
;; true if exactly three distinct points are provided
(def valid-triangle?
  (fn [& points]
    (if (not= (count points) 3)
      false
      (= (count points)
         (count (distinct points))))))
;; probably went beyond the call of duty by handling other polygons

(valid-triangle? (Point 1 1) (Point 1 2) (Point 2 1))
(valid-triangle? (Point 1 1) (Point 1 1) (Point 2 1))


(def Point
  (fn [x y]
    {:x x
     :y y

     ;; metadata
     :__class_symbol__ 'Point
     :__methods__ {
       :class :__class_symbol__
       :shift (fn [this dx dy]
         (make Point (+ (:x this) dx)
                     (+ (:y this) dy)))}}))

(def send-to
  (fn [object message & args]
    (apply (message (:__methods__ object)) object args)))
;; (:__methods__ object) gets the map of methods
;; (message method-map) gets the function, since message is a method name
;; (apply function object args) works if function takes arguments
;; [this arg1 arg2]. You know, like Python's "self". 

;; 4.1 exercise
(def Point
  (fn [x y]
    { :x x, ;; why and when do we need this comma?
      :y y
      ;; metadata
      :__class_symbol__ 'Point
      :__methods__ {
      :x :x
      :y :y
      :class :__class_symbol__

       ;; returns a new Point shifted by the supplied deltas
      :shift (fn [this dx dy]
        (make Point (+ (send-to this :x) dx)
                    (+ (send-to this :y) dy)))

      ;; 
      :add (fn add [this point]
        (send-to this :shift (send-to point :x)
                             (send-to point :y)))}}))

(fn add [this point]
  (send-to this :shift (send-to point :x) (send-to point :y)))

(def point (make Point 10 10))

(send-to point :shift 10 10)

(send-to point :add (make Point 1 1))


;; 5
;; only exercise
(def Point
{
 :__own_symbol__ 'Point
 :__instance_methods
 {
  :x :x
  :y :y
  :class :__class_symbol__
  :add-instance-values
  (fn [this x y]
    (assoc this :x x :y y))
  :shift
  (fn [this dx dy]
    (make Point (+ (:x this) dx)
                (+ (:y this) dy)))
 }
})

(let [name (+ 1 2)]
  (* name name 5))

(+ 1 (let [one 1] (* one one)) 3)
;; so (let [symbol value] ...) just creates a short-lived scope in which the
;; "variable assignment" holds true.

(let [one 1
      two 2]
  (+ one two))
;; convenient!

(def make
  (fn [class & args]
    (let [seeded {:__class_symbol__ (:__own_symbol__ class)}
          constructor (:add-instance-values (:__instance_methods class))]
      ;; applying the class's method to this instance; do I detect prototypal
      ;; inheritance coming up?
      (apply constructor seeded args))))

(make Point 1 2)

(eval (:__class_symbol__ (make Point 1 2)))

(def send-to
  (fn [object message & args]
    (let [class (eval (:__class_symbol__ object))
          method (message (:__instance_methods class))]
      (apply method object args))))

(send-to (make Point 1 2) :x)
(send-to (make Point 1 2) :shift 1 1)
;; sweet

;; 5.4 exercises
;; exercise 1
(def apply-message-to
  (fn [class instance message args]
    (let [method (message (:__instance_methods class))]
      (apply method instance args))))

(def make
  (fn [class & args]
    (let [seeded {:__class_symbol__ (:__own_symbol__ class)}]
      (apply-message-to class seeded :add-instance-values args))))

(def send-to
  (fn [object message & args]
    (let [class (eval (:__class_symbol__ object))]
      (apply-message-to class object message args))))

(send-to (make Point 1 1) :shift 1 1)
;; yep, these both still work

;; exercise 2
(def Point
{
 :__own_symbol__ 'Point
 :__instance_methods
 {
  :x :x
  :y :y
  :class-name :__class_symbol__
  :class
  (fn [this]
    (eval (send-to this :class-name)))
  :add-instance-values
  (fn [this x y]
    (assoc this :x x :y y))
  :shift
  (fn [this dx dy]
    (make Point (+ (:x this) dx)
                (+ (:y this) dy)))
 }
})

(def point (make Point 1 1))
(send-to point :class-name)
(send-to point :class)

;; exercise 3
(def point (make Point 1 1))

;; redefine Point
(def Point
{
 :__own_symbol__ 'Point
 :__instance_methods
 {
  :x :x
  :y :y
  :class-name :__class_symbol__
  :class
  (fn [this]
    (eval (send-to this :class-name)))
  :add-instance-values
  (fn [this x y]
    (assoc this :x x :y y))
  :shift
  (fn [this dx dy]
    (make Point (+ (:x this) dx)
                (+ (:y this) dy)))
  :origin
  (fn [this] (make Point 0 0))
 }
})

;; call new method on an old instance
(send-to point :origin)

;; This works because send-to does a fresh lookup of the value bound to the
;; Point symbol at runtime. Change Point and all instances have access to the
;; new version of it.

;; exercise 4
(def Holder
{
 :__own_symbol__ 'Holder
 :__instance_methods__
 {
  :add-instance-values
  (fn [this held]
    (assoc this :held held))
 }
})

(send-to (make Holder "stuff") :held) ;; NPE

;; my version
(def apply-message-to
  (fn [class instance message args]
    (let [method (if (nil? (message (:__instance_methods__ class)))
                           message
                           (message (:__instance_methods__ class)))]
      (apply method instance args))))

;; author's solution, using (or nil true) short-circuiting; this is equivalent
;; to foo = bar || baz
(def apply-message-to
     (fn [class instance message args]
       (let [method (or (message (:__instance_methods__ class))
                        message)]
       (apply method instance args))))

(send-to (make Holder "stuff") :held) ;; NPE

;; exercise 5
(send-to (make Point 1 2) :some-unknown-message)
;; It will attempt to get the value of the :some-unknown-message key directly
;; from the Point instance, which will fail. Sad.
;; Confirmed: NPE.


