(ns edge.play
  (:require
   [aleph.http :as http]
   [cheshire.core :as json]
   [byte-streams :as bs]))

#_(-> @(http/get "http://live-cdn.me-tail.net/cantor/api/wanda/garments-available/81e36a81-1921-4247-a8de-1ed7eb67840f?skus=nct_sandbox_dress_37_7cpxa7,nct_sandbox_trousers_2_5bahk2,nct_sandbox_jacket_4_btcjsf,nct_sandbox_coat_3_vxbetj,nct_sandbox_top_24_7k94td,nct_sandbox_skirt_15_04s5md,nct_sandbox_top_15_zwzenw")
    :body
    (bs/to-string)
    (json/decode)
    prn
    )

#_(-> @(http/get "http://live-cdn.me-tail.net/cantor/api/wanda/garment-details/81e36a81-1921-4247-a8de-1ed7eb67840f?skus=nct_sandbox_dress_37_7cpxa7,nct_sandbox_trousers_2_5bahk2,nct_sandbox_jacket_4_btcjsf,nct_sandbox_coat_3_vxbetj,nct_sandbox_top_24_7k94td,nct_sandbox_skirt_15_04s5md,nct_sandbox_top_15_zwzenw")
    :body
    (bs/to-string)
    (json/decode)
    count
    )
