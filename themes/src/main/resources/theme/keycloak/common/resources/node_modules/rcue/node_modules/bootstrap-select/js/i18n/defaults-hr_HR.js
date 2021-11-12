/*
 * Translated default messages for bootstrap-select.
 * Locale: HR (Croatia)
 * Region: HR (Croatia)
 */
(function ($) {
  $.fn.selectpicker.defaults = {
    noneSelectedText: 'Odaberite stavku',
    noneResultsText: 'Nema rezultata pretrage {0}',
    countSelectedText: function (numSelected, numTotal) {
      return (numSelected == 1) ? "{0} stavka selektirana" : "{0} stavke selektirane";
    },
    maxOptionsText: function (numAll, numGroup) {
      return [
        (numAll == 1) ? 'Limit je postignut ({n} stvar maximalno)' : 'Limit je postignut ({n} stavke maksimalno)',
        (numGroup == 1) ? 'Grupni limit je postignut ({n} stvar maksimalno)' : 'Grupni limit je postignut ({n} stavke maksimalno)'
      ];
    },
    selectAllText: 'Selektiraj sve',
    deselectAllText: 'Deselektiraj sve',
    multipleSeparator: ', '
  };
})(jQuery);
