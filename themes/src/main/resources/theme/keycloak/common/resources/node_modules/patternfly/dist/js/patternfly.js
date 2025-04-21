(function (window) {
  'use strict';

  var patternfly = {
    version: "3.59.4"
  };

  // definition of breakpoint sizes for tablet and desktop modes
  patternfly.pfBreakpoints = {
    'tablet': 768,
    'desktop': 1200
  };

  window.patternfly = patternfly;

})(typeof window !== 'undefined' ? window : global);

(function (window) {
  'use strict';

  // Ensure we are assigning these to the patternfly property of the window argument, and not the implicit global patternfly
  var patternfly = window.patternfly;

  // Util: PatternFly Palette colors
  patternfly.pfPaletteColors = {
    black:         '#030303',
    black100:      '#fafafa',
    black200:      '#ededed',
    black300:      '#d1d1d1',
    black400:      '#bbbbbb',
    black500:      '#8b8d8f',
    black600:      '#72767b',
    black700:      '#4d5258',
    black800:      '#393f44',
    black900:      '#292e34',
    blue:          '#0088ce',
    blue100:       '#bee1f4',
    blue200:       '#7dc3e8',
    blue300:       '#39a5dc',
    blue400:       '#0088ce',
    blue500:       '#00659c',
    blue600:       '#004368',
    blue700:       '#002235',
    gold:          '#f0ab00',
    gold100:       '#fbeabc',
    gold200:       '#f9d67a',
    gold300:       '#f5c12e',
    gold400:       '#f0ab00',
    gold500:       '#b58100',
    gold600:       '#795600',
    gold700:       '#3d2c00',
    orange:        '#ec7a08',
    orange100:     '#fbdebf',
    orange200:     '#f7bd7f',
    orange300:     '#f39d3c',
    orange400:     '#ec7a08',
    orange500:     '#b35c00',
    orange600:     '#773d00',
    orange700:     '#3b1f00',
    lightBlue:     '#00b9e4',
    lightBlue100:  '#beedf9',
    lightBlue200:  '#7cdbf3',
    lightBlue300:  '#35caed',
    lightBlue400:  '#00b9e4',
    lightBlue500:  '#008bad',
    lightBlue600:  '#005c73',
    lightBlue700:  '#002d39',
    green:         '#3f9c35',
    green100:      '#cfe7cd',
    green200:      '#9ecf99',
    green300:      '#6ec664',
    green400:      '#3f9c35',
    green500:      '#2d7623',
    green600:      '#1e4f18',
    green700:      '#0f280d',
    lightGreen:    '#92d400',
    lightGreen100: '#e4f5bc',
    lightGreen200: '#c8eb79',
    lightGreen300: '#ace12e',
    lightGreen400: '#92d400',
    lightGreen500: '#6ca100',
    lightGreen600: '#486b00',
    lightGreen700: '#253600',
    cyan:          '#007a87',
    cyan100:       '#bedee1',
    cyan200:       '#7dbdc3',
    cyan300:       '#3a9ca6',
    cyan400:       '#007a87',
    cyan500:       '#005c66',
    cyan600:       '#003d44',
    cyan700:       '#001f22',
    purple:        '#703fec',
    purple100:     '#c7bfff',
    purple200:     '#a18fff',
    purple300:     '#8461f7',
    purple400:     '#703fec',
    purple500:     '#582fc0',
    purple600:     '#40199a',
    purple700:     '#1f0066',
    red:           '#cc0000',
    red100:        '#cc0000',
    red200:        '#a30000',
    red300:        '#8b0000',
    red400:        '#470000',
    red500:        '#2c0000'
  };
})(typeof window !== 'undefined' ? window : global);


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

// Util: definition of breakpoint sizes for tablet and desktop modes
(function ($) {
  'use strict';
  if (patternfly !== undefined) {
    $.pfBreakpoints = patternfly.pfBreakpoints;
  }
}(jQuery));
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

// Util: PatternFly Sidebar
// Set height of sidebar-pf to height of document minus height of navbar-pf if not mobile
(function ($) {
  'use strict';
  $.fn.sidebar = function () {
    var documentHeight = 0,
      navbarpfHeight = 0,
      colHeight = 0;

    if ($('.navbar-pf .navbar-toggle').is(':hidden')) {
      documentHeight = $(document).height();
      navbarpfHeight = $('.navbar-pf').outerHeight();
      colHeight = documentHeight - navbarpfHeight;
    }
    $('.sidebar-pf').parent('.row').children('[class*="col-"]').css({"min-height" : colHeight});
  };

  $(document).ready(function () {
    // Call sidebar() on ready if .sidebar-pf exists and .datatable does not exist
    if ($('.sidebar-pf').length > 0 && $('.datatable').length === 0) {
      $.fn.sidebar();
    }
  });

  $(window).on('resize', function () {
    // Call sidebar() on resize if .sidebar-pf exists
    if ($('.sidebar-pf').length > 0) {
      $.fn.sidebar();
    }
  });
}(jQuery));

