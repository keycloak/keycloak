/*
 * Translated default messages for bootstrap-select.
 * Locale: SL (Slovenian)
 * Region: SI (Slovenia)
 */
(function ($) {
  $.fn.selectpicker.defaults = {
    noneSelectedText: 'Nič izbranega',
    noneResultsText: 'Ni zadetkov za {0}',
    countSelectedText: '{0} od {1} izbranih',
    maxOptionsText: function (numAll, numGroup) {
      return [
        'Omejitev dosežena (max. izbranih: {n})',
        'Omejitev skupine dosežena (max. izbranih: {n})'
      ];
    },
    selectAllText: 'Izberi vse',
    deselectAllText: 'Počisti izbor',
    multipleSeparator: ', '
  };
})(jQuery);
