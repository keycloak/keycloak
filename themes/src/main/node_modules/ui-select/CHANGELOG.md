<a name="0.19.8"></a>
## [0.19.8](https://github.com/angular-ui/ui-select/compare/v0.19.7...v0.19.8) (2017-04-15)




<a name="0.19.6"></a>
## [0.19.6](https://github.com/angular-ui/ui-select/compare/v0.19.6...v0.19.6) (2017-04-12)


### Bug Fixes

* **lockedItems:** Properly unlock locked items when lock conditions change ([10ee076](https://github.com/angular-ui/ui-select/commit/10ee076)), closes [#1824](https://github.com/angular-ui/ui-select/issues/1824)
* **select-spinner:** fix spec test ([2d62e0b](https://github.com/angular-ui/ui-select/commit/2d62e0b))
* **spec:** Use exceptionHandler to test errors. (#1879) ([b549db9](https://github.com/angular-ui/ui-select/commit/b549db9)), closes [#1877](https://github.com/angular-ui/ui-select/issues/1877)
* **spinner-class:** add glyphicon to default spinner class, update css. Fixes #1893. ([c8f69db](https://github.com/angular-ui/ui-select/commit/c8f69db)), closes [#1893](https://github.com/angular-ui/ui-select/issues/1893)
* **uiSelectChoices:** prevent template element from being modified (#1850) ([7aabdc4](https://github.com/angular-ui/ui-select/commit/7aabdc4)), closes [#1851](https://github.com/angular-ui/ui-select/issues/1851)
* **uiSelectCtrl:** Reset refreshing flag even if async request errors ([be60430](https://github.com/angular-ui/ui-select/commit/be60430)), closes [#1833](https://github.com/angular-ui/ui-select/issues/1833)
* **uiSelectMatch:** set model value to `null` when cleared ([f711ec2](https://github.com/angular-ui/ui-select/commit/f711ec2)), closes [#863](https://github.com/angular-ui/ui-select/issues/863)
* **uiSelectMultiple:** Don't call onSelectCallback if limit already reached ([b7ac99a](https://github.com/angular-ui/ui-select/commit/b7ac99a)), closes [#1836](https://github.com/angular-ui/ui-select/issues/1836)

### Features

* **Header & Footer:** Add header & footer to the dropdown list. ([f65bad1](https://github.com/angular-ui/ui-select/commit/f65bad1))
* **uiSelectSingle:** add option to avoid backspace resetting the model ([7413321](https://github.com/angular-ui/ui-select/commit/7413321)), closes [#926](https://github.com/angular-ui/ui-select/issues/926) [#525](https://github.com/angular-ui/ui-select/issues/525)



<a name="0.19.5"></a>
## [0.19.5](https://github.com/angular-ui/ui-select/compare/v0.19.5...v0.19.5) (2016-10-24)

## Reverted

* **Bug fix:** Search input isn't blocked ([0d81493](https://github.com/angular-ui/ui-select/commit/0d81493))

<a name="0.19.4"></a>
## [0.19.4](https://github.com/angular-ui/ui-select/compare/v0.19.4...v0.19.4) (2016-10-19)

### Bug Fixes

* **bootstrap:** add aria-expanded to the multiple select ([6766754](https://github.com/angular-ui/ui-select/commit/6766754)), closes [#1775](https://github.com/angular-ui/ui-select/issues/1775)
* ensure aria-activedescendant is correct ([e3be4d1](https://github.com/angular-ui/ui-select/commit/e3be4d1))
* only apply listbox role when open ([e902ffc](https://github.com/angular-ui/ui-select/commit/e902ffc))
* **bootstrap:** add search role ([f3194bf](https://github.com/angular-ui/ui-select/commit/f3194bf))

### Features

* **selectize:** add support for multiple selection  ([ff8071d](https://github.com/angular-ui/ui-select/commit/ff8071d)), closes [#295](https://github.com/angular-ui/ui-select/issues/295) [#1787](https://github.com/angular-ui/ui-select/issues/1787)

<a name="0.19.3"></a>
## [0.19.3](https://github.com/angular-ui/ui-select/compare/v0.19.3...v0.19.3) (2016-08-17)


<a name="0.19.2"></a>
## [0.19.2](https://github.com/angular-ui/ui-select/compare/v0.19.0...v0.19.2) (2016-08-16)


### Bug Fixes

* **bootstrap layout:** Restrict selected length to ui-select-container (#1680) ([01055c5](https://github.com/angular-ui/ui-select/commit/01055c5)), closes [#1576](https://github.com/angular-ui/ui-select/issues/1576)
* **release:** bump version with built files ([76cf9c3](https://github.com/angular-ui/ui-select/commit/76cf9c3))

<a name="0.19.1"></a>
## [0.19.1](https://github.com/angular-ui/ui-select/compare/v0.19.0...v0.19.1) (2016-08-09)

### Bug Fixes

* **bootstrap layout:** Restrict selected length to ui-select-container (#1680) ([01055c5](https://github.com/angular-ui/ui-select/commit/01055c5)), closes [#1576](https://github.com/angular-ui/ui-select/issues/1576)

<a name="0.19.0"></a>
# [0.19.0](https://github.com/angular-ui/ui-select/compare/v0.19.0...v0.19.0) (2016-08-07)

### Bug Fixes

* **bootstrap layout:** Restrict selected length to ui-select-container (#1680) ([01055c5](https://github.com/angular-ui/ui-select/commit/01055c5)), closes [#1576](https://github.com/angular-ui/ui-select/issues/1576)

<a name="0.18.1"></a>
## [0.18.1](https://github.com/angular-ui/ui-select/compare/v0.18.1...v0.18.1) (2016-08-07)

### Bug Fixes

* **bootstrap:** remove anchor tag in choices ([b15189d](https://github.com/angular-ui/ui-select/commit/b15189d))
* **uiSelect:** remove flicker on search change ([81c33d0](https://github.com/angular-ui/ui-select/commit/81c33d0)), closes [#1298](https://github.com/angular-ui/ui-select/issues/1298) [#1594](https://github.com/angular-ui/ui-select/issues/1594) [#1557](https://github.com/angular-ui/ui-select/issues/1557)
* **uiSelectController:** Select by click on non-multiple tagging (bis) (#1727) ([3dfde71](https://github.com/angular-ui/ui-select/commit/3dfde71))

### Features

* **events:** add open-close callback  ([21bcd5e](https://github.com/angular-ui/ui-select/commit/21bcd5e)), closes [#432](https://github.com/angular-ui/ui-select/issues/432) [#1153](https://github.com/angular-ui/ui-select/issues/1153)

### Performance Improvements

* **repeatParserService:** track groups by name  ([1770038](https://github.com/angular-ui/ui-select/commit/1770038)), closes [#1721](https://github.com/angular-ui/ui-select/issues/1721) [#1722](https://github.com/angular-ui/ui-select/issues/1722)

<a name="0.18.1"></a>
## [0.18.1](https://github.com/angular-ui/ui-select/compare/v0.18.0...v0.18.1) (2016-07-10)

### Bug Fixes

* **isDisabled:** do not modify item ([b95bf9f](https://github.com/angular-ui/ui-select/commit/b95bf9f)), closes [#1200](https://github.com/angular-ui/ui-select/issues/1200) [#1661](https://github.com/angular-ui/ui-select/issues/1661)
* **isLocked:** do not modify item ([c01d363](https://github.com/angular-ui/ui-select/commit/c01d363)), closes [#1269](https://github.com/angular-ui/ui-select/issues/1269) [#514](https://github.com/angular-ui/ui-select/issues/514)
* **removeSelected:** fix incorrect removal of preselected item  ([32b7924](https://github.com/angular-ui/ui-select/commit/32b7924)), closes [#1672](https://github.com/angular-ui/ui-select/issues/1672)
* **searchEnabled:** watch evaluated attribute value  ([4503295](https://github.com/angular-ui/ui-select/commit/4503295)), closes [#505](https://github.com/angular-ui/ui-select/issues/505)
* **select2:** Up-direction when using global theme  ([5336dc5](https://github.com/angular-ui/ui-select/commit/5336dc5)), closes [#1674](https://github.com/angular-ui/ui-select/issues/1674)
* **Selectize:** hide input box when selected data is 0 ([e179dc6](https://github.com/angular-ui/ui-select/commit/e179dc6)), closes [#1304](https://github.com/angular-ui/ui-select/issues/1304)
* **tagging:** infite digest loops when name is similar ([fcd9bc5](https://github.com/angular-ui/ui-select/commit/fcd9bc5)), closes [#1693](https://github.com/angular-ui/ui-select/issues/1693)
* **tagging:** Support paste with tagging enabled and tagging-label="false" ([668a0f3](https://github.com/angular-ui/ui-select/commit/668a0f3)), closes [#1668](https://github.com/angular-ui/ui-select/issues/1668)
* **uiSelectMultiple:** $select.refreshItems is not a function  ([a41a7fc](https://github.com/angular-ui/ui-select/commit/a41a7fc))
* **uiSelectMultiple:** Allow duplicates in $select.selected ([9f5d6ec](https://github.com/angular-ui/ui-select/commit/9f5d6ec)), closes [#1688](https://github.com/angular-ui/ui-select/issues/1688)
* **uiSelectMultiple:** tolerate null/undefined view value ([0c29b64](https://github.com/angular-ui/ui-select/commit/0c29b64))
* **uiSelectNoChoice:** support Select2 theme  ([e59e008](https://github.com/angular-ui/ui-select/commit/e59e008)), closes [#1608](https://github.com/angular-ui/ui-select/issues/1608)
* **uiSelectNoChoice:** support Selectize theme  ([a7210c4](https://github.com/angular-ui/ui-select/commit/a7210c4)), closes [#1692](https://github.com/angular-ui/ui-select/issues/1692)

<a name="0.18.0"></a>
## [0.18.0](https://github.com/angular-ui/ui-select/compare/v0.17.1...v0.18.0) (2016-06-09)

### Bug Fixes

* **positioning:** stop flicker when closed ([ca4d09e](https://github.com/angular-ui/ui-select/commit/ca4d09e))
* **positioning:** wait for animation to complete ([aa90dd8](https://github.com/angular-ui/ui-select/commit/aa90dd8)), closes [#1593](https://github.com/angular-ui/ui-select/issues/1593)
* search input width resizing ([5c8cf86](https://github.com/angular-ui/ui-select/commit/5c8cf86)), closes [#1575](https://github.com/angular-ui/ui-select/issues/1575)
* **uiSelectCtrl:** Prevent error when using ngAnimate < v1.4 ([8becac3](https://github.com/angular-ui/ui-select/commit/8becac3)), closes [#1626](https://github.com/angular-ui/ui-select/issues/1626)
* **uiSelectNoChoice:** make compatible with Angular 1.5 ([c944307](https://github.com/angular-ui/ui-select/commit/c944307)), closes [#1609](https://github.com/angular-ui/ui-select/issues/1609)
* **uiSelectNoChoice:** support bootstrap-multiple ([9d29307](https://github.com/angular-ui/ui-select/commit/9d29307)), closes [#1614](https://github.com/angular-ui/ui-select/issues/1614) [#1615](https://github.com/angular-ui/ui-select/issues/1615)

### Features

* **limit:** Change multi-select limit attr (#1632) ([f5888fb](https://github.com/angular-ui/ui-select/commit/f5888fb))
* **removeSelected:** Implement removeSelected property for multiple selects ([3ad084f](https://github.com/angular-ui/ui-select/commit/3ad084f))

<a name="0.17.1"></a>
## [0.17.1](https://github.com/angular-ui/ui-select/compare/v0.17.0...v0.17.1) (2016-05-16)

### Bug Fixes

* **parserResult:** Ignore undefined parserResult when using custom tpl ([cee24e5](https://github.com/angular-ui/ui-select/commit/cee24e5)), closes [#1597](https://github.com/angular-ui/ui-select/issues/1597)
* **select2:** hide dropdown if there are no items to show (same as #1588 for bootstrap) ([4c561ac](https://github.com/angular-ui/ui-select/commit/4c561ac))

<a name="0.17.0"></a>
## [0.17.0](https://github.com/angular-ui/ui-select/compare/v0.16.1...v0.17.0) (2016-05-11)

### Bug Fixes

* **a11y:** prevent list from being focusable ([4e9ab7e](https://github.com/angular-ui/ui-select/commit/4e9ab7e)), closes [#898](https://github.com/angular-ui/ui-select/issues/898)
* **autocomplete:** change to type="search" ([48cf1ba](https://github.com/angular-ui/ui-select/commit/48cf1ba)), closes [#991](https://github.com/angular-ui/ui-select/issues/991)
* **bootstrap:** hide clear button if disabled ([fe0c0c1](https://github.com/angular-ui/ui-select/commit/fe0c0c1)), closes [#1388](https://github.com/angular-ui/ui-select/issues/1388) [#980](https://github.com/angular-ui/ui-select/issues/980)
* **bootstrap:** hide dropdown if there are no items to show ([7c8b3a0](https://github.com/angular-ui/ui-select/commit/7c8b3a0)), closes [#1588](https://github.com/angular-ui/ui-select/issues/1588)
* **build:** fix sourcemap logic ([6d4849f](https://github.com/angular-ui/ui-select/commit/6d4849f))
* **demo-tagging:** error in Object Tags for input "a" ([7963684](https://github.com/angular-ui/ui-select/commit/7963684))
* **sortable:** remove classes properly ([4b1ed47](https://github.com/angular-ui/ui-select/commit/4b1ed47)), closes [#902](https://github.com/angular-ui/ui-select/issues/902)
* **tagging:** do not remove selected items when invalid ([331f819](https://github.com/angular-ui/ui-select/commit/331f819)), closes [#1359](https://github.com/angular-ui/ui-select/issues/1359)
* **tagging groupBy:** fix group-by to work with tagging ([80be85b](https://github.com/angular-ui/ui-select/commit/80be85b))
* **tagging multiple:** hide tagging item if null returned ([2f14045](https://github.com/angular-ui/ui-select/commit/2f14045))
* **uiSelectCtrl:** correcting input focus ([6444d6b](https://github.com/angular-ui/ui-select/commit/6444d6b)), closes [#1253](https://github.com/angular-ui/ui-select/issues/1253)
* **uiSelectSingleDirective:** strictly compare matching value ([a574cd4](https://github.com/angular-ui/ui-select/commit/a574cd4)), closes [#1328](https://github.com/angular-ui/ui-select/issues/1328)
* **uiSelectSort:** update model on sort completion ([9a40b6f](https://github.com/angular-ui/ui-select/commit/9a40b6f)), closes [#974](https://github.com/angular-ui/ui-select/issues/974) [#1036](https://github.com/angular-ui/ui-select/issues/1036)
* ensure highlighted before selecting on tab ([06bbd31](https://github.com/angular-ui/ui-select/commit/06bbd31)), closes [#1030](https://github.com/angular-ui/ui-select/issues/1030)
* properly gc on destruction ([95692e7](https://github.com/angular-ui/ui-select/commit/95692e7))
* show input when search is disabled ([83132b0](https://github.com/angular-ui/ui-select/commit/83132b0)), closes [#595](https://github.com/angular-ui/ui-select/issues/595) [#453](https://github.com/angular-ui/ui-select/issues/453)
* show select element when search is disabled ([f37bafd](https://github.com/angular-ui/ui-select/commit/f37bafd)), closes [#861](https://github.com/angular-ui/ui-select/issues/861)

### Features

* **perf:** debounce resize callback ([115ebf4](https://github.com/angular-ui/ui-select/commit/115ebf4))
* **perf:** optimize width resizing ([d78ba5f](https://github.com/angular-ui/ui-select/commit/d78ba5f))

### Performance Improvements

* **tagging multiple:** transform tagging item only once when filtering ([2b4a9ea](https://github.com/angular-ui/ui-select/commit/2b4a9ea))
* **uiSelectCtrl:** moving activate events out of $timeout ([926f462](https://github.com/angular-ui/ui-select/commit/926f462))
* change test in ctrl.isActive ([d6c14d4](https://github.com/angular-ui/ui-select/commit/d6c14d4))

<a name="0.16.1"></a>
# [0.16.1](https://github.com/angular-ui/ui-select/compare/v0.16.0...v0.16.1) (2016-03-23)

### Bug Fixes

* **$window:** change input size on window resize ([ce24981](https://github.com/angular-ui/ui-select/commit/ce24981)), closes [#522](https://github.com/angular-ui/ui-select/issues/522)
* **uiSelectMultipleDirective:** add $isEmpty handler ([fccc29a](https://github.com/angular-ui/ui-select/commit/fccc29a)), closes [#850](https://github.com/angular-ui/ui-select/issues/850)
* **uiSelectMultipleDirective:** refresh choices upon selection change ([03293ff](https://github.com/angular-ui/ui-select/commit/03293ff)), closes [#1243](https://github.com/angular-ui/ui-select/issues/1243)

<a name="0.16.0"></a>
## [0.15.0](https://github.com/angular-ui/ui-select/compare/v0.15.0...v0.16.0)

<a name="0.15.0"></a>
## [0.15.0](https://github.com/angular-ui/ui-select/compare/v0.14.9...v0.15.0) (2016-03-15)

### Bug Fixes

* corrects out of scope variable ([d5e30fb](https://github.com/angular-ui/ui-select/commit/d5e30fb))

### Features

* provide a way to skip the focusser ([302e80f](https://github.com/angular-ui/ui-select/commit/302e80f)), closes [#869](https://github.com/angular-ui/ui-select/issues/869) [#401](https://github.com/angular-ui/ui-select/issues/401) [#818](https://github.com/angular-ui/ui-select/issues/818) [#603](https://github.com/angular-ui/ui-select/issues/603) [#432](https://github.com/angular-ui/ui-select/issues/432)

<a name="0.14.10"></a>
## [0.14.10](https://github.com/angular-ui/ui-select/compare/v0.14.9...v0.14.10) (2016-03-13)

### Features

* provide a way to skip the focusser ([302e80f](https://github.com/angular-ui/ui-select/commit/302e80f)), closes [#869](https://github.com/angular-ui/ui-select/issues/869) [#401](https://github.com/angular-ui/ui-select/issues/401) [#818](https://github.com/angular-ui/ui-select/issues/818) [#603](https://github.com/angular-ui/ui-select/issues/603) [#432](https://github.com/angular-ui/ui-select/issues/432)

<a name="0.14.9"></a>
## [0.14.9](https://github.com/angular-ui/ui-select/compare/v0.14.9...v0.14.9) (2016-03-06)

<a name="0.14.8"></a>
## [0.14.8](https://github.com/angular-ui/ui-select/compare/v0.14.7...v0.14.8) (2016-02-18)

<a name="0.14.7"></a>
## [0.14.7](https://github.com/angular-ui/ui-select/compare/v0.14.6...v0.14.7) (2016-02-18)

### Bug Fixes

* **IE:** selects not working on IE8 ([ee65677](https://github.com/angular-ui/ui-select/commit/ee65677)), closes [#158](https://github.com/angular-ui/ui-select/issues/158)

<a name="0.14.6"></a>
## [0.14.6](https://github.com/angular-ui/ui-select/compare/v0.14.5...v0.14.6) (2016-02-18)

### Bug Fixes

* **paste:** add paste support ([1ad6f60](https://github.com/angular-ui/ui-select/commit/1ad6f60)), closes [#910](https://github.com/angular-ui/ui-select/issues/910) [#704](https://github.com/angular-ui/ui-select/issues/704) [#789](https://github.com/angular-ui/ui-select/issues/789) [#848](https://github.com/angular-ui/ui-select/issues/848) [#429](https://github.com/angular-ui/ui-select/issues/429)
* **uiSelectSort:** fix dependency not found error ([a5a6554](https://github.com/angular-ui/ui-select/commit/a5a6554))

<a name="0.14.5"></a>
## [0.14.5](https://github.com/angular-ui/ui-select/compare/v0.14.4...v0.14.5) (2016-02-18)

### Bug Fixes

* **uiSelectMultipleDirective:** fix track by error ([ced1cc0](https://github.com/angular-ui/ui-select/commit/ced1cc0)), closes [#1343](https://github.com/angular-ui/ui-select/issues/1343)

<a name="0.14.4"></a>
## [0.14.4](https://github.com/angular-ui/ui-select/compare/v0.14.3...v0.14.4) (2016-02-18)

### Bug Fixes

* Allow setting a ngClass on <ui-select> element ([6a99b08](https://github.com/angular-ui/ui-select/commit/6a99b08)), closes [#277](https://github.com/angular-ui/ui-select/issues/277)

<a name="0.14.3"></a>
## [0.14.3](https://github.com/angular-ui/ui-select/compare/v0.14.2...v0.14.3) (2016-02-18)

<a name="0.14.2"></a>
## [0.14.2](https://github.com/angular-ui/ui-select/compare/v0.14.1...v0.14.2) (2016-02-18)

### Bug Fixes

* make compatible with Angular 1.5 and non-cached templates ([0e85670](https://github.com/angular-ui/ui-select/commit/0e85670)), closes [#1422](https://github.com/angular-ui/ui-select/issues/1422) [#1356](https://github.com/angular-ui/ui-select/issues/1356) [#1325](https://github.com/angular-ui/ui-select/issues/1325) [#1239](https://github.com/angular-ui/ui-select/issues/1239)
* **commonjs:** remove CSS require ([81b0f03](https://github.com/angular-ui/ui-select/commit/81b0f03))
* **track by:** fix "track by" ([6c52e41](https://github.com/angular-ui/ui-select/commit/6c52e41)), closes [#806](https://github.com/angular-ui/ui-select/issues/806) [#665](https://github.com/angular-ui/ui-select/issues/665)

<a name="0.14.1"></a>
## [0.14.1](https://github.com/angular-ui/ui-select/compare/v0.14.0...v0.14.1) (2016-01-27)

<a name="0.14.0"></a>
# [0.14.0](https://github.com/angular-ui/ui-select/compare/v0.13.2...v0.14.0) (2016-01-25)

### Features

* **ngAnimate:** add support for ngAnimate ([8da8a6d](https://github.com/angular-ui/ui-select/commit/8da8a6d))

<a name="0.13.3"></a>
## 0.13.3 (2016-01-25)

### Added

- Add support for commonjs and npm

<a name="0.13.2"></a>
## [0.13.2](https://github.com/angular-ui/ui-select/compare/v0.13.1...v0.13.2) (2016-01-25)

### Bug Fixes

* **CSP:** avoid inline execution of javascript in choices template. ([fb88ec8](https://github.com/angular-ui/ui-select/commit/fb88ec8))

<a name="0.13.1"></a>
## [v0.13.1](https://github.com/angular-ui/ui-select/compare/v0.13.0...v0.13.1) (2015-09-29)

### Fixed

- Remove hardcoded source name when using (key,value) syntax [#1217](https://github.com/angular-ui/ui-select/pull/1217)
- Modify regex to accept a full 'collection expression' when not using (key,value) syntax [#1216](https://github.com/angular-ui/ui-select/pull/1216)
- Avoid to recalculate position when set 'down' [#1214](https://github.com/angular-ui/ui-select/issues/1214#issuecomment-144271352)

<a name="0.13.0"></a>

## [v0.13.0](https://github.com/angular-ui/ui-select/compare/v0.12.1...v0.13.0) (2015-09-29)

### Added

- Allow to configure default dropdown position [#1213](https://github.com/angular-ui/ui-select/pull/1213)
- Can use object as source with (key,value) syntax [#1208](https://github.com/angular-ui/ui-select/pull/1208)
- CHANGELOG.md file created

### Changed

- Do not run bower after install automatically [#982](https://github.com/angular-ui/ui-select/pull/982)
- Avoid setting activeItem on mouseenter to improve performance [#1211](https://github.com/angular-ui/ui-select/pull/1211)

### Fixed

- Position dropdown UP or DOWN correctly depending on the available space [#1212](https://github.com/angular-ui/ui-select/pull/1212)
- Scroll to selected item [#976](https://github.com/angular-ui/ui-select/issues/976)
- Change `autocomplete='off'` to `autocomplete='false'` [#1210](https://github.com/angular-ui/ui-select/pull/1210)
- Fix to work correctly with debugInfoEnabled(false) [#1131](https://github.com/angular-ui/ui-select/pull/1131)
- Limit the maximum number of selections allowed in multiple mode [#1110](https://github.com/angular-ui/ui-select/pull/1110)
