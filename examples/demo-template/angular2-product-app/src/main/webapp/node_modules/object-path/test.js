var expect = require('chai').expect,
  objectPath = require('./index.js');


function getTestObj() {
  return {
    a: 'b',
    b: {
      c: [],
      d: ['a', 'b'],
      e: [{},{f: 'g'}],
      f: 'i'
    }
  };
}

describe('get', function() {
  it('should return the value under shallow object', function() {
    var obj = getTestObj();
    expect(objectPath.get(obj, 'a')).to.be.equal('b');
    expect(objectPath.get(obj, ['a'])).to.be.equal('b');
  });

  it('should work with number path', function() {
    var obj = getTestObj();
    expect(objectPath.get(obj.b.d, 0)).to.be.equal('a');
    expect(objectPath.get(obj.b, 0)).to.be.equal(void 0);
  });

  it('should return the value under deep object', function() {
    var obj = getTestObj();
    expect(objectPath.get(obj, 'b.f')).to.be.equal('i');
    expect(objectPath.get(obj, ['b','f'])).to.be.equal('i');
  });

  it('should return the value under array', function() {
    var obj = getTestObj();
    expect(objectPath.get(obj, 'b.d.0')).to.be.equal('a');
    expect(objectPath.get(obj, ['b','d',0])).to.be.equal('a');
  });

  it('should return the value under array deep', function() {
    var obj = getTestObj();
    expect(objectPath.get(obj, 'b.e.1.f')).to.be.equal('g');
    expect(objectPath.get(obj, ['b','e',1,'f'])).to.be.equal('g');
  });

  it('should return undefined for missing values under object', function() {
    var obj = getTestObj();
    expect(objectPath.get(obj, 'a.b')).to.not.exist;
    expect(objectPath.get(obj, ['a','b'])).to.not.exist;
  });

  it('should return undefined for missing values under array', function() {
    var obj = getTestObj();
    expect(objectPath.get(obj, 'b.d.5')).to.not.exist;
    expect(objectPath.get(obj, ['b','d','5'])).to.not.exist;
  });

  it('should return the value under integer-like key', function() {
    var obj = { '1a': 'foo' };
    expect(objectPath.get(obj, '1a')).to.be.equal('foo');
    expect(objectPath.get(obj, ['1a'])).to.be.equal('foo');
  });

  it('should return the default value when the key doesnt exist', function() {
    var obj = { '1a': 'foo' };
    expect(objectPath.get(obj, '1b', null)).to.be.equal(null);
    expect(objectPath.get(obj, ['1b'], null)).to.be.equal(null);
  });

  it('should return the default value when path is empty', function() {
    var obj = { '1a': 'foo' };
    expect(objectPath.get(obj, '', null)).to.be.deep.equal({ '1a': 'foo' });
    expect(objectPath.get(obj, [])).to.be.deep.equal({ '1a': 'foo' });
    expect(objectPath.get({  }, ['1'])).to.be.equal(undefined);
  });

  it('should skip non own properties with isEmpty', function(){
    var Base = function(enabled){ };
    Base.prototype = {
      one: {
        two: true
      }
    };
    var Extended = function(){
      Base.call(this,  true);
    };
    Extended.prototype = Object.create(Base.prototype);

    var extended = new Extended();

    expect(objectPath.get(extended, ['one','two'])).to.be.equal(undefined);
    extended.enabled = true;

    expect(objectPath.get(extended, 'enabled')).to.be.equal(true);
  });
});


