/*
 * Translated default messages for bootstrap-select.
 * Locale: LT (Lithuanian)
 * Region: LT (Lithuania)
 */
(function ($) {
  $.fn.selectpicker.defaults = {
    noneSelectedText: 'Niekas nepasirinkta',
    noneResultsText: 'Niekas nesutapo su {0}',
    countSelectedText: function (numSelected, numTotal) {
      return (numSelected == 1) ? "{0} elementas pasirinktas" : "{0} elementai(-ų) pasirinkta";
    },
    maxOptionsText: function (numAll, numGroup) {
      return [
        (numAll == 1) ? 'Pasiekta riba ({n} elementas daugiausiai)' : 'Riba pasiekta ({n} elementai(-ų) daugiausiai)',
        (numGroup == 1) ? 'Grupės riba pasiekta ({n} elementas daugiausiai)' : 'Grupės riba pasiekta ({n} elementai(-ų) daugiausiai)'
      ];
    },
    selectAllText: 'Pasirinkti visus',
    deselectAllText: 'Atmesti visus',
    multipleSeparator: ', '
  };
})(jQuery);
