// Util: PatternFly Sidebar
// Set height of sidebar-pf to height of document minus height of navbar-pf if not mobile
(function ($) {
  'use strict';
  $.fn.sidebar = function () {
    var documentHeight = 0,
      navbarpfHeight = 0,
      colHeight = 0;

    if ($('.navbar-pf .navbar-toggle').is(':hidden')) {
      documentHeight = $(document).height();
      navbarpfHeight = $('.navbar-pf').outerHeight();
      colHeight = documentHeight - navbarpfHeight;
    }
    $('.sidebar-pf').parent('.row').children('[class*="col-"]').css({"min-height" : colHeight});
  };

  $(document).ready(function () {
    // Call sidebar() on ready if .sidebar-pf exists and .datatable does not exist
    if ($('.sidebar-pf').length > 0 && $('.datatable').length === 0) {
      $.fn.sidebar();
    }
  });

  $(window).on('resize', function () {
    // Call sidebar() on resize if .sidebar-pf exists
    if ($('.sidebar-pf').length > 0) {
      $.fn.sidebar();
    }
  });
}(jQuery));
