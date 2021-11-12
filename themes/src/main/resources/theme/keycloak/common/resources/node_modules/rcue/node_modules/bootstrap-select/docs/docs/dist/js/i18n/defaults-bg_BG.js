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
    noneSelectedText: 'Нищо избрано',
    noneResultsText: 'Няма резултат за {0}',
    countSelectedText: function (numSelected, numTotal) {
      return (numSelected == 1) ? "{0} избран елемент" : "{0} избрани елемента";
    },
    maxOptionsText: function (numAll, numGroup) {
      return [
        (numAll == 1) ? 'Лимита е достигнат ({n} елемент максимум)' : 'Лимита е достигнат ({n} елемента максимум)',
        (numGroup == 1) ? 'Груповия лимит е достигнат ({n} елемент максимум)' : 'Груповия лимит е достигнат ({n} елемента максимум)'
      ];
    },
    selectAllText: 'Избери всички',
    deselectAllText: 'Размаркирай всички',
    multipleSeparator: ', '
  };
})(jQuery);


}));
