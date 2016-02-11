# After [![Build Status][1]][2]

Invoke callback after n calls

## Status: production ready

## Example

    var after = require("after")
        , next = after(3, logItWorks)

    next()
    next()
    next() // it works

    function logItWorks() {
        console.log("it works!")
    }

## Example with error handling

    var after = require("after")
        , next = after(3, logError)

    next()
    next(new Error("oops")) // logs oops
    next() // does nothing

    function logError(err) {
        console.log(err)
    }

## After < 0.6.0

Older versions of after had iterators and flows in them.

These have been replaced with seperate modules

 - [iterators][8]
 - [composite][9]

## Installation

`npm install after`

## Tests

`npm test`

## Blog post

 - [Flow control in node.js][3]

## Examples :

 - [Determining the end of asynchronous operations][4]
 - [In javascript what are best practices for executing multiple asynchronous functions][5]
 - [JavaScript performance long running tasks][6]
 - [Synchronous database queries with node.js][7]

## Contributors

 - Raynos

## MIT Licenced

  [1]: https://secure.travis-ci.org/Raynos/after.png
  [2]: http://travis-ci.org/Raynos/after
  [3]: http://raynos.org/blog/2/Flow-control-in-node.js
  [4]: http://stackoverflow.com/questions/6852059/determining-the-end-of-asynchronous-operations-javascript/6852307#6852307
  [5]: http://stackoverflow.com/questions/6869872/in-javascript-what-are-best-practices-for-executing-multiple-asynchronous-functi/6870031#6870031
  [6]: http://stackoverflow.com/questions/6864397/javascript-performance-long-running-tasks/6889419#6889419
  [7]: http://stackoverflow.com/questions/6597493/synchronous-database-queries-with-node-js/6620091#6620091
  [8]: http://github.com/Raynos/iterators
  [9]: http://github.com/Raynos/composite
