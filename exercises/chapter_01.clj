(number? 1)

(fn? +)

((fn [n] (+ n n)) 4)

(n 1 2)

(def twice (fn [n] (+ n n)))

(twice 1)

;; comments! Good to know.
(def square (fn [n] (* n n)))

(square (square (square 2)))

(rest '(1 2 3 4))

(def second (fn [list] (nth list 1)))

(second '(1 2 3 4))

;; first implementation
(def third (fn [list] (nth list 2)))

;; second implementation
(def third (fn [list] (first (rest (rest list)))))

(third '(1 2 3))

(vector? (rest [1 2 3]))

(= [1 2] '(1 2)) 

[(+ 1 2) (+ 1 2)]

[inc dec] ;; functions
'[inc dec] ;; symbols

(if (odd? 3)
  (prn "Odd!") ;; guess this is how you print to the console
  (prn "Even!"))
;; note: prn evaluates nil, and "prn" is a shitty name

( (fn [& args] args) 1 2 3 4)

;; this totally works, but I don't know if it's the implementation the book is
;; looking for!
(def add-squares
  (fn [& numbers]
    (apply + (map square numbers))))

(add-squares 3 3)

(filter odd? '(1 2 3 4))

(def double (fn [n] (+ n n)))

(double 10)

(map double (map square '(1 2 3 4)))

(map + [1 2 3] [1 2 3] [1 2 3]) ;; cool

;; exercise 4
(def bizarro-factorial
  (fn [n]
    (apply * (range 1 (+ n 1)))))

(map bizarro-factorial '(1 2 3 4))
;; looks legit

;; exercise 5: try to find a use case for each of these functions

;; take
(doc take)

;; given a sequence of (fname lname address) seqs, get only names
(def names
  (fn [coll]
    (map (fn [coll] (take 2 coll)) coll)))

(names '(("Joe" "Smith" "1234 Test St")
         ("Adam" "Jones" "1235 Test St")
         ("Bill" "Jackson" "1236 Test St")))

;; distinct
(doc distinct)

(def count-uniques
  (fn [coll]
    (count (distinct coll))))
(count-uniques '(1 1 2 2 3 3)) ;; should be 3

;; concat
(doc concat)

;; given lists of M-F daily revenue, get grand total
(def total-revenue
  (fn [& weekly-reports]
    (apply + (apply concat weekly-reports))))

(total-revenue '(934 837 123 534 438)
               '(873 234 123 302 500))

;; repeat
;; interleave
(doc repeat)
(doc interleave)

;; frivolous example
(take 9 (interleave
           '(1 2 3 4 5 6 7 8)
           (repeat "is less than")))

;; drop
;; drop-last
(doc drop)
(doc drop-last)

;; given a message of the form (to from subject content)
(def message-content
  (fn [message]
    (drop 3 message)))

(def message-headers
  (fn [message]
    (interleave '("From:" "To:" "Subject:")
                (drop-last 1 message))))

(message-headers '("Vader" "Luke" "Surprise" "I am your father!"))
(message-content '("Vader" "Luke" "Surprise" "I am your father!"))

;; flatten
(doc flatten)

;; handles a weekly list of (AM PM) revenue figures
(def daily-average-revenue
  (fn [& weekly-reports]
    (float ;; I just guessed about float, but it did the job
      (/ (total-revenue weekly-reports)
         (count (flatten weekly-reports))))))

(daily-average-revenue '(934 837 123 534 438)
                       '(873 234 123 302 500))


;; partition [n coll]
(doc partition)

(def message-headers-grouped
  (fn [headers]
    (partition 2 (message-headers headers))))

(message-headers-grouped '("Vader" "Luke" "Surprise" "I am your father!"))

;; every?
(doc every?)

(def minimum-revenue-met
  (fn [minimum & weekly-reports]
    (every? (fn [r] (> r minimum)) (flatten weekly-reports))))

;; should be (and is) true
(minimum-revenue-met 100 '(934 837 123 534 438)
                         '(873 234 123 302 500))

;; should be (and is) false
(minimum-revenue-met 1000 '(934 837 123 534 438)
                          '(873 234 123 302 500))

;; remove: create the function argument with fn
(doc remove)

(def days-over
  (fn [target & weekly-reports]
    (remove (fn [day] (<= day target)) (flatten weekly-reports))))

(days-over 500 '(934 837 123 534 438)
               '(873 234 123 302 500))

;; exercise 6
(def prefix-of?
  (fn [candidate coll]
    (= candidate (take (count candidate) coll))))

;; true, true, false
(prefix-of? [1 2] [1 2 3 4])
(prefix-of? '(1 2) [1 2 3 4])
(prefix-of? '(2 3) '(1 2 3 4))


;; exercise 7
;; Given [1 2 3], returns dwindling subsequences [[1 2 3] [2 3] [3] []].
(def tails
  (fn [seq]
    ;; use (list seq) in the degenerate case so that invocations higher in the
    ;; stack have a list to cons onto; i.e. (cons '(1) '(())) yields '((1) ()),
    ;; but (cons '(1) '()) yields '((1)), which omits the empty list required
    ;; by the exercise definition.
    (if (empty? seq)
      (list seq)
      (cons seq (tails (rest seq))))))

(tails '(1 2 3 4))

;; and now to implement it as Marik suggests, using range
(def tails
  (fn [seq]
    (def drop-n (fn [n] (drop n seq)))
    (map drop-n (range (inc (count seq))))))
;; using inc/dec to correct fencepost errors feels hacky?
;; but this totally works.


;; exercise 8
(def puzzle
  (fn [list] (list list)))

(puzzle '(1 2 3))
;; ClassCastException clojure.lang.PersistentList cannot be cast to
;; clojure.lang.IFn  user/puzzle (NO_SOURCE_FILE:2)

;; This happens because within the function scope, list is used as an argument
;; variable. Without strong typing, the global function can just get clobbered.
;; This is fine as long as it is expected! Presumably the same thing happens
;; whenever we use "seq" as an arg name. Nice to know that it's harmless.
