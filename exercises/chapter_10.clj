(def tolerant-inc
  (fn [n]
    (inc (if (nil? n) 0 n))))

(tolerant-inc nil)

(def nil-patch
  (fn [function default]
    (let [nil-to-default (fn [arg] (if (nil? arg) default arg))]
      (fn [& args]
        (apply function (map nil-to-default args))))))

(+ 1 2 nil)

((nil-patch + 0) 1 2 nil) ;; success!

;; I implemented this before looking at the author's version. Mine handles
;; functions which take multiple arguments, but that's a trivial upgrade.

;; Continuation-passing style
(+ (* (+ 1 2) 3) 4)

(-> (+ 1 2)
  ((fn [step1-value]
     (+ (* step1-value 3) 4))))

(-> (+ 1 2)
  ((fn [step1-value]
     (-> (* step1-value 3)
       ((fn [step2-value]
          (+ step2-value 4)))))))

(-> (+ 1 2)
  ((fn [step1-value]
     (-> (* step1-value 3)
       ((fn [step2-value]
          (+ step2-value
             step1-value)))))))
;; this is an example of using lexical scoping; doesn't do the same thing.

;; 10.3 exercises

;; exercise 1
;; Convert to CPS:
(let [a (concat '(a b c) '(d e f))
      b (count a)]
  (odd? b))

(-> (concat '(a b c) '(d e f))
  ((fn [list]
     (-> (count list)
       ((fn [list-count]
          (odd? list-count)))))))


;; exercise 2
;; Convert to CPS:
(odd? (count (concat '(a b c) '(d e f))))

;; ...mentally evaluating the steps from inside out, it goes in the same order
;; as the code from exercise 1: concat, count, odd?. So I'd code it exactly the
;; same, really.

;; The author asks: "is it _necessarily_ the same, or could you make an
;; argument that you could (or should) choose different continuations?"

;; I'm not sure what he's asking, so I'm reading his hint: "How might you
;; write (concat '(a b c) '(d e f)) in continuation-passing style?"

