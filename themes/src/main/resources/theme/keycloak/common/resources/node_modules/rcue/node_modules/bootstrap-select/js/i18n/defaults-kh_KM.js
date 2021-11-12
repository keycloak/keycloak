/*
 * Translated default messages for bootstrap-select.
 * Locale: KH (Khmer)
 * Region: kM (Khmer)
 */
(function ($) {
  $.fn.selectpicker.defaults = {
    noneSelectedText: 'មិនមានអ្វីបានជ្រើសរើស',
    noneResultsText: 'មិនមានលទ្ធផល {0}',
    countSelectedText: function (numSelected, numTotal) {
      return (numSelected == 1) ? "{0} ធាតុដែលបានជ្រើស" : "{0} ធាតុដែលបានជ្រើស";
    },
    maxOptionsText: function (numAll, numGroup) {
      return [
        (numAll == 1) ? 'ឈានដល់ដែនកំណត់ ( {n} ធាតុអតិបរមា)' : 'អតិបរមាឈានដល់ដែនកំណត់ ( {n} ធាតុ)',
        (numGroup == 1) ? 'ដែនកំណត់ក្រុមឈានដល់ ( {n} អតិបរមាធាតុ)' : 'អតិបរមាក្រុមឈានដល់ដែនកំណត់ ( {n} ធាតុ)'
      ];
    },
    selectAllText: 'ជ្រើស​យក​ទាំងអស់',
    deselectAllText: 'មិនជ្រើស​យក​ទាំងអស',
    multipleSeparator: ', '
  };
})(jQuery);
