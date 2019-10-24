(function (window) {
  'use strict';

  // Ensure we are assigning these to the patternfly property of the window argument, and not the implicit global patternfly
  var patternfly = window.patternfly;

  // Util: PatternFly C3 Chart Defaults
  patternfly.pfSetDonutChartTitle = function (selector, primary, secondary) {
    var donutChartRightTitle = window.d3.select(selector).select('text.c3-chart-arcs-title');
    donutChartRightTitle.text("");
    donutChartRightTitle.insert('tspan').text(primary).classed('donut-title-big-pf', true).attr('y', 0).attr('x', 0);
    donutChartRightTitle.insert('tspan').text(secondary).classed('donut-title-small-pf', true).attr('y', 20).attr('x', 0);
  };

  patternfly.pfDonutTooltipContents = function (d, defaultTitleFormat, defaultValueFormat, color) {
    return '<table class="c3-tooltip">' +
      '  <tr>' +
      '    <td><span style="background-color:' + color(d[0].id) + '"></span>' + '<strong>' + d[0].value + '</strong> ' + d[0].name + '</td>' +
      '    <td>' + (Math.round(d[0].ratio * 1000) / 10) + '%</td>' +
      '  </tr>' +
      '</table>';
  };

  patternfly.pfGetUtilizationDonutTooltipContentsFn = function (units) {
    return function (d) {
      return '<span class="donut-tooltip-pf" style="white-space: nowrap;">' +
        (Math.round(d[0].ratio * 1000) / 10) + '%' + ' ' + units + ' ' + d[0].name +
        '</span>';
    };
  };

  patternfly.pfGetBarChartTooltipContentsFn = function (categories) {
    return function (d) {
      var name = categories ? categories[d[0].index] : d[0].index;
      return '<table class="c3-tooltip">' +
        '  <tr>' +
        '    <td><strong>' + name + ':</td>' +
        '    <td>' + d[0].value + '</td>' +
        '  </tr>' +
        '</table>';
    };
  };

  patternfly.pfSingleLineChartTooltipContentsFn = function (categories) {
    return function (d) {
      var name = categories ? categories[d[0].index] : d[0].index;
      return '<table class="c3-tooltip">' +
        '  <tr>' +
        '    <td><strong>' + name + ':</td>' +
        '    <td>' + d[0].value + '</td>' +
        '  </tr>' +
        '</table>';
    };
  };

  patternfly.pfPieTooltipContents = function (d, defaultTitleFormat, defaultValueFormat, color) {
    return patternfly.pfDonutTooltipContents(d, defaultTitleFormat, defaultValueFormat, color);
  };

  patternfly.c3ChartDefaults = function () {
    var
      getDefaultColors = function () {
        return {
          pattern: [
            patternfly.pfPaletteColors.blue,
            patternfly.pfPaletteColors.blue300,
            patternfly.pfPaletteColors.green,
            patternfly.pfPaletteColors.orange,
            patternfly.pfPaletteColors.red
          ]
        };
      },
      getDefaultBarGrid = function () {
        return {
          y: {
            show: true
          }
        };
      },
      getDefaultBarTooltip = function (categories) {
        return {
          contents: patternfly.pfGetBarChartTooltipContentsFn(categories)
        };
      },
      getDefaultBarLegend = function () {
        return {
          show: false
        };
      },
      getDefaultBarConfig = function (categories) {
        return {
          color: this.getDefaultColors(),
          grid: this.getDefaultBarGrid(),
          tooltip: this.getDefaultBarTooltip(categories),
          legend: this.getDefaultBarLegend()
        };
      },
      getDefaultGroupedBarGrid = function () {
        return {
          y: {
            show: true
          }
        };
      },
      getDefaultGroupedBarLegend = function () {
        return {
          show: true,
          position: 'bottom'
        };
      },
      getDefaultGroupedBarConfig = function () {
        return {
          color: this.getDefaultColors(),
          grid: this.getDefaultGroupedBarGrid(),
          legend: this.getDefaultGroupedBarLegend()
        };
      },
      getDefaultStackedBarGrid = function () {
        return {
          y: {
            show: true
          }
        };
      },
      getDefaultStackedBarLegend = function () {
        return {
          show: true,
          position: 'bottom'
        };
      },
      getDefaultStackedBarConfig = function () {
        return {
          color: this.getDefaultColors(),
          grid: this.getDefaultStackedBarGrid(),
          legend: this.getDefaultStackedBarLegend()
        };
      },
      getDefaultDonut = function (title) {
        return {
          title: title,
          label: {
            show: false
          },
          width: 11
        };
      },
      getDefaultDonutSize = function () {
        return {
          height: 171 // produces a diameter of 150 and a centered chart when there is no legend
          // Don't set a width here, the default is to center horizontally in the parent container
        };
      },
      getDefaultDonutColors = function () {
        return {
          pattern: [
            patternfly.pfPaletteColors.blue,
            patternfly.pfPaletteColors.black300
          ]
        };
      },
      getDefaultRelationshipDonutColors = function () {
        return {
          pattern: [
            patternfly.pfPaletteColors.blue,
            patternfly.pfPaletteColors.red100,
            patternfly.pfPaletteColors.orange400,
            patternfly.pfPaletteColors.green400,
            patternfly.pfPaletteColors.cyan500,
            patternfly.pfPaletteColors.gold200,
          ]
        };
      },
      getDefaultDonutTooltip = function () {
        return {
          show: false
        };
      },
      getDefaultDonutLegend = function () {
        return {
          show: false
        };
      },
      getDefaultDonutConfig = function (title) {
        return {
          donut: this.getDefaultDonut(title),
          size: this.getDefaultDonutSize(),
          legend: this.getDefaultDonutLegend(),
          color: this.getDefaultDonutColors(),
          tooltip: this.getDefaultDonutTooltip()
        };
      },
      getDefaultRelationshipDonutConfig = function (title) {
        return {
          donut: this.getDefaultDonut(title),
          size: this.getDefaultDonutSize(),
          legend: this.getDefaultDonutLegend(),
          color: this.getDefaultRelationshipDonutColors(),
          tooltip: this.getDefaultDonutTooltip()
        };
      },
      getDefaultPie = function () {
        return {
          expand: true,
          label: {
            show: false
          }
        };
      },
      getDefaultPieSize = function () {
        return {
          height: 171 // produces a diameter of 150 and a centered chart when there is no legend
          // Don't set a width here, default is to center horizontally in the parent container
        };
      },
      getDefaultPieColors = function () {
        return {
          pattern: [
            patternfly.pfPaletteColors.blue,
            patternfly.pfPaletteColors.black300
          ]
        };
      },
      getDefaultPieTooltip = function () {
        return {
          contents: patternfly.pfPieTooltipContents
        };
      },
      getDefaultPieLegend = function () {
        return {
          show: false
        };
      },
      getDefaultPieConfig = function () {
        return {
          pie: this.getDefaultPie(),
          size: this.getDefaultPieSize(),
          legend: this.getDefaultPieLegend(),
          color: this.getDefaultPieColors(),
          tooltip: this.getDefaultPieTooltip()
        };
      },
      getDefaultSparklineArea = function () {
        return {
          zerobased: true
        };
      },
      getDefaultSparklineSize = function () {
        return {
          height: 60
        };
      },
      getDefaultSparklineAxis = function () {
        return {
          x: {
            show: false
          },
          y: {
            show: false
          }
        };
      },
      getDefaultSparklineLegend = function () {
        return {
          show: false
        };
      },
      getDefaultSparklinePoint = function () {
        return {
          r: 1,
          focus: {
            expand: {
              r: 4
            }
          }
        };
      },
      getDefaultSparklineTooltip = function () {
        return {
          // because a sparkline should only contain a single data column,
          // the tooltip will only work for a single data column
          contents: function (d) {
            return '<span class="c3-tooltip-sparkline">' + d[0].value + ' ' + d[0].name + '</span>';
          }
        };
      },
      getDefaultSparklineConfig = function () {
        return {
          area: getDefaultSparklineArea(),
          size: getDefaultSparklineSize(),
          axis: getDefaultSparklineAxis(),
          color: getDefaultColors(),
          legend: getDefaultSparklineLegend(),
          point: getDefaultSparklinePoint(),
          tooltip: getDefaultSparklineTooltip()
        };
      },
      getDefaultLineAxis = function () {
        return {
          x: {
            show: true
          },
          y: {
            show: true
          }
        };
      },
      getDefaultLineGrid = function () {
        return {
          x: {
            show: false
          },
          y: {
            show: true
          }
        };
      },
      getDefaultLineLegend = function () {
        return {
          show: true
        };
      },
      getDefaultLinePoint = function () {
        return {
          r: 3,
          focus: {
            expand: {
              r: 5
            }
          }
        };
      },
      getDefaultLineConfig = function () {
        return {
          axis: getDefaultLineAxis(),
          grid: getDefaultLineGrid(),
          color: getDefaultColors(),
          legend: getDefaultLineLegend(),
          point: getDefaultLinePoint()
        };
      },
      getDefaultSingleLineTooltip = function () {
        return {
          contents: patternfly.pfGetBarChartTooltipContentsFn()
        };
      },
      getDefaultSingleLineLegend = function () {
        return {
          show: false
        };
      },
      getDefaultSingleLineConfig = function () {
        return {
          axis: getDefaultLineAxis(),
          grid: getDefaultLineGrid(),
          color: getDefaultColors(),
          legend: getDefaultSingleLineLegend(),
          point: getDefaultLinePoint(),
          tooltip: getDefaultSingleLineTooltip()
        };
      },
      getDefaultAreaAxis = function () {
        return getDefaultLineAxis();
      },
      getDefaultAreaGrid = function () {
        return getDefaultLineGrid();
      },
      getDefaultAreaLegend = function () {
        return getDefaultLineLegend();
      },
      getDefaultAreaPoint = function () {
        return getDefaultLinePoint();
      },
      getDefaultAreaConfig = function () {
        return {
          axis: getDefaultAreaAxis(),
          grid: getDefaultAreaGrid(),
          color: getDefaultColors(),
          legend: getDefaultAreaLegend(),
          point: getDefaultAreaPoint()
        };
      },
      getDefaultSingleAreaTooltip = function () {
        return {
          contents: patternfly.pfGetBarChartTooltipContentsFn()
        };
      },
      getDefaultSingleAreaLegend = function () {
        return getDefaultSingleLineLegend();
      },
      getDefaultSingleAreaConfig = function () {
        return {
          axis: getDefaultAreaAxis(),
          grid: getDefaultAreaGrid(),
          color: getDefaultColors(),
          legend: getDefaultSingleAreaLegend(),
          point: getDefaultAreaPoint(),
          tooltip: getDefaultSingleAreaTooltip()
        };
      };
    return {
      getDefaultColors: getDefaultColors,
      getDefaultBarGrid: getDefaultBarGrid,
      getDefaultBarTooltip: getDefaultBarTooltip,
      getDefaultBarLegend: getDefaultBarLegend,
      getDefaultBarConfig: getDefaultBarConfig,
      getDefaultGroupedBarGrid: getDefaultGroupedBarGrid,
      getDefaultGroupedBarLegend: getDefaultGroupedBarLegend,
      getDefaultGroupedBarConfig: getDefaultGroupedBarConfig,
      getDefaultStackedBarGrid: getDefaultStackedBarGrid,
      getDefaultStackedBarLegend: getDefaultStackedBarLegend,
      getDefaultStackedBarConfig: getDefaultStackedBarConfig,
      getDefaultDonut: getDefaultDonut,
      getDefaultDonutSize: getDefaultDonutSize,
      getDefaultDonutColors: getDefaultDonutColors,
      getDefaultDonutTooltip: getDefaultDonutTooltip,
      getDefaultDonutLegend: getDefaultDonutLegend,
      getDefaultDonutConfig: getDefaultDonutConfig,
      getDefaultRelationshipDonutConfig: getDefaultRelationshipDonutConfig,
      getDefaultPie: getDefaultPie,
      getDefaultPieSize: getDefaultPieSize,
      getDefaultPieColors: getDefaultPieColors,
      getDefaultRelationshipDonutColors: getDefaultRelationshipDonutColors,
      getDefaultPieTooltip: getDefaultPieTooltip,
      getDefaultPieLegend: getDefaultPieLegend,
      getDefaultPieConfig: getDefaultPieConfig,
      getDefaultSparklineArea: getDefaultSparklineArea,
      getDefaultSparklineSize: getDefaultSparklineSize,
      getDefaultSparklineAxis: getDefaultSparklineAxis,
      getDefaultSparklineLegend: getDefaultSparklineLegend,
      getDefaultSparklinePoint: getDefaultSparklinePoint,
      getDefaultSparklineTooltip: getDefaultSparklineTooltip,
      getDefaultSparklineConfig: getDefaultSparklineConfig,
      getDefaultLineAxis: getDefaultLineAxis,
      getDefaultLineGrid: getDefaultLineGrid,
      getDefaultLineLegend: getDefaultLineLegend,
      getDefaultLinePoint: getDefaultLinePoint,
      getDefaultLineConfig: getDefaultLineConfig,
      getDefaultSingleLineTooltip: getDefaultSingleLineTooltip,
      getDefaultSingleLineConfig: getDefaultSingleLineConfig,
      getDefaultAreaAxis: getDefaultAreaAxis,
      getDefaultAreaGrid: getDefaultAreaGrid,
      getDefaultAreaLegend: getDefaultAreaLegend,
      getDefaultAreaPoint: getDefaultAreaPoint,
      getDefaultAreaConfig: getDefaultAreaConfig,
      getDefaultSingleAreaTooltip: getDefaultSingleAreaTooltip,
      getDefaultSingleAreaConfig: getDefaultSingleAreaConfig
    };
  };
})(typeof window !== 'undefined' ? window : global);
