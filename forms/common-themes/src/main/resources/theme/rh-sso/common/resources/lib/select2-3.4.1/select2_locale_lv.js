/**
 * Select2 Latvian translation
 */
(function ($) {
    "use strict";

    $.extend($.fn.select2.defaults, {
        formatNoMatches: function () { return "Sakritību nav"; },
        formatInputTooShort: function (input, min) { var n = min - input.length; return "Lūdzu ievadiet vēl " + n + " simbol" + (n == 11 ? "us" : (/^\d*[1]$/im.test(n)? "u" : "us")); },
        formatInputTooLong: function (input, max) { var n = input.length - max; return "Lūdzu ievadiet par " + n + " simbol" + (n == 11 ? "iem" : (/^\d*[1]$/im.test(n)? "u" : "iem")) + " mazāk"; },
        formatSelectionTooBig: function (limit) { return "Jūs varat izvēlēties ne vairāk kā " + limit + " element" + (limit == 11 ? "us" : (/^\d*[1]$/im.test(limit)? "u" : "us")); },
        formatLoadMore: function (pageNumber) { return "Datu ielāde..."; },
        formatSearching: function () { return "Meklēšana..."; }
    });
	
})(jQuery);
