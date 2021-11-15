describe("bootstrap datepicker test suite", function () {

  beforeEach(function () {
    globals.readFixture(globals.fixturePath + 'bootstrap-datepicker.html');
  });

  it('should open the first datepicker after show', function (done) {

    var datePicker1 = $('#datepicker1');

    datePicker1.datepicker({
      autoclose: true,
      orientation: "top auto",
      todayBtn: "linked",
      todayHighlight: true
    });

    datePicker1.datepicker('show');

    setTimeout(function () {
      expect($('.datepicker-dropdown')).toExist();

      //close datepicker1
      datePicker1.datepicker('hide');
      expect($('.datepicker-dropdown')).not.toExist();

      done();
    }, globals.wait);

  });

});
