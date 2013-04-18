(def make-incrementer
  (fn [increment]
    (fn [x] (+ increment x))))

(def add4 (make-incrementer 4))
(add4 10)
(add4 0)
(add4 -4)

(def add3 (partial + 3)) ;; partial application special form

(add3 10)

;; I guess (partial function constant) expands to
;; (fn [& args] (apply function constant args))
;; ...but I'm not sure yet.

(def incish (partial map + [100 200 300]))

(incish [1 2 3])

(map + [1 2 3] [100 200 300])

(apply + 1 1 [1 1]) ;; looks like apply flattens?

;; the Clojure source code explains exactly what partial is: it's a macro,
;; using defn (which is sugar for def fn[]). Full details at the following URL.
;; I was more or less right though!
;;
;; https://github.com/clojure/clojure/blob/d0c380d9809fd242bec688c7134e900f0bbedcac/src/clj/clojure/core.clj#L2336

(def divisible-by-3
  (fn [n]
    (= (rem n 3) 0)))

(divisible-by-3 9)

(def indivisible-by-3 (complement divisible-by-3))

(indivisible-by-3 9)

;; function transformation!

;; transforms the input function to negate (flip the sign of) its result
(def negate
  (fn [function]
    (fn [& args] (- (apply function args)))))

((negate +) 10 10)

;; transforms the input function to add 5% to its result
(def madoffize
  (fn [function]
    (fn [& args] (* 1.05 (apply function args)))))

((madoffize *) 10 10)

;; of course, the author is leading up to a generalization:

(def lift
  (fn [modifier]
    (fn [base-function]
      (fn [& args] (modifier (apply base-function args))))))

(def negate (lift -))
(def madoffize (lift (partial * 1.05)))

;; (lift function) generates a function which takes an input function and
;; always applies the lifted function to its output. A bit confusing, but with
;; practice I think I could get used to it. Makes very simple something that
;; would be very verbose in JS, for instance.

((comp str +) 10 8 8) ;; composed function applies inputs right to left

(def input [[:times 5] [:line-number 3] ["hello" "goodbye"]])

(take-while (comp keyword? first) input) ;; take-while first is keyword

(comp not not predicate) ;; kind of sad that the !!value hack is seen here

;; "Many programming subcultures are afflicted by a cult of cleverness wherein
;; cult members show off by writing the tersest possible code; and much of
;; functional programming is one of those subcultures."
;;
;; I kind of figured.

;; 9.4 exercises

;; exercise 1
;; reimplement this using point-free style. Bonus points: more than 1 way!
(map (fn [n] (+ 2 n)) [1 2 3])

(map (partial + 2) [1 2 3]) ;; one...
(map (comp inc inc) [1 2 3]) ;; two...
(map ((lift inc) inc) [1 2 3]) ;; three...
;; ... out of ideas for the moment.


;; exercise 2

;; juxt turns its function arguments into a single function which returns a
;; vector of each function's result in turn. Using juxt, define separate.

;; original
(def separate
  (fn [predicate sequence]
    [(filter predicate sequence) (remove predicate sequence)]))

;; with complement
(def separate
  (fn [predicate sequence]
    [(filter predicate sequence) (filter (complement predicate) sequence)]))

;; with juxt -- just realized it doesn't need complement
(def separate
  (fn [predicate sequence]
    ((juxt filter remove) predicate sequence)))

;; ...is there an even simpler way? Oh, duh... there's ZERO difference between
;; the above and this:
(def separate (juxt filter remove))

(separate even? [1 2 3 4 5 6]) ;; proven working!


;; exercise 3
(def myfun
  (let [x 3]
    (fn [] x)))

;; predict the results:
;;
;; x =>
;; assuming myfun has already run? A binding error. x does not exist outside
;; the scope of myfun.
;;
;; (myfun) => A function which, when run, evaluates as 3.

x ;; yep, "Unable to resolve symbol: x"
(myfun) ;; no! Actually got 3.

;; Since this result was surprising, let's try to explain it. ...okay, I get
;; it. Since the final expression of the function is (fn [] x), and since that
;; is treated as a function, in itself, when encountered in inline code,
;; Clojure evaluates the function. This is equivalent to this:

