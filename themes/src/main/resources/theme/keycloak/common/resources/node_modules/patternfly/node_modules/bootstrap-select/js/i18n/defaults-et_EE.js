/*
 * Translated default messages for bootstrap-select.
 * Locale: ET (Eesti keel)
 * Region: EE (Estonia)
 */
(function ($) {
  $.fn.selectpicker.defaults = {
    noneSelectedText: 'Valikut pole tehtud',
    noneResultsText: 'Otsingule {0} ei ole vasteid',
    countSelectedText: function (numSelected, numTotal) {
      return (numSelected == 1) ? "{0} item selected" : "{0} items selected";
    },
    maxOptionsText: function (numAll, numGroup) {
      return [
        'Limiit on {n} max',
        'Globaalne limiit on {n} max'
      ];
    },
    selectAllText: 'Vali kõik',
    deselectAllText: 'Tühista kõik',
    multipleSeparator: ', '
  };
})(jQuery);
