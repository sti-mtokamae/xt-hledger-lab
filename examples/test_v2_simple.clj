;; examples/test_v2_simple.clj
;; XTDB v2.1.0 シンプルテスト

(println "=" 50)
(println "XTDB v2.1.0 Artifact & API Test")
(println "=" 50)

(println "\n📦 Loading XTDB v2.1.0...")
(require '[xtdb.api :as xt])
(println "✅ XTDB loaded")

(println "\n🚀 Starting in-memory node...")
(with-open [node (xt/start-node {})]
  (println "✅ Node open")
  
  (println "\n📝 Submitting document...")
  (xt/submit-tx node
    [[:put :person
      {:xt/id "alice"
       :name "Alice"
       :age 30}]])
  (println "✅ Document submitted")
  
  (println "\n🔍 Running query...")
  (let [results (xt/q (xt/open-q node)
                  '(find ?name ?age
                    :where [[?e :name ?name]
                            [?e :age ?age]]))]
    (println "✅ Results:" (seq results)))
  
  (println "\n" "=" 50)
  (println "🎉 Test Complete!")
  (println "=" 50))

(System/exit 0)
