# Edge

From "Datum edge". Also represents the 'edge' of our current thinking
on the architecture of Clojure projects.

Edge is an example system, unlikely to be useful on its own.

Edge is a simple Datomic project demonstrating the following:

- [A Stuart Sierra component reloaded project](https://github.com/stuartsierra/component)
- A recommended pattern for constructing systems
- Use of `schema.core/defrecord` to validate your system's integrity on every reset

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
