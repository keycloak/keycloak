/**
 * Select2 <Language> translation.
 * 
 * Author: Your Name <your@email>
 */
(function ($) {
    "use strict";

    $.extend($.fn.select2.defaults, {
        formatNoMatches: function () { return "Δεν βρέθηκαν αποτελέσματα"; },
        formatInputTooShort: function (input, min) { var n = min - input.length; return "Παρακαλούμε εισάγετε " + n + " περισσότερους χαρακτήρες" + (n == 1 ? "" : "s"); },
        formatInputTooLong: function (input, max) { var n = input.length - max; return "Παρακαλούμε διαγράψτε " + n + " χαρακτήρες" + (n == 1 ? "" : "s"); },
        formatSelectionTooBig: function (limit) { return "Μπορείτε να επιλέξετε μόνο " + limit + " αντικείμενο" + (limit == 1 ? "" : "s"); },
        formatLoadMore: function (pageNumber) { return "Φόρτωση περισσότερων..."; },
        formatSearching: function () { return "Αναζήτηση..."; }
    });
})(jQuery);