describe('set', function() {
  it('should set value under shallow object', function() {
    var obj = getTestObj();
    objectPath.set(obj, 'c', {m: 'o'});
    expect(obj).to.have.deep.property('c.m', 'o');
    obj = getTestObj();
    objectPath.set(obj, ['c'], {m: 'o'});
    expect(obj).to.have.deep.property('c.m', 'o');
  });

  it('should set value using number path', function() {
    var obj = getTestObj();
    objectPath.set(obj.b.d, 0, 'o');
    expect(obj).to.have.deep.property('b.d.0', 'o');
  });

  it('should set value under deep object', function() {
    var obj = getTestObj();
    objectPath.set(obj, 'b.c', 'o');
    expect(obj).to.have.deep.property('b.c', 'o');
    obj = getTestObj();
    objectPath.set(obj, ['b','c'], 'o');
    expect(obj).to.have.deep.property('b.c', 'o');
  });

  it('should set value under array', function() {
    var obj = getTestObj();
    objectPath.set(obj, 'b.e.1.g', 'f');
    expect(obj).to.have.deep.property('b.e.1.g', 'f');
    obj = getTestObj();
    objectPath.set(obj, ['b','e',1,'g'], 'f');
    expect(obj).to.have.deep.property('b.e.1.g', 'f');
  });

  it('should create intermediate objects', function() {
    var obj = getTestObj();
    objectPath.set(obj, 'c.d.e.f', 'l');
    expect(obj).to.have.deep.property('c.d.e.f', 'l');
    obj = getTestObj();
    objectPath.set(obj, ['c','d','e','f'], 'l');
    expect(obj).to.have.deep.property('c.d.e.f', 'l');
  });

  it('should create intermediate arrays', function() {
    var obj = getTestObj();
    objectPath.set(obj, 'c.0.1.m', 'l');
    expect(obj.c).to.be.an('array');
    expect(obj.c[0]).to.be.an('array');
    expect(obj).to.have.deep.property('c.0.1.m', 'l');
    obj = getTestObj();
    objectPath.set(obj, ['c','0', 1,'m'], 'l');
    expect(obj.c).to.be.an('object');
    expect(obj.c[0]).to.be.an('array');
    expect(obj).to.have.deep.property('c.0.1.m', 'l');
  });

  it('should set value under integer-like key', function() {
    var obj = getTestObj();
    objectPath.set(obj, '1a', 'foo');
    expect(obj).to.have.deep.property('1a', 'foo');
    obj = getTestObj();
    objectPath.set(obj, ['1a'], 'foo');
    expect(obj).to.have.deep.property('1a', 'foo');
  });

  it('should set value under empty array', function() {
    var obj = [];
    objectPath.set(obj, [0], 'foo');
    expect(obj[0]).to.be.equal('foo');
    obj = [];
    objectPath.set(obj, '0', 'foo');
    expect(obj[0]).to.be.equal('foo');
  });
});


describe('push', function() {
  it('should push value to existing array', function() {
    var obj = getTestObj();
    objectPath.push(obj, 'b.c', 'l');
    expect(obj).to.have.deep.property('b.c.0', 'l');
    obj = getTestObj();
    objectPath.push(obj, ['b','c'], 'l');
    expect(obj).to.have.deep.property('b.c.0', 'l');
  });

  it('should push value to new array', function() {
    var obj = getTestObj();
    objectPath.push(obj, 'b.h', 'l');
    expect(obj).to.have.deep.property('b.h.0', 'l');
    obj = getTestObj();
    objectPath.push(obj, ['b','h'], 'l');
    expect(obj).to.have.deep.property('b.h.0', 'l');
  });

  it('should push value to existing array using number path', function() {
    var obj = getTestObj();
    objectPath.push(obj.b.e, 0, 'l');
    expect(obj).to.have.deep.property('b.e.0.0', 'l');
  });

});


describe('ensureExists', function() {
  it('should create the path if it does not exists', function() {
    var obj = getTestObj();
    var oldVal = objectPath.ensureExists(obj, 'b.g.1.l', 'test');
    expect(oldVal).to.not.exist;
    expect(obj).to.have.deep.property('b.g.1.l', 'test');
    oldVal = objectPath.ensureExists(obj, 'b.g.1.l', 'test1');
    expect(oldVal).to.be.equal('test');
    expect(obj).to.have.deep.property('b.g.1.l', 'test');
  });


  it('should return the object if path is empty', function() {
    var obj = getTestObj();
    expect(objectPath.ensureExists(obj, [], 'test')).to.have.property('a', 'b');
  });

  it('Issue #26', function() {
    var any = {};
    objectPath.ensureExists(any, ['1','1'], {});
    expect(any).to.be.an('object');
    expect(any[1]).to.be.an('object');
    expect(any[1][1]).to.be.an('object');
  });
});

describe('coalesce', function(){
  it('should return the first non-undefined value', function(){
    var obj = {
      should: {have: 'prop'}
    };

    expect(objectPath.coalesce(obj, [
      'doesnt.exist',
      ['might','not','exist'],
      'should.have'
    ])).to.equal('prop');
  });

  it('should work with falsy values (null, 0, \'\', false)', function(){
    var obj = {
      is: {
        false: false,
        null: null,
        empty: '',
        zero: 0
      }
    };

    expect(objectPath.coalesce(obj, [
      'doesnt.exist',
      'is.zero'
    ])).to.equal(0);

    expect(objectPath.coalesce(obj, [
      'doesnt.exist',
      'is.false'
    ])).to.equal(false);

    expect(objectPath.coalesce(obj, [
      'doesnt.exist',
      'is.null'
    ])).to.equal(null);

    expect(objectPath.coalesce(obj, [
      'doesnt.exist',
      'is.empty'
    ])).to.equal('');
  });

  it('returns defaultValue if no paths found', function(){
    var obj = {
      doesnt: 'matter'
    };

    expect(objectPath.coalesce(obj, ['some.inexistant','path',['on','object']], 'false')).to.equal('false');
  });
});

