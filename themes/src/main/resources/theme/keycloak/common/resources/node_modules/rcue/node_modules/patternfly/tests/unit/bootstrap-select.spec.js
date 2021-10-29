describe("bootstrap-select test suite", function () {

  beforeEach(function () {
    globals.readFixture(globals.fixturePath + 'bootstrap-select.html');
  });

  it('should use the plugin to take the first select and add a dropdown menu list with 11 items', function (done) {
    var select = $('.selectpicker:first');

    //render the select using the plugin
    select.selectpicker();

    setTimeout(function () {
      var renderedMenuList = select.siblings().find('.dropdown-menu li');
      expect(renderedMenuList).toHaveLength(11);
      done();
    }, globals.wait);
  });

});
