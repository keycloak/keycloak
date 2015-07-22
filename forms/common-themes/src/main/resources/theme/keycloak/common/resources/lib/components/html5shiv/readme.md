# The HTML5 Shiv

The HTML5 Shiv enables use of HTML5 sectioning elements in legacy Internet Explorer and provides basic HTML5 styling for Internet Explorer 6-9, Safari 4.x (and iPhone 3.x), and Firefox 3.x.

### What do these files do?

#### `html5shiv.js`
*  This includes the basic `createElement()` shiv technique, along with monkeypatches for `document.createElement` and `document.createDocumentFragment` for IE6-8. It also applies [basic styling](https://github.com/aFarkas/html5shiv/blob/51da98dabd3c537891b7fe6114633fb10de52473/src/html5shiv.js#L216-220) for HTML5 elements for IE6-9, Safari 4.x and FF 3.x.

####`html5shiv-printshiv.js` 
*  This includes all of the above, as well as a mechanism allowing HTML5 elements to be styled and contain children while being printed in IE 6-8.

### Who can I get mad at now?

HTML5 Shiv is maintained by [Alexander Farkas](https://github.com/aFarkas/), [Jonathan Neal](https://twitter.com/jon_neal) and [Paul Irish](https://twitter.com/paul_irish), with many contributions from [John-David Dalton](https://twitter.com/jdalton). It is also distributed with [Modernizr](http://modernizr.com/), and the two google code projects, [html5shiv](https://code.google.com/p/html5shiv/) and [html5shim](https://code.google.com/p/html5shim/), maintained by [Remy Sharp](https://twitter.com/rem).

If you have any issues in these implementations, you can report them here! :)

For the full story of HTML5 Shiv and all of the people involved in making it, read [The Story of the HTML5 Shiv](http://paulirish.com/2011/the-history-of-the-html5-shiv/).

## Installation

###Using [Bower](http://bower.io/)

`bower install html5shiv --save-dev`

This will clone the latest version of the HTML5 shiv into the `bower_components` directory at the root of your project and also create or update the file `bower.json` which specifies your projects dependencies.

Include the HTML5 shiv in the `<head>` of your page in a conditional comment and after any stylesheets.

```html
<!--[if lt IE 9]>
	<script src="bower_components/html5shiv/html5shiv.js"></script>
<![endif]-->
```

###Manual installation

Download and extract the [latest zip package](https://github.com/aFarkas/html5shiv/archive/master.zip) from this repositiory and copy the two files `dist/html5shiv.js` and `dist/html5shiv-printshiv.js` into your project. Then include one of them into your `<head>` as above. 

## HTML5 Shiv API

HTML5 Shiv works as a simple drop-in solution. In most cases there is no need to configure HTML5 Shiv or use methods provided by HTML5 Shiv.

### `html5.elements` option

The `elements` option is a space separated string or array, which describes the **full** list of the elements to shiv. see also `addElements`.

**Configuring `elements` before `html5shiv.js` is included.**

```js
//create a global html5 options object
window.html5 = {
  'elements': 'mark section customelement' 
};
```
**Configuring `elements` after `html5shiv.js` is included.**

```js
//change the html5shiv options object 
window.html5.elements = 'mark section customelement';
//and re-invoke the `shivDocument` method
html5.shivDocument(document);
```

### `html5.shivCSS`

If `shivCSS` is set to `true` HTML5 Shiv will add basic styles (mostly display: block) to sectioning elements (like section, article). In most cases a webpage author should include those basic styles in his normal stylesheet to ensure older browser support (i.e. Firefox 3.6) without JavaScript.

The `shivCSS` is true by default and can be set false, only before html5shiv.js is included: 

```js
//create a global html5 options object
window.html5 = {
	'shivCSS': false
};
```

### `html5.shivMethods`

If the `shivMethods` option is set to `true` (by default) HTML5 Shiv will override `document.createElement`/`document.createDocumentFragment` in Internet Explorer 6-8 to allow dynamic DOM creation of HTML5 elements. 

Known issue: If an element is created using the overridden `createElement` method this element returns a document fragment as its `parentNode`, but should be normally `null`. If a script relies on this behavior, `shivMethods`should be set to `false`.
Note: jQuery 1.7+ has implemented his own HTML5 DOM creation fix for Internet Explorer 6-8. If all your scripts (including Third party scripts) are using jQuery's manipulation and DOM creation methods, you might want to set this option to `false`.

**Configuring `shivMethods` before `html5shiv.js` is included.**

```js
//create a global html5 options object
window.html5 = {
	'shivMethods': false
};
```
**Configuring `elements` after `html5shiv.js` is included.**

```js
//change the html5shiv options object 
window.html5.shivMethods = false;
```

### `html5.addElements( newElements [, document] )`

The `html5.addElements` method extends the list of elements to shiv. The newElements argument can be a whitespace separated list or an array.

```js
//extend list of elements to shiv
html5.addElements('element content');
```

### `html5.createElement( nodeName [, document] )`

The `html5.createElement` method creates a shived element, even if `shivMethods` is set to false.

```js
var container = html5.createElement('div');
//container is shived so we can add HTML5 elements using `innerHTML`
container.innerHTML = '<section>This is a section</section>';
```

### `html5.createDocumentFragment( [document] )`

The `html5.createDocumentFragment` method creates a shived document fragment, even if `shivMethods` is set to false.

```js
var fragment = html5.createDocumentFragment();
var container = document.createElement('div');
fragment.appendChild(container);
//fragment is shived so we can add HTML5 elements using `innerHTML`
container.innerHTML = '<section>This is a section</section>';
```

## HTML5 Shiv Known Issues and Limitations

- The `shivMethods` option (overriding `document.createElement`) and the `html5.createElement` method create elements, which are not disconnected and have a parentNode (see also issue #64)
- The cloneNode problem is currently not addressed by HTML5 Shiv. HTML5 elements can be dynamically created, but can't be cloned in all cases.
- The printshiv version of HTML5 Shiv has to alter the print styles and the whole DOM for printing. In case of complex websites and or a lot of print styles this might cause performance and/or styling issues. A possible solution could be the [htc-branch](https://github.com/aFarkas/html5shiv/tree/iepp-htc) of HTML5 Shiv, which uses another technique to implement print styles for Internet Explorer 6-8.

### What about the other HTML5 element projects?

- The original conception and community collaboration story of the project is described at [The History of the HTML5 Shiv](http://paulirish.com/2011/the-history-of-the-html5-shiv/). 
- [IEPP](https://code.google.com/p/ie-print-protector), by Jon Neal, addressed the printing fault of the original `html5shiv`. It was merged into `html5shiv`.
- **Shimprove**, in April 2010, patched `cloneNode` and `createElement` was later merged into `html5shiv`
- **innerShiv**, introduced in August 2010 by JD Barlett, addressed dynamically adding new HTML5 elements into the DOM. [jQuery added support](http://blog.jquery.com/2011/11/03/jquery-1-7-released/) that made innerShiv redundant and `html5shiv` addressed the same issues as well, so the project was completed.
- The **html5shim** and **html5shiv** sites on Google Code are maintained by Remy Sharp and are identical distribution points of this `html5shiv` project.
- **Modernizr** is developed by the same people as `html5shiv` and can include the latest version in any custom builds created at modernizr.com
- This `html5shiv` repo now contains tests for all the edge cases pursued by the above libraries and has been extensively tested, both in development and production. 

A [detailed changelog of html5shiv](https://github.com/aFarkas/html5shiv/wiki) is available.

### Why is it called a *shiv*?

The term **shiv** [originates](http://ejohn.org/blog/html5-shiv/) from [John Resig](https://github.com/jeresig), who was thought to have used the word for its slang meaning, *a sharp object used as a knife-like weapon*, intended for Internet Explorer. Truth be known, John probably intended to use the word [shim](http://en.wikipedia.org/wiki/Shim_(computing\)), which in computing means *an application compatibility workaround*. Rather than correct his mispelling, most developers familiar with Internet Explorer appreciated the visual imagery. And that, [kids](http://html5homi.es/), is [etymology](https://en.wikipedia.org/wiki/Etymology).
