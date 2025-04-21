'use strict';
var itIfSymbols = typeof Symbol === 'function' && typeof Symbol.iterator === 'symbol' ? it : it.skip;

describe('find-package-json', function () {
  var assume = require('assume')
    , path = require('path')
    , find = require('./');

  it('is exported as function', function () {
    assume(find).is.a('function');
  });

  it('returns iterator', function () {
    var f = find(__dirname);

    assume(f).is.a('object');
    assume(f.next).is.a('function');
    assume(f.next()).is.a('object');
    assume(f.next().done).is.a('boolean');
  });

  itIfSymbols('returns an iterable', function () {
    var f = find(__dirname);

    assume(f).is.a('object');
    assume(f[Symbol.iterator]).is.a('function');
    assume(f[Symbol.iterator]()).equals(f);

    var next = f.next();
    assume(next).is.a('object');
    assume(next[Symbol.iterator]).is.a('function');
    assume(next[Symbol.iterator]()).equals(next);
    assume(next.done).equals(false);

    var next = f.next();
    assume(next).is.a('object');
    assume(next[Symbol.iterator]).is.a('function');
    assume(next[Symbol.iterator]()).equals(next);
    assume(next.done).equals(true);
  });

  it('finds the package.json in this directory', function () {
    var f = find(__dirname)
      , data = f.next();

    assume(data.value).is.a('object');
    assume(data.filename).equals(path.join(__dirname, 'package.json'));
    assume(data.value.name).equals('find-package-json');
  });

  it('accepts a filename', function () {
    var f = find(__filename)
      , data = f.next();

    assume(data.value).is.a('object');
    assume(data.filename).equals(path.join(__dirname, 'package.json'));
    assume(data.value.name).equals('find-package-json');
  });

  it('accepts a module object', function () {
    var f = find(require('./fixture/module-test'))
      , data = f.next();

    assume(data.value).is.a('object');
    assume(data.filename).equals(path.join(__dirname, 'fixture', 'package.json'));
    assume(data.value.name).equals('fixture');
  });

  it('returns false when it cannot find more packages', function () {
    var f = find(__dirname);

    while (true) {
      if (f.next().done) break;
    }
  });

  it('supports package.json with BOM', function () {
    var f = find(path.join(__dirname, 'fixture'))
      , data = f.next();

    assume(data.value.name).equals('fixture');
  });

  it('ignores broken fixtures', function () {
    var f = find(path.join(__dirname, 'fixture', 'broken'))
      , data = f.next();

    assume(data.value.name).equals('fixture');
  });
});
