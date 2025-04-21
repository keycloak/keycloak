1.1.4 / 2022-04-14
=================
 * [Refactor] use `has-property-descriptors`
 * [readme] add github actions/codecov badges
 * [Docs] fix header parsing; remove testling
 * [Deps] update `object-keys`
 * [meta] use `prepublishOnly` script for npm 7+
 * [meta] add `funding` field; create FUNDING.yml
 * [actions] add "Allow Edits" workflow; automatic rebasing / merge commit blocking
 * [actions] reuse common workflows
 * [actions] update codecov uploader
 * [actions] use `node/install` instead of `node/run`; use `codecov` action
 * [Tests] migrate tests to Github Actions
 * [Tests] run `nyc` on all tests; use `tape` runner
 * [Tests] use shared travis-ci config
 * [Tests] use `npx aud` instead of `nsp` or `npm audit` with hoops
 * [Tests] remove `jscs`
 * [Dev Deps] update `eslint`, `@ljharb/eslint-config`, `safe-publish-latest`, `tape`; add `aud`, `safe-publish-latest`

1.1.3 / 2018-08-14
=================
 * [Refactor] use a for loop instead of `foreach` to make for smaller bundle sizes
 * [Robustness] cache `Array.prototype.concat` and `Object.defineProperty`
 * [Deps] update `object-keys`
 * [Dev Deps] update `eslint`, `@ljharb/eslint-config`, `nsp`, `tape`, `jscs`; remove unused eccheck script + dep
 * [Tests] use pretest/posttest for linting/security
 * [Tests] fix npm upgrades on older nodes

1.1.2 / 2015-10-14
=================
 * [Docs] Switch from vb.teelaun.ch to versionbadg.es for the npm version badge SVG
 * [Deps] Update `object-keys`
 * [Dev Deps] update `jscs`, `tape`, `eslint`, `@ljharb/eslint-config`, `nsp`
 * [Tests] up to `io.js` `v3.3`, `node` `v4.2`

1.1.1 / 2015-07-21
=================
 * [Deps] Update `object-keys`
 * [Dev Deps] Update `tape`, `eslint`
 * [Tests] Test on `io.js` `v2.4`

1.1.0 / 2015-07-01
=================
 * [New] Add support for symbol-valued properties.
 * [Dev Deps] Update `nsp`, `eslint`
 * [Tests] Test up to `io.js` `v2.3`

1.0.3 / 2015-05-30
=================
 * Using a more reliable check for supported property descriptors.

1.0.2 / 2015-05-23
=================
 * Test up to `io.js` `v2.0`
 * Update `tape`, `jscs`, `nsp`, `eslint`, `object-keys`, `editorconfig-tools`, `covert`

1.0.1 / 2015-01-06
=================
 * Update `object-keys` to fix ES3 support

1.0.0 / 2015-01-04
=================
  * v1.0.0
