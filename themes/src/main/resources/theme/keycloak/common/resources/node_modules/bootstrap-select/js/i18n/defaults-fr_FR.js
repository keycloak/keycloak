/*
 * Translated default messages for bootstrap-select.
 * Locale: FR (French; Français)
 * Region: FR (France)
 */
(function ($) {
  $.fn.selectpicker.defaults = {
    noneSelectedText: 'Aucune sélection',
    noneResultsText: 'Aucun résultat pour {0}',
    countSelectedText: function (numSelected, numTotal) {
      return (numSelected > 1) ? "{0} éléments sélectionnés" : "{0} élément sélectionné";
    },
    maxOptionsText: function (numAll, numGroup) {
      return [
        (numAll > 1) ? 'Limite atteinte ({n} éléments max)' : 'Limite atteinte ({n} élément max)',
        (numGroup > 1) ? 'Limite du groupe atteinte ({n} éléments max)' : 'Limite du groupe atteinte ({n} élément max)'
      ];
    },
    multipleSeparator: ', ',
    selectAllText: 'Tout Sélectionner',
    deselectAllText: 'Tout Dé-selectionner',
  };
})(jQuery);