describe('empty', function(){
  it('should ignore invalid arguments safely', function(){
    var obj = {};
    expect(objectPath.empty()).to.equal(void 0);
    expect(objectPath.empty(obj, 'path')).to.equal(void 0);
    expect(objectPath.empty(obj, '')).to.equal(obj);

    obj.path = true;

    expect(objectPath.empty(obj, 'inexistant')).to.equal(obj);
  });

  it('should empty each path according to their types', function(){
    function Instance(){
      this.notOwn = true;
    }

    /*istanbul ignore next: not part of code */
    Instance.prototype.test = function(){};
    /*istanbul ignore next: not part of code */
    Instance.prototype.arr = [];

    var
      obj = {
        string: 'some string',
        array: ['some','array',[1,2,3]],
        number: 21,
        boolean: true,
        object: {
          some:'property',
          sub: {
            'property': true
          }
        },
        instance: new Instance()
      };

    /*istanbul ignore next: not part of code */
    obj['function'] = function(){};

    objectPath.empty(obj, ['array','2']);
    expect(obj.array[2]).to.deep.equal([]);

    objectPath.empty(obj, 'object.sub');
    expect(obj.object.sub).to.deep.equal({});

    objectPath.empty(obj, 'instance.test');
    expect(obj.instance.test).to.equal(null);
    expect(Instance.prototype.test).to.be.a('function');

    objectPath.empty(obj, 'string');
    objectPath.empty(obj, 'number');
    objectPath.empty(obj, 'boolean');
    objectPath.empty(obj, 'function');
    objectPath.empty(obj, 'array');
    objectPath.empty(obj, 'object');
    objectPath.empty(obj, 'instance');

    expect(obj.string).to.equal('');
    expect(obj.array).to.deep.equal([]);
    expect(obj.number).to.equal(0);
    expect(obj.boolean).to.equal(false);
    expect(obj.object).to.deep.equal({});
    expect(obj.instance.notOwn).to.be.an('undefined');
    expect(obj.instance.arr).to.be.an('array');
    expect(obj['function']).to.equal(null);
  });
});

describe('del', function(){
  it('should return undefined on empty object', function(){
    expect(objectPath.del({}, 'a')).to.equal(void 0);
  });

  it('should work with number path', function(){
    var obj = getTestObj();
    objectPath.del(obj.b.d, 1);
    expect(obj.b.d).to.deep.equal(['a']);
  });

  it('should delete deep paths', function(){
    var obj = getTestObj();

    expect(objectPath.del(obj)).to.be.equal(obj);

    objectPath.set(obj, 'b.g.1.0', 'test');
    objectPath.set(obj, 'b.g.1.1', 'test');
    objectPath.set(obj, 'b.h.az', 'test');

    expect(obj).to.have.deep.property('b.g.1.0','test');
    expect(obj).to.have.deep.property('b.g.1.1','test');
    expect(obj).to.have.deep.property('b.h.az','test');

    objectPath.del(obj, 'b.h.az');
    expect(obj).to.not.have.deep.property('b.h.az');
    expect(obj).to.have.deep.property('b.h');

    objectPath.del(obj, 'b.g.1.1');
    expect(obj).to.not.have.deep.property('b.g.1.1');
    expect(obj).to.have.deep.property('b.g.1.0','test');

    objectPath.del(obj, ['b','g','1','0']);
    expect(obj).to.not.have.deep.property('b.g.1.0');
    expect(obj).to.have.deep.property('b.g.1');

    expect(objectPath.del(obj, ['b'])).to.not.have.deep.property('b.g');
    expect(obj).to.be.deep.equal({'a':'b'});
  });

  it('should remove items from existing array', function(){
    var obj = getTestObj();

    objectPath.del(obj, 'b.d.0');
    expect(obj.b.d).to.have.length(1);
    expect(obj.b.d).to.be.deep.equal(['b']);

    objectPath.del(obj, 'b.d.0');
    expect(obj.b.d).to.have.length(0);
    expect(obj.b.d).to.be.deep.equal([]);
  });

  it('should skip undefined paths', function(){
    var obj = getTestObj();

    expect(objectPath.del(obj, 'do.not.exist')).to.be.equal(obj);
    expect(objectPath.del(obj, 'a.c')).to.be.equal('b');
  });
});

