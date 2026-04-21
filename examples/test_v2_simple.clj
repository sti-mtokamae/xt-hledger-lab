;; examples/test_v2_simple.clj
;; XTDB v2.1.0 シンプルテスト

(println "=" 50)
(println "XTDB v2.1.0 Artifact & API Test")
(println "=" 50)

(println "\n📦 Loading XTDB v2.1.0...")
(require '[xtdb.node :as xtn]
         '[xtdb.api :as xt])
(println "✅ XTDB loaded")

(println "\n🚀 Starting in-memory node...")
(with-open [node (xtn/start-node {})]
  (println "✅ Node open")

  (println "\n📝 Submitting document...")
  (xt/execute-tx node
                 [[:put-docs :person
                   {:xt/id "alice"
                    :name "Alice"
                    :age 30}]])
  (println "✅ Document submitted")

  (println "\n🔍 Running query...")
  (let [results (xt/q node
                      "SELECT name, age FROM person")]
    (println "✅ Results:" (seq results)))

  (println "\n" "=" 50)
  (println "🎉 Test Complete!")
  (println "=" 50))

;; (System/exit 0)
