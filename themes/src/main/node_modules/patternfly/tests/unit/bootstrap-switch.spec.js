describe("bootstrap switch test suite", function () {

  beforeEach(function () {
    globals.readFixture(globals.fixturePath + 'bootstrap-switch.html');

    //initialize switches with the plugin
    $('#switch-state').bootstrapSwitch();

    //initialize switch listener
    $("[data-switch-set]").on("click", function () {
      var type;
      type = $(this).data("switch-set");
      return $("#switch-" + type).bootstrapSwitch(type, $(this).data("switch-value"));
    });
  });

  it('should toggle the first switch to OFF', function (done) {

    var offButton = $('.btn-group:first button[data-switch-value="false"]');

    offButton.click();

    setTimeout(function () {
      var switch1 = $('.bootstrap-switch:first');
      expect(switch1).toHaveClass('bootstrap-switch-off');
      done();
    }, globals.wait);

  });

  it('should toggle the first switch to ON', function (done) {

    var onButton = $('.btn-group:first button[data-switch-value="true"]');

    onButton.click();

    setTimeout(function () {
      var switch1 = $('.bootstrap-switch:first');
      expect(switch1).toHaveClass('bootstrap-switch-on');
      done();
    }, globals.wait);

  });

});