describe('insert', function(){
  it('should insert value into existing array', function(){
    var obj = getTestObj();

    objectPath.insert(obj, 'b.c', 'asdf');
    expect(obj).to.have.deep.property('b.c.0', 'asdf');
    expect(obj).to.not.have.deep.property('b.c.1');
  });

  it('should create intermediary array', function(){
    var obj = getTestObj();

    objectPath.insert(obj, 'b.c.0', 'asdf');
    expect(obj).to.have.deep.property('b.c.0.0', 'asdf');
  });

  it('should insert in another index', function(){
    var obj = getTestObj();

    objectPath.insert(obj, 'b.d', 'asdf', 1);
    expect(obj).to.have.deep.property('b.d.1', 'asdf');
    expect(obj).to.have.deep.property('b.d.0', 'a');
    expect(obj).to.have.deep.property('b.d.2', 'b');
  });

  it('should handle sparse array', function(){
    var obj = getTestObj();
    obj.b.d = new Array(4);
    obj.b.d[0] = 'a';
    obj.b.d[1] = 'b';

    objectPath.insert(obj, 'b.d', 'asdf', 3);
    expect(obj.b.d).to.have.members([
      'a',
      'b',
      ,
      ,
      'asdf'
    ]);
  });
});

describe('has', function () {
  it('should return false for empty object', function () {
    expect(objectPath.has({}, 'a')).to.be.false;
  });

  it('should return false for empty path', function () {
    var obj = getTestObj();
    expect(objectPath.has(obj, '')).to.be.false;
    expect(objectPath.has(obj, [])).to.be.false;
    expect(objectPath.has(obj, [''])).to.be.false;
  });

  it('should test under shallow object', function() {
    var obj = getTestObj();
    expect(objectPath.has(obj, 'a')).to.be.true;
    expect(objectPath.has(obj, ['a'])).to.be.true;
    expect(objectPath.has(obj, 'z')).to.be.false;
    expect(objectPath.has(obj, ['z'])).to.be.false;
  });

  it('should work with number path', function() {
    var obj = getTestObj();
    expect(objectPath.has(obj.b.d, 0)).to.be.true;
    expect(objectPath.has(obj.b, 0)).to.be.false;
    expect(objectPath.has(obj.b.d, 10)).to.be.false;
    expect(objectPath.has(obj.b, 10)).to.be.false;
  });

  it('should test under deep object', function() {
    var obj = getTestObj();
    expect(objectPath.has(obj, 'b.f')).to.be.true;
    expect(objectPath.has(obj, ['b','f'])).to.be.true;
    expect(objectPath.has(obj, 'b.g')).to.be.false;
    expect(objectPath.has(obj, ['b','g'])).to.be.false;
  });

  it('should test value under array', function() {
    var obj = getTestObj();
    expect(objectPath.has(obj, 'b.d.0')).to.be.true;
    expect(objectPath.has(obj, ['b','d',0])).to.be.true;
  });

  it('should test the value under array deep', function() {
    var obj = getTestObj();
    expect(objectPath.has(obj, 'b.e.1.f')).to.be.true;
    expect(objectPath.has(obj, ['b','e',1,'f'])).to.be.true;
    expect(objectPath.has(obj, 'b.e.1.f.g.h.i')).to.be.false;
    expect(objectPath.has(obj, ['b','e',1,'f','g','h','i'])).to.be.false;
  });

  it('should test the value under integer-like key', function() {
    var obj = { '1a': 'foo' };
    expect(objectPath.has(obj, '1a')).to.be.true;
    expect(objectPath.has(obj, ['1a'])).to.be.true;
  });

  it('should distinct nonexistent key and key = undefined', function() {
    var obj = {};
    expect(objectPath.has(obj, 'key')).to.be.false;

    obj.key = undefined;
    expect(objectPath.has(obj, 'key')).to.be.true;
  });
});



