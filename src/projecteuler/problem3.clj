(ns projecteuler.problem3
  (:require [clojure.set :as s])
  (:require [clojure.core.matrix :as m])
  (:require [incanter.core :refer :all :as incanter])
  (:use [projecteuler.libs.numbertheory :as nt])
  (:import (java.lang Math)))

;; The prime factors of 13195 are 5, 7, 13 and 29.
;; What is the largest prime factor of the number 600851475143?

(defn- debug-smooth
  "I put this in a separate function becuase it seems I rely on this a bit. It
  takes as input the whatever the current state of a factorization is, the number
  being reduced by division of prime factors, and the facts that have been removed
  from the target value.

  Input <- factorization [a vector],
           to-reduce, an integer,
           factors [a vector]

  Output -> (Side effects, println)"
  [factorization to-reduce factors]
  (println "Factorization: " factorization,
           ", to-reduce: " to-reduce,
           ", factors:" factors))

(defn even-coefficients?
  "When applied to a vector of binary coefficients, reduces the vector by addition.
  If the sum is 0, true is returned, because an even coefficient is denoted by 0 in our
  vectors of binary coefficients. Therefore, any non-zero result will return false.

  Input <- v, a vector of binary coefficients (even or odd coefficient values)

  Output -> A boolean, true if reduce returns 0, false otherwise."
  [v]
  (if (zero? (reduce + v))
    true
    false))

(defn reduce-to-binary-coefficients
  "This function will take a vector, such as one produced by reduce-to-coefficients,
  and transform it into a vector where if the coefficient is even, it is replaced with
  a 0. If the coefficient is odd, it is replaced with a 1.

  Input <- v, a vector of coefficients for the factorization of some number

  Output -> A vector of the same length of v where each index represents whether
            or not a value is even (0) or odd (1)."
  [v]
  (into []
        (map (fn [i]
               (if (even? i)
                 0
                 1))
             v)))

(defn reduce-to-coefficients
  "reduce-to-coefficients will take as input a non-false result from b-smooth?
  i.e. a vector of prime numbers in decreasing order. From this input, the function
  will return a smaller vector, proportional to the size of the factor base, which
  contains the coefficients of each prime number in the factor base in ascending
  order with respect to the factor base.

  Example: [7 5 5 3 2 2 2 2] will be reduced to [4 1 2 1]
           For factorizations that lack factors that are in the factor base, nil
           is replaced with 0, so that [7 5 3 2 2] with a factor base of [2 3 5 7 11]
           will return a coefficient vector of [2 1 1 1 0].

  Input <- v, a vector of prime numbers to be operated on.
           factor-base, the vector of prime factors used in finding smooth numbers.

  Output -> A vector consisting of the coefficients of prime numbers, in ascending
            order with respect to the primes."
  [v factor-base]
  (loop [frequency-map (frequencies (rseq v)),
         base factor-base,
         coefficient-vec '[]]
    (if (empty? base)
      coefficient-vec
      (recur frequency-map
             (rest base)
             (conj coefficient-vec (get frequency-map
                                        (first base)
                                        0))))))

(defn b-smooth?
  "A function, that when given some random integer z, the integer n (which we
  wish to factor,) and the appropriately sized factor base for n factor-base,
  will determine whether or not the square of z is B-smooth with respect to the
  integer to be factorized and the factor base.

  Input <- z, an integer, randomly chosen by dixon-factorization,
           n, the integer to be factorized. Participates in the relationship
              (z^2 mod n), the result of which is checked for B-smoothness,
           factor-base, a vector of integers constituting the factor base.

  Output -> Either a vector representing the expanded prime factorization of
            the value of (z^2 mod n) or false if there is no B-smooth
            factorization with respect to the factor base."
  [z n factor-base]
  (let [z2 (Math/pow z 2.0)
        z2-mod-n (long (mod z2 n))]
    (loop [factorization '[]
           to-reduce z2-mod-n
           factors (into [] (rseq factor-base))]

      ;; Debug output
      ;; (debug-smooth factorization to-reduce factors)
      ;; End debug output

      (cond
        (= to-reduce 1)
        (reduce-to-coefficients factorization factor-base),

        (nil? (peek factors))
        false,

        :else
        (if (= (mod to-reduce (first factors)) 0)
          (recur (conj factorization (first factors))
                 (/ to-reduce (first factors))
                 factors)
          (recur factorization
                 to-reduce
                 (into [] (rest factors))))))))

(defn optimal-b-value
  "Given an integer, n, this function will employ the relation exp(sqrt(log n log log n))
  to come up with an ideal, optimized B-value to define the factor base to be used in
  determining the B-smoothness of a candidate z value.

  Input <- n, an integer, or long, potentially very large.

  Output -> An integer, ceil'd from the floating point result of the calculation"
  [n]
  (Math/ceil (Math/exp (Math/sqrt (* (Math/log10 n)
                                     (Math/log10 (Math/log10 n)))))))

