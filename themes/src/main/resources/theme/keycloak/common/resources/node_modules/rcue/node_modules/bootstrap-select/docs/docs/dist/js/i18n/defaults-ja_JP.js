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
    noneSelectedText: '何もが選択した',
    noneResultsText: '\'{0}\'が結果を返さない',
    countSelectedText: '{0}/{1}が選択した',
    maxOptionsText: ['限界は達した({n}{var}最大)', '限界をグループは達した({n}{var}最大)', ['アイテム', 'アイテム']],
    selectAllText: '全部を選択する',
    deselectAllText: '何も選択しない',
    multipleSeparator: ', '
  };
})(jQuery);


}));
