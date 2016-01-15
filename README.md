# Edge

From "Datum edge". Also represents the 'edge' of our current thinking
on the baseline architecture of Clojure projects. It contains
architectural patterns for the rapid construction of robust and
flexible systems.

## Features

Edge is a simple project demonstrating the following:

### Front-end

- boot powered
- sass

### API server

- [A Stuart Sierra component reloaded project](https://github.com/stuartsierra/component)
- Use of `schema.core/defrecord` to validate your system's integrity on every reset
- Run all your tests with `(run-all-tests)`
- bidi & yada for serving web resources and APIs

## Getting started

### Front-end

Edit the sass files in sass

```
boot dev
```

## Infrastructure

```
lein repl
(dev)
(go)
(reset-and-test)
```

## Libraries


## A recommended pattern for constructing systems

In order to enjoy the most flexibility from SScomp, we offer the following rules

1. Declare dependencies (using `component/using`) in the components
   themselves - it's the component that knows it needs something,
   that's the perfect place to declare that need!

1. Use simple (non-namespaced) keywords to denote dependencies in components

1. Declare the components in system.clj using namespaced keys

This scheme will allow your system to expand. It allows you to use
components multiple times in the same system, rather than be tied to a
singleton model. It ensures that when you do this, you won't be bogged
down in keyword clashes.

## A boot-driven ClojureScript dev and prod environment

```
boot dev
```

Browse to localhost:3001

## Copyright & License

The MIT License (MIT)

Copyright © 2016 JUXT LTD.

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
