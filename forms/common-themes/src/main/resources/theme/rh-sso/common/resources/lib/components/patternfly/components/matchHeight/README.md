# jquery.matchHeight.js #

> *matchHeight* makes the height of all selected elements exactly equal.<br>
It handles many edge cases that cause similar plugins to fail.

[brm.io/jquery-match-height](http://brm.io/jquery-match-height/)

### Demo

See the [jquery.matchHeight.js demo](http://brm.io/jquery-match-height-demo).

[![jquery.matchHeight.js screenshot](http://brm.io/img/content/jquery-match-height/jquery-match-height.png)](http://brm.io/jquery-match-height-demo)

### Features

- match the heights for groups of elements automatically
- use the maximum height or define a specific target element
- anywhere on the page and anywhere in the DOM
- responsive (updates on window resize)
- row aware (handles floating elements and wrapping)
- accounts for `box-sizing` and mixed `padding`, `margin`, `border` values
- handles images and other media (updates after loading)
- supports hidden or none-visible elements (e.g. those inside tab controls)
- throttled to balance performance and smoothness
- easily removed when needed
- maintain scroll position
- data attributes API
- callback events
- tested in IE8+, Chrome, Firefox, Chrome Android

### Status

Current version is `v0.6.0`. <br>
Use the master build for the latest features. <br>
Please report any [issues](https://github.com/liabru/jquery-match-height/issues) you find.

### Install

[jQuery](http://jquery.com/download/) is required, so include it first.
<br>Download [jquery.matchHeight.js](https://github.com/liabru/jquery-match-height/blob/master/jquery.matchHeight.js) and include the script in your HTML file:

	<script src="jquery.matchHeight.js" type="text/javascript"></script>

#### Or install using [Bower](http://bower.io/)

	bower install matchHeight

### Usage

	$(elements).matchHeight(options);

Where options is an optional parameter. <br>
See below for a description of the available options and defaults.

Call this on the [DOM ready](http://api.jquery.com/ready/) event (the plugin will automatically update on window load). <br>
See below for examples or the included [test.html](https://github.com/liabru/jquery-match-height/blob/master/test.html).

Also see the [Data API](#data-api) below for a simple, alternative inline usage.

### Options

The default options are:

    $(elements).matchHeight({
        byRow: true,
        property: 'height',
        target: null,
        remove: false
    });

Where:

- `byRow` is `true` or `false` to enable row detection
- `property` is the CSS property name to set (e.g. `'height'` or `'min-height'`)
- `target` is an optional element to use instead of the element with maximum height
- `remove` is `true` or `false` to remove previous bindings instead of applying new ones

### Examples

	$(function() {
		$('.item').matchHeight();
	});

The above will set all elements with the class `item` to the height of the tallest.<br>
If the items are on multiple rows, the items of each row will be set to the tallest of that row (see `byRow`).

	$(function() {
		$('.item').matchHeight({
            target: $('.sidebar')
        });
	});

The above will set all elements with the class `item` to the height of the first item with class `sidebar`.

	<div data-mh="my-group">My text</div>
	<div data-mh="my-group">Some other text</div>
	<div data-mh="my-other-group">Even more text</div>
	<div data-mh="my-other-group">The last bit of text</div>

The above will set both elements in `my-group` to the same height as each other.
It will set both elements in `my-other-group` to be the same height as each other.

See the included [test.html](https://github.com/liabru/jquery-match-height/blob/master/test.html) for a working example.

### Data API

Use the data attribute `data-mh="group-name"` where `group-name` is an arbitrary string to identify which elements should be considered as a group.

All elements with the same group name will be set to the same height when the page is loaded, regardless of their position in the DOM, without any extra code required. 

Note that `byRow` will be enabled when using the data API, if you don't want this (or require other options) then use the alternative method above.

### Advanced Usage

There are a few internal properties and functions you should know about:

#### Manually trigger an update

	$.fn.matchHeight._update()

If you need to manually trigger an update of all currently set groups, for example if you've modified some content.

#### Callback events

Since matchHeight automatically handles updating the layout after certain window events, you can supply functions as global callbacks if you need to be notified:

    $.fn.matchHeight._beforeUpdate = function(event, groups) {
        // do something before any updates are applied
    }

    $.fn.matchHeight._afterUpdate = function(event, groups) {
        // do something after all updates are applied
    }

Where `event` a jQuery event object (e.g. `load`, `resize`, `orientationchange`) and `groups` is a reference to `$.fn.matchHeight._groups` (see below).

#### Manually apply match height

	$.fn.matchHeight._apply(elements, options)

Use the apply function directly if you wish to avoid the automatic update functionality.

#### Throttling resize updates

	$.fn.matchHeight._throttle = 80;

By default, the `_update` method is throttled to execute at a maximum rate of once every `80ms`.
Decreasing the above `_throttle` property will update your layout quicker, appearing smoother during resize, at the expense of performance.
If you experience lagging or freezing during resize, you should increase the `_throttle` property.

#### Maintain scroll position

	$.fn.matchHeight._maintainScroll = true;

Under certain conditions where the size of the page is dynamically changing, such as during resize or when adding new elements, browser bugs cause the page scroll position to change unexpectedly.

If you are observing this behaviour, use the above line to automatically attempt to force scroll position to be maintained (approximately). This is a global setting and by default it is `false`.

#### Accessing current group bindings

	$.fn.matchHeight._groups

The array that contains all element groups that have had `matchHeight` applied. Used internally for automatically updating on resize events, but you may modify this array if you need to manually access any groups (e.g. if you're deleting elements).

### Known limitations

#### CSS transitions and animations are not supported

You should ensure that there are no transitions or other animations that will delay the height changes of the elements you are matching, including any `transition: all` rules. Otherwise the plugin will produce unexpected results, as animations can't be accounted for.

#### Delayed webfonts may cause incorrect height

Some browsers [do not wait](http://www.stevesouders.com/blog/2009/10/13/font-face-and-performance/) for webfonts to load before firing the window load event, so if the font loads too slowly the plugin may produce unexpected results.

If this is a problem, you should call `_update` once your font has loaded by using something like the [webfontloader](https://github.com/typekit/webfontloader) script.

#### Content changes require a manual update

If you change the content inside an element that has had `matchHeight` applied, then you must manually call `$.fn.matchHeight._update()` afterwards. This will update of all currently set equal heights groups.

### Changelog

To see what's new or changed in the latest version, see the [changelog](https://github.com/liabru/jquery-match-height/blob/master/CHANGELOG.md)

### License

jquery.matchHeight.js is licensed under [The MIT License (MIT)](http://opensource.org/licenses/MIT)
<br/>Copyright (c) 2014 Liam Brummitt

This license is also supplied with the release and source code.
<br/>As stated in the license, absolutely no warranty is provided.

##### Why not use CSS?

Making robust, responsive equal height columns for _arbitrary content_ is [difficult or impossible](http://filamentgroup.com/lab/setting_equal_heights_with_jquery/) to do with CSS alone (at least without hacks or trickery, in a backwards compatible way).

Note you should probably ensure your layout is still usable if JavaScript is disabled.
