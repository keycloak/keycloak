/*
 * Translated default messages for bootstrap-select.
 * Locale: IT (Italian; italiano)
 * Region: IT (Italy; Italia)
 * Author: Michele Beltrame <mb@cattlegrid.info>
 */
(function ($) {
  $.fn.selectpicker.defaults = {
    noneSelectedText: 'Nessuna selezione',
    noneResultsText: 'Nessun risultato per {0}',
    countSelectedText: function (numSelected, numTotal){
      return (numSelected == 1) ? 'Selezionato {0} di {1}' : 'Selezionati {0} di {1}';
    },
    maxOptionsText: ['Limite raggiunto ({n} {var} max)', 'Limite del gruppo raggiunto ({n} {var} max)', ['elementi', 'elemento']],
    multipleSeparator: ', ',
    selectAllText: 'Seleziona Tutto',
    deselectAllText: 'Deseleziona Tutto'
  };
})(jQuery);