describe('bind object', function () {
  // just get one scenario from each feature, so whole functionality is proxied well
  it('should return the value under shallow object', function() {
    var obj = getTestObj();
    var model = objectPath(obj);
    expect(model.get('a')).to.be.equal('b');
    expect(model.get(['a'])).to.be.equal('b');
  });

  it('should set value under shallow object', function() {
    var obj = getTestObj();
    var model = objectPath(obj);
    model.set('c', {m: 'o'});
    expect(obj).to.have.deep.property('c.m', 'o');
    obj = getTestObj();
    model = objectPath(obj);
    model.set(['c'], {m: 'o'});
    expect(obj).to.have.deep.property('c.m', 'o');
  });

  it('should push value to existing array', function() {
    var obj = getTestObj();
    var model = objectPath(obj);
    model.push('b.c', 'l');
    expect(obj).to.have.deep.property('b.c.0', 'l');
    obj = getTestObj();
    model = objectPath(obj);
    model.push(['b','c'], 'l');
    expect(obj).to.have.deep.property('b.c.0', 'l');
  });

  it('should create the path if it does not exists', function() {
    var obj = getTestObj();
    var model = objectPath(obj);
    var oldVal = model.ensureExists('b.g.1.l', 'test');
    expect(oldVal).to.not.exist;
    expect(obj).to.have.deep.property('b.g.1.l', 'test');
    oldVal = model.ensureExists('b.g.1.l', 'test1');
    expect(oldVal).to.be.equal('test');
    expect(obj).to.have.deep.property('b.g.1.l', 'test');
  });

  it('should return the first non-undefined value', function(){
    var obj = {
      should: {have: 'prop'}
    };
    var model = objectPath(obj);

    expect(model.coalesce([
      'doesnt.exist',
      ['might','not','exist'],
      'should.have'
    ])).to.equal('prop');
  });

  it('should empty each path according to their types', function(){
    function Instance(){
      this.notOwn = true;
    }

    /*istanbul ignore next: not part of code */
    Instance.prototype.test = function(){};
    /*istanbul ignore next: not part of code */
    Instance.prototype.arr = [];

    var
      obj = {
        string: 'some string',
        array: ['some','array',[1,2,3]],
        number: 21,
        boolean: true,
        object: {
          some:'property',
          sub: {
            'property': true
          }
        },
        instance: new Instance()
      };

    /*istanbul ignore next: not part of code */
    obj['function'] = function(){};

    var model = objectPath(obj);

    model.empty(['array','2']);
    expect(obj.array[2]).to.deep.equal([]);

    model.empty('object.sub');
    expect(obj.object.sub).to.deep.equal({});

    model.empty('instance.test');
    expect(obj.instance.test).to.equal(null);
    expect(Instance.prototype.test).to.be.a('function');

    model.empty('string');
    model.empty('number');
    model.empty('boolean');
    model.empty('function');
    model.empty('array');
    model.empty('object');
    model.empty('instance');

    expect(obj.string).to.equal('');
    expect(obj.array).to.deep.equal([]);
    expect(obj.number).to.equal(0);
    expect(obj.boolean).to.equal(false);
    expect(obj.object).to.deep.equal({});
    expect(obj.instance.notOwn).to.be.an('undefined');
    expect(obj.instance.arr).to.be.an('array');
    expect(obj['function']).to.equal(null);
  });

  it('should delete deep paths', function(){
    var obj = getTestObj();
    var model = objectPath(obj);

    expect(model.del()).to.be.equal(obj);

    model.set('b.g.1.0', 'test');
    model.set('b.g.1.1', 'test');
    model.set('b.h.az', 'test');

    expect(obj).to.have.deep.property('b.g.1.0','test');
    expect(obj).to.have.deep.property('b.g.1.1','test');
    expect(obj).to.have.deep.property('b.h.az','test');

    model.del('b.h.az');
    expect(obj).to.not.have.deep.property('b.h.az');
    expect(obj).to.have.deep.property('b.h');

    model.del('b.g.1.1');
    expect(obj).to.not.have.deep.property('b.g.1.1');
    expect(obj).to.have.deep.property('b.g.1.0','test');

    model.del(['b','g','1','0']);
    expect(obj).to.not.have.deep.property('b.g.1.0');
    expect(obj).to.have.deep.property('b.g.1');

    expect(model.del(['b'])).to.not.have.deep.property('b.g');
    expect(obj).to.be.deep.equal({'a':'b'});
  });

  it('should insert value into existing array', function(){
    var obj = getTestObj();
    var model = objectPath(obj);

    model.insert('b.c', 'asdf');
    expect(obj).to.have.deep.property('b.c.0', 'asdf');
    expect(obj).to.not.have.deep.property('b.c.1');
  });

  it('should test under shallow object', function() {
    var obj = getTestObj();
    var model = objectPath(obj);

    expect(model.has('a')).to.be.true;
    expect(model.has(['a'])).to.be.true;
    expect(model.has('z')).to.be.false;
    expect(model.has(['z'])).to.be.false;
  });

});
