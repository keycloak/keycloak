node-filesaver
==============

Manage collections of files easily with node.js

[![Build Status](https://travis-ci.org/jacoborus/node-filesaver.svg?branch=master)](https://travis-ci.org/jacoborus/node-filesaver)


## Installation

Install with npm
```
$ npm install filesaver
```


## Features

- Add files avoiding duplicated names
- Put files overwriting old ones if exists
- Use safe file names

## Example

```js
var Filesaver = require( 'filesaver' );

var folders = {
	images: './images'
}

var filesaver = new Filesaver({ folders: folders, safenames: true });

filesaver.add( 'images', ./path/to/file.jpg, 'photo.jpg', function (err, data) {
    console.log( data );
    // => {filename: 'photo_2.jpg', filepath: './images/photo_2.jpg'}
});
```

