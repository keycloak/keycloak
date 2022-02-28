# STYLIS

[![stylis](https://stylis.js.org/assets/logo.svg)](https://github.com/thysultan/stylis.js)

light – weight css preprocessor

- 3Kb

[![npm](https://img.shields.io/npm/v/stylis.svg?style=flat)](https://www.npmjs.com/package/stylis) [![licence](https://img.shields.io/badge/licence-MIT-blue.svg?style=flat)](https://github.com/thysultan/stylis.js/blob/master/LICENSE.md) [![Build Status](https://semaphoreci.com/api/v1/thysultan/stylis-js/branches/master/shields_badge.svg)](https://semaphoreci.com/thysultan/stylis-js) ![dependencies](https://img.shields.io/badge/dependencies-none-green.svg?style=flat)

## Supports

* Edge
* IE 8+
* Chrome
* Firefox
* Safari
* Node

---

## Installation

#### direct download

```html
<script src=stylis.min.js></script>
```

#### CDN


```html
<script src=https://unpkg.com/stylis@latest/stylis.min.js></script>
```

#### npm

```
npm install stylis --save
```

## Features

- selector namespacing/isolation
- inline global injection ex. `:global(selector)`
- nesting `a { &:hover {} }`
- vendor prefixing (flex-box, etc...)
- flat stylesheets `color: red; h1 { color: red; }`
- keyframe and animation namespacing
- plugins
- minification
- built to support [closure-compiler advanced mode](https://developers.google.com/closure/compiler)

---

## Input

```javascript
stylis('#id', `
font-size: 2em;

// line comments
/* block comments */

:global(body) {background:red}

h1 {
	h2 {
		h3 {
			content:'nesting'
		}
	}
}

@media (max-width: 600px) {
	& {display:none}
}

&:before {
	animation: slide 3s ease infinite
}

@keyframes slide {
	from { opacity: 0}
	to { opacity: 1}
}

& {
	display: flex
}

&::placeholder {
	color:red
}
`);
```

## Output

```css
#id {font-size: 2em;}

body {background:red}
h1 h2 h3 {content: 'nesting'}

@media (max-width: 600px) {
	#id {display:none}
}

#id:before {
	-webkit-animation: slide-id 3s ease infinite;
	animation: slide-id 3s ease infinite;
}


@-webkit-keyframes slide-id {
	from { opacity: 0}
	to { opacity: 1}
}
@keyframes slide-id {
	from { opacity: 0}
	to { opacity: 1}
}

#id {
	display:-webkit-box;
	display:-webkit-flex;
	display:-ms-flexbox;
	display:flex;
}

#id::-webkit-input-placeholder {color:red;}
#id::-moz-placeholder {color:red;}
#id:-ms-input-placeholder {color:red;}
#id::placeholder {color:red;}
```

## API

#### Stylis

```javascript
stylis(selector: {String}, css: {String})
```

#### Factory

```js
// factory pattern
var stylis = new stylis(options)

// singleton pattern
var stylis = stylis
```

When using the factory pattern the if an object is passed as optional `options` argument, this will be passed to `stylis.set(options)`

#### Set

```javascript
// all options except compress and cascade are enabled by default
stylis.set({
	// (dis/en)able :global selectors
	global: {Boolean}

	// (dis/en)able aggressive cascade isolation
	// true for normal cascade(default) false for no cascading
	cascade: {Boolean}

	// (dis/en)able namespace keyframes + animations
	keyframe: {Boolean}

	// (dis/en)able vendor prefixing
	prefix: {Boolean|Function(key: string, value: string, context: number)}

	// (dis/en)able aggressive minification
	compress: {Boolean}

	// (dis/en)able (no)semicolon support
	// false to enable no-semicolons (default)
	semicolon: {Boolean},

	// tell stylis to make an effort to preserve empty rules,
	// i.e `.selector{ }`
	preserve: {Boolean}
})
```

#### Vendor Prefixing

By default vendor is enabled, however there is an option to disable vendor prefixing, either completely or dynamically.

The following would disable prefixing.

```js
stylis.set({prefix: false})
```

Alternatively you can disable prefixing on a case by case basis by providing a function that returns a `boolean` indiciating whether to prefixing that particular rule.

```js
stylis.set({
	prefix: (key, value, context) => {
		return false
	}
})
```

The arguments correspond to the rule that is about to be vendor prefixed. For example:

```js
// transform: none;
key = 'transform'
value = 'none'
context = 1

// :read-only {...}
key = ':read-only'
value = '...'
context = 2

// @keyframes {...}
key = '@keyframes'
value = '...'
context = 3
```

#### Use

```javascript
stylis.use(plugin: {Function|Array<Function>|null})
```

The use function is chainable ex. `stylis.use()()()`

## Plugins

The optional middleware function accepts four arguments with `this` pointing to a reference of the current stylis instance.

```js
(context, content, selectors, parent, line, column, length)
```

Plugins are executed in stages identified by an `context` interger value.

```
-2 /* post-process context */
-1 /* preparation context */
0  /* newline context */

1  /* property context */
2  /* selector block context */
3  /* @at-rule block context */
```

> Note: Since the newline context is intended for source-map plugins by default stylis will not execute plugins in this context unless enabled, this can be done through `stylis.use(true)` or disabled after that through `stylis.use(false)`.

- `-2` post processed context, before the compiled css output is returned
- `-1` preparation context, before the compiler starts
- `0` after every newline
- `1` on a property declaration ex. `color: red;`
- `2` after a selector block of css has been processed ex. `.foo {color:red;}`
- `3` after a `@at-rule` block of css has been processed ex. `@media {h1{color:red;}}`

If at any context(except 0) that the middleware returns a different string the content of css will be replaced with the return value.

To remove all plugins just call `.use` with null/no arguments.

```js
// removes all previously added plugins, then adds one
stylis.use(null)(ctx => {})
```

For example we can add a feature `random()` to our css that when used prints a random number.

```javascript
/**
 * plugin
 *
 * @param  {number} context
 * @param  {Array<string>} selector
 * @param  {Array<string>} parent
 * @param  {string} content
 * @param  {number} line
 * @param  {number} column
 * @param  {number} length
 * @return {(string|void)?}
 */
const plugin = (context, content, selectors, parent, line, column, length) => {
	switch (context) {
		case 1: return content.replace(/random\(\)/g, Math.random())
	}
}

/**
 * use
 *
 * @param {(Array<function>|function|null|boolean)} plugin
 * @return {Function} use
 */
stylis.use(plugin)

stylis(``, `h1 { width: calc(random()*10); }`)
```

This will replace all instances of `random()` with a random number.

Internally Before stylis processes `calc(random()*10);` it passes it to the plugin if a plugin exists; If in turn the plugin returns something different from what it received stylis will replace the content of the property with the return value and continue processing that.

The same can be said for a selector block, in both contexts an argument `selector` is passed that contains the current array of selectors that the block of css/property stylis is working on.

This array of selectors is mutable and will reflect the output of selectors if changed.

## Benchmark

Stylis is fast, and though it does not generate an AST you can with a plugin create an AST out of the resulting input, this and other aspects allow it to be very small(3KB).

The benchmark results are using [https://github.com/postcss/benchmark](https://github.com/postcss/benchmark)

> Note that the benchmark is not a one-2-one comparison because each library was developed for different goals and different set of features.

Stylis appears in all the benchmarks because by default stylis both parsers, processes and auto prefixes in one pass.

#### Parsers

```
Stylis       x 54.28 ops/sec ±4.45% (58 runs sampled)
CSSTree      x 39.73 ops/sec ±9.18% (56 runs sampled)
PostCSS      x 21.11 ops/sec ±8.46% (57 runs sampled)
CSSOM        x 19.20 ops/sec ±6.53% (36 runs sampled)
Mensch       x 17.85 ops/sec ±13.39% (37 runs sampled)
Rework       x 12.80 ops/sec ±4.42% (36 runs sampled)
PostCSS Full x 8.15 ops/sec ±7.79% (45 runs sampled)
Gonzales     x 5.21 ops/sec ±7.75% (18 runs sampled)
Gonzales PE  x 3.99 ops/sec ±10.37% (15 runs sampled)
Stylecow     x 3.97 ops/sec ±9.48% (15 runs sampled)
ParserLib    x 1.79 ops/sec ±8.58% (9 runs sampled)

Fastest test is Stylis at 1.37x faster than CSSTree
```

---

#### Preprocessors

```
Stylis    x 26.26 ops/sec ±5.95% (49 runs sampled)
PostCSS   x 16.23 ops/sec ±11.21% (47 runs sampled)
Rework    x 10.65 ops/sec ±3.86% (55 runs sampled)
libsass   x 6.83 ops/sec ±2.29% (22 runs sampled)
Less      x 4.75 ops/sec ±9.14% (29 runs sampled)
Stylus    x 3.67 ops/sec ±28.12% (25 runs sampled)
Stylecow  x 2.15 ops/sec ±6.36% (15 runs sampled)
Ruby Sass x 0.31 ops/sec ±8.12% (6 runs sampled)

Fastest test is Stylis at 1.62x faster than PostCSS
```

---

#### Prefixers

```
Stylis       x 45.52 ops/sec ±14.61% (77 runs sampled)
Autoprefixer x 13.32 ops/sec ±6.51% (67 runs sampled)
Stylecow     x 2.28 ops/sec ±5.97% (16 runs sampled)
nib          x 1.79 ops/sec ±25.32% (15 runs sampled)
Compass      x 0.15 ops/sec ±9.34% (5 runs sampled)

Fastest test is Stylis at 3.4x faster than Autoprefixer
```
