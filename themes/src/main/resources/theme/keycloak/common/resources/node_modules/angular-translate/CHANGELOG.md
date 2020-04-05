<a name="2.18.2"></a>
## [2.18.2](https://github.com/angular-translate/angular-translate/compare/2.18.1...2.18.2) (2020-01-04)


### Bug Fixes

* **build:** fix build issue ([a14918d](https://github.com/angular-translate/angular-translate/commit/a14918d))
* Allow line breaks in interpolated expressions. ([70957a3](https://github.com/angular-translate/angular-translate/commit/70957a3)), closes [#1884](https://github.com/angular-translate/angular-translate/issues/1884) [#1824](https://github.com/angular-translate/angular-translate/issues/1824)


### Features

* throw TypeError for translationId ([b363e3b](https://github.com/angular-translate/angular-translate/commit/b363e3b))



<a name="2.18.1"></a>
## [2.18.1](https://github.com/angular-translate/angular-translate/compare/2.18.0...2.18.1) (2018-05-19)



<a name="2.18.0"></a>
# [2.18.0](https://github.com/angular-translate/angular-translate/compare/2.17.1...2.18.0) (2018-05-17)


### Features

* add test scope for AngularJS 1.7 ([7a44ddf](https://github.com/angular-translate/angular-translate/commit/7a44ddf))



<a name="2.17.1"></a>
## [2.17.1](https://github.com/angular-translate/angular-translate/compare/2.17.0...2.17.1) (2018-04-15)


### Bug Fixes

* **release:** fix zip artifacts on GitHub won't have `.` ([adb982f](https://github.com/angular-translate/angular-translate/commit/adb982f)), closes [#1840](https://github.com/angular-translate/angular-translate/issues/1840) [#1835](https://github.com/angular-translate/angular-translate/issues/1835)


### Features

* support google closure compiler ([fe47ae7](https://github.com/angular-translate/angular-translate/commit/fe47ae7))



<a name="2.17.0"></a>
# [2.17.0](https://github.com/angular-translate/angular-translate/compare/2.16.0...2.17.0) (2017-12-21)


### Bug Fixes

* **partial loader:** add check for added/removed part while refreshing ([3520418](https://github.com/angular-translate/angular-translate/commit/3520418)), closes [#1781](https://github.com/angular-translate/angular-translate/issues/1781)


### Features

* **service:** format bcp47 with script and language only correctly ([6c3b63e](https://github.com/angular-translate/angular-translate/commit/6c3b63e))



<a name="2.16.0"></a>
# [2.16.0](https://github.com/angular-translate/angular-translate/compare/2.15.2...2.16.0) (2017-11-01)


### Bug Fixes

* Stop using Angular.js lowercase internal method ([efc91c3](https://github.com/angular-translate/angular-translate/commit/efc91c3)), closes [#1797](https://github.com/angular-translate/angular-translate/issues/1797)
* **service:** fix invalid waiting for `forceLanguage` ([0c1a266](https://github.com/angular-translate/angular-translate/commit/0c1a266)), closes [#1770](https://github.com/angular-translate/angular-translate/issues/1770)
* **service:** ignore case when matching wildcards in available languages map ([7f25843](https://github.com/angular-translate/angular-translate/commit/7f25843))
* **service:** respect case in available languages ([8fb6f5d](https://github.com/angular-translate/angular-translate/commit/8fb6f5d))


### Features

* **directive:** introduce attr translate-sanitize-strategy ([41c7e1f](https://github.com/angular-translate/angular-translate/commit/41c7e1f))
* **loader-partial:** addPart specific urlTemplate override ([633fbc9](https://github.com/angular-translate/angular-translate/commit/633fbc9))
* **service:** add sanitizeStrategy to $translate ([4a2c3ab](https://github.com/angular-translate/angular-translate/commit/4a2c3ab))



<a name="2.15.2"></a>
## [2.15.2](https://github.com/angular-translate/angular-translate/compare/2.15.1...v2.15.2) (2017-06-22)


### Bug Fixes

* Timezone and DST agnostic Unit test ([b3b04bd](https://github.com/angular-translate/angular-translate/commit/b3b04bd))
* **$translateSanitizationProvider:** fix sanitization of boolean values ([70f4843](https://github.com/angular-translate/angular-translate/commit/70f4843)), closes [#1747](https://github.com/angular-translate/angular-translate/issues/1747)
* **service:** fixed IE8 "Expected identifier" error ([a30e37a](https://github.com/angular-translate/angular-translate/commit/a30e37a))



<a name="2.15.1"></a>
## [2.15.1](https://github.com/angular-translate/angular-translate/compare/2.15.0...v2.15.1) (2017-03-04)


### Bug Fixes

* **cloak:** fix missing decloak introduced by optimize [#1694](https://github.com/angular-translate/angular-translate/issues/1694) ([a9ec123](https://github.com/angular-translate/angular-translate/commit/a9ec123)), closes [#1705](https://github.com/angular-translate/angular-translate/issues/1705)



<a name="2.15.0"></a>
# [2.15.0](https://github.com/angular-translate/angular-translate/compare/2.14.0...v2.15.0) (2017-02-27)


### Features

* **cookies:** use $cookies (1.4+) or $cookieStore (<1.4) ([51330f5](https://github.com/angular-translate/angular-translate/commit/51330f5))
* **filter:** ensure no this==undefined will be injected ([5cb94cb](https://github.com/angular-translate/angular-translate/commit/5cb94cb))



<a name="2.14.0"></a>
# [2.14.0](https://github.com/angular-translate/angular-translate/compare/2.13.1...v2.14.0) (2017-02-11)


### Bug Fixes

* **$translate:** reassign language promises in refresh, update translation tables at the appropriate time, and simplify the routine ([351eb8f](https://github.com/angular-translate/angular-translate/commit/351eb8f))
* **$translatePartialLoader:** prevent duplicate simultaneous HTTP requests ([8b2cea8](https://github.com/angular-translate/angular-translate/commit/8b2cea8))
* **service:** add explicit promise rejection handler for $translate.use ([f4dc14a](https://github.com/angular-translate/angular-translate/commit/f4dc14a))
* **service:** avoid sanitize/esape calls on null/undefined param values ([331e0dd](https://github.com/angular-translate/angular-translate/commit/331e0dd))
* **service:** fix missing promise rejection handlers ([776993b](https://github.com/angular-translate/angular-translate/commit/776993b))
* **staticFilesLoader:** do not use empty string as $http params ([ac2a038](https://github.com/angular-translate/angular-translate/commit/ac2a038)), closes [#1646](https://github.com/angular-translate/angular-translate/issues/1646)
* **tests:** rewrite tests for AJS 1.6 compatibility ([7c9d2c9](https://github.com/angular-translate/angular-translate/commit/7c9d2c9))
* **translate:** handle null translation ([1e57b4f](https://github.com/angular-translate/angular-translate/commit/1e57b4f)), closes [#665](https://github.com/angular-translate/angular-translate/issues/665)
* **translateCloak:** incorrect element reference, inappropriate decloak at onReady, inappropriate decloak at $translateChangeSuccess ([a4d2795](https://github.com/angular-translate/angular-translate/commit/a4d2795))


### Features

* **dependencies:** update to messageformat 1.0.2 ([d4a0468](https://github.com/angular-translate/angular-translate/commit/d4a0468))
* **service:** add translationId as param of custom interpolation service interface ([5de40de](https://github.com/angular-translate/angular-translate/commit/5de40de))
* **tests:** add current AngularJS 1.6 in test scopes ([d8abdc5](https://github.com/angular-translate/angular-translate/commit/d8abdc5))



<a name="2.13.1"></a>
## [2.13.1](https://github.com/angular-translate/angular-translate/compare/2.13.0...v2.13.1) (2016-12-06)



<a name="2.13.0"></a>
# [2.13.0](https://github.com/angular-translate/angular-translate/compare/2.12.1...v2.13.0) (2016-10-30)


### Bug Fixes

* **service:** fix .instant() not handling TrustedValueHolderType correctly ([1ede55e](https://github.com/angular-translate/angular-translate/commit/1ede55e)), closes [#1618](https://github.com/angular-translate/angular-translate/issues/1618)
* **service:** reject promise if handler returns undefined ([8fe6f23](https://github.com/angular-translate/angular-translate/commit/8fe6f23)), closes [#1600](https://github.com/angular-translate/angular-translate/issues/1600)
* **service:** return empty string when found in fallback ([d76227e](https://github.com/angular-translate/angular-translate/commit/d76227e))


### Features

* **sanitize:** sanitize override on instant call ([01fecd0](https://github.com/angular-translate/angular-translate/commit/01fecd0))
* **service:** add $translate.getTranslationTable(langKey) ([40f9e35](https://github.com/angular-translate/angular-translate/commit/40f9e35))
* **service:** add file map lookup into static-files loader ([132e49a](https://github.com/angular-translate/angular-translate/commit/132e49a))
* **service:** add mf configurer [#1619](https://github.com/angular-translate/angular-translate/issues/1619) ([676114b](https://github.com/angular-translate/angular-translate/commit/676114b))



<a name="2.12.1"></a>
## [2.12.1](https://github.com/angular-translate/angular-translate/compare/2.12.0...v2.12.1) (2016-09-15)


### Bug Fixes

* **build:** Add missing translate-attr directive to Gruntfile.js ([e70e9ad](https://github.com/angular-translate/angular-translate/commit/e70e9ad)), closes [#1577](https://github.com/angular-translate/angular-translate/issues/1577)
* **style:** fix code style issues in ~-attr directive ([1848bc8](https://github.com/angular-translate/angular-translate/commit/1848bc8))



<a name="2.12.0"></a>
# [2.12.0](https://github.com/angular-translate/angular-translate/compare/2.11.1...v2.12.0) (2016-09-05)


### Bug Fixes

* **service:** fix infinite loop when fallback language async loading fails ([233f30c](https://github.com/angular-translate/angular-translate/commit/233f30c))
* **service:** treat date param as-is (no sanitize/escape) ([ab1ecce](https://github.com/angular-translate/angular-translate/commit/ab1ecce)), closes [#1560](https://github.com/angular-translate/angular-translate/issues/1560)


### Features

* **directive:** introduce standalone translate-attr directive ([bcb0f2c](https://github.com/angular-translate/angular-translate/commit/bcb0f2c))
* **partial loader:** add error response to errorHandler ([e3aba1c](https://github.com/angular-translate/angular-translate/commit/e3aba1c))
* **service:** introduce new sanitize strategies: sce/sceParameters ([1624df5](https://github.com/angular-translate/angular-translate/commit/1624df5))
* **service:** provide for sanitize/escape strategy 3rd argument context ([8504c60](https://github.com/angular-translate/angular-translate/commit/8504c60))



<a name="2.11.1"></a>
## [2.11.1](https://github.com/angular-translate/angular-translate/compare/2.11.0...v2.11.1) (2016-07-17)


### Bug Fixes

* **dependencies:** Update messageformat to ~0.3.1 ([04e11c9](https://github.com/angular-translate/angular-translate/commit/04e11c9))
* **grunt:** add work-around for uglify preserveComments as expected ([32cdedb](https://github.com/angular-translate/angular-translate/commit/32cdedb)), closes [#1461](https://github.com/angular-translate/angular-translate/issues/1461)
* **service:** allow instant function to also take care of post process configuration ([b7d7907](https://github.com/angular-translate/angular-translate/commit/b7d7907))
* **service:** avoid sanitizing of functions ([492d8e5](https://github.com/angular-translate/angular-translate/commit/492d8e5)), closes [#1529](https://github.com/angular-translate/angular-translate/issues/1529)
* **service:** Correct descriptive ngdocs to match parameters on the service calls ([91711f7](https://github.com/angular-translate/angular-translate/commit/91711f7))
* **service:** fix interpolation issue with non-string as input ([fa4a80e](https://github.com/angular-translate/angular-translate/commit/fa4a80e)), closes [#1511](https://github.com/angular-translate/angular-translate/issues/1511)
* **service:** fix lost of data in async loader / error in runtime ([5ee0c3e](https://github.com/angular-translate/angular-translate/commit/5ee0c3e))


### Features

* **directive:** introduce a global keepContent setting ([2015f79](https://github.com/angular-translate/angular-translate/commit/2015f79))



<a name="2.11.0"></a>
# [2.11.0](https://github.com/angular-translate/angular-translate/compare/2.10.0...v2.11.0) (2016-03-20)


### Bug Fixes

* **directive:** reduced number of watchers by applying translateLanguage watcher only when direc ([961fc92](https://github.com/angular-translate/angular-translate/commit/961fc92))
* **service:** add missing hasOwnProperty check ([823afc0](https://github.com/angular-translate/angular-translate/commit/823afc0))
* **service:** avoid try to load languages which are explicitly not wanted ([bde935e](https://github.com/angular-translate/angular-translate/commit/bde935e)), closes [#1390](https://github.com/angular-translate/angular-translate/issues/1390)
* **service:** fix edge-case with .use() and .preferredLanguage() ([02688f2](https://github.com/angular-translate/angular-translate/commit/02688f2))
* **service:** translations for `forceLanguage` will be loaded on demand ([14bc956](https://github.com/angular-translate/angular-translate/commit/14bc956)), closes [#1389](https://github.com/angular-translate/angular-translate/issues/1389)

### Features

* **depenceny:** Update messageformat.js to current 0.3.0 release ([fb48f78](https://github.com/angular-translate/angular-translate/commit/fb48f78))
* **directive:** introduce attr translate-keep-content ([b2cf8a3](https://github.com/angular-translate/angular-translate/commit/b2cf8a3))
* **service:** add `$translate.resolveClientLocale()` (also at provider) ([d0469ac](https://github.com/angular-translate/angular-translate/commit/d0469ac))
* **service:** add support for uniformLanguageTag('iso639-1') ([1e037ec](https://github.com/angular-translate/angular-translate/commit/1e037ec)), closes [#1181](https://github.com/angular-translate/angular-translate/issues/1181)
* **service:** improve messageformat.js output caching ([cb31608](https://github.com/angular-translate/angular-translate/commit/cb31608))
* **service:** introduce getter returning available languages ([3988af0](https://github.com/angular-translate/angular-translate/commit/3988af0)), closes [#1304](https://github.com/angular-translate/angular-translate/issues/1304)
* **service:** introduce post processing for translations ([f0c4874](https://github.com/angular-translate/angular-translate/commit/f0c4874))
* **service:** support for default translation in missingTranslationHandler ([8c5044c](https://github.com/angular-translate/angular-translate/commit/8c5044c))



<a name="2.10.0"></a>
# [2.10.0](https://github.com/angular-translate/angular-translate/compare/2.9.2...v2.10.0) (2016-02-28)


### Bug Fixes

* **service:** make the fallback $uses / $translate.use work in a correct manner ([7e71a5a](https://github.com/angular-translate/angular-translate/commit/7e71a5a))



<a name="2.9.2"></a>
## [2.9.2](https://github.com/angular-translate/angular-translate/compare/2.9.1...v2.9.2) (2016-02-21)


### Bug Fixes

* **package:** redefine dependency version range (AJS 1.5) ([94eb844](https://github.com/angular-translate/angular-translate/commit/94eb844)), closes [#1394](https://github.com/angular-translate/angular-translate/issues/1394) [#1395](https://github.com/angular-translate/angular-translate/issues/1395) [#1397](https://github.com/angular-translate/angular-translate/issues/1397)
* **package:** redefine dependency version range (AJS 1.5) (fixup) ([20da73d](https://github.com/angular-translate/angular-translate/commit/20da73d)), closes [#1394](https://github.com/angular-translate/angular-translate/issues/1394) [#1395](https://github.com/angular-translate/angular-translate/issues/1395) [#1397](https://github.com/angular-translate/angular-translate/issues/1397)
* **service:** avoid call stack size error, print proper message ([73ea6e3](https://github.com/angular-translate/angular-translate/commit/73ea6e3))
* **service:** ensure fallback language can be selected as `$uses` ([40ad523](https://github.com/angular-translate/angular-translate/commit/40ad523))
* **service:** remove invalid argument for promise.finally ([2d72908](https://github.com/angular-translate/angular-translate/commit/2d72908))



<a name="2.9.1"></a>
## [2.9.1](https://github.com/angular-translate/angular-translate/compare/2.9.0...v2.9.1) (2016-02-13)


### Bug Fixes

* **package:** redefine dependency version range (AJS 1.5) ([9ccce6b](https://github.com/angular-translate/angular-translate/commit/9ccce6b)), closes [#1394](https://github.com/angular-translate/angular-translate/issues/1394) [#1395](https://github.com/angular-translate/angular-translate/issues/1395) [#1397](https://github.com/angular-translate/angular-translate/issues/1397)



<a name="2.9.0"></a>
# [2.9.0](https://github.com/angular-translate/angular-translate/compare/2.8.1...v2.9.0) (2016-01-24)


### Bug Fixes

* **$translate:** apply notFoundIndicators only when all configured language checked in $translate ([25b13c4](https://github.com/angular-translate/angular-translate/commit/25b13c4)), closes [#1314](https://github.com/angular-translate/angular-translate/issues/1314)
* **directive:** add additional watcher validating cloak ([e7536b5](https://github.com/angular-translate/angular-translate/commit/e7536b5)), closes [#1287](https://github.com/angular-translate/angular-translate/issues/1287)
* **directive:** enforce update on default text change only ([ea94acd](https://github.com/angular-translate/angular-translate/commit/ea94acd))
* **docs:** correct all occurrences of language names PR #1243 ([5f89d55](https://github.com/angular-translate/angular-translate/commit/5f89d55))
* **docs:** fix broken link ([e641fe4](https://github.com/angular-translate/angular-translate/commit/e641fe4))
* **docs:** Fix some typos in spanish ([830a84b](https://github.com/angular-translate/angular-translate/commit/830a84b))
* **docs:** refresh outdated link ([392cab0](https://github.com/angular-translate/angular-translate/commit/392cab0))
* **package:** add missing run-scriptlet "clean-test-scopes" ([c22c727](https://github.com/angular-translate/angular-translate/commit/c22c727))
* **service:** partial loader service refetches list of parts ([069eafd](https://github.com/angular-translate/angular-translate/commit/069eafd)), closes [#1326](https://github.com/angular-translate/angular-translate/issues/1326)

### Features

* **build:** update test scope "AJS 1.5" using rc0 ([26cdc05](https://github.com/angular-translate/angular-translate/commit/26cdc05))
* **dependencies:** add `angular` as the required dependency ([475a9b6](https://github.com/angular-translate/angular-translate/commit/475a9b6))
* **service:** expose `$translate.negotiateLocale` being public ([9247000](https://github.com/angular-translate/angular-translate/commit/9247000))
* **service:** force language used for translating ([e591462](https://github.com/angular-translate/angular-translate/commit/e591462))



<a name="2.8.1"></a>
## [2.8.1](https://github.com/angular-translate/angular-translate/compare/2.8.0...v2.8.1) (2015-10-01)


### Bug Fixes

* **service:** Fix `$translate.isReady()` won't return true if ready ([b40a344](https://github.com/angular-translate/angular-translate/commit/b40a344)), closes [#1239](https://github.com/angular-translate/angular-translate/issues/1239)
* **service:** should not abort fallback languages (feature #1070) ([cc410b1](https://github.com/angular-translate/angular-translate/commit/cc410b1)), closes [#1070](https://github.com/angular-translate/angular-translate/issues/1070)



<a name="2.8.0"></a>
# [2.8.0](https://github.com/angular-translate/angular-translate/compare/2.7.2...2.8.0) (2015-09-18)


### Bug Fixes

* **build:** ensure MessageFormat will be added correctly when using UMD ([f5e039c](https://github.com/angular-translate/angular-translate/commit/f5e039c))
* **directive:** Fix behavior of translate-cloak timing ([a6adf47](https://github.com/angular-translate/angular-translate/commit/a6adf47)), closes [#929](https://github.com/angular-translate/angular-translate/issues/929) [#1175](https://github.com/angular-translate/angular-translate/issues/1175)
* **directive:** Fix special IE11 issue #925 ([c4b16d3](https://github.com/angular-translate/angular-translate/commit/c4b16d3)), closes [#925](https://github.com/angular-translate/angular-translate/issues/925)
* **docs:** avoid using absolute links in lang chooser #1136 ([2cdc902](https://github.com/angular-translate/angular-translate/commit/2cdc902))
* **docs:** Fix more typos in CONTRIBUTING.md, add some infos about tests ([e88b990](https://github.com/angular-translate/angular-translate/commit/e88b990))
* **docs:** Fix typo in CONTRIBUTING.md ([1c2ac47](https://github.com/angular-translate/angular-translate/commit/1c2ac47))
* **docs:** Fix typo in zh-cn docs ([2a16eb6](https://github.com/angular-translate/angular-translate/commit/2a16eb6))
* **service:** abort the last loader if not finished #1070 ([dd4a8b4](https://github.com/angular-translate/angular-translate/commit/dd4a8b4))
* **service:** update storage before triggering $translateChangeSuccess ([77dd5a2](https://github.com/angular-translate/angular-translate/commit/77dd5a2))
* **service provider:** change/fix return of preferredLanguage() ([6014a81](https://github.com/angular-translate/angular-translate/commit/6014a81))

### Features

* **directive:** translate-namespace directive ([45523bb](https://github.com/angular-translate/angular-translate/commit/45523bb))
* **loaders:** addition to e7516dc #1080 (disable legacy $http cbs) ([233a012](https://github.com/angular-translate/angular-translate/commit/233a012))
* **loaders:** remove use of legacy methods on $http promises #1080 ([e7516dc](https://github.com/angular-translate/angular-translate/commit/e7516dc))
* **meta:** enrich copyright header with a leagl person ([21da61c](https://github.com/angular-translate/angular-translate/commit/21da61c))
* **sanitize:** Allow sanitize strategy defined as a service ([8a6cc07](https://github.com/angular-translate/angular-translate/commit/8a6cc07))
* **service:** add option to customize the nested delimiter ([78161f8](https://github.com/angular-translate/angular-translate/commit/78161f8))
* **service:** introduce `isReady()` and `onReady()` with event ([9a4bd0d](https://github.com/angular-translate/angular-translate/commit/9a4bd0d))



<a name="2.7.2"></a>
## [2.7.2](https://github.com/angular-translate/angular-translate/compare/2.7.1...2.7.2) (2015-06-01)


### Bug Fixes

* **directive:** ensure value of `translate` will be translated always ([454d702](https://github.com/angular-translate/angular-translate/commit/454d702))
* **sanitization:** fix/workaround issue when jQuery is not available ([ef1b10a](https://github.com/angular-translate/angular-translate/commit/ef1b10a))
* **service:** fix silence on error, add missing catch on `refresh()` ([f3ec956](https://github.com/angular-translate/angular-translate/commit/f3ec956))
* **service:** fix silence on error, add missing catch on `refresh()` ([5a85a64](https://github.com/angular-translate/angular-translate/commit/5a85a64))
* **service:** make provider's storageKey chainable ([de8c253](https://github.com/angular-translate/angular-translate/commit/de8c253))



<a name="2.7.1"></a>
## [2.7.1](https://github.com/angular-translate/angular-translate/compare/2.7.0...2.7.1) (2015-06-01)


### Bug Fixes

* **docs:** fix typo in $translateChangeSuccess ([89e2569](https://github.com/angular-translate/angular-translate/commit/89e2569))
* **service:** handle error "this.replace is not a function" ([8616dca](https://github.com/angular-translate/angular-translate/commit/8616dca))
* **service:** integrate translationCache into service distribution file ([2fcbc60](https://github.com/angular-translate/angular-translate/commit/2fcbc60))

### Features

* **$translateProvider:** add a new option to force async reload ([bdee77f](https://github.com/angular-translate/angular-translate/commit/bdee77f))



<a name="2.7.0"></a>
# [2.7.0](https://github.com/angular-translate/angular-translate/compare/2.6.1...2.7.0) (2015-05-02)


### Bug Fixes

* **directive:** fix issue with `data-` prefixed attributes #954 ([ee253bc](https://github.com/angular-translate/angular-translate/commit/ee253bc)), closes [#954](https://github.com/angular-translate/angular-translate/issues/954)
* **directive:** fix translate-value-* weren't be available on init ([98e8279](https://github.com/angular-translate/angular-translate/commit/98e8279))
* **directive:** fix wrong initial translation causing overloading ([657ed8a](https://github.com/angular-translate/angular-translate/commit/657ed8a))
* **directive:** handle interpolation of undefined keys correctly in updateTranslations, fixes is ([3f7cf4c](https://github.com/angular-translate/angular-translate/commit/3f7cf4c)), closes [#971](https://github.com/angular-translate/angular-translate/issues/971)
* **directive:** Make interpolate message format work smoothly also on message format > 0.1.7 - f ([2533f2d](https://github.com/angular-translate/angular-translate/commit/2533f2d)), closes [#789](https://github.com/angular-translate/angular-translate/issues/789)
* **directive:** make translate-values interpolate correctly with newer MessageFormat.js ([887dc1b](https://github.com/angular-translate/angular-translate/commit/887dc1b))
* **docs:** bug in "Flash of untranslated content" section ([af5d746](https://github.com/angular-translate/angular-translate/commit/af5d746))
* **docs:** fix invalid link in directive ([985cfd5](https://github.com/angular-translate/angular-translate/commit/985cfd5))
* **docs:** typo in module type ([f0527b1](https://github.com/angular-translate/angular-translate/commit/f0527b1))
* **feat:** export module name improving usage module loaders #944 ([cb33f63](https://github.com/angular-translate/angular-translate/commit/cb33f63))
* **messageformat:** add duck type check for numbers #789 ([bbc1cbe](https://github.com/angular-translate/angular-translate/commit/bbc1cbe))
* **refresh:** it has to clear all tables if no language key is specified ([3cce795](https://github.com/angular-translate/angular-translate/commit/3cce795))
* **service:** always remove stored ref for lang promises ([dbd5be9](https://github.com/angular-translate/angular-translate/commit/dbd5be9)), closes [#824](https://github.com/angular-translate/angular-translate/issues/824) [#969](https://github.com/angular-translate/angular-translate/issues/969)
* **service:** do not try to load a predefined fallback language ([3be14df](https://github.com/angular-translate/angular-translate/commit/3be14df))
* **service:** fix an issue resolving after missing translations ([a13899f](https://github.com/angular-translate/angular-translate/commit/a13899f))
* **service:** fix possible npe ([1aaab98](https://github.com/angular-translate/angular-translate/commit/1aaab98))
* **test/refresh:** fix current table refreshing test ([a298ed8](https://github.com/angular-translate/angular-translate/commit/a298ed8))

### Features

* **$translatePartialLoader:** accept function in urlTemplate ([401204a](https://github.com/angular-translate/angular-translate/commit/401204a))
* **build:** introduce module definition ([00b73ff](https://github.com/angular-translate/angular-translate/commit/00b73ff))
* **filter:** add new option `$translate.statefulFilter()` ([dec4bf3](https://github.com/angular-translate/angular-translate/commit/dec4bf3))
* **missingTranslationHandlerFactory:** pass interpolationParams to missingTranslationHandlerFactory ([a361fd0](https://github.com/angular-translate/angular-translate/commit/a361fd0))
* **sanitization:** refactored, fixed and extended sanitization #993 ([12dbc57](https://github.com/angular-translate/angular-translate/commit/12dbc57)), closes [#993](https://github.com/angular-translate/angular-translate/issues/993)
* **service:** add uniformLanguageTagResolver ([b534e1a](https://github.com/angular-translate/angular-translate/commit/b534e1a))

### Performance Improvements

* **directive:** watch parameters only if exist ([f0e2585](https://github.com/angular-translate/angular-translate/commit/f0e2585))


### BREAKING CHANGES

* You will get a warning message when using the default setting (not escaping the content).
You can fix (and remove) this warning by explicit set a sanitization strategy
within your config phase configuring $translateProvider. Even configuring the `null` mode will let the
warning disapper. You are highly encouraged specifing any mode except `null` because of security concerns.


<a name="2.6.1"></a>
## [2.6.1](https://github.com/angular-translate/angular-translate/compare/2.6.0...2.6.1) (2015-03-01)


### Bug Fixes

* **bower spec:** fix bower main property #922 ([3a1ad10](https://github.com/angular-translate/angular-translate/commit/3a1ad10)), closes [#922](https://github.com/angular-translate/angular-translate/issues/922)
* **custom interpolator:** improve handling of interpolator ids which don't exist ([373b46f](https://github.com/angular-translate/angular-translate/commit/373b46f))
* **static-files-loader:** fix multiple files definition (docu update) #923, pr #936 ([e637c01](https://github.com/angular-translate/angular-translate/commit/e637c01)), closes [#923](https://github.com/angular-translate/angular-translate/issues/923) [#936](https://github.com/angular-translate/angular-translate/issues/936)
* **static-files-loader:** fix multiple files definition #923 ([1b6256a](https://github.com/angular-translate/angular-translate/commit/1b6256a)), closes [#923](https://github.com/angular-translate/angular-translate/issues/923)



<a name="2.6.0"></a>
# [2.6.0](https://github.com/angular-translate/angular-translate/compare/2.5.2...2.6.0) (2015-02-08)


### Bug Fixes

* **directive:** ensure internal watcher will be removed ([e69f4a1](https://github.com/angular-translate/angular-translate/commit/e69f4a1))
* **directive:** fix minor memory leak ([5e4533a](https://github.com/angular-translate/angular-translate/commit/5e4533a))
* **directive:** fix missing update using dynamic translationIds ([faebe19](https://github.com/angular-translate/angular-translate/commit/faebe19)), closes [#854](https://github.com/angular-translate/angular-translate/issues/854)
* **directive:** newlines before/after translation ids should be ignored ([8dcf3e2](https://github.com/angular-translate/angular-translate/commit/8dcf3e2)), closes [#909](https://github.com/angular-translate/angular-translate/issues/909)
* **directive, service:** return value of translate-default also in case fallback languages are used - rel ([fcd6b3e](https://github.com/angular-translate/angular-translate/commit/fcd6b3e))
* **filter:** apply notFoundIndicators also for instant translations correctly ([5a9f436](https://github.com/angular-translate/angular-translate/commit/5a9f436)), closes [#866](https://github.com/angular-translate/angular-translate/issues/866)
* **service:** fallback languages follow shortcuts (fixes #758) ([cce897a](https://github.com/angular-translate/angular-translate/commit/cce897a)), closes [#758](https://github.com/angular-translate/angular-translate/issues/758)
* **service:** fix an issue with default interpolator and expressions ([75b7381](https://github.com/angular-translate/angular-translate/commit/75b7381))
* **service:** use $window/$windowProvider instead of window ([bfa7b7b](https://github.com/angular-translate/angular-translate/commit/bfa7b7b))

### Features

* **$translatePartialLoader:** adds optional priority param to the addPart function ([570617c](https://github.com/angular-translate/angular-translate/commit/570617c))
* **directive:** add $translateProvider.directityPriority ([b0b7716](https://github.com/angular-translate/angular-translate/commit/b0b7716))
* **loader:** support for multiple static translation files ([c462ee6](https://github.com/angular-translate/angular-translate/commit/c462ee6))
* **service:** extend loader api: add isPartLoaded and getRegisteredParts to $translatePartialL ([54f8ab3](https://github.com/angular-translate/angular-translate/commit/54f8ab3))



<a name="2.5.2"></a>
## [2.5.2](https://github.com/angular-translate/angular-translate/compare/2.5.0...2.5.2) (2014-12-10)


### Bug Fixes

* **directive:** missing watch for expression within elements text nodes ([31c0356](https://github.com/angular-translate/angular-translate/commit/31c0356)), closes [#701](https://github.com/angular-translate/angular-translate/issues/701)



<a name="2.5.0"></a>
# [2.5.0](https://github.com/angular-translate/angular-translate/compare/2.4.2...2.5.0) (2014-12-07)


### Bug Fixes

* **directive:** ensure directive's text will be parsed at least once ([49cfef0](https://github.com/angular-translate/angular-translate/commit/49cfef0))
* **loader:** under circum understances translation table got lost ([df37381](https://github.com/angular-translate/angular-translate/commit/df37381))
* **messageformat-interpolation:** fix support for messageformat 0.2.* ([ac8d5ed](https://github.com/angular-translate/angular-translate/commit/ac8d5ed))
* **service:** apply fix for empty strings in `navigator.language` ([5b4edd9](https://github.com/angular-translate/angular-translate/commit/5b4edd9))
* **service:** fix npe when resolving fallback language for `instant` ([7c09d89](https://github.com/angular-translate/angular-translate/commit/7c09d89))

### Features

* **$translateUrlLoader:** allow to use custom query parameter name for url loader ([e360bf8](https://github.com/angular-translate/angular-translate/commit/e360bf8))
* **module:** use same fallback for module.run when no storage key is set ([247253d](https://github.com/angular-translate/angular-translate/commit/247253d)), closes [#739](https://github.com/angular-translate/angular-translate/issues/739)
* **storage:** rename set() into put() ([ef6a613](https://github.com/angular-translate/angular-translate/commit/ef6a613))


### BREAKING CHANGES

* This marks storage.set() as deprecated. In the
next major release v3, the old method `set()` will be dropped in favor
of `put()`.
Relates #772


<a name="2.4.2"></a>
## [2.4.2](https://github.com/angular-translate/angular-translate/compare/2.4.1...2.4.2) (2014-10-21)


### Bug Fixes

* **partialloader:** fix possible circular dependency ([25f252c](https://github.com/angular-translate/angular-translate/commit/25f252c)), closes [#766](https://github.com/angular-translate/angular-translate/issues/766)

### Features

* **directive:** translate attributes (optimize process flow) ([508fd32](https://github.com/angular-translate/angular-translate/commit/508fd32))
* **directive:** translate attributes using directive ([1d06d2a](https://github.com/angular-translate/angular-translate/commit/1d06d2a)), closes [#568](https://github.com/angular-translate/angular-translate/issues/568)
* **directive:** translate-cloak supports optional value for cloaking ([f7ccb7f](https://github.com/angular-translate/angular-translate/commit/f7ccb7f))



<a name="2.4.1"></a>
## [2.4.1](https://github.com/angular-translate/angular-translate/compare/2.4.0...2.4.1) (2014-10-03)


### Bug Fixes

* **service:** add missing final event on new (async) translations ([22cc8b4](https://github.com/angular-translate/angular-translate/commit/22cc8b4))
* **service:** constructor `useUrlLoader()` missed optional options ([22f5c4b](https://github.com/angular-translate/angular-translate/commit/22f5c4b))
* **service, loaders:** the loader options ($http) have been merged wrong ([0c35a95](https://github.com/angular-translate/angular-translate/commit/0c35a95)), closes [#754](https://github.com/angular-translate/angular-translate/issues/754) [#547](https://github.com/angular-translate/angular-translate/issues/547)



<a name="2.4.0"></a>
# [2.4.0](https://github.com/angular-translate/angular-translate/compare/2.3.0...2.4.0) (2014-09-22)


### Bug Fixes

* **filter:** interpolated params w/ scope aren't possible starting AJS1.3 ([9465318](https://github.com/angular-translate/angular-translate/commit/9465318))
* **filter:** mark filter being stateful required since Angular 1.3 rc2 ([bffbf04](https://github.com/angular-translate/angular-translate/commit/bffbf04))
* **service:** `$nextLang` should be not unset parallel loadings ([d1745e4](https://github.com/angular-translate/angular-translate/commit/d1745e4)), closes [#647](https://github.com/angular-translate/angular-translate/issues/647)
* **service:** avoid possible doubled requested on refresh() ([98d429d](https://github.com/angular-translate/angular-translate/commit/98d429d))
* **service:** avoid possible npe in internal getTranslationTable() ([9aaa9a0](https://github.com/angular-translate/angular-translate/commit/9aaa9a0))
* **service:** correctly iterate in fallback languages (fixes #690) ([ac2f35c](https://github.com/angular-translate/angular-translate/commit/ac2f35c)), closes [#690](https://github.com/angular-translate/angular-translate/issues/690)

### Features

* **loader:** apply support for loaderOptions.$http ([8613bef](https://github.com/angular-translate/angular-translate/commit/8613bef))
* **loaders:** introduce loader cache ([b685601](https://github.com/angular-translate/angular-translate/commit/b685601)), closes [#529](https://github.com/angular-translate/angular-translate/issues/529)
* **service:** enrich events with the currently handled language key ([73b289d](https://github.com/angular-translate/angular-translate/commit/73b289d))
* **service:** interpolate translationId in case of rejected translation ([3efaac5](https://github.com/angular-translate/angular-translate/commit/3efaac5)), closes [#730](https://github.com/angular-translate/angular-translate/issues/730)
* **service:** introduce `versionInfo` function ([e37d89c](https://github.com/angular-translate/angular-translate/commit/e37d89c))
* **service:** prefer detecting language by `navigator.languages` #722 ([2204f4f](https://github.com/angular-translate/angular-translate/commit/2204f4f))


### BREAKING CHANGES

* Since filters are stateless and have no access to its scope anymore (see https://github.com/angular/angular.js/commit/8863b9d04c722b278fa93c5d66ad1e578ad6eb1f), a context must be given explicitly. This removes the feature of an interpolation based on the scope (context), even without the $rootScope.
However, the feature will still work in AJS <=1.2, so we won't remove it completely yet. Handle the feature as slightly deprecated.


<a name="2.3.0"></a>
# [2.3.0](https://github.com/angular-translate/angular-translate/compare/2.2.0...2.3.0) (2014-09-16)


### Bug Fixes

* **$translate:** return $missingTranslationHandler result when no translation was found ([7625951](https://github.com/angular-translate/angular-translate/commit/7625951))
* **bower.json:** Avoid 'invalid-meta angular-bootstrap-affix is missing "ignore" entry in bower.j ([595501a](https://github.com/angular-translate/angular-translate/commit/595501a)), closes [bower/bower#1388](https://github.com/bower/bower/issues/1388)
* **demo:** fixes wrong method call in demo ([47fc943](https://github.com/angular-translate/angular-translate/commit/47fc943))
* **directive:** change event for listening to `$translateChangeEnd` ([98fe649](https://github.com/angular-translate/angular-translate/commit/98fe649)), closes [#658](https://github.com/angular-translate/angular-translate/issues/658)
* **directive:** improve the cloak-directive's performance ([acab18a](https://github.com/angular-translate/angular-translate/commit/acab18a))
* **docs:** fix example in directive ngdoc-documentation (fixes #678) ([176b3e9](https://github.com/angular-translate/angular-translate/commit/176b3e9)), closes [#678](https://github.com/angular-translate/angular-translate/issues/678)
* **docs:** Fix typo ([6c2ab30](https://github.com/angular-translate/angular-translate/commit/6c2ab30))
* **package.json:** remove unnecessary relative paths from package.json ([8e5b87e](https://github.com/angular-translate/angular-translate/commit/8e5b87e))
* **service:** add shim for indexOf and trim #638 ([b951fd5](https://github.com/angular-translate/angular-translate/commit/b951fd5))
* **service:** addition of preferred language to fallback language stack is now preventing dupl ([b2bb166](https://github.com/angular-translate/angular-translate/commit/b2bb166))
* **service:** load fallback languages also for instant and filter ([ed6023a](https://github.com/angular-translate/angular-translate/commit/ed6023a))
* **service:** use hasOwnProperty of prototype #638 ([d8a5060](https://github.com/angular-translate/angular-translate/commit/d8a5060))
* **storage:** fix 'DOM Exception 18' at feature detection ([75504cb](https://github.com/angular-translate/angular-translate/commit/75504cb)), closes [#629](https://github.com/angular-translate/angular-translate/issues/629)
* **storage:** fixup 75504cbe ([53a8bad](https://github.com/angular-translate/angular-translate/commit/53a8bad))
* **translateService:** fixup/rewrite for b48f6bb (specs) ([45ac14d](https://github.com/angular-translate/angular-translate/commit/45ac14d))
* **translateService:** prevent multiple XHR calls ([b48f6bb](https://github.com/angular-translate/angular-translate/commit/b48f6bb))

### Features

* **directive:** add possibility to mix translation interpolation with other text in element body ([be62131](https://github.com/angular-translate/angular-translate/commit/be62131)), closes [#461](https://github.com/angular-translate/angular-translate/issues/461)



<a name="2.2.0"></a>
# [2.2.0](https://github.com/angular-translate/angular-translate/compare/2.1.0...2.2.0) (2014-06-03)


### Bug Fixes

* **$translate:** checks modification ([b91e4de](https://github.com/angular-translate/angular-translate/commit/b91e4de))
* **$translate:** if translation exists, use the translated string even if it's empty ([4ba736f](https://github.com/angular-translate/angular-translate/commit/4ba736f))
* **$translate:** if translation exists, use the translated string even if it's empty ([eeb8c2a](https://github.com/angular-translate/angular-translate/commit/eeb8c2a))
* **$translate:** use case-insensitive check for language key aliases ([09a8bf1](https://github.com/angular-translate/angular-translate/commit/09a8bf1)), closes [#431](https://github.com/angular-translate/angular-translate/issues/431)
* **$translate:** use case-insensitive check for language key aliases ([26ec308](https://github.com/angular-translate/angular-translate/commit/26ec308)), closes [#431](https://github.com/angular-translate/angular-translate/issues/431)
* **$translateProvider:** determinePreferredLanguage was not chainable ([7c29f2f](https://github.com/angular-translate/angular-translate/commit/7c29f2f)), closes [#487](https://github.com/angular-translate/angular-translate/issues/487)
* **$translateProvider:** fix comparison in one case of negotiateLocale() ([c2b94ca](https://github.com/angular-translate/angular-translate/commit/c2b94ca))
* **$translateProvider:** fix comparison in one case of negotiateLocale() ([fe04c72](https://github.com/angular-translate/angular-translate/commit/fe04c72))
* **demo:** correct demo of `translate-values` ([efa74fa](https://github.com/angular-translate/angular-translate/commit/efa74fa))
* **demo:** correct demo of `translate-values` ([7de2ae2](https://github.com/angular-translate/angular-translate/commit/7de2ae2))
* **demo:** use `.instant()` ([6bea192](https://github.com/angular-translate/angular-translate/commit/6bea192))
* **directive:** Make translate-value-* work inside ng-if and ng-repeat ([e07eea7](https://github.com/angular-translate/angular-translate/commit/e07eea7)), closes [#433](https://github.com/angular-translate/angular-translate/issues/433)
* **directive:** Make translate-value-* work inside ng-if and ng-repeat ([f22624b](https://github.com/angular-translate/angular-translate/commit/f22624b)), closes [#433](https://github.com/angular-translate/angular-translate/issues/433)
* **docs:** removes explicit protocol declaration for assets ([eaa9bf7](https://github.com/angular-translate/angular-translate/commit/eaa9bf7)), closes [#513](https://github.com/angular-translate/angular-translate/issues/513)
* **gruntfile:** fix image link ([65fc8be](https://github.com/angular-translate/angular-translate/commit/65fc8be))
* **package.json:** fix repository url ([40af7ce](https://github.com/angular-translate/angular-translate/commit/40af7ce))
* **package.json:** fix repository url ([a410c9a](https://github.com/angular-translate/angular-translate/commit/a410c9a))
* **partialLoader:** fixes deprecated usage of arguments.callee ([1ac3a0a](https://github.com/angular-translate/angular-translate/commit/1ac3a0a))
* **service:** docs annotation ([8ef0415](https://github.com/angular-translate/angular-translate/commit/8ef0415))
* **service:** docs annotation ([839c4e8](https://github.com/angular-translate/angular-translate/commit/839c4e8))
* **service:** use the aliased language key if available ([675e9a2](https://github.com/angular-translate/angular-translate/commit/675e9a2)), closes [#530](https://github.com/angular-translate/angular-translate/issues/530)
* **storageLocal:** fixes QUOTAEXCEEDEDERROR (safari private browsing) ([59aa2a0](https://github.com/angular-translate/angular-translate/commit/59aa2a0))
* fix npe on empty strings (trim()) ([c69de7b](https://github.com/angular-translate/angular-translate/commit/c69de7b))
* **translateInterpolator:** make it work with 1.3-beta ([97e2241](https://github.com/angular-translate/angular-translate/commit/97e2241))

### Features

* **directive:** add option to define a default translation text ([a802665](https://github.com/angular-translate/angular-translate/commit/a802665))
* **directive:** add option to define a default translation text ([fc57d26](https://github.com/angular-translate/angular-translate/commit/fc57d26))
* **directive:** Support for camel casing interpolation variables. ([b345041](https://github.com/angular-translate/angular-translate/commit/b345041))
* **directive:** Support for camel casing interpolation variables. ([4791e25](https://github.com/angular-translate/angular-translate/commit/4791e25))
* **messageformat-support:** enhancing for sanitization like default ([ad01686](https://github.com/angular-translate/angular-translate/commit/ad01686))
* **missingFallbackDefaultText:** enables a feature to return a default text for displaying in case of missing tra ([f24b15e](https://github.com/angular-translate/angular-translate/commit/f24b15e))
* **service:** add possibility to translate a set of translation ids ([612dc27](https://github.com/angular-translate/angular-translate/commit/612dc27))
* **service:** add possibility to translate a set of translation ids ([57bd07c](https://github.com/angular-translate/angular-translate/commit/57bd07c))
* **service:** allow using wildcards in language aliases ([6f0ae3b](https://github.com/angular-translate/angular-translate/commit/6f0ae3b)), closes [#426](https://github.com/angular-translate/angular-translate/issues/426)



<a name="2.0.1"></a>
## [2.0.1](https://github.com/angular-translate/angular-translate/compare/2.0.0...2.0.1) (2014-02-25)


### Bug Fixes

* **$translate:** Ensuring that languages will be set based on the order they are requested, not t ([c909cd2](https://github.com/angular-translate/angular-translate/commit/c909cd2))
* **$translate:** Ensuring that languages will be set based on the order they are requested, not t ([ebd62af](https://github.com/angular-translate/angular-translate/commit/ebd62af))
* **$translate:** Ensuring that languages will be set based on the order they are requested, not t ([32e1851](https://github.com/angular-translate/angular-translate/commit/32e1851))
* **instant:** $translate.instant(id) does not return correct fallback ([eec1d77](https://github.com/angular-translate/angular-translate/commit/eec1d77))
* **instant:** fix possible npe in case of filters with undefineds ([61a9490](https://github.com/angular-translate/angular-translate/commit/61a9490))
* **refresh:** fix bug in refresh if using partial loader ([95c43b4](https://github.com/angular-translate/angular-translate/commit/95c43b4))

### Features

* **instant:** invoke missing handler within `$translate.instant(id)` ([aaf52b5](https://github.com/angular-translate/angular-translate/commit/aaf52b5))



<a name="2.0.0"></a>
# [2.0.0](https://github.com/angular-translate/angular-translate/compare/1.1.1...2.0.0) (2014-02-16)


### Bug Fixes

* ***:** jshint fixes ([1e3f8a6](https://github.com/angular-translate/angular-translate/commit/1e3f8a6))
* **$translate:** check for fallbacklanguage ([321803d](https://github.com/angular-translate/angular-translate/commit/321803d))
* **$translate:** Trim whitespace off translationId ([4939424](https://github.com/angular-translate/angular-translate/commit/4939424))
* **$translatePartialLoader:** fixes docs annotation ([d6ea84b](https://github.com/angular-translate/angular-translate/commit/d6ea84b))
* **demo:** fix server routes + add index page ([eb0a2dc](https://github.com/angular-translate/angular-translate/commit/eb0a2dc))
* **demo:** links to demo resources updated to new locactions ([fddaa49](https://github.com/angular-translate/angular-translate/commit/fddaa49))
* **deps:** add missing resolution ([a98a2f6](https://github.com/angular-translate/angular-translate/commit/a98a2f6))
* **docs:** fixes links for languages ([265490f](https://github.com/angular-translate/angular-translate/commit/265490f))
* **fallbackLanguage:** Fix fallback languages loading and applying ([4c5c47c](https://github.com/angular-translate/angular-translate/commit/4c5c47c))
* **grunt:** includes translate-cloak directive ([84a59d2](https://github.com/angular-translate/angular-translate/commit/84a59d2))
* avoid calls with empty translationId (sub issue of #298) ([08f087b](https://github.com/angular-translate/angular-translate/commit/08f087b))
* fix npe introduced in 4939424a30 (#281) ([173a9bc](https://github.com/angular-translate/angular-translate/commit/173a9bc)), closes [(#281](https://github.com/(/issues/281) [#298](https://github.com/angular-translate/angular-translate/issues/298)
* **guide/ru,uk:** Fix uses->use in multi language ([af59c6a](https://github.com/angular-translate/angular-translate/commit/af59c6a))
* **instant:** remove language-preload if there were used within instant ([9a3eda6](https://github.com/angular-translate/angular-translate/commit/9a3eda6))
* **loader-static-files.js:** Now allows empty string as prefix and postfix. ([051f431](https://github.com/angular-translate/angular-translate/commit/051f431))
* **service:** fallback languages could not load when using `instant()` ([26de486](https://github.com/angular-translate/angular-translate/commit/26de486))
* **translateCloak:** makes jshint happy ([2058fd3](https://github.com/angular-translate/angular-translate/commit/2058fd3))
* **translateDirective:** fixes bad coding convention ([d5db4ad](https://github.com/angular-translate/angular-translate/commit/d5db4ad))

### Features

* **$translateProvider:** adds determinePreferredLanguage() ([7cbfabe](https://github.com/angular-translate/angular-translate/commit/7cbfabe))
* **$translateProvider:** adds registerAvailableLanguagesKeys for negotiation ([6bef6bd](https://github.com/angular-translate/angular-translate/commit/6bef6bd))
* **filter:** filter now use $translate.instant() since promises could not use ([a1b8a17](https://github.com/angular-translate/angular-translate/commit/a1b8a17))
* **service:** add $translate.instant() for instant translations ([3a855eb](https://github.com/angular-translate/angular-translate/commit/3a855eb))
* add an option for post processing compiling ([d5cd943](https://github.com/angular-translate/angular-translate/commit/d5cd943))
* add option to html escape all values ([e042c44](https://github.com/angular-translate/angular-translate/commit/e042c44))
* **translateCloak:** adds translate-cloak directive ([c125c56](https://github.com/angular-translate/angular-translate/commit/c125c56))
* **translateDirective:** teaches directive custom translate-value-* attr ([5c27467](https://github.com/angular-translate/angular-translate/commit/5c27467)), closes [#188](https://github.com/angular-translate/angular-translate/issues/188)



<a name="1.1.1"></a>
## [1.1.1](https://github.com/angular-translate/angular-translate/compare/1.1.0...1.1.1) (2013-11-24)


### Bug Fixes

* fixes encoding ([084f08c](https://github.com/angular-translate/angular-translate/commit/084f08c))
* **docs:** fixes typo ([7e1c4e9](https://github.com/angular-translate/angular-translate/commit/7e1c4e9))
* **docs:** fixes typo in landing page ([0b999ab](https://github.com/angular-translate/angular-translate/commit/0b999ab))
* **grunt:** fixes missing storage-key ([635d290](https://github.com/angular-translate/angular-translate/commit/635d290))
* **translateDirective:** fixes occuring 'translation id undefined' erros ([bb5a2c4](https://github.com/angular-translate/angular-translate/commit/bb5a2c4))

### Features

* add option to html escape all values ([fe94c1f](https://github.com/angular-translate/angular-translate/commit/fe94c1f))
* shortcuts and links\n\nShortcuts creates a shorter translationId if the last key ([f9f2cf2](https://github.com/angular-translate/angular-translate/commit/f9f2cf2))
* Update required Node up `0.10` ([b7cf5f4](https://github.com/angular-translate/angular-translate/commit/b7cf5f4))



<a name="1.1.0"></a>
# [1.1.0](https://github.com/angular-translate/angular-translate/compare/1.0.2...1.1.0) (2013-09-02)


### Bug Fixes

* **translateDirective:** fixes bug that directive writes into scope ([4e06468](https://github.com/angular-translate/angular-translate/commit/4e06468)), closes [#128](https://github.com/angular-translate/angular-translate/issues/128)
* **translateDirective:** fixes scope handling ([c566586](https://github.com/angular-translate/angular-translate/commit/c566586))
* **translateService:** reset proposed language if there's no pending loader ([6b477fc](https://github.com/angular-translate/angular-translate/commit/6b477fc))

### Features

* **$translatePartialLoader:** Basic implementation ([81222bf](https://github.com/angular-translate/angular-translate/commit/81222bf))
* **invalidate:** added invalidate() method ([d41f91e](https://github.com/angular-translate/angular-translate/commit/d41f91e))
* **translateProvider:** makes methods chainable ([cdc9e9e](https://github.com/angular-translate/angular-translate/commit/cdc9e9e))



<a name="1.0.2"></a>
## [1.0.2](https://github.com/angular-translate/angular-translate/compare/1.0.1...1.0.2) (2013-08-07)


### Bug Fixes

* **fallbackLanguage:** fixes bug that fallbackLanguage is loaded without loader ([6aa3747](https://github.com/angular-translate/angular-translate/commit/6aa3747))
* **translateService:** uses should only load if a loader is registered ([604daec](https://github.com/angular-translate/angular-translate/commit/604daec))
* **typo:** remove unnecessary semicolon ([54cb232](https://github.com/angular-translate/angular-translate/commit/54cb232))



<a name="1.0.1"></a>
## [1.0.1](https://github.com/angular-translate/angular-translate/compare/1.0.0...1.0.1) (2013-07-26)


### Bug Fixes

* **demo:** change src to angular-translate script ([4be93b6](https://github.com/angular-translate/angular-translate/commit/4be93b6))
* **dependency:** add 'angular-cookies' as bower devDependency ([b6f1426](https://github.com/angular-translate/angular-translate/commit/b6f1426))
* **platolink:** deep link ([d368bf3](https://github.com/angular-translate/angular-translate/commit/d368bf3))



<a name="1.0.0"></a>
# [1.0.0](https://github.com/angular-translate/angular-translate/compare/0.9.4...1.0.0) (2013-07-23)


### Bug Fixes

* **docs:** fixes methodOf declaration of addInterpolation method ([f1eeba7](https://github.com/angular-translate/angular-translate/commit/f1eeba7))
* **gh-pages:** plato report ([b85e19b](https://github.com/angular-translate/angular-translate/commit/b85e19b))
* **tests:** travis CI ([c8624bf](https://github.com/angular-translate/angular-translate/commit/c8624bf))
* **tests:** travis CI ([629bb8d](https://github.com/angular-translate/angular-translate/commit/629bb8d))
* fixes gruntfile ([0d500db](https://github.com/angular-translate/angular-translate/commit/0d500db))

### Features

* **messageformat-interpolation:** implements usage of messageformat ([5596e8b](https://github.com/angular-translate/angular-translate/commit/5596e8b))
* **translateDirective:** teaches directives to use custom interpolation ([bf3dbbb](https://github.com/angular-translate/angular-translate/commit/bf3dbbb))
* **translateFilter:** teaches filter to use custom interpolation ([46f03cc](https://github.com/angular-translate/angular-translate/commit/46f03cc))
* **translateService:** adds method to configure indicators for not found translations ([52a039f](https://github.com/angular-translate/angular-translate/commit/52a039f)), closes [#77](https://github.com/angular-translate/angular-translate/issues/77)
* **translateService:** extracts default interpolation in standalone service ([5d8cb56](https://github.com/angular-translate/angular-translate/commit/5d8cb56))
* **translateService:** implements proposedLanguage() ([6d34792](https://github.com/angular-translate/angular-translate/commit/6d34792))
* **translateService:** implements usage of different interpolation services ([5e20e24](https://github.com/angular-translate/angular-translate/commit/5e20e24))
* **translateService:** informs interpolator when locale has changed ([e59b141](https://github.com/angular-translate/angular-translate/commit/e59b141))
* **translateService:** missingTranslationHandler receives language ([6fe6bb1](https://github.com/angular-translate/angular-translate/commit/6fe6bb1))



<a name="0.9.4"></a>
## [0.9.4](https://github.com/angular-translate/angular-translate/compare/0.9.3...0.9.4) (2013-06-21)


### Bug Fixes

* **translateService:** fixes missingTranslationHandler-invokation bug ([525b353](https://github.com/angular-translate/angular-translate/commit/525b353)), closes [#74](https://github.com/angular-translate/angular-translate/issues/74)

### Features

* **translateService:** removes empty options object requirement for loaders ([c09d1db](https://github.com/angular-translate/angular-translate/commit/c09d1db))



<a name="0.9.3"></a>
## [0.9.3](https://github.com/angular-translate/angular-translate/compare/0.9.2...0.9.3) (2013-06-10)


### Features

* **translateService:** let translate service handle multiple promises ([0e5d6d9](https://github.com/angular-translate/angular-translate/commit/0e5d6d9)), closes [#70](https://github.com/angular-translate/angular-translate/issues/70)



<a name="0.9.2"></a>
## [0.9.2](https://github.com/angular-translate/angular-translate/compare/0.9.1...0.9.2) (2013-05-30)


### Bug Fixes

* fix bower.json ([c389882](https://github.com/angular-translate/angular-translate/commit/c389882))

### Features

* **translateProvider:** add fallbackLanguage() method ([018991e](https://github.com/angular-translate/angular-translate/commit/018991e)), closes [#67](https://github.com/angular-translate/angular-translate/issues/67)



<a name="0.9.1"></a>
## [0.9.1](https://github.com/angular-translate/angular-translate/compare/0.9.0...0.9.1) (2013-05-25)


### Bug Fixes

* **translate.js:** Allow blank translation values ([97591a8](https://github.com/angular-translate/angular-translate/commit/97591a8))



<a name="0.9.0"></a>
# [0.9.0](https://github.com/angular-translate/angular-translate/compare/0.8.1...0.9.0) (2013-05-22)


### Features

* **translateProvider:** add use*() methods for async loaders ([f2329cc](https://github.com/angular-translate/angular-translate/commit/f2329cc)), closes [#58](https://github.com/angular-translate/angular-translate/issues/58)



<a name="0.8.1"></a>
## [0.8.1](https://github.com/angular-translate/angular-translate/compare/0.8.0...0.8.1) (2013-05-16)


### Bug Fixes

* **translate.js:** corrected typo ([82569f0](https://github.com/angular-translate/angular-translate/commit/82569f0))

### Features

* **translateProvider:** add methods to use different missingTranslationHandlers ([f6ed3e3](https://github.com/angular-translate/angular-translate/commit/f6ed3e3))


### BREAKING CHANGES

* S:  missingTranslationHandler is no longer supported since its functionality will be replaced with  useMissingTranslationHandlerLog.


<a name="0.8.0"></a>
# [0.8.0](https://github.com/angular-translate/angular-translate/compare/0.7.1...0.8.0) (2013-05-14)




<a name="0.7.1"></a>
## [0.7.1](https://github.com/angular-translate/angular-translate/compare/0.7.0...0.7.1) (2013-05-13)


### Features

* **chore:** rename ngTranslate folder to src ([65012d9](https://github.com/angular-translate/angular-translate/commit/65012d9))



<a name="0.7.0"></a>
# [0.7.0](https://github.com/angular-translate/angular-translate/compare/0.6.0...0.7.0) (2013-05-12)


### Bug Fixes

* **directive:** trim off white space around element.text() ([e10173a](https://github.com/angular-translate/angular-translate/commit/e10173a))
* **tests:** Fix preferredLanguage tests ([73efcfc](https://github.com/angular-translate/angular-translate/commit/73efcfc))
* **tests:** fix tests for preferredLanguage() ([f1b5084](https://github.com/angular-translate/angular-translate/commit/f1b5084))
* **tests:** Old values won't be ignored, so they have to be discarded ([625b1d6](https://github.com/angular-translate/angular-translate/commit/625b1d6))

### Features

* nested objects will be transformed when using `$translateProvider.translations`  ([b15cee4](https://github.com/angular-translate/angular-translate/commit/b15cee4))
* **docs:** add documentation comments ([b1efbca](https://github.com/angular-translate/angular-translate/commit/b1efbca))
* **storageKey:** add a storageKey method ([dabf822](https://github.com/angular-translate/angular-translate/commit/dabf822))
* **translateProvider:** add a preferredLanguage property ([563e9bf](https://github.com/angular-translate/angular-translate/commit/563e9bf))
* **translateProvider:** add storagePrefix() method ([64cd99b](https://github.com/angular-translate/angular-translate/commit/64cd99b))
* **translateProvider:** add useLoaderFactory() as shortcut method ([2915e8b](https://github.com/angular-translate/angular-translate/commit/2915e8b))
* **translateProvider:** make translationTable extendable ([8e3a455](https://github.com/angular-translate/angular-translate/commit/8e3a455)), closes [#33](https://github.com/angular-translate/angular-translate/issues/33)
* **translateProvider:** missingTranslationHandler ([3a5819e](https://github.com/angular-translate/angular-translate/commit/3a5819e))
* **translateService:** add storage() method ([98c2b12](https://github.com/angular-translate/angular-translate/commit/98c2b12))


### BREAKING CHANGES

* The $STORAGE_KEY isn't represent a current storage key
from now. To discover which key is used now you have to call the storageKey
method without params.


<a name="0.6.0"></a>
# [0.6.0](https://github.com/angular-translate/angular-translate/compare/0.5.2...0.6.0) (2013-05-03)


### Features

* **ngmin:** add grunt-ngmin ([f630958](https://github.com/angular-translate/angular-translate/commit/f630958)), closes [#20](https://github.com/angular-translate/angular-translate/issues/20)



<a name="0.5.2"></a>
## [0.5.2](https://github.com/angular-translate/angular-translate/compare/0.5.1...0.5.2) (2013-04-30)


### Bug Fixes

* **translateDirective:** check for truthy value in watch callback ([98087c7](https://github.com/angular-translate/angular-translate/commit/98087c7)), closes [#18](https://github.com/angular-translate/angular-translate/issues/18)



<a name="0.5.1"></a>
## [0.5.1](https://github.com/angular-translate/angular-translate/compare/0.5.0...0.5.1) (2013-04-29)


### Features

* **.bowerrc:** add .bowerrc ([42363ee](https://github.com/angular-translate/angular-translate/commit/42363ee)), closes [#16](https://github.com/angular-translate/angular-translate/issues/16)
* **.jshintrc:** add .jshintrc ([0c8d3da](https://github.com/angular-translate/angular-translate/commit/0c8d3da)), closes [#17](https://github.com/angular-translate/angular-translate/issues/17)
* **bower.json:** rename component.json to bower.json ([17acd10](https://github.com/angular-translate/angular-translate/commit/17acd10))



<a name="0.5.0"></a>
# [0.5.0](https://github.com/angular-translate/angular-translate/compare/0.4.4...0.5.0) (2013-04-25)


### Features

* **conventional-changelogs:** Add grunt-conventional-changelog task ([c8093a7](https://github.com/angular-translate/angular-translate/commit/c8093a7)), closes [#11](https://github.com/angular-translate/angular-translate/issues/11)



<a name="0.4.4"></a>
## [0.4.4](https://github.com/angular-translate/angular-translate/compare/0.4.2...0.4.4) (2013-04-23)




<a name="0.4.2"></a>
## [0.4.2](https://github.com/angular-translate/angular-translate/compare/0.4.0...0.4.2) (2013-04-17)




<a name="0.4.0"></a>
# [0.4.0](https://github.com/angular-translate/angular-translate/compare/0.3.0...0.4.0) (2013-04-07)




<a name="0.3.0"></a>
# [0.3.0](https://github.com/angular-translate/angular-translate/compare/0.2.1...0.3.0) (2013-04-06)




<a name="0.2.1"></a>
## [0.2.1](https://github.com/angular-translate/angular-translate/compare/0.2.0...0.2.1) (2013-04-05)




<a name="0.2.0"></a>
# [0.2.0](https://github.com/angular-translate/angular-translate/compare/0.1.2...0.2.0) (2013-04-03)




<a name="0.1.2"></a>
## [0.1.2](https://github.com/angular-translate/angular-translate/compare/0.1.1...0.1.2) (2013-04-02)




<a name="0.1.1"></a>
## [0.1.1](https://github.com/angular-translate/angular-translate/compare/0.1.0...0.1.1) (2013-04-01)




<a name="0.1.0"></a>
# [0.1.0](https://github.com/angular-translate/angular-translate/compare/0.0.5...0.1.0) (2013-04-01)




<a name="0.0.5"></a>
## [0.0.5](https://github.com/angular-translate/angular-translate/compare/0.0.4...0.0.5) (2013-04-01)




<a name="0.0.4"></a>
## [0.0.4](https://github.com/angular-translate/angular-translate/compare/0.0.2...0.0.4) (2013-04-01)




<a name="0.0.2"></a>
## [0.0.2](https://github.com/angular-translate/angular-translate/compare/0.0.1...0.0.2) (2013-03-30)




<a name="0.0.1"></a>
## 0.0.1 (2013-03-28)




