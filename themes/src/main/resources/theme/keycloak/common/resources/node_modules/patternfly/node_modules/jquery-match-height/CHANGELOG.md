<a name="0.7.2"></a>
## 0.7.2 (2017-02-19)




<a name="0.7.1"></a>
## 0.7.1 (2017-02-19)


* change $.bind to $.on where supported, closes #120 and #145 ([e468ae9](https://github.com/liabru/jquery-match-height/commit/e468ae9)), closes [#120](https://github.com/liabru/jquery-match-height/issues/120) [#145](https://github.com/liabru/jquery-match-height/issues/145)
* deleted redundant min file (moved to dist) ([f530833](https://github.com/liabru/jquery-match-height/commit/f530833))
* fix release tasks ([1462c36](https://github.com/liabru/jquery-match-height/commit/1462c36))
* Merge pull request #115 from robeerob/patch-1 ([ab31d58](https://github.com/liabru/jquery-match-height/commit/ab31d58))
* Update README.md with raw downloadable install url ([dcc9ad6](https://github.com/liabru/jquery-match-height/commit/dcc9ad6))



<a name="0.7.0"></a>
# 0.7.0 (2016-01-04)

### release summary

* added build tasks
* added selenium unit tests
* added lint tests
* added `matchHeight.version` property
* added to npm

* fixed unitless properties
* fixed inline styles being removed
* fixed `display: flex` issue
* fixed `display: inline-flex` issue
* fixed row detection when items contain floating elements
* fixed compatibility for module loaders

### commit log

* add custom version argument to gulp build ([ad8aac5](https://github.com/liabru/jquery-match-height/commit/ad8aac5))
* add delay to jasmine boot ([30824fb](https://github.com/liabru/jquery-match-height/commit/30824fb))
* add lint to all test tasks ([6b16f67](https://github.com/liabru/jquery-match-height/commit/6b16f67))
* add test for _parse on string values with units ([4a64208](https://github.com/liabru/jquery-match-height/commit/4a64208))
* add to npm ([0055660](https://github.com/liabru/jquery-match-height/commit/0055660))
* added a section on tests to readme ([e0be682](https://github.com/liabru/jquery-match-height/commit/e0be682))
* added changelog task ([5263ab1](https://github.com/liabru/jquery-match-height/commit/5263ab1))
* added cloud selenium, local emulated ie testing, lint task, build task, release task, improved tests ([06bd876](https://github.com/liabru/jquery-match-height/commit/06bd876))
* added gulpfile, jasmine test specs, browser test runner, selenium test runner ([ca926de](https://github.com/liabru/jquery-match-height/commit/ca926de))
* added libscore ([03a4317](https://github.com/liabru/jquery-match-height/commit/03a4317))
* added matchHeight.version property ([431e4d0](https://github.com/liabru/jquery-match-height/commit/431e4d0))
* added release tasks ([49cc72f](https://github.com/liabru/jquery-match-height/commit/49cc72f))
* added test for property option ([7bdada7](https://github.com/liabru/jquery-match-height/commit/7bdada7))
* added test for remove ([445799d](https://github.com/liabru/jquery-match-height/commit/445799d))
* added tests for custom toBeWithinTolerance matcher ([a89b1c2](https://github.com/liabru/jquery-match-height/commit/a89b1c2))
* bump jquery package version ([cc9c416](https://github.com/liabru/jquery-match-height/commit/cc9c416))
* change tests to use jquery type checking functions ([6cf52f0](https://github.com/liabru/jquery-match-height/commit/6cf52f0))
* faster selenium testing ([a6b2da3](https://github.com/liabru/jquery-match-height/commit/a6b2da3))
* fix bower instructions in readme ([91e50ad](https://github.com/liabru/jquery-match-height/commit/91e50ad))
* fix for display: inline-flex, closes #68 ([e769b9f](https://github.com/liabru/jquery-match-height/commit/e769b9f)), closes [#68](https://github.com/liabru/jquery-match-height/issues/68)
* fix for unitless properties by forcing px, closes #64 ([d8cc365](https://github.com/liabru/jquery-match-height/commit/d8cc365)), closes [#64](https://github.com/liabru/jquery-match-height/issues/64)
* fix issue maintaining inline styles, closes #95 ([878ff96](https://github.com/liabru/jquery-match-height/commit/878ff96)), closes [#95](https://github.com/liabru/jquery-match-height/issues/95)
* fix issue with 'display:flex', closes #77 ([dc53a49](https://github.com/liabru/jquery-match-height/commit/dc53a49)), closes [#77](https://github.com/liabru/jquery-match-height/issues/77)
* fix issues with build script ([1195421](https://github.com/liabru/jquery-match-height/commit/1195421))
* fix linter issues ([0165e74](https://github.com/liabru/jquery-match-height/commit/0165e74))
* Fix package manager registries URLs ([036df1b](https://github.com/liabru/jquery-match-height/commit/036df1b))
* fixed local test config for non-windows ([d67ca25](https://github.com/liabru/jquery-match-height/commit/d67ca25))
* fixed missing dependencies ([c608b80](https://github.com/liabru/jquery-match-height/commit/c608b80))
* handle error when test server is already running ([9e6487d](https://github.com/liabru/jquery-match-height/commit/9e6487d))
* ignore linebreak style on lint ([1510b58](https://github.com/liabru/jquery-match-height/commit/1510b58))
* Improve row detection when cells contain floating contents ([8844acb](https://github.com/liabru/jquery-match-height/commit/8844acb))
* improved readme ([1cf2c27](https://github.com/liabru/jquery-match-height/commit/1cf2c27))
* improved tasks ([61a9ed4](https://github.com/liabru/jquery-match-height/commit/61a9ed4))
* improved tests ([b1cadb5](https://github.com/liabru/jquery-match-height/commit/b1cadb5))
* Make plugin compatible with module loaders ([b5988c1](https://github.com/liabru/jquery-match-height/commit/b5988c1))
* Merge branch 'feature/tests' into develop ([a7d35dc](https://github.com/liabru/jquery-match-height/commit/a7d35dc))
* Merge branch 'floatingcontents' of https://github.com/jorrit/jquery-match-height into jorrit-floatin ([89b74a7](https://github.com/liabru/jquery-match-height/commit/89b74a7))
* Merge branch 'jorrit-floatingcontents' ([dc9716b](https://github.com/liabru/jquery-match-height/commit/dc9716b))
* Merge pull request #81 from afelicioni/patch-1 ([c5566da](https://github.com/liabru/jquery-match-height/commit/c5566da))
* Merge pull request #82 from JulienMelissas/patch-1 ([63d8ca4](https://github.com/liabru/jquery-match-height/commit/63d8ca4))
* remove ie testing meta tags ([44ed2fe](https://github.com/liabru/jquery-match-height/commit/44ed2fe))
* replace browserstack tunnel with ngrok ([2c67ca0](https://github.com/liabru/jquery-match-height/commit/2c67ca0))
* run webdriver spec for all breakpoints ([3440598](https://github.com/liabru/jquery-match-height/commit/3440598))
* update master build ([df2e0c2](https://github.com/liabru/jquery-match-height/commit/df2e0c2))
* update master build ([f4b4b98](https://github.com/liabru/jquery-match-height/commit/f4b4b98))
* updated min file ([99648ca](https://github.com/liabru/jquery-match-height/commit/99648ca))
* use a spy for callback tests ([a72a2cf](https://github.com/liabru/jquery-match-height/commit/a72a2cf))
* use gutil.log ([00a91bc](https://github.com/liabru/jquery-match-height/commit/00a91bc))
* use local test images ([02398d9](https://github.com/liabru/jquery-match-height/commit/02398d9))
* Use unminified version in Bower's "main" argument ([eedca73](https://github.com/liabru/jquery-match-height/commit/eedca73))



<a name="0.6.0"></a>
# 0.6.0 (2015-03-31)

### release summary

- added options parameter
- added `property` option
- added `target` option
- added callback events
- added maintain scroll
- added inline-block support
- added hidden elements support
- improved performance and throttling
- improved demo

- fixed declaration order issue when using requirejs
- fixed issues for people using build concatenation
- fixed data api issue with missing data-mh
- fixed IE8 border calculation
- fixed Safari row detection
- fixed inline style preservation

### commit log

* Fix usage of data-mh attribute ([816850d](https://github.com/liabru/jquery-match-height/commit/816850d))
* Improve support when concatenated or minified ([09c4b1a](https://github.com/liabru/jquery-match-height/commit/09c4b1a))
* Merge branch 'kwoodfriend-patch-1' ([dde46f9](https://github.com/liabru/jquery-match-height/commit/dde46f9))
* Merge branch 'nyordanov-master' ([dc77dbe](https://github.com/liabru/jquery-match-height/commit/dc77dbe))
* Merge branch 'patch-1' of https://github.com/kwoodfriend/jquery-match-height into kwoodfriend-patch- ([e009c4c](https://github.com/liabru/jquery-match-height/commit/e009c4c))
* Merge branch 'stefanozoffoli-patch-1' ([c0104c4](https://github.com/liabru/jquery-match-height/commit/c0104c4))
* Preserve inline styles when using byRow ([72ba5cf](https://github.com/liabru/jquery-match-height/commit/72ba5cf))
* added display property tests ([5dafa0c](https://github.com/liabru/jquery-match-height/commit/5dafa0c))
* added gitignore ([d76b02c](https://github.com/liabru/jquery-match-height/commit/d76b02c))
* added local jquery ([9239f4e](https://github.com/liabru/jquery-match-height/commit/9239f4e))
* added maintainScroll functionality, closes #18 ([ee83317](https://github.com/liabru/jquery-match-height/commit/ee83317)), closes [#18](https://github.com/liabru/jquery-match-height/issues/18)
* added support for hidden elements, closes #12 ([9a8944b](https://github.com/liabru/jquery-match-height/commit/9a8944b)), closes [#12](https://github.com/liabru/jquery-match-height/issues/12)
* added support for options, added property option for min-height ([94c9d28](https://github.com/liabru/jquery-match-height/commit/94c9d28))
* added update callback events ([0b31e21](https://github.com/liabru/jquery-match-height/commit/0b31e21))
* avoid call to .is when no target specified ([db9996d](https://github.com/liabru/jquery-match-height/commit/db9996d))
* changed master build description ([6dcc13d](https://github.com/liabru/jquery-match-height/commit/6dcc13d))
* early out on options parser ([b4326d3](https://github.com/liabru/jquery-match-height/commit/b4326d3))
* fix for single item rows, closes #48 ([64b9a54](https://github.com/liabru/jquery-match-height/commit/64b9a54)), closes [#48](https://github.com/liabru/jquery-match-height/issues/48)
* fix handling of hidden elements by row, closes #28 ([71a5151](https://github.com/liabru/jquery-match-height/commit/71a5151)), closes [#28](https://github.com/liabru/jquery-match-height/issues/28)
* fix row detection on safari (windows) ([b52448a](https://github.com/liabru/jquery-match-height/commit/b52448a))
* fix to preserve inline styles ([e9de702](https://github.com/liabru/jquery-match-height/commit/e9de702))
* fix typo in target option, closes #63 ([290dfcf](https://github.com/liabru/jquery-match-height/commit/290dfcf)), closes [#63](https://github.com/liabru/jquery-match-height/issues/63)
* fixed IE8 border reset issue, closes #10 ([246820d](https://github.com/liabru/jquery-match-height/commit/246820d)), closes [#10](https://github.com/liabru/jquery-match-height/issues/10)
* fixed support for inline-block ([b3df801](https://github.com/liabru/jquery-match-height/commit/b3df801))
* fixed throttling issue ([fdc8f7a](https://github.com/liabru/jquery-match-height/commit/fdc8f7a))
* implemented target option ([a01fb70](https://github.com/liabru/jquery-match-height/commit/a01fb70))
* improved readme ([9ba9529](https://github.com/liabru/jquery-match-height/commit/9ba9529))
* preserve inline styles on hidden parents, closes #46 ([4917d6c](https://github.com/liabru/jquery-match-height/commit/4917d6c)), closes [#46](https://github.com/liabru/jquery-match-height/issues/46)
* refactored plugin definition ([467d928](https://github.com/liabru/jquery-match-height/commit/467d928))
* release 0.6.0 ([aef80df](https://github.com/liabru/jquery-match-height/commit/aef80df))
* removed redundant css setter ([6c7e6ad](https://github.com/liabru/jquery-match-height/commit/6c7e6ad))
* reorganised source, closes #27 ([cae21cd](https://github.com/liabru/jquery-match-height/commit/cae21cd)), closes [#27](https://github.com/liabru/jquery-match-height/issues/27)
* skip apply to rows with only one item ([f72ab91](https://github.com/liabru/jquery-match-height/commit/f72ab91))
* updated min file ([56214a1](https://github.com/liabru/jquery-match-height/commit/56214a1))
* updated min file ([9aa96f1](https://github.com/liabru/jquery-match-height/commit/9aa96f1))
* updated min file ([b6f612a](https://github.com/liabru/jquery-match-height/commit/b6f612a))
* updated min file ([128c363](https://github.com/liabru/jquery-match-height/commit/128c363))
* updated readme ([667e516](https://github.com/liabru/jquery-match-height/commit/667e516))
* updated readme ([a30551f](https://github.com/liabru/jquery-match-height/commit/a30551f))
* updated readme with known limitations ([57ee64a](https://github.com/liabru/jquery-match-height/commit/57ee64a))
* updating minified version ([ab3963f](https://github.com/liabru/jquery-match-height/commit/ab3963f))



<a name="0.5.2"></a>
# 0.5.2 (2014-06-10)

### release summary

- improved demo
- added matchHeight('remove')
- added update throttling
- removed forced `display:block` after application

### commit log

* added matchHeight('remove') ([8f5f13f](https://github.com/liabru/jquery-match-height/commit/8f5f13f))
* added updated throttling ([6d9a6a7](https://github.com/liabru/jquery-match-height/commit/6d9a6a7))
* prettier demo ([f7ea426](https://github.com/liabru/jquery-match-height/commit/f7ea426))
* release 0.5.2 ([4b8f8e4](https://github.com/liabru/jquery-match-height/commit/4b8f8e4))
* removed forced `display:block` after application ([a3a058c](https://github.com/liabru/jquery-match-height/commit/a3a058c))
* updated changelog ([ecee5f9](https://github.com/liabru/jquery-match-height/commit/ecee5f9))
* updated readme, changelog, build ([ae0a825](https://github.com/liabru/jquery-match-height/commit/ae0a825))



<a name="0.5.1"></a>
# 0.5.1 (2014-04-15)

### release summary

- fixed IE8 NaN bug when parsing 'auto' properties
- fixed IE8 window resize event loop bug
- fixed compatibility with older jQuery versions
- added bower package file
- added jquery package file

### commit log

* Making the library compatible with old jQuery versions < 1.7 ([4c3f945](https://github.com/liabru/jquery-match-height/commit/4c3f945))
* Making the library compatible with old jQuery versions < 1.7 ([7d467aa](https://github.com/liabru/jquery-match-height/commit/7d467aa))
* Merge pull request #3 from dcorb/master ([18a6fa1](https://github.com/liabru/jquery-match-height/commit/18a6fa1))
* added CHANGELOG ([b1ed72d](https://github.com/liabru/jquery-match-height/commit/b1ed72d))
* added bower package ([56c9902](https://github.com/liabru/jquery-match-height/commit/56c9902))
* added minified version ([44c4554](https://github.com/liabru/jquery-match-height/commit/44c4554))
* fixed IE8 NaN bug when parsing 'auto' properties ([702eea6](https://github.com/liabru/jquery-match-height/commit/702eea6))
* fixed IE8 window resize event loop bug ([22b74da](https://github.com/liabru/jquery-match-height/commit/22b74da))
* increment version ([0cb6082](https://github.com/liabru/jquery-match-height/commit/0cb6082))
* updated minified build ([3873f7d](https://github.com/liabru/jquery-match-height/commit/3873f7d))
* updated readme ([b62297b](https://github.com/liabru/jquery-match-height/commit/b62297b))



<a name="0.5.0"></a>
# 0.5.0 (2014-03-02)

### release summary

- initial release

### commit log

* added jquery package file ([3fdbeae](https://github.com/liabru/jquery-match-height/commit/3fdbeae))
* initial commit ([35b8209](https://github.com/liabru/jquery-match-height/commit/35b8209))
* updated readme ([ae41130](https://github.com/liabru/jquery-match-height/commit/ae41130))
* updated readme ([6e1b0f8](https://github.com/liabru/jquery-match-height/commit/6e1b0f8))



