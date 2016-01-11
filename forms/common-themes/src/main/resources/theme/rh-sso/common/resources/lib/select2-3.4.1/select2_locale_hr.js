/**
 * Select2 Croatian translation.
 *
 * Author: Edi Modrić <edi.modric@gmail.com>
 */
(function ($) {
    "use strict";

    var specialNumbers = {
        1: function(n) { return (n % 100 != 11 ? "znak" : "znakova"); },
        2: function(n) { return (n % 100 != 12 ? "znaka" : "znakova"); },
        3: function(n) { return (n % 100 != 13 ? "znaka" : "znakova"); },
        4: function(n) { return (n % 100 != 14 ? "znaka" : "znakova"); }
    };

    $.extend($.fn.select2.defaults, {
        formatNoMatches: function () { return "Nema rezultata"; },
        formatInputTooShort: function (input, min) {
            var n = min - input.length;
            var nMod10 = n % 10;

            if (nMod10 > 0 && nMod10 < 5) {
                return "Unesite još " + n + " " + specialNumbers[nMod10](n);
            }

            return "Unesite još " + n + " znakova";
        },
        formatInputTooLong: function (input, max) {
            var n = input.length - max;
            var nMod10 = n % 10;

            if (nMod10 > 0 && nMod10 < 5) {
                return "Unesite " + n + " " + specialNumbers[nMod10](n) + " manje";
            }

            return "Unesite " + n + " znakova manje";
        },
        formatSelectionTooBig: function (limit) { return "Maksimalan broj odabranih stavki je " + limit; },
        formatLoadMore: function (pageNumber) { return "Učitavanje rezultata..."; },
        formatSearching: function () { return "Pretraga..."; }
    });
})(jQuery);
