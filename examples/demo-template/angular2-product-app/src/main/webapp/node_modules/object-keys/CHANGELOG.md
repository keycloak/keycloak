1.0.0 / 2014-08-26
=================
  * v1.0.0

0.6.1 / 2014-08-25
=================
  * v0.6.1
  * Updating dependencies (tape, covert, is)
  * Update badges in readme
  * Use separate var statements

0.6.0 / 2014-04-23
=================
  * v0.6.0
  * Updating dependencies (tape, covert)
  * Make sure boxed primitives, and arguments objects, work properly in ES3 browsers
  * Improve test matrix: test all node versions, but only latest two stables are a failure
  * Remove internal foreach shim.

0.5.1 / 2014-03-09
=================
  * 0.5.1
  * Updating dependencies (tape, covert, is)
  * Removing forEach from the module (but keeping it in tests)

0.5.0 / 2014-01-30
=================
  * 0.5.0
  * Explicitly returning the shim, instead of returning native Object.keys when present
  * Adding a changelog.
  * Cleaning up IIFE wrapping
  * Testing on node 0.4 through 0.11

0.4.0 / 2013-08-14
==================

  * v0.4.0
  * In Chrome 4-10 and Safari 4, typeof (new RegExp) === 'function'
  * If it's a string, make sure to use charAt instead of brackets.
  * Only use Function#call if necessary.
  * Making sure the context tests actually run.
  * Better function detection
  * Adding the android browser
  * Fixing testling files
  * Updating tape
  * Removing the "is" dependency.
  * Making an isArguments shim.
  * Adding a local forEach shim and tests.
  * Updating paths.
  * Moving the shim test.
  * v0.3.0

0.3.0 / 2013-05-18
==================

  * README tweak.
  * Fixing constructor enum issue. Fixes [#5](https://github.com/ljharb/object-keys/issues/5).
  * Adding a test for [#5](https://github.com/ljharb/object-keys/issues/5)
  * Updating readme.
  * Updating dependencies.
  * Giving credit to lodash.
  * Make sure that a prototype's constructor property is not enumerable. Fixes [#3](https://github.com/ljharb/object-keys/issues/3).
  * Adding additional tests to handle arguments objects, and to skip "prototype" in functions. Fixes [#2](https://github.com/ljharb/object-keys/issues/2).
  * Fixing a typo on this test for [#3](https://github.com/ljharb/object-keys/issues/3).
  * Adding node 0.10 to travis.
  * Adding an IE < 9 test per [#3](https://github.com/ljharb/object-keys/issues/3)
  * Adding an iOS 5 mobile Safari test per [#2](https://github.com/ljharb/object-keys/issues/2)
  * Moving "indexof" and "is" to be dev dependencies.
  * Making sure the shim works with functions.
  * Flattening the tests.

0.2.0 / 2013-05-10
==================

  * v0.2.0
  * Object.keys should work with arrays.

0.1.8 / 2013-05-10
==================

  * v0.1.8
  * Upgrading dependencies.
  * Using a simpler check.
  * Fixing a bug in hasDontEnumBug browsers.
  * Using the newest tape!
  * Fixing this error test.
  * "undefined" is probably a reserved word in ES3.
  * Better test message.

0.1.7 / 2013-04-17
==================

  * Upgrading "is" once more.
  * The key "null" is breaking some browsers.

0.1.6 / 2013-04-17
==================

  * v0.1.6
  * Upgrading "is"

0.1.5 / 2013-04-14
==================

  * Bumping version.
  * Adding more testling browsers.
  * Updating "is"

0.1.4 / 2013-04-08
==================

  * Using "is" instead of "is-extended".

0.1.3 / 2013-04-07
==================

  * Using "foreach" instead of my own shim.
  * Removing "tap"; I'll just wait for "tape" to fix its node 0.10 bug.

0.1.2 / 2013-04-03
==================

  * Adding dependency status; moving links to an index at the bottom.
  * Upgrading is-extended; version 0.1.2
  * Adding an npm version badge.

0.1.1 / 2013-04-01
==================

  * Adding Travis CI.
  * Bumping the version.
  * Adding indexOf since IE sucks.
  * Adding a forEach shim since older browsers don't have Array#forEach.
  * Upgrading tape - 0.3.2 uses Array#map
  * Using explicit end instead of plan.
  * Can't test with Array.isArray in older browsers.
  * Using is-extended.
  * Fixing testling files.
  * JSHint/JSLint-ing.
  * Removing an unused object.
  * Using strict mode.

0.1.0 / 2013-03-30
==================

  * Changing the exports should have meant a higher version bump.
  * Oops, fixing the repo URL.
  * Adding more tests.
  * 0.0.2
  * Merge branch 'export_one_thing'; closes [#1](https://github.com/ljharb/object-keys/issues/1)
  * Move shim export to a separate file.
