describe("bootstrap-combobox test suite", function () {

  beforeAll(function () {
    globals.readFixture(globals.fixturePath + 'bootstrap-combobox.html');

    //render the combobox using the plugin
    $('.combobox').combobox();
  });


  it('should use the plugin to take the first select element and render a menu list with 51 items', function (done) {
    var select = $('select.combobox:first');
    var toggle = $('.dropdown-toggle:first');

    toggle.click();

    setTimeout(function () {
      var renderedMenuList = select.siblings().find('div.input-group ul li');
      expect(renderedMenuList).toHaveLength(51);
      done();
    }, globals.wait);
  });


});
