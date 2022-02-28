var assert = require('assert');
var pipe = require('..');
var Stream = require('stream');
var through = require('through2');

describe('pipe()', function(){
  it('should return a stream', function(done){
    assert(pipe(done));
  });
  it('should accept options', function(){
    assert.equal(pipe({ objectMode: false })._readableState.objectMode, false)
  });
});

describe('pipe(a)', function(){
  it('should pass through to a', function(done){
    Readable().pipe(pipe(Transform())).pipe(Writable(done))
  });
  it('should accept options', function(){
    var readable = Readable({ objectMode: true });
    assert.equal(pipe(readable, { objectMode: false })._readableState.objectMode, false)
  });
});

describe('pipe(a, b, c)', function(){
  it('should pipe internally', function(done){
    pipe(Readable(), Transform(), Writable(done));
  });
  
  it('should be writable', function(done){
    var stream = pipe(Transform(), Writable(done));
    assert(stream.writable);
    Readable().pipe(stream);
  });

  it('should be readable', function(done){
    var stream = pipe(Readable(), Transform());
    assert(stream.readable);
    stream.pipe(Writable(done));
  });
  
  it('should be readable and writable', function(done){
    var stream = pipe(Transform(), Transform());
    assert(stream.readable);
    assert(stream.writable);
    Readable()
    .pipe(stream)
    .pipe(Writable(done));
  });
 
  describe('errors', function(){
    it('should reemit', function(done){
      var a = Transform();
      var b = Transform();
      var c = Transform();
      var stream = pipe(a, b, c);
      var err = new Error;
      var i = 0;
      
      stream.on('error', function(_err){
        i++;
        assert.equal(_err, err);
        assert(i <= 3);
        if (i == 3) done();
      });
      
      a.emit('error', err);
      b.emit('error', err);
      c.emit('error', err);
    });

    it('should not reemit endlessly', function(done){
      var a = Transform();
      var b = Transform();
      var c = Transform();
      c.readable = false;
      var stream = pipe(a, b, c);
      var err = new Error;
      var i = 0;
      
      stream.on('error', function(_err){
        i++;
        assert.equal(_err, err);
        assert(i <= 3);
        if (i == 3) done();
      });
      
      a.emit('error', err);
      b.emit('error', err);
      c.emit('error', err);
    });
  });
  it('should accept options', function(){
    var a = Readable()
    var b = Transform()
    var c = Writable()
    assert.equal(pipe(a, b, c, { objectMode: false })._readableState.objectMode, false)
  });
});

describe('pipe(a, b, c, fn)', function(){
  it('should call on finish', function(done){
    var finished = false;
    var a = Readable();
    var b = Transform();
    var c = Writable(function(){
      finished = true;
    });

    pipe(a, b, c, function(err){
      assert(!err);
      assert(finished);
      done();
    });
  });

  it('should call with error once', function(done){
    var a = Readable();
    var b = Transform();
    var c = Writable();
    var err = new Error;

    pipe(a, b, c, function(err){
      assert(err);
      done();
    });

    a.emit('error', err);
    b.emit('error', err);
    c.emit('error', err);
  });

  it('should call on destroy', function(done){
    var a = Readable();
    var b = Transform();
    var c = through();

    pipe(a, b, c, function(err){
      assert(!err);
      done();
    });

    c.destroy();
  });

  it('should call on destroy with error', function(done){
    var a = Readable();
    var b = Transform();
    var c = through();
    var err = new Error;

    pipe(a, b, c, function(_err){
      assert.equal(_err, err);
      done();
    });

    c.destroy(err);
  });

  it('should accept options', function(done){
    var a = Readable()
    var b = Transform()
    var c = Writable()
    assert.equal(pipe(a, b, c, { objectMode: false }, done)._readableState.objectMode, false)
  });

  it('should ignore parameters on non error events', function(done){
    var a = Readable();
    var b = Transform();
    var c = Writable();
    pipe(a, b, c, done);
    c.emit('finish', true);
  });
});

function Readable(){
  var readable = new Stream.Readable({ objectMode: true });
  readable._read = function(){
    this.push('a');
    this.push(null);
  };
  return readable;
}

function Transform(){
  var transform = new Stream.Transform({ objectMode: true });
  transform._transform = function(chunk, _, done){
    done(null, chunk.toUpperCase());
  };
  return transform;
}

function Writable(cb){
  var writable = new Stream.Writable({ objectMode: true });
  writable._write = function(chunk, _, done){
    assert.equal(chunk, 'A');
    done();
    cb && cb();
  };
  return writable;
}
