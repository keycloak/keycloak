/**
 * Select2 French translation
 */
(function ($) {
    "use strict";

    $.extend($.fn.select2.defaults, {
        formatNoMatches: function () { return "Aucun résultat trouvé"; },
        formatInputTooShort: function (input, min) { var n = min - input.length; return "Merci de saisir " + n + " caractère" + (n == 1? "" : "s") + " de plus"; },
        formatInputTooLong: function (input, max) { var n = input.length - max; return "Merci de supprimer " + n + " caractère" + (n == 1? "" : "s"); },
        formatSelectionTooBig: function (limit) { return "Vous pouvez seulement sélectionner " + limit + " élément" + (limit == 1 ? "" : "s"); },
        formatLoadMore: function (pageNumber) { return "Chargement de résultats supplémentaires..."; },
        formatSearching: function () { return "Recherche en cours..."; }
    });
})(jQuery);
