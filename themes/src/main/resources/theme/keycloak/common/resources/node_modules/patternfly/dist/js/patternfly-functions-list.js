// PatternFly pf-list
(function ($) {
  'use strict';

  $.fn.pfList = function () {
    function init (list) {
      // Ensure the state of the expansion elements is consistent
      list.find('[data-list=expansion], .list-pf-item, .list-pf-expansion').each(function (index, element) {
        var $expansion = $(element),
          $collapse = $expansion.find('.collapse').first(),
          expanded = $collapse.hasClass('in');
        updateChevron($expansion, expanded);
        if ($expansion.hasClass('list-pf-item')) {
          updateActive($expansion, expanded);
        }
      });
      list.find('.list-pf-container').each(function (index, element) {
        var $element = $(element);
        // The toggle element is the element with the data-list=toggle attribute
        // or the entire .list-pf-container as a fallback
        var $toggles = $element.find('[data-list=toggle]');
        $toggles.length || ($toggles = $element);
        $toggles.on('keydown', function (event) {
          if (event.keyCode === 13 || event.keyCode === 32) {
            toggleCollapse(this);
            event.stopPropagation();
            event.preventDefault();
          }
        });
        $toggles.on('click', function (event) {
          toggleCollapse(this);
          event.stopPropagation();
          event.preventDefault();
        });
      });
    }

    function toggleCollapse (toggle) {
      var $toggle, $expansion, $collapse, expanded, $listItem;
      $toggle = $(toggle);
      // Find the parent expansion of the toggle
      $expansion = $toggle.parentsUntil('.list-pf', '[data-list=expansion]').first();
      $expansion.length || ($expansion = $toggle.closest('.list-pf-item, .list-pf-expansion'));

      // toggle the "in" class of its  first .collapse child
      $collapse = $expansion.find('.collapse').first();
      $collapse.toggleClass('in');

      // update the state of the expansion element
      updateChevron($expansion, $collapse.hasClass('in'));
      $listItem = $expansion.closest('.list-pf-item');
      updateActive($listItem, $listItem.find('.collapse').first().hasClass('in'));
    }

    function updateActive ($listItem, expanded) {
      // Find the closest .list-pf-item of the expansion, and set its "active" class
      if (expanded) {
        $listItem.addClass('active');
      } else {
        $listItem.removeClass('active');
      }
    }

    function updateChevron ($expansion, expanded) {
      var $chevron = $expansion.find('.list-pf-chevron .fa').first();
      if (expanded) {
        $chevron.removeClass('fa-angle-right');
        $chevron.addClass('fa-angle-down');
      } else {
        $chevron.addClass('fa-angle-right');
        $chevron.removeClass('fa-angle-down');
      }
    }

    init(this);

    return this;
  };
}(jQuery));
