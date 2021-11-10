describe("bar-charts test suite", function () {

  beforeEach(function () {
    globals.readFixture(globals.fixturePath + 'bar-charts.html');
    renderBarCharts();
  });

  it('should render a vertical bar chart with four bars', function (done) {
    var verticalBarChart = $('#verticalBarChart');
    var verticalBars = verticalBarChart.find('.c3-event-rects-single rect');

    setTimeout(function () {
      expect(verticalBarChart).toExist();
      expect(verticalBars).toHaveLength(4);
      done();
    }, globals.wait);
  });

  it('should render a stacked vertical bar chart with four bars', function (done) {
    var verticalBarChart = $('#stackedVerticalBarChart');
    var verticalBars = verticalBarChart.find('.c3-chart-bars .c3-chart-bar');

    setTimeout(function () {
      expect(verticalBarChart).toExist();
      expect(verticalBars).toHaveLength(4);
      done();
    }, globals.wait);
  });

  it('should render a stacked horizontal bar chart with four bars', function (done) {
    var horizontalBarChart = $('#stackedHorizontalBarChart');
    var horizontalBars = horizontalBarChart.find('.c3-chart-bars .c3-chart-bar');

    setTimeout(function () {
      expect(horizontalBarChart).toExist();
      expect(horizontalBars).toHaveLength(4);
      done();
    }, globals.wait);
  });

  function renderBarCharts() {
    var c3ChartDefaults = $().c3ChartDefaults();

    var chartUrls = [
      'https://www.gogole.com',
      'https://www.yahoo.com',
      'https://www.bing.com/',
      'https://duckduckgo.com/'
    ];
    var categories = ['Q1', 'Q2', 'Q3', 'Q4'];
    var columnsData = [
      ['data1', 400, 360, 320, 175]
    ];

    //vertical bar
    var verticalBarChartConfig = $().c3ChartDefaults().getDefaultBarConfig(categories);
    verticalBarChartConfig.bindto = '#verticalBarChart';
    verticalBarChartConfig.axis = {
      x: {
        categories: categories,
        type: 'category'
      }
    };
    verticalBarChartConfig.data = {
      type: 'bar',
      columns: columnsData,
      // optional drilldown behavior
      onclick: function (d, element) {
        window.location = chartUrls[d.index];
      }
    };
    var verticalBarChart = c3.generate(verticalBarChartConfig);

    //grouped vertical bar
    var groupedcCategories = ['2013', '2014', '2015'];
    var groupedColumnsData = [
      ['Q1', 400, 250, 375],
      ['Q2', 355, 305, 300],
      ['Q3', 315, 340, 276],
      ['Q4', 180, 390, 190]
    ];
    var groupedColors = {
      pattern: [
        $.pfPaletteColors.red,
        $.pfPaletteColors.blue,
        $.pfPaletteColors.orange,
        $.pfPaletteColors.green
      ]
    };

    var groupedVerticalBarChartConfig = $().c3ChartDefaults().getDefaultGroupedBarConfig();
    groupedVerticalBarChartConfig.bindto = '#groupedVerticalBarChart';
    groupedVerticalBarChartConfig.axis = {
      x: {
        categories: groupedcCategories,
        type: 'category'
      }
    };
    groupedVerticalBarChartConfig.data = {
      type: 'bar',
      columns: groupedColumnsData,
      // optional drilldown behavior
      onclick: function (d, element) {
        window.location = chartUrls[d.index];
      }
    };
    groupedVerticalBarChartConfig.color = groupedColors;
    var groupedVerticalBarChart = c3.generate(groupedVerticalBarChartConfig);

    //stacked vertical bar
    var stackedColumnsData = [
      ['Q1', 400, 250, 375],
      ['Q2', 355, 305, 300],
      ['Q3', 315, 340, 276],
      ['Q4', 180, 390, 190]
    ];
    var stackedGroups = [['Q1', 'Q2', 'Q3', 'Q4']];
    var stackedCategories = ['2013', '2014', '2015'];
    var stackedColors = {
      pattern: [
        $.pfPaletteColors.red,
        $.pfPaletteColors.blue,
        $.pfPaletteColors.orange,
        $.pfPaletteColors.green
      ]
    };

    var stackedVerticalBarChartConfig = $().c3ChartDefaults().getDefaultStackedBarConfig();
    stackedVerticalBarChartConfig.bindto = '#stackedVerticalBarChart';
    stackedVerticalBarChartConfig.axis = {
      x: {
        categories: stackedCategories,
        type: 'category'
      }
    };
    stackedVerticalBarChartConfig.data = {
      type: 'bar',
      columns: stackedColumnsData,
      groups: stackedGroups,
      // optional drilldown behavior
      onclick: function (d, element) {
        window.location = chartUrls[d.index];
      },
      order: null
    };
    stackedVerticalBarChartConfig.color = stackedColors;
    var stackedVerticalBarChart = c3.generate(stackedVerticalBarChartConfig);

    //horizontal bar
    var horizontalBarChartConfig = $().c3ChartDefaults().getDefaultBarConfig(categories);
    horizontalBarChartConfig.bindto = '#horizontalBarChart';
    horizontalBarChartConfig.axis = {
      rotated: true,
      x: {
        categories: categories,
        type: 'category'
      }
    };
    horizontalBarChartConfig.data = {
      type: 'bar',
      columns: columnsData,
      // optional drilldown behavior
      onclick: function (d, element) {
        window.location = chartUrls[d.index];
      }
    };
    var horizontalBarChart = c3.generate(horizontalBarChartConfig);

    //grouped horizontal bar
    var groupedHorizontalBarChartConfig = $().c3ChartDefaults().getDefaultGroupedBarConfig();
    groupedHorizontalBarChartConfig.bindto = '#groupedHorizontalBarChart';
    groupedHorizontalBarChartConfig.axis = {
      rotated: true,
      x: {
        categories: groupedcCategories,
        type: 'category'
      }
    };
    groupedHorizontalBarChartConfig.data = {
      type: 'bar',
      columns: groupedColumnsData,
      // optional drilldown behavior
      onclick: function (d, element) {
        window.location = chartUrls[d.index];
      }
    };
    groupedHorizontalBarChartConfig.color = groupedColors;
    var groupedHorizontalBarChart = c3.generate(groupedHorizontalBarChartConfig);

    //stacked horizontal bar
    var stackedHorizontalBarChartConfig = $().c3ChartDefaults().getDefaultStackedBarConfig();
    stackedHorizontalBarChartConfig.bindto = '#stackedHorizontalBarChart';
    stackedHorizontalBarChartConfig.axis = {
      rotated: true,
      x: {
        categories: stackedCategories,
        type: 'category'
      }
    };
    stackedHorizontalBarChartConfig.data = {
      type: 'bar',
      columns: stackedColumnsData,
      groups: stackedGroups,
      // optional drilldown behavior
      onclick: function (d, element) {
        window.location = chartUrls[d.index];
      },
      order: null
    };
    stackedHorizontalBarChartConfig.color = stackedColors;
    var stackedHorizontalBarChart = c3.generate(stackedHorizontalBarChartConfig);
  }

});
