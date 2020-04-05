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

Filesaver API
============


- [Filesaver](#Filesaver)
- [folder](#folder)
- [put](#put)
- [add](#add)

<a name="Filesaver"></a>
Filesaver( options )
------------------------------------------------------------

Filesaver constructor.

**Parameters:**

- **options** *Object*: folders and safenames


Options:

- folders: *Object*		with folder routes
- safename: *Boolean*	use safe name for files

Example:

```js
var folders = {
images: './images',
books: './books'
}
var filesaver = new Filesaver({
folders: folders,
safenames: true
});
```

<a name="folder"></a>
folder( name, path, callback )
------------------------------------------------------------

Add a new folder

**Parameters:**

- **name** *String*: name of new folder collection
- **path** *Object*: path to its folder
- **callback** *Function*: no signature callback


Example:

```js
filesaver.folder( 'documents', './path/to/folder', function () {
// do something
});
```

<a name="put"></a>
put( folder, oldPath, newPath, callback )
------------------------------------------------------------

Write or overwrite file

**Parameters:**

- **folder** *String*: name of parent folder folder
- **oldPath** *String*: path to origin file
- **newPath** *String*: name of newPath file
- **callback** *Function*: Signature: error, data. Data signature:{filename, filepath}


Example:

```js
filesaver.put( 'images', '/path/temp/file.jpg', 'photo.jpg', function (err, data) {
console.log( data );
// ->
// filename: 'photo.jpg',
// filepath: './images/photo.jpg'
});
```

<a name="add"></a>
add( folder, oldPath, newPath, callback )
------------------------------------------------------------

Write a file without overwriting anyone.

**Parameters:**

- **folder** *String*: name of parent folder folder
- **oldPath** *String*: path to origin file
- **newPath** *String*: Optional: name of newPath file
- **callback** *Function*: Optional: Signature: error, data. Data signature:{filename, filepath}


Example:

```js
filesaver.add( 'images', '/path/temp/file.jpg', 'photo_1.jpg', function (err, data) {
console.log( data );
// ->
// filename: 'photo_2.jpg',
// filepath: './images/photo_2.jpg'
});
```




Tests
-----

```
npm install && npm test
```

Build API docs
--------------

```
npm install && npm run build-docs
```


<br><br>

---

© 2014 [jacoborus](https://github.com/jacoborus)

Released under [MIT License](https://raw.github.com/jacoborus/node-filesaver/master/LICENSE)