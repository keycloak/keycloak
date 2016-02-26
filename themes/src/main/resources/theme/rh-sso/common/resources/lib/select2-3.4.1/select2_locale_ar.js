/**
 * Select2 Arabic translation.
 * 
 * Author: Your Name <amedhat3@gmail.com>
 */
(function ($) {
    "use strict";

    $.extend($.fn.select2.defaults, {
        formatNoMatches: function () { return "لا توجد نتائج"; },
        formatInputTooShort: function (input, min) { var n = min - input.length; return "من فضلك أدخل " + n + " حروف أكثر"; },
        formatInputTooLong: function (input, max) { var n = input.length - max; return "من فضلك أحذف  " + n + " حروف"; },
        formatSelectionTooBig: function (limit) { return "يمكنك ان تختار " + limit + " أختيارات فقط"; },
        formatLoadMore: function (pageNumber) { return "تحمل المذيد من النتائج ..."; },
        formatSearching: function () { return "جاري البحث ..."; }
    });
})(jQuery);
