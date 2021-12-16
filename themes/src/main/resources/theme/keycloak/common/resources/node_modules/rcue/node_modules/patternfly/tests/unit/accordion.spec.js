describe("accordion test suite", function () {

  beforeEach(function () {
    globals.readFixture(globals.fixturePath + 'accordions.html');
  });

  it('should add the "in" class after accordion collapsed', function (done) {
    var accordion = $('a[href="#collapseTwo"]');
    var collapse = $('#collapseTwo');

    expect(collapse).not.toHaveClass('in');

    accordion.click();

    setTimeout(function () {
      expect(collapse).toHaveClass('in');
      done();
    }, globals.wait);
  });

});
