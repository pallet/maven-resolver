# maven-resolver

Resolve maven artifacts non-transitively, for use in deploys, etc.  If the s3 or
s3p wagons are on the classpath, they will be registered automatically with
pomegranate.

Note that this library will hopefully be obsoleted by
https://github.com/cemerick/pomegranate/pull/57 or similar.

## Usage

Provides a top level
[`resolve-coordinates`](http://palletops.com/maven-resolver/0.1/api/com.palletops.maven-resolver.html#var-resolve-coordinates)
function.

Add the following to your `:dependencies`:

```clj
[com.palletops/maven-resolver "0.1.0"]
```

## License

Copyright © 2013 Hugo Duncan

Distributed under the Eclipse Public License.