// Util: PatternFly Popovers
// Add data-close="true" to insert close X icon
(function ($) {
  'use strict';

  $.fn.popovers = function () {
    // Initialize
    this.popover();

    // Add close icons
    this.filter('[data-close=true]').each(function (index, element) {
      var $this = $(element),
        title = $this.attr('data-original-title') + '<button type="button" class="close" aria-hidden="true"><span class="pficon pficon-close"></span></button>';

      $this.attr('data-original-title', title);
    });

    // Bind Close Icon to Toggle Display
    this.on('click', function (e) {
      var $this = $(this),
        $title = $this.next('.popover').find('.popover-title');

      // Only if data-close is true add class "x" to title for right padding
      $title.find('.close').parent('.popover-title').addClass('closable');

      // Bind x icon to close popover
      $title.find('.close').on('click', function () {
        $this.popover('hide');
      });

      // Prevent href="#" page scroll to top
      e.preventDefault();
    });

    return this;
  };
}(jQuery));

// Util: DataTables Settings
(function ($) {
  'use strict';
  if ($.fn.dataTableExt) {
    /* Set the defaults for DataTables initialisation */
    $.extend(true, $.fn.dataTable.defaults, {
      "bDestroy": true,
      "bAutoWidth": false,
      "iDisplayLength": 20,
      "sDom":
        "<'dataTables_header' f i r >" +
        "<'table-responsive'  t >" +
        "<'dataTables_footer' p >",
      "oLanguage": {
        "sInfo": "Showing <b>_START_</b> to <b>_END_</b> of <b>_TOTAL_</b> Items",
        "sInfoFiltered" : "(of <b>_MAX_</b>)",
        "sInfoEmpty" : "Showing <b>0</b> Results",
        "sZeroRecords":
          "<p>Suggestions</p>" +
          "<ul>" +
            "<li>Check the javascript regular expression syntax of the search term.</li>" +
            "<li>Check that the correct menu option is chosen (token ID vs. user ID).</li>" +
            "<li>Use wildcards (* to match 0 or more characters, + to match 1 or more characters, ? to match 0 or 1 character).</li>" +
            "<li>Clear the search field, then click Search to return to the 20 most recent records.</li>" +
          "</ul>",
        "sSearch": ""
      },
      "sPaginationType": "bootstrap_input",
      "oSearch": {
        "sSearch": "",
        "bRegex": true,
        "bSmart": false
      }
    });

    /* Default class modification */
    $.extend($.fn.dataTableExt.oStdClasses, {
      "sWrapper": "dataTables_wrapper"
    });

    /* API method to get paging information */
    $.fn.dataTableExt.oApi.fnPagingInfo = function (oSettings) {
      return {
        "iStart":         oSettings._iDisplayStart,
        "iEnd":           oSettings.fnDisplayEnd(),
        "iLength":        oSettings._iDisplayLength,
        "iTotal":         oSettings.fnRecordsTotal(),
        "iFilteredTotal": oSettings.fnRecordsDisplay(),
        "iPage":          oSettings._iDisplayLength === -1 ? 0 : Math.ceil(oSettings._iDisplayStart / oSettings._iDisplayLength),
        "iTotalPages":    oSettings._iDisplayLength === -1 ? 0 : Math.ceil(oSettings.fnRecordsDisplay() / oSettings._iDisplayLength)
      };
    };

    /* Combination of Bootstrap + Input Text style pagination control */
    $.extend($.fn.dataTableExt.oPagination, {
      "bootstrap_input": {
        "fnInit": function (oSettings, nPaging, fnDraw) {
          var fnClickHandler = function (e) {
              e.preventDefault();
              if (oSettings.oApi._fnPageChange(oSettings, e.data.action)) {
                fnDraw(oSettings);
              }
            },
            els,
            nInput;

          $(nPaging).append(
            '<ul class="pagination">' +
              '  <li class="first disabled"><span class="i fa fa-angle-double-left"></span></li>' +
              '  <li class="prev disabled"><span class="i fa fa-angle-left"></span></li>' +
              '</ul>' +
              '<div class="pagination-input">' +
              '  <input type="text" class="paginate_input">' +
              '  <span class="paginate_of">of <b>3</b></span>' +
              '</div>' +
              '<ul class="pagination">' +
              '  <li class="next disabled"><span class="i fa fa-angle-right"></span></li>' +
              '  <li class="last disabled"><span class="i fa fa-angle-double-right"></span></li>' +
              '</ul>'
          );

          els = $('li', nPaging);
          $(els[0]).bind('click.DT', { action: "first" }, fnClickHandler);
          $(els[1]).bind('click.DT', { action: "previous" }, fnClickHandler);
          $(els[2]).bind('click.DT', { action: "next" }, fnClickHandler);
          $(els[3]).bind('click.DT', { action: "last" }, fnClickHandler);

          nInput = $('input', nPaging);
          $(nInput).keyup(function (e) {
            var iNewStart;
            if (e.which === 38 || e.which === 39) {
              this.value += 1;
            } else if ((e.which === 37 || e.which === 40) && this.value > 1) {
              this.value -= 1;
            }

            if (this.value === "" || !this.value.match(/[0-9]/)) {
              /* Nothing entered or non-numeric character */
              return;
            }

            iNewStart = oSettings._iDisplayLength * (this.value - 1);
            if (iNewStart >= oSettings.fnRecordsDisplay()) {
              /* Display overrun */
              oSettings._iDisplayStart = (Math.ceil((oSettings.fnRecordsDisplay() - 1) /
                oSettings._iDisplayLength) - 1) * oSettings._iDisplayLength;
              fnDraw(oSettings);
              return;
            }

            oSettings._iDisplayStart = iNewStart;
            fnDraw(oSettings);
          });
        },

        "fnUpdate": function (oSettings, fnDraw) {
          var oPaging = oSettings.oInstance.fnPagingInfo(),
            an = oSettings.aanFeatures.p,
            ien = an.length,
            iPages = Math.ceil((oSettings.fnRecordsDisplay()) / oSettings._iDisplayLength),
            iCurrentPage = Math.ceil(oSettings._iDisplayStart / oSettings._iDisplayLength) + 1,
            i;

          for (i = 0; i < ien; i += 1) {
            $('.paginate_input', an[i]).val(iCurrentPage)
              .siblings('.paginate_of').find('b').html(iPages);

            // Add / remove disabled classes from the static elements
            if (oPaging.iPage === 0) {
              $('li.first', an[i]).addClass('disabled');
              $('li.prev', an[i]).addClass('disabled');
            } else {
              $('li.first', an[i]).removeClass('disabled');
              $('li.prev', an[i]).removeClass('disabled');
            }

            if (oPaging.iPage === oPaging.iTotalPages - 1 || oPaging.iTotalPages === 0) {
              $('li.next', an[i]).addClass('disabled');
              $('li.last', an[i]).addClass('disabled');
            } else {
              $('li.next', an[i]).removeClass('disabled');
              $('li.last', an[i]).removeClass('disabled');
            }
          }
        }
      }
    });
  }
}(jQuery));

