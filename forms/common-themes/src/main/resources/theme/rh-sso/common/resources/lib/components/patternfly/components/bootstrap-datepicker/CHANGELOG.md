Changelog
=========

1.3.1
-----

Repo changes:
* Automated screenshots have been added to the docs. These probably need to be documented so that contributors can add them when appropriate.
* Grunt support
* Missing description and keywords for Packagist
* Composer: Include translation files into deployment dir
* Add package name and version to npm package.json

Bugfixes:
* Remove font-family declaration for datepicker 
* Don't deselect date unless datepicker is multidate
* Removed comment from compiled CSS.
* Don't clear input after typing date and hitting Enter when keyboard nav is disabled
* Fixing the ui displaying 'undefined nan' when typing dates in Firefox & IE 
* Reset tooltip to a default empty value 
* Fix colspan if calendarWeeks & clearBtn are true 
* Removed fixed width and height in large and small group addon
* z-index calculation should not stop at first element
* Fix IE8 bug with Array#splice with one argument 

Documentation:
* ghpages: jQuery js not being loaded when using HTTPS
* Adds clearBtn option to sandbox page
* Minor fixes (typo's, links,...)

Locale changes

Updated languages:
* Clear translation in czech
* Dutch translation
* Swedish translation
* Japanese translation
* Ukrainian translation fixes
* Add spanish clear, week start and format
* Added galician clear, week start and format
* Added missing clear localization value for polish translation
* Add clear zh-CN translation
* Fixed Albanian translation typo's
* Add missing clear and format localization value for Russian translation
* Updated Serbian translation
* Fixed Ukrainian iso code to uk instead of ua 
* Updated greek translation
* Update Catalan and Spanish localizations
* Added missing armenian translations

New languages:
* Basque
* Khmer (Cambodia)
* Bosnian
* British english
* Armenian
* Faroese
* Swiss Italian and Swiss French

1.3.0
-----

New features:
* Bootstrap 3 support.  Added build files `build/build_standalone3.less` and `build/build3.less`, and source files `less/datepicker3.less` and `css/datepicker3.css` (built from `build_standalone3.less`).
* Multi-date functionality.  This required rethinking several areas of the picker:
    * The internals have been modified to be completely multidate-centric.
    * Attributes and methods availabel on events have changed, but the old attributes and functions will still work.
    * Keyboard navigation has been revamped, as it didn't work at all properly with multidate selection.
    * The picker now explicitly supports "no selected date".

Non-API changes:
* Keyboard navigation has been changed.  See `docs/keyboard.rst`.
* Empty pickers in a range picker setup will be populated with the first date selected by the user to make finding the next date easier.

Bug squashed:
* Jan 1, 1970 is now highlighted when selected
* `touchstart` added to document-bound picker-closing events (alongside `mousedown`)
* Fixed a display bug with component add-on icons being vertically higher than they should have been.
* Input is refocused after clicking the picker.
* `changeDate` event is triggered when `setDate` is called.

Locale changes:
* Added Ukrainian, Belgium-Dutch, Welsh, Galician, Vietnamese, and Azerbaijani
* `clear` for German, Danish, Italian, and Romanian
* Fixed `weekStart` and `format` for Norwegian
* `weekStart` and `format` for Georgian
* Tweaks for Latvian, French, Vietnamese, Swedish, and Croatian
* De-duplicated Ukrainian files from `uk` and `ua` to just `ua`

Repository changes:
* Documentation has been moved from the base `README.md` file to the `docs/` folder, and been re-written to use sphinx docs.  The docs are now viewable online at http://bootstrap-datepicker.readthedocs.org/.  The [gh-pages](http://eternicode.github.io/bootstrap-datepicker/) branch has been reduced to the sandbox demo.
* Changed the js file header to point at repo/demo/docs urls instead of eyecon.ro
* The css files are now the output of the standalone build scripts instead of `build/build.less` etc.
* `composer.json` now supports component-installer
* Added [JSHint](http://www.jshint.com/docs/) and [JSCS](https://github.com/mdevils/node-jscs) configurations


1.2.0
-----

New features:
* Google Closure Compiler Compatibility
* Smart orientation by default, and explicit picker orientation with the `orientation` option
* Text inside the picker is no longer user-selectable
* Packagist/Composer support (I think...)
* No longer depends on glyphicons for arrows
* `clearDate` event added, fired when the date is cleared

Bug squashed:
* `noConflict` fixed
* Fix for large years causing an infinite loop in date parsing
* Fixed cases where `changeYear` and `changeMonth` events were not being triggered
* `component.js` moved to `bower.js`
* Falsey values for `startDate` and `endDate` translate to `-Infinity` and `Infinity`, respectively (effectively, falsey values mean "no bounds")
* Fixed `autoclose` for non-input, non-component elements
* Fixed 50% param in `mix()` less function -- expands compatibility with less compilers
* Fixed `update` method to update the selected date
* `beforeShowDay` was getting UTC dates, now it gets local dates (all dates that developers are given should be in local time, not UTC).
* `startDate` and `endDate` were a bit confused when given `new Date()` -- they would not allow today to be selected (the range should be inclusive), they would change whether it was selectable based on local time, etc.  These quirks should be fixed now.  They both also now expect local dates (which will then be time-zeroed and converted to UTC).
* Fixed selected date not being automatically constrained to the specified range when `setStartDate` and `setEndDate` were called.
* No longer uses jQuery's `.size()` (deprecated in favor of `.length`)
* `changeDate` triggered during manual user input
* `change` event fired when input value changed, it wasn't in some cases

Locale changes:
* Added Arabic, Norwegian, Georgian
* `clear` for French
* `today` and `clear` for Bahasa
* `today` and `clear` for Portuguese (both `pt` and `pt-BR`)
* `format` for Turkish
* `format` and `weekStart` for Swedish
* `format` and `weekStart` for Simplified Chinese; `today`, `format`, and `weekStart` for Traditional Chinese
* Fixed typo in Serbian latin (`rs-latin`)
* More appropriate use of Traditional Chinese habit in `zh-TW`


1.1.3
 ----------
 
 Clicking the clear button now triggers the input's `change` and datepicker's `changeDate` events.
 Fixed a bug that broke the event-attached `format` function.
 
 
1.1.2
----------

Botched release, no change from 1.1.1


1.1.1
----------

Fixes a bug when setting startDate or endDate during initialization.


1.1.0
----------

New features:
* Date range picker.
* Data API / noConflict.
* `getDate` and `setDate` methods.
* `format` method for events; this allows you to easily format the `date` associated with the event.
* New options:
  * `beforeShowDay` option: a dev-provided function that can enable/disable dates, add css classes, and add tooltips.
  * `clearBtn`, a button for resetting the picker.

Internal changes:
* Cleaner and more reliable method for extracting options from all potential sources (defaults, locale overrides, data-attrs, and instantiation options, in that order).  This also populates `$.fn.datepicker.defaults` with the default values, and uses this hash as the actual source of defaults, meaning you can globally change the default value for a given option.

Bugs squashed:
* Resolved a conflict with bootstrap's native `.switch` class.
* Fixed a bug with components where they would be stuck with a stale value when editing the value manually.
* The `date` attributes on events are now local dates instead of internal UTC dates.
* Separate `Date` objects for internal selected and view date references.
* Clicking multiple times inside inputs no longer hides the picker.

Minor improvements:
* Better text color for highlighted "today" date.
* Last year in decade view now marked as "new" instead of "old".
* Formats now properly handle trailing separators.

Locale changes:
* Added Albanian, Estonian, and Macedonian
* Added `weekStart` for Russian
* Added `weekStart` and `format` for Finnish

Potentially backward-incompatible changes:
* Options revamp:
  * This fixes bugs in the correlation of some data-attrs to their associated option names.  If you use `data-date-weekstart`, `data-date-startdate`, or `data-date-enddate`, you should update these to `data-date-week-start`, `data-date-start-date`, or `data-date-end-date`, respectively.
  * All options for datepicker are now properties on the datepicker's `o` property; options are no longer stored on the Datepicker instance itself.  If you have code that accesses options stored on the datepicker instance (eg, `datepicker.format`), you will need to update it to access those options via the `o` property (eg, `datepicker.o.format`).  "Raw" options are available via the `_o` property.

1.0.2
----------

Small optimizations release

* Reduced the number of times `update` is called on initialization.
* Datepicker now detaches the picker dropdown when it is hidden, and appends it when shown.  This removes the picker from the DOM when it is not in use.
* No longer listens to document/window events unless picker is visible.

v1.0.1
------

* Support for [Bower](http://twitter.github.com/bower/)
* Component pickers are now aligned under the input, not the add-on element.
* Japanese locale now has "today" and "format".
* "remove" method removes `.data().date` if the datepicker is on a non-input.
* Events on initialized elements are no longer blocked from bubbling up the DOM (jQuery.live et al can now catch the events).
* Component triggers now include `.btn` in addition to `.add-on`.
* Updates to README contents.

v1.0.0
------

Initial release:

* format option
* weekStart option
* calendarWeeks option
* startDate / endDate options
* daysOfWeekDisabled option
* autoclose option
* startView / mnViewMode options
* todayBtn / todayHighlight options
* keyboardNavigation option
* language option
* forceParse option
