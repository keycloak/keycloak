/*
 * Translated default messages for bootstrap-select.
 * Locale: FI (Finnish)
 * Region: FI (Finland)
 */
(function ($) {
  $.fn.selectpicker.defaults = {
    noneSelectedText: 'Ei valintoja',
    noneResultsText: 'Ei hakutuloksia {0}',
    countSelectedText: function (numSelected, numTotal) {
      return (numSelected == 1) ? "{0} valittu" : "{0} valitut";
    },
    maxOptionsText: function (numAll, numGroup) {
      return [
        (numAll == 1) ? 'Valintojen maksimimäärä ({n} saavutettu)' : 'Valintojen maksimimäärä ({n} saavutettu)',
        (numGroup == 1) ? 'Ryhmän maksimimäärä ({n} saavutettu)' : 'Ryhmän maksimimäärä ({n} saavutettu)'
      ];
    },
    selectAllText: 'Valitse kaikki',
    deselectAllText: 'Poista kaikki',
    multipleSeparator: ', '
  };
})(jQuery);
