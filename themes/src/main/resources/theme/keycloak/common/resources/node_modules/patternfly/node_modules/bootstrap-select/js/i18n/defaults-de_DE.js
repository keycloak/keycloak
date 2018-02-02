/*
 * Translated default messages for bootstrap-select.
 * Locale: DE (German, deutsch)
 * Region: DE (Germany, Deutschland)
 */
(function ($) {
  $.fn.selectpicker.defaults = {
    noneSelectedText: 'Bitte wählen...',
    noneResultsText: 'Keine Ergebnisse für {0}',
    countSelectedText: function (numSelected, numTotal) {
      return (numSelected == 1) ? "{0} Element ausgewählt" : "{0} Elemente ausgewählt";
    },
    maxOptionsText: function (numAll, numGroup) {
      return [
        (numAll == 1) ? 'Limit erreicht ({n} Element max.)' : 'Limit erreicht ({n} Elemente max.)',
        (numGroup == 1) ? 'Gruppen-Limit erreicht ({n} Element max.)' : 'Gruppen-Limit erreicht ({n} Elemente max.)'
      ];
    },
    selectAllText: 'Alles auswählen',
    deselectAllText: 'Nichts auswählen',
    multipleSeparator: ', '
  };
})(jQuery);
