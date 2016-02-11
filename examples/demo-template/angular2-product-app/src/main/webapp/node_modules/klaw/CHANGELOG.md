1.1.3 / 2015-12-23
------------------
- bugfix: if `readdir` error, got hung up. See: https://github.com/jprichardson/node-klaw/issues/1

1.1.2 / 2015-11-12
------------------
- assert that param `dir` is a `string`

1.1.1 / 2015-10-25
------------------
- bug fix, options not being passed

1.1.0 / 2015-10-25
------------------
- added `queueMethod` and `pathSorter` to `options` to affect searching strategy.

1.0.0 / 2015-10-25
------------------
- removed unused `filter` param
- bugfix: always set `streamOptions` to `objectMode`
- simplified, converted from push mode (streams 1) to proper pull mode (streams 3)

0.1.0 / 2015-10-25
------------------
- initial release
