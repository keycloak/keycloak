<p align="center">
<a href="https://ci.appveyor.com/project/shakyShane/browser-sync" title="AppVeyor branch">
 <img src="https://img.shields.io/appveyor/ci/shakyshane/browser-sync/master.svg?style=flat-square&label=windows" />
</a>
<a href="https://travis-ci.org/BrowserSync/browser-sync" title="Travis branch">
 <img src="https://img.shields.io/travis/BrowserSync/browser-sync/master.svg?style=flat-square&label=linux" />
</a>
<a href="https://coveralls.io/r/BrowsersSync/browser-sync?branch=master" title="Coverage Status">
 <img src="https://img.shields.io/coveralls/BrowserSync/browser-sync.svg?style=flat-square" />
</a>
<a href="https://www.npmjs.com/package/browser-sync">
 <img src="https://img.shields.io/npm/dm/browser-sync.svg?style=flat-square" />
</a>
</p>
<p align="center">
<a href="https://www.npmjs.com/package/browser-sync" title="NPM version">
 <img src="https://img.shields.io/npm/v/browser-sync.svg?style=flat-square" />
</a>
<a href="https://david-dm.org/Browsersync/browser-sync" title="Dependency Status">
 <img src="https://img.shields.io/david/Browsersync/browser-sync.svg?style=flat-square&label=deps" />
</a>
<a href="https://david-dm.org/Browsersync/browser-sync#info=devDependencies" title="devDependency Status">
 <img src="https://img.shields.io/david/dev/Browsersync/browser-sync.svg?style=flat-square&label=devDeps" />
</a>
</p>
<p align="center"><a href="http://www.browsersync.io"><img src="https://raw.githubusercontent.com/BrowserSync/browsersync.github.io/master/img/logo-gh.png" /></a></p>
<p align="center">Keep multiple browsers & devices in sync when building websites.</p>

<p align="center">Browsersync is developed and maintained internally at <a href="http://www.wearejh.com">JH</a></p>
<p align="center">Follow <a href="https://twitter.com/browsersync">@Browsersync</a> on twitter for news & updates.</p>
<p align="center">Community <a href="https://browsersync.herokuapp.com"><img src="https://browsersync.herokuapp.com/badge.svg" /></a></p>

## Features

Please visit [browsersync.io](http://browsersync.io) for a full run-down of features

## Requirements

Browsersync works by injecting an asynchronous script tag (`<script async>...</script>`) right after the `<body>` tag
during initial request. In order for this to work properly the `<body>` tag must be present. Alternatively you
can provide a custom rule for the snippet using [snippetOptions](http://www.browsersync.io/docs/options/#option-snippetOptions)

## Upgrading from 1.x to 2.x ?
Providing you haven't accessed any internal properties, everything will just work as
 there are no breaking changes to the public API. Internally however, we now use an
 immutable data structure for storing/retrieving options. So whereas before you could access urls like this...

```js
browserSync({server: true}, function(err, bs) {
    console.log(bs.options.urls.local);
});
```

... you now access them in the following way:

```js
browserSync({server: true}, function(err, bs) {
    console.log(bs.options.getIn(["urls", "local"]));
});
```

## Install and trouble shooting

[browsersync.io docs](http://browsersync.io)

## Integrations / recipes

[Browsersync recipes](https://github.com/Browsersync/recipes)

## Support

If you've found Browser-sync useful and would like to contribute to its continued development & support, please feel free to send a donation of any size - it would be greatly appreciated!

[![Support via Gittip](https://rawgithub.com/chris---/Donation-Badges/master/gittip.jpeg)](https://www.gittip.com/shakyshane)
[![Support via PayPal](https://rawgithub.com/chris---/Donation-Badges/master/paypal.jpeg)](https://www.paypal.com/cgi-bin/webscr?cmd=_donations&business=shakyshane%40gmail%2ecom&lc=US&item_name=browser%2dsync)

## Contributors

```
  1294	Shane Osbourne
    60	Shinnosuke Watanabe
    19	Shane Daniel
    13	Hugo Bessa
    11	Paul Kinlan
     8	shinnn
     4	Matt Green
     3	Jon Stevens
     3	Werner van Deventer
     3	Simen Bekkhus
     3	Adam Lynch
     3	Dave Hall
     3	Marek 'saji' Augustynowicz
     2	38elements
     2	Piotr Kaleta
     2	Eugeny Vlasenko
     2	Steve Mao
     2	Michael Branch
     2	The Gitter Badger
     2	Dan Tello
     2	Olivier Combe
     2	brutaldev
     2	chase_chou
     2	Hugo Dias
     2	Paul Robertson
     1	Tony Holdstock-Brown
     1	Victor Fernandez de Alba
     1	Yazhong Liu
     1	dsc
     1	mericson
     1	viktor hesselbom
     1	wesruv
     1	Andrew Schmadel
     1	Austin Pray
     1	Benjamín Eidelman
     1	Björn Söderqvist
     1	Cameron Spear
     1	Carl Henderson
     1	Cedric Kastner
     1	Christian Vuerings
     1	Craig Morris
     1	Dimitri Nicolas
     1	Guillaume Lambert
     1	Imran Sulemanji
     1	Jason Dreyzehner
     1	Jory Graham
     1	Michael Leigeber
     1	Naoya Kanai
     1	Paul Love
     1	Paul Miller
     1	Peter Blazejewicz
     1	Philipp Nowinski
     1	Plou
     1	Robert Vock
     1	Stian Didriksen
     1	Sylvain Emery
```

## License

Apache 2
Copyright (c) 2015 Shane Osbourne
