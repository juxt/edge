;; Copyright © 2020, JUXT LTD.

(ns juxt.mmxx.compiler)

(defprotocol PayloadCompiler
  (last-modified-date [_]
    "Return the computed last modified date, usually the most recent
  modification date of any of the resources used in the production of the
  representation.")
  (entity-tag [_] "")
  (payload [_] "Return the body payload of the representation."))
