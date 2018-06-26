/* eslint-env node */
/* eslint no-console: 0 strict: 0 */
'use strict';

let Liquid = require('liquid-node'),
  fs = require('mz/fs'),
  engine = new Liquid.Engine;

class CustomFileSystem extends Liquid.BlankFileSystem {
  readTemplateFile (path) {
    return fs.readFile(`tests/pages/_includes/${path}`, 'utf8');
  }
}

class StripTag extends Liquid.Block {
  render (context) {
    return super.render(context).then(function (chunks) {
      // let text = chunks.join('');
      let text = flatten(chunks).join('');
      let stripped = String(text).replace(/^\s+|\s+$/g, '');
      return stripped;
    });
  }
}

function flatten (_array) {
  return _array.reduce(function (a, b) {
    if (Array.isArray(b)) {
      return a.concat(flatten(b));
    }
    return a.concat(b);
  }, []);
}

engine = new Liquid.Engine();
engine.registerTag('strip', StripTag);
engine.fileSystem = new CustomFileSystem();

module.exports = engine;
