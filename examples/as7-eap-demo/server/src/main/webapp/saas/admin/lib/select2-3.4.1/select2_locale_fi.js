/**
 * Select2 Finnish translation
 */
(function ($) {
    "use strict";
    $.extend($.fn.select2.defaults, {
        formatNoMatches: function () {
            return "Ei tuloksia";
        },
        formatInputTooShort: function (input, min) {
            var n = min - input.length;
            return "Ole hyvä ja anna " + n + " merkkiä lisää.";
        },
        formatInputTooLong: function (input, max) {
            var n = input.length - max;
            return "Ole hyvä ja annar " + n + " merkkiä vähemmän.";
        },
        formatSelectionTooBig: function (limit) {
            return "Voit valita ainoastaan " + limit + " kpl";
        },
        formatLoadMore: function (pageNumber) {
            return "Ladataan lisää tuloksia...";
        },
        formatSearching: function () {
            return "Etsitään...";
        }
    });
})(jQuery);
