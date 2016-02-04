/**
 * Select2 lithuanian translation.
 * 
 * Author: CRONUS Karmalakas <cronus dot karmalakas at gmail dot com>
 */
(function ($) {
    "use strict";

    $.extend($.fn.select2.defaults, {
        formatNoMatches: function () { return "Atitikmenų nerasta"; },
        formatInputTooShort: function (input, min) {
        	var n = min - input.length,
        	    suffix = (n % 10 == 1) && (n % 100 != 11) ? 'į' : (((n % 10 >= 2) && ((n % 100 < 10) || (n % 100 >= 20))) ? 'ius' : 'ių');
        	return "Įrašykite dar " + n + " simbol" + suffix;
        },
        formatInputTooLong: function (input, max) {
        	var n = input.length - max,
        	    suffix = (n % 10 == 1) && (n % 100 != 11) ? 'į' : (((n % 10 >= 2) && ((n % 100 < 10) || (n % 100 >= 20))) ? 'ius' : 'ių');
        	return "Pašalinkite " + n + " simbol" + suffix;
        },
        formatSelectionTooBig: function (limit) {
        	var n = limit,
                suffix = (n % 10 == 1) && (n % 100 != 11) ? 'ą' : (((n % 10 >= 2) && ((n % 100 < 10) || (n % 100 >= 20))) ? 'us' : 'ų');
        	return "Jūs galite pasirinkti tik " + limit + " element" + suffix;
        },
        formatLoadMore: function (pageNumber) { return "Kraunama daugiau rezultatų..."; },
        formatSearching: function () { return "Ieškoma..."; }
    });
})(jQuery);
