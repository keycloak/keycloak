//

describe('System', function () {

  describe('#toString', function() {
    it('Module toString is "Module"', function() {
      expect(System.newModule({}).toString()).to.equal('Module');
    });
  });

  describe('#normalize', function () {

    describe('when having no argument', function () {

      it('should throw with no specified name', function () {
        expect(function () { System.normalize(); }).to.throwException();
      });

    });

    describe('when having one argument', function () {

      it('should allow no referer', function () {
        expect(System.normalize('d/e/f')).to.equal(baseURL + 'd/e/f');
      });

      var backTrack
      // in the browser, double backtracking goes below the hostname -> just keep at hostname
      if (typeof window != 'undefined')
        backTrack = baseURI.substr(0, baseURI.length - 1);
      else
        backTrack = baseURI.split('/').splice(0, baseURI.split('/').length - 2).join('/')
      
      if (typeof window != 'undefined')

      it('should backtracking below baseURL', function () {
        expect(System.normalize('../e/f')).to.equal(backTrack + '/e/f');
      });

      it('should double dotted backtracking', function () {
        expect(System.normalize('./../a.js')).to.equal(backTrack + '/a.js');
      });

      it('should normalize ./ and plain names to the same base', function () {
        expect(System.normalize('./a.js')).to.equal(baseURI + 'a.js');
      });

    });

    describe('when having two arguments', function () {

      var refererAddress = 'http://parent.com/dir/file';

      it('should normalize relative paths against the parent name', function () {
        expect(System.normalize('./d/e/f', refererAddress)).to.equal('http://parent.com/dir/d/e/f');
        expect(System.normalize('../e/f', refererAddress)).to.equal('http://parent.com/e/f');
      });

    });
  });

  describe('#locate', function () {

    it('should be the identity function', function () {
      expect(System.locate({name: '@some/name'})).to.equal('@some/name');
    });

  });
});