(def myfun
  (let [x 3
        x-getter (fn [] x)]
    x-getter)) ;; this does not actually work!

;; Oh, I'm stupid! I just realized the _real_ reason I wasn't getting the
;; results I expected. myfun is not a damn function in the first place.
;; I spaced out and thought it was this:
(def myfun
  (fn []
    (let [x 3]
      (fn [] x))))

;; But of course it's not. myfun is defined as a function -- the one inside the
;; let statement. This means that lets can create clojures, which is cool.

(def add-5
  (let [x 5]
    (fn [n] (+ n x))))

(add-5 10) ;; sure enough!


;; exercise 4
(def myfun
  ((fn [n] (fn [] n)) 3))

(myfun) ;; works -- but looks gross.


;; exercise 5
(def my-atom (atom 0))
(swap! my-atom inc)
(deref my-atom)
;; not entirely sure I get atoms, but I'll bear with them

;; so swap! applies the function to the value of the target symbol, and
;; replaces that symbol's value with the result. I guess? Looks suspiciously
;; like a variable.

(swap! my-atom (fn [n] 33)) ;; not terribly impressive? But it works.


;; exercise 6
(def always
  (fn [value]
    (fn [& args] value)))

((always 8) 1 'a :foo)


;; exercise 7
;; find the typo among the following three ISBNs:
;; 0131774115, 0977716614, and 1934356190

;; Given a vector of numbers, returns the sum of each number multiplied by its
;; 1-based index.
(def check-sum
  (fn [numbers]
    (let [pairs (partition 2 (interleave
                               numbers
                               (map inc (range (count numbers)))))]
      (apply + (map (fn [pair] (apply * pair)) pairs)))))

;; I had a recursive solution in mind, but the author says to use map. I also
;; considered holding the multiplier in a closure which would increment it on
;; each execution, but that's not possible given the constructs on display thus
;; far. (Resisted the temptation to try to use an atom as a variable.)
;; So instead I came up with this crazy-ass version, which works, but is
;; surely not what the author had in mind. Here is what the author did:

(def check-sum
     (fn [sequence]
       (apply + (map *
                     (range 1 (inc (count sequence)))
                     sequence))))

;; I guess this is not completely different, but he deftly skips the pair
;; creation phase by taking advantage of how map can take two damn sequence
;; arguments and then apply the first sequence to the second. Essentially,
;; map interleaves itself.

(map * [1 1 1] [1 2 3]) ;; yep!

(check-sum [1 1 1 1 1])

(partition 2 (interleave [1 1 1 1 1] (range (count [1 1 1 1 1]))))


;; exercise 8
;; The exercise says to determine if a string, specifically, is a valid ISBN;
;; so I'm using strings.
(def isbns ["0131774115", "0977716614", "1934356190"])

;; from the author's exercise source code
(def reversed-digits
     (fn [string]
       (map (fn [digit-char]
              (-> digit-char str Integer.))
            (reverse string))))

(def isbn?
  (fn [isbn]
    (let [digits (reversed-digits isbn)]
      (= (rem (check-sum digits) 11) 0))))

(map isbn? isbns) ;; seems to work! Only the middle one returns false.


;; exercise 9

;; Now to handle the UPC checksum by multiplying by '(1, 3) repeating.

(def upc-check-sum
  (fn [sequence]
    (let [indices (range 1 (inc (count sequence)))]
      (apply + (map (fn [n i] (* n (if (odd? i) 1 3)))
                    sequence indices)))))

;; I feel like there might be a simpler solution for my map function... but
;; after checking the solutions file, it looks like I was on target.

(def upc?
  (fn [upc]
    (let [digits (reversed-digits upc)]
      (= (rem (upc-check-sum digits) 10) 0))))


(upc? "074182265830")
(upc? "731124100023")
(upc? "722252601404") ;; This one is incorrect.
;; confirmed working!


;; exercise 10

(def number-checker
  (fn [check-sum divisor]
    (fn [numbers]
      (let [digits (reversed-digits numbers)]
        (= (rem (check-sum digits) divisor) 0)))))

(def upc-checker (number-checker upc-check-sum 10))

(upc-checker "074182265830")
(upc-checker "731124100023")
(upc-checker "722252601404") ;; This one is incorrect.
;; confirmed working!


