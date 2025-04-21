# attr-accept
JavaScript implementation of the "accept" attribute for HTML5 `<input type="file">`

[![Build Status](https://travis-ci.org/okonet/attr-accept.svg?branch=master)](https://travis-ci.org/okonet/attr-accept)
[![npm version](https://badge.fury.io/js/attr-accept.svg)](https://badge.fury.io/js/attr-accept)
[![semantic-release](https://img.shields.io/badge/%20%20%F0%9F%93%A6%F0%9F%9A%80-semantic--release-e10079.svg)](https://github.com/semantic-release/semantic-release)

See https://developer.mozilla.org/en-US/docs/Web/HTML/Element/Input#Attributes for more information.

Installation
=====
```sh
npm install --save attr-accept
```

Usage
=====
```javascript
var accept = require('attr-accept');
accept({
    name: 'my file.png',
    type: 'image/png'
}, 'image/*') // => true

accept({
    name: 'my file.json',
    type: 'application/json'
}, 'image/*') // => false

accept({
    name: 'my file.srt',
    type: ''
}, '.srt') // => true
```

You can also pass multiple mime types as a comma delimited string or array.
```javascript
accept({
    name: 'my file.json',
    type: 'application/json'
}, 'application/json,video/*') // => true

accept({
    name: 'my file.json',
    type: 'application/json'
}, ['application/json', 'video/*']) // => true
```
