/**
 * Select2 Dutch translation
 */
(function ($) {
    "use strict";

    $.extend($.fn.select2.defaults, {
        formatNoMatches: function () { return "Geen resultaten gevonden"; },
        formatInputTooShort: function (input, min) { var n = min - input.length; return "Vul " + n + " karakter" + (n == 1? "" : "s") + " meer in"; },
        formatInputTooLong: function (input, max) { var n = input.length - max; return "Vul " + n + " karakter" + (n == 1? "" : "s") + " minder in"; },
        formatSelectionTooBig: function (limit) { return "Maximaal " + limit + " item" + (limit == 1 ? "" : "s") + " toegestaan"; },
        formatLoadMore: function (pageNumber) { return "Meer resultaten laden..."; },
        formatSearching: function () { return "Zoeken..."; },
    });
})(jQuery);