// Util: PatternFly Collapsible Left Hand Navigation
// Must have navbar-toggle in navbar-pf-alt for expand/collapse
(function ($) {

  'use strict';

  $.fn.navigation = function () {

    var navElement = $('.layout-pf-alt-fixed .nav-pf-vertical-alt'),
      bodyContentElement = $('.container-pf-alt-nav-pf-vertical-alt'),
      toggleNavBarButton = $('.navbar-toggle'),
      explicitCollapse = false,
      checkNavState = function () {
        var width = $(window).width();

        //Always remove the hidden & peek class
        navElement.removeClass('hidden show-mobile-nav collapsed');

        //Set the body class back to the default
        bodyContentElement.removeClass('collapsed-nav hidden-nav');

        // Check to see if the nav needs to collapse
        if (width < $.pfBreakpoints.desktop || explicitCollapse) {
          navElement.addClass('collapsed');
          bodyContentElement.addClass('collapsed-nav');
        }

        // Check to see if we need to move down to the mobile state
        if (width < $.pfBreakpoints.tablet) {
          //Set the nav to being hidden
          navElement.addClass('hidden');

          //Make sure this is expanded
          navElement.removeClass('collapsed');

          //Set the body class to the correct state
          bodyContentElement.removeClass('collapsed-nav');
          bodyContentElement.addClass('hidden-nav');
        }
      },
      collapseMenu = function () {
        //Make sure this is expanded
        navElement.addClass('collapsed');
        //Set the body class to the correct state
        bodyContentElement.addClass('collapsed-nav');

        explicitCollapse = true;
      },
      enableTransitions = function () {
        // enable transitions only when toggleNavBarButton is clicked or window is resized
        $('html').addClass('transitions');
      },
      expandMenu = function () {
        //Make sure this is expanded
        navElement.removeClass('collapsed');
        //Set the body class to the correct state
        bodyContentElement.removeClass('collapsed-nav');

        explicitCollapse = false;
      },
      bindMenuBehavior = function () {
        toggleNavBarButton.on('click', function (e) {
          var inMobileState = bodyContentElement.hasClass('hidden-nav');
          enableTransitions();

          if (inMobileState && navElement.hasClass('show-mobile-nav')) {
            //In mobile state just need to hide the nav
            navElement.removeClass('show-mobile-nav');
          } else if (inMobileState) {
            navElement.addClass('show-mobile-nav');
          } else if (navElement.hasClass('collapsed')) {
            expandMenu();
          } else {
            collapseMenu();
          }
        });
      },
      setTooltips = function () {
        $('.nav-pf-vertical-alt [data-toggle="tooltip"]').tooltip({'container': 'body', 'delay': { 'show': '500', 'hide': '200' }});

        $(".nav-pf-vertical-alt").on("show.bs.tooltip", function (e) {
          return $(this).hasClass("collapsed");
        });

      },
      init = function () {
        //Set correct state on load
        checkNavState();

        // Bind Top level hamburger menu with menu behavior;
        bindMenuBehavior();

        //Set tooltips
        setTooltips();
      };

    //Listen for the window resize event and collapse/hide as needed
    $(window).on('resize', function () {
      checkNavState();
      enableTransitions();
    });

    init();

  };

  $(document).ready(function () {
    if ($('.nav-pf-vertical-alt').length > 0) {
      $.fn.navigation();
    }
  });

}(jQuery));

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

