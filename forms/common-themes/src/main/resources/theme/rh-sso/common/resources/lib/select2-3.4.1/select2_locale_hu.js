/**
 * Select2 Hungarian translation
 */
(function ($) {
    "use strict";

    $.extend($.fn.select2.defaults, {
        formatNoMatches: function () { return "Nincs találat."; },
        formatInputTooShort: function (input, min) { var n = min - input.length; return "Túl rövid. Még " + n + " karakter hiányzik."; },
        formatInputTooLong: function (input, max) { var n = input.length - max; return "Túl hosszú. " + n + " kerekterrel több mint kellene."; },
        formatSelectionTooBig: function (limit) { return "Csak " + limit + " elemet lehet kiválasztani."; },
        formatLoadMore: function (pageNumber) { return "Töltés..."; },
        formatSearching: function () { return "Keresés..."; }
    });
})(jQuery);
