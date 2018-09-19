describe("about-modal suite", function () {

  beforeEach(function () {
    globals.readFixture(globals.fixturePath + 'about-modal.html');
  });

  it('should launch the about modal', function (done) {
    var button = $('button[data-toggle="modal"]');
    var modal = $('.modal');

    //expect modal to be hidden initially
    expect(modal).toBeHidden();

    button.click();

    setTimeout(function () {
      expect(modal).not.toBeHidden();
      done();
    }, globals.wait);
  });

  it('should close the about modal and the backdrop should disappear', function (done) {
    var closeButton = $('button.close');

    closeButton.click();

    setTimeout(function () {
      expect($('.modal')).toBeHidden();

      //manually remove backdrop
      $('.modal-backdrop').remove();
      done();
    }, globals.wait);
  });

});