// Util: PatternFly Palette colors
(function ($) {
  'use strict';

  if (patternfly !== undefined) {
    $.pfPaletteColors = patternfly.pfPaletteColors;
  }
}(jQuery));

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
    $(window).on('resize', setCollapseHeights);

  };

  $.fn.initFixedAccordion = function () {
    var fixedAccordion = this, initOpen;

    fixedAccordion.on('show.bs.collapse','.collapse', function (event) {
      $(event.target.parentNode).addClass('panel-open');
    });

    fixedAccordion.on('hide.bs.collapse','.collapse', function (event) {
      $(event.target.parentNode).removeClass('panel-open');
    });

    fixedAccordion.find('.panel').each(function (index, item) {
      $(item).removeClass('panel-open');
    });

    initOpen = $(fixedAccordion.find('.collapse.in'))[0];
    if (initOpen) {
      $(initOpen.parentNode).addClass('panel-open');
    }
  };

}(jQuery));

// Util: PatternFly TreeGrid Tables
(function ($) {
  'use strict';

  function getParent (rows, node) {
    var parent = node.attr('data-parent');

    if (typeof parent === "string") {
      if (isNaN(parent)) {
        parent = $(parent);
        if (parent.length > 1) {
          parent = rows.closest(parent);
        }
      } else {
        parent = $(rows[parseInt(parent, 10)]);
      }
      return parent;
    }
    return undefined;
  }

  function renderItem (item, parent) {
    if (parent) {
      parent.find('.treegrid-node > span.expand-icon')
        .toggleClass('fa-angle-right', parent.hasClass('collapsed'))
        .toggleClass('fa-angle-down', !parent.hasClass('collapsed'));
      item.toggleClass('hidden', parent.hasClass('collapsed'));
      if (parent.hasClass('collapsed')) {
        item.addClass('collapsed');
      }
    }
  }

  function reStripe (tree) {
    tree.find('tbody > tr').removeClass('odd');
    tree.find('tbody > tr:not(.hidden):odd').addClass('odd');
  }

  $.fn.treegrid = function (options) {
    var i, rows, _this;
    rows = this.find('tbody > tr');
    _this = this;
    $.each(rows, function () {
      var node, parent;
      node = $(this);
      parent = getParent(rows, node);
      // Append expand icon dummies
      node.children('.treegrid-node').prepend('<span class="icon expand-icon fa"/>');

      // Set up an event listener for the node
      node.children('.treegrid-node').on('click', function (e) {
        var icon = node.find('span.expand-icon');

        if (options && typeof options.callback === 'function') {
          options.callback(e);
        }

        if (icon.hasClass('fa-angle-right')) {
          node.removeClass('collapsed');
        }
        if (icon.hasClass('fa-angle-down')) {
          node.addClass('collapsed');
        }
        $.each(rows.slice(rows.index(node) + 1), function () {
          renderItem($(this), getParent(rows, $(this)));
        });
        reStripe(_this);
      });

      if (parent) {
        // Calculate indentation depth
        i = parent.find('.treegrid-node > span.indent').length + 1;
        for (; i > 0; i -= 1) {
          node.children('.treegrid-node').prepend('<span class="indent"/>');
        }
        // Render expand/collapse icons
        renderItem(node, parent);
      }
    });
    reStripe(_this);
  };
}(jQuery));

