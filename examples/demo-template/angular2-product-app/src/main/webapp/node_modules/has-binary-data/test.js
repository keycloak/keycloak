
var hasBinary = require('./');
var assert = require('better-assert');
var fs = require('fs');

var start = new Date();

describe('has-binarydata', function(){

  it('should work with buffer', function(){
    assert(hasBinary(fs.readFileSync('./test.js')));
  });

  it('should work with an array that does not contain binary', function() {
    var arr = [1, 'cool', 2];
    assert(!hasBinary(arr));
  });

  it('should work with an array that contains a buffer', function() {
    var arr = [1, new Buffer('asdfasdf', 'utf8'), 2];
    assert(hasBinary(arr));
  });

  it('should work with an object that does not contain binary', function() {
    var ob = {a: 'a', b: [], c: 1234};
    assert(!hasBinary(ob));
  });

  it('should work with an object that contains a buffer', function() {
    var ob = {a: 'a', b: new Buffer('abc'), c: 1234};
    assert(hasBinary(ob));
  });

  it('should work with null', function() {
    assert(!hasBinary(null));
  });

  it('should work with undefined', function() {
    assert(!hasBinary(undefined));
  });

  it('should work with a complex object that contains undefined and no binary', function() {
    var ob = {
      x: ['a', 'b', 123],
      y: undefined,
      z: {a: 'x', b: 'y', c: 3, d: null},
      w: []
    };
    assert(!hasBinary(ob));
  });

  it('should work with a complex object that contains undefined and binary', function() {
    var ob = {
      x: ['a', 'b', 123],
      y: undefined,
      z: {a: 'x', b: 'y', c: 3, d: null},
      w: [],
      bin: new Buffer('xxx')
    };
    assert(hasBinary(ob));
  });

  it('should handle a very large json object with no binary', function(done) {
    this.timeout();
    fs.readFile(__dirname + '/fixtures/big.json', function(err, data) {
      if (err) {
        console.log(err);
        assert(false);
      }
      data = JSON.parse(data);
      assert(!hasBinary(data));
      done();
    });
  });

  it('should handle a very large json object with binary', function(done) {
    this.timeout();
    fs.readFile(__dirname + '/fixtures/big.json', function(err, data) {
      if (err) {
        console.log(err);
        assert(false);
      }
      var ob = JSON.parse(data);
      ob.bin = {bin: {bin: {bin: new Buffer('abc')}}};
      assert(hasBinary(ob));
      done();
    });
  });

  if (global.ArrayBuffer) {
      it('should work with an ArrayBuffer', function() {
        assert(hasBinary(new ArrayBuffer()));
      });
  }

  if (global.Blob) {
     it('should work with a Blob', function() {
        assert(hasBinary(new Blob()));
     });
  }

  it('should print the test time', function() {
    var end = new Date();
    var diff = end - start;
    console.log('\ntest time: ' + diff + ' ms');
  });

});
