safename
========

Get safe file name from a given string.

![tests badge](https://travis-ci.org/jacoborus/safename.svg?branch=master)


## Example

```js
// require only in node/browserify
var safename = require( 'safename' );

safename('my file name.txt');
// => my_file_name.txt

safename('my file name.txt', '-');
// => my-file-name.txt

safename.low('my file name.txt');
// => my_file_name.txt

safename.middle('my file name.txt', '-');
// => my-file-name.txt

safename.dot('my file name.txt', '.');
// => my.file.name.txt
```



## Installation

Install with npm
```
$ npm install safename
```

Install with Bower
```
$ bower install safename
```


safename API
============


- [safename](#safename)
- [low](#low)
- [middle](#middle)
- [dot](#dot)

<a name="safename"></a>
safename( name, space )
------------------------------------------------------------

Get safe name for files

**Parameters:**

- **name** *String*: string to transform
- **space** *String*: replace for spaces. Optional, low dash (&#x27;_&#x27;) by default
- **Return** *String*: safe name




<a name="low"></a>
low(  )
------------------------------------------------------------

Safe name with low dash '_'.

**Parameters:**



Same as `safename('your file name.txt', '_');`

<a name="middle"></a>
middle(  )
------------------------------------------------------------

Safe name with middle dash '-'.

**Parameters:**



Same as `safename('your file name.txt', '-');`

<a name="dot"></a>
dot(  )
------------------------------------------------------------

Safe name with dots '.'.

**Parameters:**



Same as `safename('your file name.txt', '.');`




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

Released under [MIT License](https://raw.github.com/jacoborus/safename/master/LICENSE)