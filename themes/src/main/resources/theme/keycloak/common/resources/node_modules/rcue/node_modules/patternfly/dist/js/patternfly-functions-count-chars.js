// Count and Display Remaining Characters
(function ($) {

  'use strict';

  $.fn.countRemainingChars = function (options) {

    var settings = $.extend({
        // These are the defaults.
        charsMaxLimit: 100,
        charsWarnRemaining: 5,
        blockInputAtMaxLimit: false
      }, options),
      $taFld = this,
      $countFld = $('#'  + settings.countFld).text(settings.charsMaxLimit),
      charsRemainingFn = function (charsLength) {
        var charsRemaining = settings.charsMaxLimit - charsLength;
        $countFld.text(charsRemaining);
        $countFld.toggleClass('chars-warn-remaining-pf', charsRemaining <= settings.charsWarnRemaining);
        if (charsRemaining < 0) {
          $taFld.trigger("overCharsMaxLimitEvent", $taFld.attr('id'));
        } else {
          $taFld.trigger("underCharsMaxLimitEvent", $taFld.attr('id'));
        }
      };

    this.on('paste', function (event) {
      setTimeout(function () {
        var charsLength = $taFld.val().length, maxTxt;

        if (settings.blockInputAtMaxLimit && charsLength > settings.charsMaxLimit) {
          maxTxt = $taFld.val();
          maxTxt = maxTxt.substring(0, settings.charsMaxLimit);
          $taFld.val(maxTxt);
          charsLength = $taFld.val().length;
        }

        charsRemainingFn(charsLength);
      }, 100);
    });

    this.keyup(function (event) {
      charsRemainingFn($taFld.val().length);
    });

    this.keydown(function (event) {
      var charsLength = $taFld.val().length;

      if (settings.blockInputAtMaxLimit && charsLength >= settings.charsMaxLimit) {
        // Except backspace
        if (event.keyCode !== 8) {
          event.preventDefault();
        }
      }
    });

    return this;
  };
}(jQuery));
