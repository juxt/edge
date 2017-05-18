# Edge

From "Datum edge". Also represents the 'edge' of our current thinking
on the baseline architecture of Clojure projects. It contains
architectural patterns for the rapid construction of robust and
flexible systems.

## Features

Edge is a simple project demonstrating the following:

### A boot-driven Clojure/ClojureScript dev and prod environment

```
cd edge
boot dev
```

Browse to localhost:3000

To fire up a REPL, open a new session and

```
cd edge
boot repl
```

Make changes to the Clojure code and reset the system

```
REPL-y 0.3.7, nREPL 0.2.12
Clojure 1.8.0
OpenJDK 64-Bit Server VM 1.8.0_92-b14
        Exit: Control+D or (exit) or (quit)
    Commands: (user/help)
        Docs: (doc function-name-here)
              (find-doc "part-of-name-here")
Find by Name: (find-name "part-of-name-here")
      Source: (source function-name-here)
     Javadoc: (javadoc java-object-or-class-here)
    Examples from clojuredocs.org: [clojuredocs or cdoc]
              (user/clojuredocs name-here)
              (user/clojuredocs "ns-here" "name-here")
user> (reset)
```

### A SASS CSS build

Make changes to the sass files under sass.

### API server

- [A Stuart Sierra component reloaded project](https://github.com/stuartsierra/component)
- Use of `schema.core/defrecord` to validate your system's integrity on every reset
- Run all your tests with `(test-all)`
- bidi & yada for serving web resources and APIs

## ClojureScript REPL

An optional ClojureScript REPL is available. Create a new REPL by connecting to localhost on port 5600. You can do this with boot, lein, CIDER or with another IDE. Once the REPL has started, launch the CLJS repl with the following:

```
user> (cljs-repl)
```

This should connect with your browser and you can then interactively work with cljs.

## Libraries

- aero
- aleph
- bidi
- hiccup
- selmer
- yada

## CIDER integration

These instructions are for use with CIDER 0.12 (Seattle). If your
Emacs is using a previous version, you should upgrade now.

Add the following to your `$HOME/.boot/profile.boot`

```clojure
(deftask cider "CIDER profile"
  []
  (require 'boot.repl)
  (swap! @(resolve 'boot.repl/*default-dependencies*)
         concat '[[org.clojure/tools.nrepl "0.2.12"]
                  [cider/cider-nrepl "0.14.0"]
                  [refactor-nrepl "2.2.0"]])
  (swap! @(resolve 'boot.repl/*default-middleware*)
         concat '[cider.nrepl/cider-middleware
                  refactor-nrepl.middleware/wrap-refactor])
  identity)
```

Ensure the version of the `cider/cider-nrepl` dependency matches the version of CIDER you are using.

Start your REPL with the following:

```
boot cider dev
```

From Emacs, use `M-x cider-connect`

Use port 5600 to connect for a server CLJ REPL

## Deployment

A systemd wrapper script is provided in the top level directory. It takes a parameter which determines the Aero profile to use. In this way you can have multiple systemd units with different profiles on the same machine, even under the same username.

## Copyright & License

The MIT License (MIT)

Copyright © 2016-2017 JUXT LTD.

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
