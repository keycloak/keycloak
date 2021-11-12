/*!
 * Bootstrap-select v1.13.1 (https://developer.snapappointments.com/bootstrap-select)
 *
 * Copyright 2012-2018 SnapAppointments, LLC
 * Licensed under MIT (https://github.com/snapappointments/bootstrap-select/blob/master/LICENSE)
 */

(function (root, factory) {
  if (typeof define === 'function' && define.amd) {
    // AMD. Register as an anonymous module unless amdModuleId is set
    define(["jquery"], function (a0) {
      return (factory(a0));
    });
  } else if (typeof module === 'object' && module.exports) {
    // Node. Does not work with strict CommonJS, but
    // only CommonJS-like environments that support module.exports,
    // like Node.
    module.exports = factory(require("jquery"));
  } else {
    factory(root["jQuery"]);
  }
}(this, function (jQuery) {

(function ($) {
  $.fn.selectpicker.defaults = {
    noneSelectedText: 'មិនមានអ្វីបានជ្រើសរើស',
    noneResultsText: 'មិនមានលទ្ធផល {0}',
    countSelectedText: function (numSelected, numTotal) {
      return (numSelected == 1) ? "{0} ធាតុដែលបានជ្រើស" : "{0} ធាតុដែលបានជ្រើស";
    },
    maxOptionsText: function (numAll, numGroup) {
      return [
        (numAll == 1) ? 'ឈានដល់ដែនកំណត់ ( {n} ធាតុអតិបរមា)' : 'អតិបរមាឈានដល់ដែនកំណត់ ( {n} ធាតុ)',
        (numGroup == 1) ? 'ដែនកំណត់ក្រុមឈានដល់ ( {n} អតិបរមាធាតុ)' : 'អតិបរមាក្រុមឈានដល់ដែនកំណត់ ( {n} ធាតុ)'
      ];
    },
    selectAllText: 'ជ្រើស​យក​ទាំងអស់',
    deselectAllText: 'មិនជ្រើស​យក​ទាំងអស',
    multipleSeparator: ', '
  };
})(jQuery);


}));
