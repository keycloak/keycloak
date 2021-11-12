/*
 * Dịch các văn bản mặc định cho bootstrap-select.
 * Locale: VI (Vietnamese)
 * Region: VN (Việt Nam)
 */
(function ($) {
  $.fn.selectpicker.defaults = {
    noneSelectedText: 'Chưa chọn',
    noneResultsText: 'Không có kết quả cho {0}',
    countSelectedText: function (numSelected, numTotal) {
      return "{0} mục đã chọn";
    },
    maxOptionsText: function (numAll, numGroup) {
      return [
        'Không thể chọn (giới hạn {n} mục)',
        'Không thể chọn (giới hạn {n} mục)'
      ];
    },
    selectAllText: 'Chọn tất cả',
    deselectAllText: 'Bỏ chọn',
    multipleSeparator: ', '
  };
})(jQuery);
