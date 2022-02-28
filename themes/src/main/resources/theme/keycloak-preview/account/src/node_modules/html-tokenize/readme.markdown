# html-tokenize

transform stream to tokenize html

[![build status](https://secure.travis-ci.org/substack/html-tokenize.png)](http://travis-ci.org/substack/html-tokenize)

# example

``` js
var fs = require('fs');
var tokenize = require('html-tokenize');
var through = require('through2');

fs.createReadStream(__dirname + '/table.html')
    .pipe(tokenize())
    .pipe(through.obj(function (row, enc, next) {
        row[1] = row[1].toString();
        console.log(row);
        next();
    }))
;
```

this html:

``` html
<table>
  <tbody>blah blah blah</tbody>
  <tr><td>there</td></tr>
  <tr><td>it</td></tr>
  <tr><td>is</td></tr>
</table>
```

generates this output:

```
[ 'open', '<table>' ]
[ 'text', '\n  ' ]
[ 'open', '<tbody>' ]
[ 'text', 'blah blah blah' ]
[ 'close', '</tbody>' ]
[ 'text', '\n  ' ]
[ 'open', '<tr>' ]
[ 'open', '<td>' ]
[ 'text', 'there' ]
[ 'close', '</td>' ]
[ 'close', '</tr>' ]
[ 'text', '\n  ' ]
[ 'open', '<tr>' ]
[ 'open', '<td>' ]
[ 'text', 'it' ]
[ 'close', '</td>' ]
[ 'close', '</tr>' ]
[ 'text', '\n  ' ]
[ 'open', '<tr>' ]
[ 'open', '<td>' ]
[ 'text', 'is' ]
[ 'close', '</td>' ]
[ 'close', '</tr>' ]
[ 'text', '\n' ]
[ 'close', '</table>' ]
[ 'text', '\n' ]
```

# methods

``` js
var tokenize = require('html-tokenize');
```

## var t = tokenize()

Return a tokenize transform stream `t` that takes html input and produces rows
of output. The output rows are of the form:

* `[ name, buffer ]`

The input stream maps completely onto the buffers from the object stream.

The types of names are:

* open
* close
* text

cdata, comments, and scripts all use `'open'` with their contents appearing in
subsequent `'text'` rows.

# usage

There is an html-tokenize command too.

```
usage: html-tokenize {FILE}

  Tokenize FILE into newline-separated json arrays for each tag.
  If FILE is not specified, use stdin.

```

# install

With [npm](https://npmjs.org), to get the library do:

```
npm install html-tokenize
```

or to get the command do:

```
npm install -g html-tokenize
```

# license

MIT
