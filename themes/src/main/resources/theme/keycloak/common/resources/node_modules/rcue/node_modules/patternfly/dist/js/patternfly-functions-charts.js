// Util: PatternFly C3 Chart Defaults
(function ($) {
  'use strict';
  if (patternfly !== undefined) {
    $.fn.pfSetDonutChartTitle = patternfly.pfSetDonutChartTitle;
    $.fn.pfDonutTooltipContents = patternfly.pfDonutTooltipContents;
    $.fn.pfGetUtilizationDonutTooltipContentsFn = patternfly.pfGetUtilizationDonutTooltipContentsFn;
    $.fn.pfGetBarChartTooltipContentsFn = patternfly.pfGetBarChartTooltipContentsFn;
    $.fn.pfSingleLineChartTooltipContentsFn = patternfly.pfSingleLineChartTooltipContentsFn;
    $.fn.pfPieTooltipContents = patternfly.pfPieTooltipContents;
    $.fn.c3ChartDefaults = patternfly.c3ChartDefaults;
  }
}(jQuery));
