describe("badges test suite", function () {

  beforeEach(function () {
    globals.readFixture(globals.fixturePath + 'badges.html');
  });

  it('should contain a button with a badge span element', function () {
    var button = $('button');
    var span = $('button span.badge');

    expect(button).toExist();
    expect(span).toHaveClass('badge');
  });

});