(-> '(a b c)
  ((fn [list]
     (concat list '(d e f)))))
;; ...I guess? So maybe I should make the concatenation a function?

(-> '(a b c)
  ((fn [list]
     (-> (concat list '(d e f))
       ((fn [list]
          (-> (count list)
            ((fn [list-count]
               (odd? list-count))))))))))

;; This seems to work. Let's check the official solution. ...yep, further
;; decomposing the concatenation operation was the trick.


;; exercise 3
;; Convert to CPS:
(-> 3
  (+ 2)
  inc)

(-> 3
  ((fn [n]
     (-> (+ n 2)
       ((fn [n]
          (inc n)))))))


;; I don't yet see the value of continuation-passing style, but I know from
;; other reading that it has something to do with closures. Possibly also
;; coroutines?

;; 10.5
(-> (+ 1 2)
  ((fn [step1-value]
     (-> (* step1-value 3)
       ((fn [step2-value]
          (+ step2-value 4)))))))

(def decider
  (fn [step-value continuation]
    (continuation step-value)))

;; note that this calculation is not the same as the preceding
(-> (+ 1 2)
  (decider (fn [step1-value]
             (-> (* step1-value 3)
               (decider (fn [step2-value]
                          (+ step2-value step1-value)))))))

;; Redefine as a guard against nil.
(def decider
  (fn [step-value continuation]
    (if (nil? step-value)
      nil
      (continuation step-value))))

;; Test:
(-> (+ 1 2 nil)
  (decider (fn [step1-value]
             (prn "Step 2")
             (-> (* step1-value 3)
               (decider (fn [step2-value]
                          (prn "Step 3")
                          (+ step2-value step1-value)))))))

;; Definitely returns nil before applying any continuations.

;; 10.6 Monads are evidently always scary

;; 10.7 Metadata

(pprint (meta )) ;; this prints and returns nil for me in leiningen

(def a {:map "me"})

(def b (with-meta a {:type :error}))

(meta b) ;; maybe + just didn't have metadata, because this works

(= a b) ;; true, because almost everything ignores metadata

(meta "hi")
(meta nil)
;; it's always safe to request metadata, though it may be nil

(:open? (meta a)) ;; also safe to try to get nonexistent keywords

(if (:open? (meta a))
  (prn "Did a thing"))

(type b)
;; Haven't heard much yet about Clojure types; guess they're held in metadata.
;; Good to know. "Stringly typed," as they say?

;; 10.8 Cond
;; I remember cond from SICP. Kind of an if/else.

(def classify
  (fn [n]
    (cond (zero? n) "zero"
          (even? n) "even"
          :else "unclassified"))) ;; note the else keyword

;; Pretty cool how even this special form is clearly written as a macro that
;; uses Clojure syntax. No special syntax, really. Actually, the author says
;; that _any_ keyword counts as true, so using :else or :default at the end
;; would be purely a convention.

(cond) ;; useful! (Not actually useful.)

;; 10.9 Exercises

;; exercise 1

;; Looks like we're implementing the Error monad! Or something.
(use 'clojure.algo.monads)

(def oops!
  (fn [message key value]
    (prn message key value)))

(def factorial
  (fn [n]
    (cond (< n 0) (oops! "Factorial can never be less than zero."
                   :number n)
          (< n 2) 1
          :else (* n (factorial (dec n))))))

(factorial -1)

;; We want to enable this code:
(def result
  (with-monad error-monad ;; so as not to collide with error-m?
    (domonad [big-number (factorial -1)
              even-bigger (* 2 big-number)]
             (repeat :a even-bigger))))

(oopsie? result) ;; should be true

(:reason result) ;; should be the error message

(:number result) ;; should be the actual number that was passed in

;; Given a "reason" (an error message) and any even number of additional
;; arguments which form key-value pairs, returns a hashmap of :reason and each
;; key-value pair, with a :type metadata of :error.
(def oops!
  (fn [reason & args]
    (with-meta (merge {:reason reason}
                      (apply hash-map args)) ;; pairs to hashmap
               {:type :error})))

(def oopsie?
  (fn [value]
    (= (type value) :error)))

;; example maybe monad
(def decider
  (fn [step-value continuation]
    (if (nil? step-value)
      nil
      (continuation step-value))))

(def maybe-monad
  (monad [m-result identity
          m-bind decider]))

;; our Error monad can just be...
(def error-monad
  (monad [m-result identity
          m-bind (fn [step-value continuation]
                   (if (= (type step-value) :error)
                     step-value
                     (continuation step-value)))]))

;; pasting the test code again for convenience...
(def result
  (with-monad error-monad ;; so as not to collide with error-m?
    (domonad [big-number (factorial -1)
              even-bigger (* 2 big-number)]
             (repeat :a even-bigger))))

(oopsie? result) ;; should be true

(:reason result) ;; should be the error message

(:number result) ;; should be the actual number that was passed in

;; All tests clear.


;; 10.10 Monadic Lift

(def +?
  (fn [arg1 arg2]
    (with-monad maybe-m
      (domonad [a1 arg1
                a2 arg2]
        (+ a1 a2)))))

(+? 1 nil)

;; this works because the monad acts as a let. Do all monads work that way?
;; Either way, the syntax seems awkward.

;; Interesting note: Clojure has no arity query. This may explain all the
;; argument list things I'm seeing for macros? I thought it was pattern
;; matching, but maybe it's more basic than that.

(def +?
  (with-monad maybe-m
    (m-lift 2 +)))

;; I guess (m-lift n function) returns a version of the function which applies
;; the enclosing monad to exactly n arguments of the function.

;; I guess I'll have to work with this more before I start to really grasp the
;; real-world applications of this technique.

;; 10.11 Loops into Flows

(for [a [1, 2]
      b [10, 100]
      c [-1, 1]]
  (* a b c))

;; => (-10 10 -100 100 -20 20 -200 200)

;; So this kind of abstracts away a loop-like process: it takes the operation
;; specified after the for statement -- another let-like statement, like
;; domonad -- and applies it to each value in the loops.

(for [a ["Hello ", "Bonjour "]
      b ["friend", "mon ami"]]
  (str a b))

;; => ("Hello friend" "Hello mon ami" "Bonjour friend" "Bonjour mon ami")
;; This makes it a bit more clear.

(for [a [1 2 3]
      b (repeat a "hi")]
  [a b])

;; for statements return ALL the results of the loops as a single list. In the
;; preceding, the first iteration of a is 1; thus the first iteration of b is
;; (repeat 1 "hi"), or a single [1 "hi"]. The third is 3; so b is (repeat 3
;; "hi"), or three [3 "hi"]s. All results are concatted in the return value of
;; the for statement.

;; Unlike the first example, the second element of the for [] statement is
;; an expression, not a loop bound.

(with-monad sequence-m
  (domonad [a [1 2 3]
            b (repeat a "hi!")]
    [a b]))

;; I'm still unclear on how repeat interacts with the macro/monad. I think
;; there's an implicit step in processing the output that I'm not quite
;; getting.

(pairwise-plus [1 2 3] [4 5 6])
;; => (5 6 7 6 7 8 7 8 9)
;; i.e. add the first list's element to each of the last list's elements,
;; then advance to the next element of the first list, etc.

(def pairwise-plus
  (fn [seq1 seq2]
    (for [a seq1
          b seq2]
      (+ a b))))

;; Okay, this helps me understand the repeat thing. Each iteration of the a
;; term begins a loop over the b term. Say the a term is 3; the b term is now
;; ["hi!" "hi!" "hi!"]. When looping over that vector, b is set to each
;; separate "hi!" in turn, so [a b] refers to a (which is 3) and ONE of the
;; "hi!"s in the b vector.
;;
;; I will probably continue to forget this.

(def pairwise-plus
  (with-monad sequence-m (m-lift 2 +)))

(pairwise-plus [1 2 3] [4 5 6])

;; stampy mind blown

;; 10.12 Exercises

;; exercise 1

;; All non-prime multiples of 2 up to 100.
(range (* 2 2) 101 2) ;; last parameter is the step increment?

;; And of 3:
(range (* 3 2) 101 3)

;; I see where this is going. sequence-m Sieve of Eratosthenes?
(range (* 4 2) 101 4)

(def multiples
  (fn [n]
    (range (* n 2) 101 n)))

(map multiples [2 3 5])


;; exercise 2

(def non-primes
  (fn []
    (for [factor (range 2 100)
          non-prime (multiples factor)]
      non-prime)))

(non-primes) ;; confirmed

;; This works mainly because (multiples 51), for instance, attempts to
;; use (range 102 101), which is ().


;; exercise 3
(use 'clojure.set)

(difference (set (range 2 10)) #{2 3 4})

;; The Sieve of Eratosthenes
(def primes
  (fn []
    (sort (into [] (difference (set (range 2 101))
                               (set (non-primes)))))))
;; I had to look up the "into" statement, but it looks super useful. My
;; solution did not use remove, as the author suggested, but it amounts to the
;; same thing.

(primes) ;; confirmed

