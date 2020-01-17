# [![angular-translate](https://raw.github.com/angular-translate/angular-translate/canary/identity/logo/angular-translate-alternative/angular-translate_alternative_medium2.png)](http://angular-translate.github.io)

![Bower](https://img.shields.io/bower/v/angular-translate.svg) [![NPM](https://img.shields.io/npm/v/angular-translate.svg)](https://www.npmjs.com/package/angular-translate) [![cdnjs](https://img.shields.io/cdnjs/v/angular-translate.svg)](https://cdnjs.com/libraries/angular-translate) [![Build Status](https://img.shields.io/travis/angular-translate/angular-translate.svg)](https://travis-ci.org/angular-translate/angular-translate) ![License](https://img.shields.io/npm/l/angular-translate.svg) ![Code Climate](https://img.shields.io/codeclimate/github/angular-translate/angular-translate.svg) ![Code Coverage](https://img.shields.io/codeclimate/coverage/github/angular-translate/angular-translate.svg)

This is the repository for angular-translate.

angular-translate is a JavaScript translation library for AngularJS 1.x app.

For more information about the angular-translate project, please visit our [website](https://angular-translate.github.io).

## Status
| Branch        | Status         |
| ------------- |:-------------:|
| master        | [![Build Status](https://travis-ci.org/angular-translate/angular-translate.svg?branch=master)](https://travis-ci.org/angular-translate/angular-translate) |
| canary        |[![Build Status](https://travis-ci.org/angular-translate/angular-translate.svg?branch=canary)](https://travis-ci.org/angular-translate/angular-translate)     |

## Install
We strongly *recommend* using a package manager like NPM and Bower, or even variants like Yarn or jspm.

### NPM
```
npm install --save-dev angular-translate
```

### Bower
```
bower install --save-dev angular-translate
```

For more information please visit [chapter "Installation" at our website](https://angular-translate.github.io/docs/#/guide/00_installation).

## Get started
Check out out [chapter "Getting started" at out website](https://angular-translate.github.io/docs/#/guide/02_getting-started).

## Get support
Most of the time, we are getting support questions of invalid configurations. We encourage everyone to have a look at our [documentation website](https://angular-translate.github.io/docs/#/guide). If you think the documentation is not correct (bug) or should be optimized (enhancement) please file an issue.

If you are still having difficulty after looking over your configuration carefully, please post a question to [StackOverflow with a specific tag](http://stackoverflow.com/questions/tagged/angular-translate). Especially if the question are related to AngularJS or even JavaScript/browser basic technologies (maybe your issue is not related to angular-translate after all).

If you have discovered a bug or have a feature suggestion, feel free to create an issue on GitHub. Please follow the guideline within the issue template. See also next headline.

*Please note: We cannot provide support for neither JavaScript nor AngularJS itself. In both cases, a platform like StackOverflow is much more ideal.*

# Contribute
We got a lot of great feedback from the community so far! More and more people
use this module and they are always thankful for it and the awesome support they
get. I just want to make sure that you guys know: All this wouldn't have been
possible without these [great contributors](https://github.com/angular-translate/angular-translate/contributors)
and everybody who comes with new ideas and feature requests! So **THANK YOU**!

Contributing to <code>angular-translate</code> is fairly easy.

[This document](CONTRIBUTING.md) shows you how to
get the project, run all provided tests and generate a production ready build.


## Public talks
[![Dutch AngularJS Meetup 2013](presentation.png)](https://www.youtube.com/watch?v=9CWifOK_Wi8)
[![Kod.io 2014](presentation2.png)](https://www.youtube.com/watch?v=C7xqaExvaQ4)

### Links
* Website [angular-translate.github.io](https://angular-translate.github.io/)
* API Reference [angular-translate.github.io/docs/#/api](https://angular-translate.github.io/docs/#/api)
* [Contribution Guidelines](https://github.com/angular-translate/angular-translate/blob/master/CONTRIBUTING.md)

### Useful resources
There are some very useful things on the web that might be interesting for you,
so make sure to check this list.

- [Tutorial on ng-newsletter.com](http://ng-newsletter.com/posts/angular-translate.html)
- [Examples and demos](https://github.com/angular-translate/angular-translate/wiki/Demos) - Currently on plnkr.co
- [Tutorial on angularjs.de](http://angularjs.de/artikel/angularjs-i18n-ng-translate) - German article
- [angular-translate on GitHub](https://github.com/angular-translate/angular-translate) - The GitHub repository
- [angular-translate on ngmodules.org](http://ngmodules.org/modules/angular-translate)
- [angular-translate mailinglist](https://groups.google.com/forum/#!forum/angular-translate) - Discuss, ask et al!
- [angular-translate-quality](https://www.npmjs.com/package/angular-translate-quality) - Quality check at build time

## Tests

### Unit tests

Note: Check that dependencies are be installed (`npm install`).

The *unit tests* are available with `npm test` which is actually a shortcut for `grunt test`. It performs tests under the current primary target version of AngularJS. Use `npm run-script test-scopes` for testing other scoped versions as well.

## License

Licensed under MIT.
