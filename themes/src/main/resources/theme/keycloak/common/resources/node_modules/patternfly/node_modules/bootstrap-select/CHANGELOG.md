# v1.12.4 (2017-07-19)

### Bug Fixes
- [#1286]: Event creation throws illegal constructor error on stock Android Browser < 5.0
- [#1764]: Bootstrap-select steals focus on form.checkValidity

[#1286]: https://github.com/silviomoreto/bootstrap-select/issues/1286
[#1764]: https://github.com/silviomoreto/bootstrap-select/issues/1764

-------------------

# v1.12.3 (2017-07-06)

### Bug Fixes
- [#1529]: add selectAllText and deselectAllText to translation files (used Google Translate)
- [#1604]: Keydown improvements
- [#1630]: htmlEscape inline style
- [#1631]: Livesearch performance

### New Features
- Add/update various translations

[#1529]: https://github.com/silviomoreto/bootstrap-select/issues/1529
[#1604]: https://github.com/silviomoreto/bootstrap-select/pull/1604
[#1630]: https://github.com/silviomoreto/bootstrap-select/issues/1630
[#1631]: https://github.com/silviomoreto/bootstrap-select/pull/1631

-------------------

# v1.12.2 (2017-01-30)

### Bug Fixes
* [#1563]: key word searching broken in [#1516].
* [#1570]: properly adjust size when inside form-group-sm or form-group-lg
* [#1590]: menu height calculated improperly when using liveSearch and input has custom height

[#1563]: https://github.com/silviomoreto/bootstrap-select/issues/1563
[#1570]: https://github.com/silviomoreto/bootstrap-select/issues/1570
[#1590]: https://github.com/silviomoreto/bootstrap-select/issues/1590

-------------------

# v1.12.1 (2016-11-22)

### Bug Fixes
* [#1167], [#1366]: using a method before initializing bootstrap-select throws an error

[#1167]: https://github.com/silviomoreto/bootstrap-select/issues/1167
[#1366]: https://github.com/silviomoreto/bootstrap-select/issues/1366

-------------------

# v1.12.0 (2016-11-18)

### Bug Fixes
* [#1220]: unescape button title
* [#1348]: escape HTML for optgroup label
* [#1506]: Fix bs-placeholder usage for jQuery>=3.0
* [#1509]: inline style Content Security Policy
* [#1477]: using liveSearchNormalize and liveSearchStyle="startsWith" simultaneously breaks search
* [#1489] fix selectOnTab with liveSearch enabled which was broken when [#1489] was fixed
* [#1533]: remove touchstart event listener (issues with FastClick)
* remove destroyLi function - improve refresh() performance
* [#1531]: add Spanish (Spain) translations
* [#1553]: don't use replace in normalizeToBase if text is undefined (throws error otherwise)

### New Features
* [#1503]: Add windowPadding option (either a number or an array of numbers - [top, right, bottom, left])
* [#1516]: Improve liveSearch performance (addresses [#1275])
* [#1440]: allow HTML in placeholder title for non-multiple selects
* [#1555]: Use default with SCSS variables

[#1220]: https://github.com/silviomoreto/bootstrap-select/issues/1220
[#1275]: https://github.com/silviomoreto/bootstrap-select/issues/1275
[#1348]: https://github.com/silviomoreto/bootstrap-select/issues/1348
[#1506]: https://github.com/silviomoreto/bootstrap-select/issues/1506
[#1509]: https://github.com/silviomoreto/bootstrap-select/issues/1509
[#1477]: https://github.com/silviomoreto/bootstrap-select/issues/1477
[#1489]: https://github.com/silviomoreto/bootstrap-select/issues/1489
[#1533]: https://github.com/silviomoreto/bootstrap-select/issues/1533
[#1531]: https://github.com/silviomoreto/bootstrap-select/issues/1531
[#1503]: https://github.com/silviomoreto/bootstrap-select/issues/1503
[#1516]: https://github.com/silviomoreto/bootstrap-select/issues/1516
[#1440]: https://github.com/silviomoreto/bootstrap-select/issues/1440
[#1553]: https://github.com/silviomoreto/bootstrap-select/issues/1553
[#1555]: https://github.com/silviomoreto/bootstrap-select/issues/1555

-------------------

# v1.11.2 (2016-09-09)

### Bug Fixes
* fix sourceMappingURL in bootstrap-select.min.js

-------------------

# v1.11.1 (2016-09-09)

### Bug Fixes
* [#1475]: fix Cannot read property 'apply' of null error
* [#1484]: Change events fire twice on IE8
* [#1489]: hide.bs.select and hidden.bs.select events not fired when "Esc" key pressed with live search enabled

[#1475]: https://github.com/silviomoreto/bootstrap-select/issues/1475
[#1484]: https://github.com/silviomoreto/bootstrap-select/issues/1484
[#1489]: https://github.com/silviomoreto/bootstrap-select/issues/1489

-------------------

# v1.11.0 (2016-08-16)

### Bug Fixes
* [#1291]: don't trigger change event if selecting an option that passes the limit
* [#1284]: check if all options are already selected/deselected before triggering changed/changed.bs.select
* [#1245], [#1310]: With livesearch, when keypress, focus to search field isn't working with some characters
* [#1257]: fix issue with Norwegian translation
* [#1346]: fix edge case where default values are not respected when initializing the plugin
* [#1338]: improve support for disabled optgroups and hidden options
* [#1373]: prevent selectAll and deselectAll from being called on standard select boxes
* [#1363]: if hideDisabled is enabled, and all options in an optgroup are disabled, the optgroup is still visible
* [#1422]: fix menu position inside a scrolling container
* [#1451]: fix select with input-group-addon on both sides
* [#1465]: changed.bs.select not firing for native mobile menu
* [#1459]: jQuery 3 support - $.expr[':'] -> $.expr.pseudos

### New Features
* [#1139]: add placeholder styling via `bs-placeholder` class
* [#1290]: auto close the menu if maxOptions is set to 1 (instead of leaving open)
* [#1127], [#1016], [#1160], [#1269]: add 'auto' option for dropdownAlignRight
* [58ed408]: support using a string for maxOptionsText
* [#541]: ARIA - Accessibility

[#1291]: https://github.com/silviomoreto/bootstrap-select/issues/1291
[#1284]: https://github.com/silviomoreto/bootstrap-select/issues/1284
[#1245]: https://github.com/silviomoreto/bootstrap-select/issues/1245
[#1257]: https://github.com/silviomoreto/bootstrap-select/issues/1257
[#1310]: https://github.com/silviomoreto/bootstrap-select/issues/1310
[#1346]: https://github.com/silviomoreto/bootstrap-select/issues/1346
[#1338]: https://github.com/silviomoreto/bootstrap-select/issues/1338
[#1373]: https://github.com/silviomoreto/bootstrap-select/issues/1373
[#1363]: https://github.com/silviomoreto/bootstrap-select/issues/1363
[#1422]: https://github.com/silviomoreto/bootstrap-select/issues/1422
[#1451]: https://github.com/silviomoreto/bootstrap-select/issues/1451
[#1465]: https://github.com/silviomoreto/bootstrap-select/issues/1465
[#1459]: https://github.com/silviomoreto/bootstrap-select/issues/1459
[#1139]: https://github.com/silviomoreto/bootstrap-select/issues/1139
[#1290]: https://github.com/silviomoreto/bootstrap-select/issues/1290
[#1127]: https://github.com/silviomoreto/bootstrap-select/issues/1127
[#1016]: https://github.com/silviomoreto/bootstrap-select/issues/1016
[#1160]: https://github.com/silviomoreto/bootstrap-select/issues/1160
[#1269]: https://github.com/silviomoreto/bootstrap-select/issues/1269
[58ed408]: https://github.com/silviomoreto/bootstrap-select/commit/58ed4085019526141be07beeada37788dfe2d316
[#541]: https://github.com/silviomoreto/bootstrap-select/issues/541

-------------------

# v1.10.0 (2016-02-17)

### Bug Fixes
* [#1268]: performance bug in clickListener
* [#1273]: html5 validation message disappears in Chrome 47+
* [#1295]: hide select by default (so there is no flash of unstyled content)

### New Features
* [#950]: add `.selectpicker('toggle')` method to allow menu to be open/closed programmatically
* [#1272]: add showTick option
* [#1284]: selectAll and deselectAll now trigger the `changed.bs.select` event

Add Lithuanian translations.

[#1268]: https://github.com/silviomoreto/bootstrap-select/issues/1268
[#1273]: https://github.com/silviomoreto/bootstrap-select/issues/1273
[#1295]: https://github.com/silviomoreto/bootstrap-select/issues/1295
[#950]: https://github.com/silviomoreto/bootstrap-select/issues/950
[#1272]: https://github.com/silviomoreto/bootstrap-select/issues/1272
[#1284]: https://github.com/silviomoreto/bootstrap-select/issues/1284

-------------------

# v1.9.4 (2016-01-18)

### Bug fixes
* [#1250]: don't destroy original select when using `destroy` method
* [#1230]: Optgroup label missing when first option is disabled and `hideDisabled` is true

Add new translations.

[#1250]: https://github.com/silviomoreto/bootstrap-select/issues/1250
[#1230]: https://github.com/silviomoreto/bootstrap-select/issues/1230

-------------------

# v1.9.3 (2015-12-16)

### Bug fixes
* Fix [#1235] - issue with selects that had `form-control` class

[#1235]: https://github.com/silviomoreto/bootstrap-select/issues/1235
