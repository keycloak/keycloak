/**
 * Select2 Polish translation.
 * 
 * Author: Jan Kondratowicz <jan@kondratowicz.pl>
 */
(function ($) {
    "use strict";
    
    var pl_suffix = function(n) {
        if(n == 1) return "";
        if((n%100 > 1 && n%100 < 5) || (n%100 > 20 && n%10 > 1 && n%10 < 5)) return "i";
        return "ów";
    };

    $.extend($.fn.select2.defaults, {
        formatNoMatches: function () {
            return "Brak wyników.";
        },
        formatInputTooShort: function (input, min) {
            var n = min - input.length;
            return "Wpisz jeszcze " + n + " znak" + pl_suffix(n) + ".";
        },
        formatInputTooLong: function (input, max) {
            var n = input.length - max;
            return "Wpisana fraza jest za długa o " + n + " znak" + pl_suffix(n) + ".";
        },
        formatSelectionTooBig: function (limit) {
            return "Możesz zaznaczyć najwyżej " + limit + " element" + pl_suffix(limit) + ".";
        },
        formatLoadMore: function (pageNumber) {
            return "Ładowanie wyników...";
        },
        formatSearching: function () {
            return "Szukanie...";
        }
    });
})(jQuery);