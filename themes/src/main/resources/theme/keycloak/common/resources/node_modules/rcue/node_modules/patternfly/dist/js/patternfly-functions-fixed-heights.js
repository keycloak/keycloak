// Util: PatternFly Collapse with fixed heights
// Update the max-height of collapse elements based on the parent container's height.
(function ($) {
  'use strict';

  $.fn.initCollapseHeights = function (scrollSelector) {
    var parentElement = this, setCollapseHeights, targetScrollSelector = scrollSelector;

    setCollapseHeights = function () {
      var height, openPanel, contentHeight, bodyHeight, overflowY = 'hidden';

      height = parentElement.height();

      // Close any open panel
      openPanel = parentElement.find('.collapse.in');
      if (openPanel && openPanel.length > 0) {
        openPanel.removeClass('in');
      }

      // Determine the necessary height for the closed content
      contentHeight = 0;
      parentElement.children().each($.proxy(function (i, element) {
        var $element = $(element);
        contentHeight += $element.outerHeight(true);
      }, parentElement)).end();

      // Determine the height remaining for opened collapse panels
      bodyHeight = height - contentHeight;

      // Make sure we have enough height to be able to scroll the contents if necessary
      if (bodyHeight < 25) {
        bodyHeight = 25;

        // Allow the parent to scroll so the child elements are accessible
        overflowY = 'auto';
      }

      // Reopen the initially opened panel
      if (openPanel && openPanel.length > 0) {
        openPanel.addClass("in");
      }

      setTimeout(function () {
        // Set the max-height for the collapse panels
        parentElement.find('[data-toggle="collapse"]').each($.proxy(function (i, element) {
          var $element, selector, $target, scrollElement, innerHeight = 0;
          $element = $(element);

          // Determine the selector to find the target
          selector = $element.attr('data-target');
          if (!selector) {
            selector = $element.attr('href');
          }

          // Determine the scroll element (either the target or the child of the target based on the given selector)
          $target = $(selector);
          scrollElement = $target;
          if (targetScrollSelector) {
            scrollElement = $target.find(targetScrollSelector);
            if (scrollElement.length === 1) {
              innerHeight = 0;
              $target.children().each($.proxy(function (j, sibling) {
                var $sibling = $(sibling);
                if (sibling !== scrollElement[0]) {
                  innerHeight += $sibling.outerHeight(true);
                }
              }, $target)).end();
              bodyHeight -= innerHeight;
            } else {
              scrollElement = $target;
            }
          }
          // Set the max-height and vertical scroll of the scroll element
          scrollElement.css({'max-height': (bodyHeight - innerHeight) + 'px', 'overflow-y': 'auto'});
        }, parentElement)).end();

        parentElement.css({'overflow-y': overflowY});
      }, 100);
    };

    setCollapseHeights();

    // Update on window resizing
    $(window).resize(setCollapseHeights);

  };
}(jQuery));
