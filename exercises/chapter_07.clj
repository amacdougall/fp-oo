(set [1 2 3 1 1])

#{1 2 3} ;; set literal

(contains? #{1 2 3} 1)
(contains? '(1 2 3) 1) ;; false!
(contains? [1 2 3] 1) ;; true

(#{1 2 3} 1) ;; set as callable: its argument is retrieved
(#{1 2 3} "b") ;; nil
(#{"a" "b" "c"} "b") ;; "b"

;; This is comparable to hash[key] in other languages, so it isn't that weird.

(use 'clojure.set) ;; hello, libraries

(union #{1 2} #{3 4})
(union #{1 2} #{2 3})

(intersection #{1 2} #{2 3})

(difference #{1 2} #{2 3})

;; Nice; I know from experience that "difference" can be a tough operation...

(difference
  (set (range 0 10))
  (set (range 10 20)))
;; actually only shows set1 elements not present in set2

(difference
  (set (range 0 1000000))
  (set (range 1 1000001)))
;; Also not super fast -- comparing these two sets takes about 8 seconds.
;; Granted, they're both million-item sets, so maybe 8 is incredible.

(filter odd? #{1 2 3}) ;; generates a sequence, beware

(select odd? #{1 2 3}) ;; generates a new set

;; 7.4

(def test-courses
  [{:course-name "Alpha"
    :morning? true
    :registered 5
    :limit 7}
   {:course-name "Beta"
    :morning? true
    :registered 10
    :limit 10}
   {:course-name "Gamma"
    :morning? true
    :registered 0
    :limit 1}
   {:course-name "Phi"
    :morning? false
    :registered 5
    :limit 7}
   {:course-name "Omicron"
    :morning? false
    :registered 2
    :limit 7}])

(def test-registrants
  [["Alpha", "Beta", "Gamma"]
   ["Beta", "Gamma", "Omicron"]])

;; Given a sequence of course maps, and a sequence of the courses a given
;; registrant is enrolled in, annotate the course map with :spaces-left and
;; :already-in? properties. :already-in? is true if the registrant is already
;; in this course. (Imagine registrants-courses as "registrant's courses" and
;; it will make more sense. Not the name I would have chosen.)
(def answer-annotations
  (fn [courses registrants-courses]
    (let [checking-set (set registrants-courses)]
      (map (fn [course]
             (assoc course
                    :spaces-left (- (:limit course)
                                    (:registered course))
                    :already-in? (contains? checking-set
                                            (:course-name course))))
           courses))))

(def current
  (answer-annotations test-courses (nth test-registrants 0)))


;; I think I would define the inline annotation function as a local, if that's
;; the applicable term, and call (map annotate courses) at the end. I don't
;; know Clojure idioms yet though.

;; looks like hashes have to be comma-delimited OR newline-delimited
(answer-annotations [{:course-name "alpha", :limit 4, :registered 3}
                     {:course-name "beta", :limit 1, :registered 1}]
                    ["beta"])

;; Given a sequence of courses already annotated by answer-annotations for a
;; given student, returns a sequence with :empty? and :full? for each course.
(def domain-annotations
  (fn [courses]
    (map (fn [course]
           (assoc course
                  :empty? (zero? (:registered course))
                  :full? (zero? (:spaces-left course))))
         courses)))

(domain-annotations [{:registered 1, :spaces-left 1},
                     {:registered 0, :spaces-left 1},
                     {:registered 1, :spaces-left 0}])

(def current
  (domain-annotations current))

;; this kind of thing is why _.pick is useful in underscore.js, I suppose.

;; Given a sequence of courses already annotated by domain-annotations, and a
;; total instructor count, returns a sequence with :unavailable? set for each
;; course if the course is full, or if it is empty but no instructors remain to
;; teach it.
(def note-unavailability
  (fn [courses instructor-count]
    (let [not-empty? (fn [course] (not (:empty? course)))
          occupied-courses (count (filter not-empty? courses))
          out-of-instructors? (= occupied-courses instructor-count)]
      (map (fn [course]
             (assoc course
                    :unavailable? (or (:full? course)
                                      (and out-of-instructors?
                                           (:empty? course)))))
           courses))))

(def current 
  (note-unavailability current 4))

;; As a result of the map function, (:unavailable? course) is true if the
;; course is full, or if it is empty and no instructors are available. The
;; author's let statement only sets out-of-instructors?, using a big nested
;; statement; I found it more readable to define each component of the
;; operation in small chunks.

(def annotate
  (fn [courses registrants-courses instructor-count]
    (note-unavailability
      (domain-annotations (answer-annotations courses registrants-courses))
      instructor-count)))

;; This has to be read "inside out," which is common in lisps. The author
;; suggests a more "culturally appropriate" flow:

(def annotate
  (fn [courses registrants-courses instructor-count]
    (let [answers (answer-annotations courses registrants-courses)
          domain (domain-annotations answers)
          complete (note-unavailability domain instructor-count)]
      complete)))

;; the -> version, which I don't quite get yet:
(def annotate
  (fn [courses registrants-courses instructor-count]
    (->
      courses
      (answer-annotations registrants-courses)
      domain-annotations ;; i.e. (domain-annotations courses)?
      (note-unavailability instructor-count))))

;; 7.5 exercises

;; exercise 1
(->
  [1]
  first ;; Could be (first), but parens are optional if
  inc   ;; there is only one argument to the function.
  list)

;; exercise 2
(->
  [1]
  first
  inc
  (* 3)
  list)

;; exercise 3
(->
  3
  ((fn [n] (* 2 n)))
  inc)

;; predefining the function as (def double ...) lets me just say (-> 3 double
;; inc), but with an inline function, I have to wrap it in parens. What gives?
;; The author says "An extra pair of parentheses is needed to 'protect' the
;; `fn` from `->`"... ah, I get it. The -> form tries to create this:
;; (fn [n] 3 (* 2 n)), which means it tries to call 3 on the result of (* 2 n).
;; Predefining the function would save us from ugly syntax.

;; exercise 4
(+ (* (+ 1 2) 3) 4) ;; evaluate from inside out

(->
  1
  (+ 2)
  (* 3)
  (+ 4))

;; 7.6

;; Returns a vector of two sequences: first those elements for which the
;; predicate returned true, then those for which it returned false.
(def separate
  (fn [predicate sequence]
    [(filter predicate sequence) (remove predicate sequence)]))

;; This sets us up for destructuring binds. Yay!

(def visible-courses
  (fn [courses]
    (let [[guaranteed possibles] (separate :already-in? courses)]
      (concat guaranteed (remove :unavailable? possibles)))))

(def final-shape
  (fn [courses]
    (let [desired-keys [:course-name :morning? :registered
                        :spaces-left :already-in?]]
      (map (fn [course]
             (select-keys course desired-keys))
           courses))))

(def half-day-solution
  (fn [courses registrants-courses instructor-count]
    (let [sort-by-name (fn [courses] (sort-by :course-name courses))]
      (-> courses
        (annotate registrants-courses instructor-count)
        visible-courses
        sort-by-name
        final-shape))))

;; I really like defining this kind of stuff in a let so the final data flow is
;; as clear as possible. I don't know if this is a good coding practice or if
;; I'm just catering to my own inexperience.

(def solution
  (fn [courses registrants-courses instructor-count]
    (map (fn [courses]
           (half-day-solution courses
                              registrants-courses
                              instructor-count))
         (separate :morning? courses))))

;; This makes sense! Though I'd have to zoom back down to individual functions
;; to tell you exactly what is happening in any given step.

;; 7.9 exercises

;; exercise 1: Managers cannot take afternoon courses. Implement.

;; So far, the only registrant information has been a list of the courses
;; the registrant is taking. Now we also need to know if the registrant
;; is a manager. Looks like we want to change arguments like this:
;; 
;; ["auto shop", "literary theory"]
;;
;; into this:
;;
;; {:manager? true, :courses ["auto shop", "literary theory"]}
;; 
;; Actually, I think we can handle this in the solution function, by simply
;; editing all afternoon classes after they have passed through the main
;; pipeline.

(def solution
  (fn [courses registrant instructor-count]
    (let [manager? (:manager? registrant)
          deny-to-manager (fn [course]
                        (if (and manager? (not (:morning? course)))
                          (assoc course :unavailable? true)
                          course))
          deny-all-to-manager (fn [courses]
                                (map deny-to-manager courses))]
      (map (fn [courses]
             (-> courses
               (half-day-solution (:courses registrant)
                                  instructor-count)
               (deny-all-to-manager)))
           (separate :morning? courses)))))

(solution test-courses
          {:manager? false, :courses ["Alpha", "Beta"]}
          3)

;; Clojure's crappy error messages almost discouraged me to the point where I
;; skipped a bugfix, but... oh, wait. These courses don't have an unavailable
;; property at this point, because it's removed by the final-shape function.
;; Okay, let's implement it more like the author intended. It was a hack
;; anyway.

;; Here is a cut and paste from sources/scheduling.clj. I'll edit it to
;; fit the new requirement.

(def answer-annotations
     (fn [courses registrant]
       (let [checking-set (set (:enrolled-in registrant))]
         (map (fn [course]
                (assoc course
                       :spaces-left (- (:limit course)
                                       (:registered course))
                       :already-in? (contains? checking-set
                                               (:course-name course))))
              courses))))

(def domain-annotations
     (fn [courses]
       (map (fn [course]
              (assoc course
                :empty? (zero? (:registered course))
                :full? (zero? (:spaces-left course))))
            courses)))

(def note-unavailability
     (fn [courses instructor-count]
       (let [out-of-instructors?
             (= instructor-count
                (count (filter (fn [course] (not (:empty? course)))
                               courses)))]
         (map (fn [course]
                (assoc course
                       :unavailable? (or (:full? course)
                                         (and out-of-instructors?
                                              (:empty? course)))))
              courses))))

(def annotate
     (fn [courses registrant instructor-count]
       (-> courses
           (answer-annotations registrant)
           domain-annotations
           (note-unavailability instructor-count))))

(def separate
     (fn [pred sequence]
       [(filter pred sequence) (remove pred sequence)]))

(def visible-courses
     (fn [courses]
       (let [[guaranteed possibles] (separate :already-in? courses)]
         (concat guaranteed (remove :unavailable? possibles)))))

(def final-shape
     (fn [courses]
       (let [desired-keys [:course-name :morning? :registered :spaces-left :already-in?]]
         (map (fn [course]
                (select-keys course desired-keys))
              courses))))

(def half-day-solution
     (fn [courses registrant instructor-count]
       (-> courses
           (annotate registrant instructor-count)
           visible-courses
           ((fn [courses] (sort-by :course-name courses)))
           final-shape)))

(def solution
     (fn [courses registrant instructor-count]
       (map (fn [courses]
              (half-day-solution courses registrant instructor-count))
            (separate :morning? courses))))


;; test data
(def test-registrants
  [{:manager? true, :enrolled-in ["Alpha", "Beta", "Gamma"]}
   {:manager? true, :enrolled-in ["Beta", "Gamma", "Omicron"]}
   {:manager? false, :enrolled-in ["Alpha", "Beta", "Gamma"]}
   {:manager? false, :enrolled-in ["Beta", "Gamma", "Omicron"]}])

;; intermediate tests

(answer-annotations test-courses (nth test-registrants 0))

(def current
  (answer-annotations test-courses (nth test-registrants 0)))

(domain-annotations current)

(def current
  (domain-annotations current))

(note-unavailability current 4)

(def current 
  (note-unavailability current 4))

(map (fn [n]
       (solution test-courses
                 (nth test-registrants n)
                 3))
     (range (count test-registrants)))
;; looks like this works!


