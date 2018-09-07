(ns brawl-haus.data)

#_(def long-texts ["A fast text to type"])
(def long-texts ["In computer science, ACID (Atomicity, Consistency, Isolation, Durability) is a set of properties of database transactions intended to guarantee validity even in the event of errors, power failures, etc. In the context of databases, a sequence of database operations that satisfies the ACID properties, and thus can be perceived as a single logical operation on the data, is called a transaction"
                 "For example, a transfer of funds from one bank account to another, even involving multiple changes such as debiting one account and crediting another, is a single transaction."
                 "In 1983, Andreas Reuter and Theo Harder coined the acronym ACID as shorthand for Atomicity, Consistency, Isolation, and Durability, building on earlier work by Jim Gray who enumerated Atomicity, Consistency, and Durability but left out Isolation when characterizing the transaction concept."
                 "These four properties describe the major guarantees of the transaction paradigm, which has influenced many aspects of development in database systems."
                 "Atomicity. Transactions are often composed of multiple statements. Atomicity guarantees that each transaction is treated as a single \"unit\", which either succeeds completely, or fails completely: if any of the statements constituting a transaction fails to complete, the entire transaction fails and the database is left unchanged."
                 "An atomic system must guarantee atomicity in each and every situation, including power failures, errors and crashes."
                 "Consistency. Consistency ensures that a transaction can only bring the database from one valid state to another, maintaining database invariants: any data written to the database must be valid according to all defined rules, including constraints, cascades, triggers, and any combination thereof."
                 "This prevents database corruption by an illegal transaction, but does not guarantee that a transaction is correct."
                 #_"Isolation. Transactions are often executed concurrently (eg. reading and writing to multiple tables at the same time). Isolation ensures that concurrent execution of transactions leaves the database in the same state that would've been obtained if the transactions were executed sequentially. Isolation is the main goal of concurrency control; depending on the method used, the effects of an incomplete transaction might not even be visible to other transactions."
                 "Durability. Durability guarantees that once a transaction has been committed, it will remain committed even in the case of a system failure (eg. power outage or crash). This usually means that completed transactions (or their effects) are recorded in non-volatile memory."])

(def names (-> (slurp "resources/public/names.txt")
               (clojure.string/split #"\n")))
