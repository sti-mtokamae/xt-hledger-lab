;; manifest.scm - Guix environment definition for xt-hledger-lab
;; Usage: guix shell -m manifest.scm
;; 
;; Note: PostgreSQL server setup may be required separately for production use.
;; For development/testing, you can use the provided docker-compose or local setup.

(specifications->manifest
 '(;; Clojure + Java environment for XTDB v2
   "openjdk@21"              ; Java 21 LTS (recent stable)
   "clojure-tools"           ; Clojure CLI for REPL and deps.edn
   "rlwrap"                  ; Readline wrapper for better REPL experience

   ;; PostgreSQL backend (required for XTDB v2 SQL storage)
   "postgresql"              ; PostgreSQL server and client tools

   ;; Plain Text Accounting tools
   "hledger"                 ; Primary accounting tool (Haskell-based)
   "ledger"                  ; Original reference implementation

   ;; Development and documentation
   "git"                     ; Version control
   "emacs"                   ; Text editor (optional but recommended)
   ))
