# Respond.js
### A fast & lightweight polyfill for min/max-width CSS3 Media Queries (for IE 6-8, and more)

 - Copyright 2011: Scott Jehl, scottjehl.com

 - Licensed under the MIT license.
 
The goal of this script is to provide a fast and lightweight (3kb minified / 1kb gzipped) script to enable [responsive web designs](http://www.alistapart.com/articles/responsive-web-design/) in browsers that don't support CSS3 Media Queries - in particular, Internet Explorer 8 and under. It's written in such a way that it will probably patch support for other non-supporting browsers as well (more information on that soon).

If you're unfamiliar with the concepts surrounding Responsive Web Design, you can read up [here](http://www.alistapart.com/articles/responsive-web-design/) and also [here](http://filamentgroup.com/examples/responsive-images/)

[Demo page](https://rawgithub.com/scottjehl/Respond/master/test/test.html) (the colors change to show media queries working)


Usage Instructions
======

1. Craft your CSS with min/max-width media queries to adapt your layout from mobile (first) all the way up to desktop


<pre>
    @media screen and (min-width: 480px){
        ...styles for 480px and up go here
    }
</pre>

2. Reference the respond.min.js script (1kb min/gzipped) after all of your CSS (the earlier it runs, the greater chance IE users will not see a flash of un-media'd content)

3. Crack open Internet Explorer and pump fists in delight


CDN/X-Domain Setup
======

Respond.js works by requesting a pristine copy of your CSS via AJAX, so if you host your stylesheets on a CDN (or a subdomain), you'll need to upload a proxy page to enable cross-domain communication.

See `cross-domain/example.html` for a demo:

- Upload `cross-domain/respond-proxy.html` to your external domain
- Upload `cross-domain/respond.proxy.gif` to your origin domain
- Reference the file(s) via `<link />` element(s):

<pre>
	&lt;!-- Respond.js proxy on external server --&gt;
	&lt;link href=&quot;http://externalcdn.com/respond-proxy.html&quot; id=&quot;respond-proxy&quot; rel=&quot;respond-proxy&quot; /&gt;

	&lt;!-- Respond.js redirect location on local server --&gt;
	&lt;link href=&quot;/path/to/respond.proxy.gif&quot; id=&quot;respond-redirect&quot; rel=&quot;respond-redirect&quot; /&gt;

	&lt;!-- Respond.js proxy script on local server --&gt;
	&lt;script src="/path/to/respond.proxy.js"&gt;&lt;/script&gt;
</pre>

If you are having problems with the cross-domain setup, make sure respond-proxy.html does not have a query string appended to it.

Note: HUGE thanks to @doctyper for the contributions in the cross-domain proxy!


Support & Caveats
======

Some notes to keep in mind:

- This script's focus is purposely very narrow: only min-width and max-width media queries and all media types (screen, print, etc) are translated to non-supporting browsers. I wanted to keep things simple for filesize, maintenance, and performance, so I've intentionally limited support to queries that are essential to building a (mobile-first) responsive design. In the future, I may rework things a bit to include a hook for patching-in additional media query features - stay tuned!

- Browsers that natively support CSS3 Media Queries are opted-out of running this script as quickly as possible. In testing for support, all other browsers are subjected to a quick  test to determine whether they support media queries or not before proceeding to run the script. This test is now included separately at the top, and uses the window.matchMedia polyfill found here: https://github.com/paulirish/matchMedia.js . If you are already including this polyfill via Modernizr or otherwise, feel free to remove that part.

- This script relies on no other scripts or frameworks (aside from the included matchMedia polyfill), and is optimized for mobile delivery (~1kb total filesize min/gzip)

- As you might guess, this implementation is quite dumb in regards to CSS parsing rules. This is a good thing, because that allows it to run really fast, but its looseness may also cause unexpected behavior. For example: if you enclose a whole media query in a comment intending to disable its rules, you'll probably find that those rules will end up enabled in non-media-query-supporting browsers.

- Respond.js doesn't parse CSS referenced via @import, nor does it work with media queries within style elements, as those styles can't be re-requested for parsing.

- Due to security restrictions, some browsers may not allow this script to work on file:// urls (because it uses xmlHttpRequest). Run it on a web server.

- If the request for the CSS file that includes MQ-specific styling is
  behind a redirect, Respond.js will fail silently. CSS files should
respond with a 200 status.

- Currently, media attributes on link elements are supported, but only if the linked stylesheet contains no media queries. If it does contain queries, the media attribute will be ignored and the internal queries will be parsed normally. In other words, @media statements in the CSS take priority.

- Reportedly, if CSS files are encoded in UTF-8 with Byte-Order-Mark (BOM), they will not work with Respond.js in IE7 or IE8. Noted in issue #97

- WARNING: Including @font-face rules inside a media query will cause IE7 and IE8 to hang during load. To work around this, place @font-face rules in the wide open, as a sibling to other media queries.

- If you have more than 32 stylesheets referenced, IE will throw an error, `Invalid procedure call or argument`. Concatenate your CSS and the issue should go away.

- Sass/SCSS source maps are not supported; `@media -sass-debug-info` will break respond.js. Noted in issue [#148](https://github.com/scottjehl/Respond/issues/148)

- Internet Explorer 9 supports css3 media queries, but not within frames when the CSS containing the media query is in an external file (this appears to be a bug in IE9 — see http://stackoverflow.com/questions/10316247/media-queries-fail-inside-ie9-iframe). See this commit for a fix if you're having this problem. https://github.com/NewSignature/Respond/commit/1c86c66075f0a2099451eb426702fc3540d2e603

- Nested Media Queries are not supported


How's it work?
======
Basically, the script loops through the CSS referenced in the page and runs a regular expression or two on their contents to find media queries and their associated blocks of CSS. In Internet Explorer, the content of the stylesheet is impossible to retrieve in its pre-parsed state (which in IE 8-, means its media queries are removed from the text), so Respond.js re-requests the CSS files using Ajax and parses the text response from there. Be sure to configure your CSS files' caching properly so that this re-request doesn't actually go to the server, hitting your browser cache instead.

From there, each media query block is appended to the head in order via style elements, and those style elements are enabled and disabled (read: appended and removed from the DOM) depending on how their min/max width compares with the browser width. The media attribute on the style elements will match that of the query in the CSS, so it could be "screen", "projector", or whatever you want. Any relative paths contained in the CSS will be prefixed by their stylesheet's href, so image paths will direct to their proper destination

API Options?
======
Sure, a couple:

- respond.update() : rerun the parser (helpful if you added a stylesheet to the page and it needs to be translated)
- respond.mediaQueriesSupported: set to true if the browser natively supports media queries.
- respond.getEmValue() : returns the pixel value of one em


Alternatives to this script
======
This isn't the only CSS3 Media Query polyfill script out there; but it damn well may be the fastest.

If you're looking for more robust CSS3 Media Query support, you might check out http://code.google.com/p/css3-mediaqueries-js/. In testing, I've found that script to be noticeably slow when rendering complex responsive designs (both in filesize and performance), but it really does support a lot more media query features than this script. Big hat tip to the authors! :)
