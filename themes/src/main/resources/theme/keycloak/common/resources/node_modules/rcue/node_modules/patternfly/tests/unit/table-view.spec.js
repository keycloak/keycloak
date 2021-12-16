describe("data tables test suite", function () {

  beforeEach(function () {
    globals.readFixture(globals.fixturePath + 'table-view.html');

    //run the plugin before each test
    $('#table1').dataTable();
  });

  it('should render a table with striped rows and borders', function (done) {
    var table = $('#table1');

    setTimeout(function () {
      expect(table).toHaveClass('table');
      expect(table).toHaveClass('table-striped');
      expect(table).toHaveClass('table-bordered');
      expect(table).toHaveClass('table-hover');
      done();
    }, globals.wait);
  });
/** Pagination is not currently implemented
  it('should go to page two after clicking next', function (done) {
    var pager = $('.dataTables_paginate ul li.next');
    var page = $('.dataTables_footer .pagination-input input');

    pager.click();

    setTimeout(function () {
      expect(page.val()).toEqual('2');
      done();
    }, globals.wait);
  });
*/
});