// Util: PatternFly Vertical Navigation
// Must have navbar-toggle in navbar-pf-vertical for expand/collapse
(function ($) {
  'use strict';

  $.fn.setupVerticalNavigation = function (handleItemSelections, ignoreDrawer, userOptions) {

    var options = $.extend({
        hoverDelay: 500,
        hideDelay: 700,
        rememberOpenState: true,
        storage: 'localStorage',
      }, userOptions || {}),

      navElement = $('.nav-pf-vertical'),
      bodyContentElement = $('.container-pf-nav-pf-vertical'),
      toggleNavBarButton = $('.navbar-toggle'),
      handleResize = true,
      explicitCollapse = false,
      subDesktop = false,
      storageLocation = options.storage === 'sessionStorage' ? 'sessionStorage' : 'localStorage',

      inMobileState = function () {
        return bodyContentElement.hasClass('hidden-nav');
      },

      forceResize = function (delay) {
        setTimeout(function () {
          $(window).trigger('resize');
        }, delay);
      },

      showSecondaryMenu = function () {
        if (inMobileState() || !subDesktop) {
          navElement.addClass('secondary-visible-pf');
          bodyContentElement.addClass('secondary-visible-pf');
        }

        // Dispatch a resize event when showing the secondary menu in non-subdesktop state to
        // allow content to adjust to the secondary menu sizing
        if (!subDesktop) {
          forceResize(100);
        }
      },

      hideSecondaryMenu = function () {
        navElement.removeClass('secondary-visible-pf');
        bodyContentElement.removeClass('secondary-visible-pf');

        if (navElement.find('.secondary-nav-item-pf.is-hover').length <= 1) {
          navElement.removeClass('hover-secondary-nav-pf');
        }

        navElement.find('.mobile-nav-item-pf').each(function (index, item) {
          $(item).removeClass('mobile-nav-item-pf');
        });

        navElement.find('.is-hover').each(function (index, item) {
          $(item).removeClass('is-hover');
        });
      },

      hideTertiaryMenu = function () {
        navElement.removeClass('tertiary-visible-pf');
        bodyContentElement.removeClass('tertiary-visible-pf');

        if (navElement.find('.tertiary-nav-item-pf.is-hover').length <= 1) {
          navElement.removeClass('hover-tertiary-nav-pf');
        }

        navElement.find('.mobile-nav-item-pf').each(function (index, item) {
          $(item).removeClass('mobile-nav-item-pf');
        });

        navElement.find('.is-hover').each(function (index, item) {
          $(item).removeClass('is-hover');
        });
      },

      setActiveItem = function (item) {
        // remove all .active
        $('.nav-pf-vertical .list-group-item.active').removeClass('active');

        // add .active to item and its parents
        item.addClass('active').parents('.list-group-item').addClass('active');
      },

      updateSecondaryMenuDisplayAfterSelection = function () {
        if (inMobileState()) {
          navElement.removeClass('show-mobile-nav');
          hideSecondaryMenu();
          navElement.find('.mobile-nav-item-pf').each(function (index, item) {
            $(item).removeClass('mobile-nav-item-pf');
          });
        } else {
          showSecondaryMenu();
        }
      },

      updateSecondaryCollapsedState = function (setCollapsed, collapsedItem) {
        if (setCollapsed) {
          collapsedItem.addClass('collapsed');
          navElement.addClass('collapsed-secondary-nav-pf');
          bodyContentElement.addClass('collapsed-secondary-nav-pf');
        } else {
          if (collapsedItem) {
            collapsedItem.removeClass('collapsed');
          } else {
            // Remove any collapsed secondary menus
            navElement.find('[data-toggle="collapse-secondary-nav"]').each(function (index, element) {
              var $e = $(element);
              $e.removeClass('collapsed');
            });
          }
          navElement.removeClass('collapsed-secondary-nav-pf');
          bodyContentElement.removeClass('collapsed-secondary-nav-pf');
        }
      },

      updateTertiaryCollapsedState = function (setCollapsed, collapsedItem) {
        if (setCollapsed) {
          collapsedItem.addClass('collapsed');
          navElement.addClass('collapsed-tertiary-nav-pf');
          bodyContentElement.addClass('collapsed-tertiary-nav-pf');
          updateSecondaryCollapsedState(false);
        } else {
          if (collapsedItem) {
            collapsedItem.removeClass('collapsed');
          } else {
            // Remove any collapsed tertiary menus
            navElement.find('[data-toggle="collapse-tertiary-nav"]').each(function (index, element) {
              var $e = $(element);
              $e.removeClass('collapsed');
            });
          }
          navElement.removeClass('collapsed-tertiary-nav-pf');
          bodyContentElement.removeClass('collapsed-tertiary-nav-pf');
        }
      },

      updateMobileMenu = function (selected, secondaryItem) {
        $(document).find('.list-group-item.mobile-nav-item-pf').each(function (index, item) {
          $(item).removeClass('mobile-nav-item-pf');
        });
        $(document).find('.list-group-item.mobile-secondary-item-pf').each(function (index, item) {
          $(item).removeClass('mobile-secondary-item-pf');
        });
        if (selected) {
          selected.addClass('mobile-nav-item-pf');
          if (secondaryItem) {
            secondaryItem.addClass('mobile-secondary-item-pf');
            navElement.removeClass('show-mobile-secondary');
            navElement.addClass('show-mobile-tertiary');
          } else {
            navElement.addClass('show-mobile-secondary');
            navElement.removeClass('show-mobile-tertiary');
          }
        } else {
          navElement.removeClass('show-mobile-secondary');
          navElement.removeClass('show-mobile-tertiary');
        }
      },

      enterMobileState = function () {
        if (!navElement.hasClass('hidden')) {
          //Set the nav to being hidden
          navElement.addClass('hidden');
          navElement.removeClass('collapsed');

          //Set the body class to the correct state
          bodyContentElement.removeClass('collapsed-nav');
          bodyContentElement.addClass('hidden-nav');

          // Reset the collapsed states
          updateSecondaryCollapsedState(false);
          updateTertiaryCollapsedState(false);

          explicitCollapse = false;
        }
      },

      exitMobileState = function () {
        // Always remove the hidden & peek class
        navElement.removeClass('hidden show-mobile-nav');

        // Set the body class back to the default
        bodyContentElement.removeClass('hidden-nav');
      },

      checkNavState = function () {
        var width = $(window).width(), makeSecondaryVisible;
        if (!handleResize) {
          return;
        }
        // Check to see if we need to enter/exit the mobile state
        if (width < $.pfBreakpoints.tablet && !explicitCollapse) {
          enterMobileState();
        } else if (navElement.hasClass('hidden')) {
          exitMobileState();
        }

        // Check to see if we need to enter/exit the sub desktop state
        if (width < $.pfBreakpoints.desktop) {
          if (!subDesktop) {
            // Collapse the navigation bars when entering sub desktop mode
            navElement.addClass('collapsed');
            bodyContentElement.addClass('collapsed-nav');
          }
          if (width >= $.pfBreakpoints.tablet) {
            hideSecondaryMenu();
          }
          subDesktop = true;
        } else {
          makeSecondaryVisible = subDesktop && (navElement.find('.secondary-nav-item-pf.active').length > 0);
          subDesktop = false;
          if (makeSecondaryVisible) {

            showSecondaryMenu();
          }
        }

        if (explicitCollapse) {
          navElement.addClass('collapsed');
          bodyContentElement.addClass('collapsed-nav');
        } else {
          navElement.removeClass('collapsed');
          bodyContentElement.removeClass('collapsed-nav');
        }
      },

      collapseMenu = function () {
        //Make sure this is expanded
        navElement.addClass('collapsed');
        //Set the body class to the correct state
        bodyContentElement.addClass('collapsed-nav');

        if (subDesktop) {
          hideSecondaryMenu();
        }

        explicitCollapse = true;
      },

      enableTransitions = function () {
        // enable transitions only when toggleNavBarButton is clicked or window is resized
        $('html').addClass('transitions');
      },

      expandMenu = function () {
        //Make sure this is expanded
        navElement.removeClass('collapsed');
        //Set the body class to the correct state
        bodyContentElement.removeClass('collapsed-nav');

        explicitCollapse = false;

        // Dispatch a resize event when showing the expanding then menu to
        // allow content to adjust to the menu sizing
        if (!subDesktop) {
          forceResize(100);
        }
      },

      bindMenuBehavior = function () {
        toggleNavBarButton.on('click', function (e) {
          var $drawer;

          enableTransitions();

          if (inMobileState()) {
            // Toggle the mobile nav
            if (navElement.hasClass('show-mobile-nav')) {
              navElement.removeClass('show-mobile-nav');
            } else {
              // Always start at the primary menu
              updateMobileMenu();
              navElement.addClass('show-mobile-nav');

              // If the notification drawer is shown, hide it
              if (!ignoreDrawer) {
                $drawer = $('.drawer-pf');
                if ($drawer.length) {
                  $('.drawer-pf-trigger').removeClass('open');
                  $drawer.addClass('hide');
                }
              }
            }
          } else if (navElement.hasClass('collapsed')) {
            if (options.rememberOpenState) {
              window[storageLocation].setItem('patternfly-navigation-primary', 'expanded');
            }
            expandMenu();
          } else {
            if (options.rememberOpenState) {
              window[storageLocation].setItem('patternfly-navigation-primary', 'collapsed');
            }
            collapseMenu();
          }
        });
      },

      forceHideSecondaryMenu = function () {
        navElement.addClass('force-hide-secondary-nav-pf');
        setTimeout(function () {
          navElement.removeClass('force-hide-secondary-nav-pf');
        }, 500);
      },

      bindMenuItemsBehavior = function (handleSelection) {
        $(document).find('.nav-pf-vertical .list-group-item').each(function (index, item) {
          var onClickFn,
            $item = $(item),
            $nav = $item.closest('[class*="nav-pf-"]');

          if ($nav.hasClass('nav-pf-vertical')) {
            // Set main nav active item on click or show secondary nav if it has a secondary nav bar and we are in the mobile state
            onClickFn = function (event) {
              var $this = $(this), $secondaryItem, $tertiaryItem, $activeItem;

              if (!$this.hasClass('secondary-nav-item-pf')) {
                hideSecondaryMenu();
                if (inMobileState()) {
                  updateMobileMenu();
                  navElement.removeClass('show-mobile-nav');
                }
                if (handleSelection) {
                  setActiveItem($this);
                  // Don't process the click on the item
                  event.stopImmediatePropagation();
                }
              } else if (inMobileState()) {
                updateMobileMenu($this);
              } else if (handleSelection) {
                $activeItem = $secondaryItem = $item.find('.nav-pf-secondary-nav > .list-group > .list-group-item').eq(0);

                if ($secondaryItem.hasClass('tertiary-nav-item-pf')) {
                  $activeItem = $secondaryItem.find('.nav-pf-tertiary-nav > .list-group > .list-group-item').eq(0);
                }

                setActiveItem($activeItem);
                event.stopImmediatePropagation();
              }
            };

          } else if ($nav.hasClass('nav-pf-secondary-nav')) {
            // Set secondary nav active item on click or show tertiary nav if it has a tertiary nav bar and we are in the mobile state
            onClickFn = function (event) {
              var $this = $(this), $tertiaryItem, $primaryItem;
              if (!$this.hasClass('tertiary-nav-item-pf')) {
                if (inMobileState()) {
                  updateMobileMenu();
                  navElement.removeClass('show-mobile-nav');
                }
                updateSecondaryMenuDisplayAfterSelection();
                if (handleSelection) {
                  setActiveItem($item);
                  hideSecondaryMenu();
                  event.stopImmediatePropagation();
                }
              } else if (inMobileState()) {
                $primaryItem = $item.parents('.list-group-item');
                updateMobileMenu($this, $primaryItem);
                event.stopImmediatePropagation();
              } else if (handleSelection) {
                $tertiaryItem = $item.find('.nav-pf-tertiary-nav > .list-group > .list-group-item').eq(0);
                setActiveItem($tertiaryItem);
                event.stopImmediatePropagation();
              }
            };

          } else if ($nav.hasClass('nav-pf-tertiary-nav')) {
            // Set tertiary nav active item on click
            onClickFn = function (event) {
              if (inMobileState()) {
                updateMobileMenu();
                navElement.removeClass('show-mobile-nav');
              }
              updateSecondaryMenuDisplayAfterSelection();
              if (handleSelection) {
                setActiveItem($item);
                hideTertiaryMenu();
                hideSecondaryMenu();
                event.stopImmediatePropagation();
              }
            };
          }

          // register event handler
          $item.on('click.pf.secondarynav.data-api', onClickFn);
        });

        $(document).find('.secondary-nav-item-pf').each(function (index, secondaryItem) {
          var $secondaryItem = $(secondaryItem);

          // Collapse the secondary nav bar when the toggle is clicked
          $secondaryItem.on('click.pf.secondarynav.data-api', '[data-toggle="collapse-secondary-nav"]', function (e) {
            var $this = $(this);
            if (inMobileState()) {
              updateMobileMenu();
              e.stopImmediatePropagation();
            } else {
              if ($this.hasClass('collapsed')) {
                if (options.rememberOpenState) {
                  window[storageLocation].setItem('patternfly-navigation-secondary', 'expanded');
                  window[storageLocation].setItem('patternfly-navigation-tertiary', 'expanded');
                }
                updateSecondaryCollapsedState(false, $this);
                forceHideSecondaryMenu();
              } else {
                if (options.rememberOpenState) {
                  window[storageLocation].setItem('patternfly-navigation-secondary', 'collapsed');
                }
                updateSecondaryCollapsedState(true, $this);
              }
            }
            navElement.removeClass('hover-secondary-nav-pf');
            if (handleSelection) {
              // Don't process the click on the parent item
              e.stopImmediatePropagation();
            }
          });

          $secondaryItem.find('.tertiary-nav-item-pf').each(function (index, primaryItem) {
            var $primaryItem = $(primaryItem);
            // Collapse the tertiary nav bar when the toggle is clicked
            $primaryItem.on('click.pf.tertiarynav.data-api', '[data-toggle="collapse-tertiary-nav"]', function (e) {
              var $this = $(this);
              if (inMobileState()) {
                updateMobileMenu($secondaryItem);
                e.stopImmediatePropagation();
              } else {
                if ($this.hasClass('collapsed')) {
                  if (options.rememberOpenState) {
                    window[storageLocation].setItem('patternfly-navigation-secondary', 'expanded');
                    window[storageLocation].setItem('patternfly-navigation-tertiary', 'expanded');
                  }
                  updateTertiaryCollapsedState(false, $this);
                  forceHideSecondaryMenu();
                } else {
                  if (options.rememberOpenState) {
                    window[storageLocation].setItem('patternfly-navigation-tertiary', 'collapsed');
                  }
                  updateTertiaryCollapsedState(true, $this);
                }
              }
              navElement.removeClass('hover-secondary-nav-pf');
              navElement.removeClass('hover-tertiary-nav-pf');
              if (handleSelection) {
                // Don't process the click on the parent item
                e.stopImmediatePropagation();
              }
            });
          });
        });

        // Show secondary nav bar on hover of secondary nav items
        $(document).on('mouseenter.pf.tertiarynav.data-api', '.secondary-nav-item-pf', function (e) {
          var $this = $(this);
          if (!inMobileState()) {
            if ($this[0].navUnHoverTimeout !== undefined) {
              clearTimeout($this[0].navUnHoverTimeout);
              $this[0].navUnHoverTimeout = undefined;
            } else if ($this[0].navHoverTimeout === undefined) {
              $this[0].navHoverTimeout = setTimeout(function () {
                navElement.addClass('hover-secondary-nav-pf');
                $this.addClass('is-hover');
                $this[0].navHoverTimeout = undefined;
              }, options.hoverDelay);
            }
          }
        });

        $(document).on('mouseleave.pf.tertiarynav.data-api', '.secondary-nav-item-pf', function (e) {
          var $this = $(this);
          if ($this[0].navHoverTimeout !== undefined) {
            clearTimeout($this[0].navHoverTimeout);
            $this[0].navHoverTimeout = undefined;
          } else if ($this[0].navUnHoverTimeout === undefined &&
              navElement.find('.secondary-nav-item-pf.is-hover').length > 0) {
            $this[0].navUnHoverTimeout = setTimeout(function () {
              if (navElement.find('.secondary-nav-item-pf.is-hover').length <= 1) {
                navElement.removeClass('hover-secondary-nav-pf');
              }
              $this.removeClass('is-hover');
              $this[0].navUnHoverTimeout = undefined;
            }, options.hideDelay);
          }
        });

        // Show tertiary nav bar on hover of secondary nav items
        $(document).on('mouseover.pf.tertiarynav.data-api', '.tertiary-nav-item-pf', function (e) {
          var $this = $(this);
          if (!inMobileState()) {
            if ($this[0].navUnHoverTimeout !== undefined) {
              clearTimeout($this[0].navUnHoverTimeout);
              $this[0].navUnHoverTimeout = undefined;
            } else if ($this[0].navHoverTimeout === undefined) {
              $this[0].navHoverTimeout = setTimeout(function () {
                navElement.addClass('hover-tertiary-nav-pf');
                $this.addClass('is-hover');
                $this[0].navHoverTimeout = undefined;
              }, options.hoverDelay);
            }
          }
        });
        $(document).on('mouseout.pf.tertiarynav.data-api', '.tertiary-nav-item-pf', function (e) {
          var $this = $(this);
          if ($this[0].navHoverTimeout !== undefined) {
            clearTimeout($this[0].navHoverTimeout);
            $this[0].navHoverTimeout = undefined;
          } else if ($this[0].navUnHoverTimeout === undefined) {
            $this[0].navUnHoverTimeout = setTimeout(function () {
              if (navElement.find('.tertiary-nav-item-pf.is-hover').length <= 1) {
                navElement.removeClass('hover-tertiary-nav-pf');
              }
              $this.removeClass('is-hover');
              $this[0].navUnHoverTimeout = undefined;
            }, options.hideDelay);
          }
        });
      },

      loadFromLocalStorage = function () {
        if (inMobileState()) {
          return;
        }

        if (window[storageLocation].getItem('patternfly-navigation-primary') === 'collapsed') {
          collapseMenu();
        }

        if ($('.nav-pf-vertical.nav-pf-vertical-collapsible-menus').length > 0) {
          if (window[storageLocation].getItem('patternfly-navigation-secondary') === 'collapsed') {
            updateSecondaryCollapsedState(true, $('.secondary-nav-item-pf.active [data-toggle=collapse-secondary-nav]'));
          }

          if (window[storageLocation].getItem('patternfly-navigation-tertiary') === 'collapsed') {
            updateTertiaryCollapsedState(true, $('.tertiary-nav-item-pf.active [data-toggle=collapse-tertiary-nav]'));
          }
        }
      },

      setTooltips = function () {
        var tooltipOptions = {
          container: 'body',
          placement: 'bottom',
          delay: { 'show': '500', 'hide': '200' },
          template: '<div class="nav-pf-vertical-tooltip tooltip" role="tooltip"><div class="tooltip-arrow"></div><div class="tooltip-inner"></div></div>'
        };
        $('.nav-pf-vertical [data-toggle="tooltip"]').tooltip(tooltipOptions);

        $('.nav-pf-vertical').on("show.bs.tooltip", function (e) {
          return $(this).hasClass("collapsed");
        });
      },

      init = function (handleItemSelections) {
        // Hide the nav menus during initialization
        navElement.addClass('hide-nav-pf');
        bodyContentElement.addClass('hide-nav-pf');

        //Set correct state on load
        checkNavState();

        // Bind Top level hamburger menu with menu behavior;
        bindMenuBehavior();

        // Bind menu items
        bindMenuItemsBehavior(handleItemSelections);

        //Set tooltips
        setTooltips();

        if (options.rememberOpenState) {
          loadFromLocalStorage();
        }

        // Show the nav menus
        navElement.removeClass('hide-nav-pf');
        bodyContentElement.removeClass('hide-nav-pf');
        forceResize(250);
      },

      self = {
        hideMenu: function () {
          handleResize = false;
          enterMobileState();
        },
        showMenu: function () {
          handleResize = true;
          exitMobileState();
        },
        isVisible: function () {
          return handleResize;
        }
      };

    if (!$.fn.setupVerticalNavigation.self) {
      $.fn.setupVerticalNavigation.self = self;
      //Listen for the window resize event and collapse/hide as needed
      $(window).on('resize', function () {
        checkNavState();
        enableTransitions();
      });

      init(handleItemSelections);
    }
    return $.fn.setupVerticalNavigation.self;
  };
}(jQuery));