(defn generate-b-smooths
  "Given a value of n, will compute an optimal B-value, the associated
  factor base for that B-value and n, followed by the number of B-smooth
  numbers with respect to n that the algorithm needs to compute in order
  to factorize n. The function will then generate enough of these b-smooth
  relations and then return a map in which the keys are the z values used
  to generate the small prime factorizations, mapped to those factorizations.

  Input <- n, the integer to be factorized.

  Ouput -> smooth-factorizations, a map where the keys are the z values and
           the vals are coefficient vectors that satisfy the relation z^2 mod n
           with respect to the factor base generated by the function. This may
           return a map smaller than that of desired-smooth-numbers if we stumble
           upon a relation whose b-smooth factorization consists only of even
           exponents."
  [n]
  (let [factor-base (nt/generate-primes-to (optimal-b-value n)),
        desired-smooth-numbers (inc (count factor-base)),
        constant-map {:constants {:factorbase factor-base,
                                  :n n}}]
    (loop [smooth-factorizations {},
           smooth-counter 0,
           z (nt/random-in-range (Math/ceil (Math/sqrt n)) n)]
      (let [maybe-smooth (b-smooth? z n factor-base)]
        (cond
          (and maybe-smooth (even-coefficients? (reduce-to-binary-coefficients maybe-smooth)))
          (assoc constant-map z maybe-smooth)

          (= smooth-counter desired-smooth-numbers)
          (merge constant-map smooth-factorizations)

          maybe-smooth
          (recur (assoc smooth-factorizations z maybe-smooth)
                 (inc smooth-counter)
                 (nt/random-in-range (Math/ceil (Math/sqrt n)) n))

          :else
          (recur smooth-factorizations
                 smooth-counter
                 (nt/random-in-range (Math/ceil (Math/sqrt n)) n)))))))

(defn test-for-divisor
  "Given a relation of two squares in the form of a random z value and a
  1-d matrix, the first element of which being the coefficients, the second
  being the factor base, will check for the existence of a non-trivial
  divisor for n i.e. not 1.

  Input -> m, a map, comprised of a nested map of :constants consisting of them
          :factorbase and :n value, the remaining elements of the map should being
          a z value and its exponent vector.

  Output -> A vector consisting of one or two non-trivial divisors of n,
            or the vector [1 1]"
  [m]
  (let [factor-base (:factorbase (:constants m))
        n (:n (:constants m))
        coefficients (-> m
                         last
                         last)
        a (-> m
              last
              first)
        b (Math/sqrt (reduce * (map (fn [x y]
                                      (Math/pow x y))
                                    factor-base
                                    coefficients)))]

    (println "Congruence of squares between: " a "and" b)

    [(int (nt/gcd (+ a b) n))
     (int (nt/gcd (- a b) n))]))

(defn matrix-computation
  "Given a matrix consisting of the constants factor-base and n, will attempt to
  find a linear dependence between the rows of coefficients and, from there,
  locating a congruence of squares from which we can derive, hopefully, a non
  trivial pair of factors.

  Input <- m, a map, comprised of a nested map of :constants consisting of them
          :factorbase and :n value, the remaining elements of the map should be
          pairs of a z value and its exponent vector.

  Output -> A vector consisting of one or two non-trivial divisors of n,
            otherwise containing n and the trivial case (1)."
  [m]
  (let [factor-base (:factorbase (:constants m))
        n (:n (:constants m))
        z-values (into [] (-> m
                              rest
                              keys))
        coefficients (into [] (map reduce-to-binary-coefficients
                                   (-> m
                                       rest
                                       vals)))]
    (println "Z-values: " z-values ", Coefficients: " coefficients))
  ;; Woe is me, I am incomplete. I am undone.)

(defn test-primality
  "Provided a map produced by generate-b-smooths, will apply test-for-divisor
  and test the returned elements for primality.

  Input <- A map produced by generate-b-smooths which will contain the n value
           we are attempting to factor, the factor base for the factorization,
           as well as either 1 key/value pair of z and an exponent vector or
           (inc (count factor-base)) pairs of the same.

  Output -> A set of prime factors of n. These may be trivial."
  [m]
  (hash-set (filter integer?
                    (map prime? (test-for-divisor m)))))

(defn accumulate-primes
  "Accumulate primes, when given an integer n, will attempt to find congruences
  of squares by means of the function generate-b-smooths. If it finds a congruence
  with an even coefficient vector, it will terminate its search for more b-smooth
  relations and test to see if that value provides a prime factor of n.

  If generate-b-smooths returns a map with greater than 1 z-value/coefficient
  vector pairs, the map will be passed to matrix-computation to search for
  prime factors. matrix-computation will return a set containing one or two
  prime factors, should they be found. If so, they are added to the set of
  prime factors, prime-factorization, which is the function's output.

  Input <- n, an integer.

  Output -> a set of all prime numbers that compose the given integer n."
  [n]
  (loop [to-reduce (int n),
         prime-factorization #{}]

    (if #spy/p (<= to-reduce 1)
      prime-factorization

      (let [smooth-factorization (generate-b-smooths to-reduce)]
        (cond
          (= (count smooth-factorization) 2)
          (let [potential-primes (test-primality smooth-factorization)]
            (if potential-primes
              (recur (reduce / to-reduce potential-primes)
                     (s/union prime-factorization potential-primes))
              (recur to-reduce
                     prime-factorization))),
          :else
          ;; (let [matrix-result (matrix-computation smooth-factorization)]
          ;;   (if matrix-result
          ;;     (recur (reduce / to-reduce matrix-result)
          ;;            (s/union prime-factorization matrix-result))
          ;;     (recur to-reduce
          ;;            prime-factorization)))
          nil)))))

(defn dixon-factorization
  "Given a value, n, this function will attempt to locate the prime factors
  of n. This technique employs random sampling of integers between sqrt(n) and
  n and attempts to ascertain whether or not the squares of those random integers
  are B-smooth, where B is parameter for defining the factor base used in
  the algorithm. The B value for a given n is computed using the function
  optimal-b-value, defined above.

  Input <- n, an integer, potentially very large.

  Output -> p, an integer, the largest prime factor of n."
  [n]
  (loop [b-smooths (generate-b-smooths n)]
    (if (= 1 (count (rest b-smooths)))
      (test-for-divisor b-smooths)
      (println "ehhhhhh"))))
