describe("area-charts test suite", function () {

  beforeEach(function () {
    globals.readFixture(globals.fixturePath + 'area-charts.html');
    renderAreaCharts();
  });

  it('should render an area chart with four data points', function (done) {
    var chart = $('#areaChart svg');
    var chartLegends = $('#areaChart .c3-legend-item');

    setTimeout(function () {
      expect(chart).toExist();
      expect(chartLegends).toHaveLength(4);
      done();
    }, globals.wait);
  });

  function renderAreaCharts() {
    //area chart
    var areaChartDataColumns = [
      ['data1', 350, 400, 350, 0],
      ['data2', 140, 100, 150, 205, 145, 50],
      ['data3', 10, 60, 90, 10, 325, 400],
      ['data4', 260, 10, 305, 100, 50, 150]
    ];
    var c3ChartDefaults = $().c3ChartDefaults();
    var areaChartConfig = c3ChartDefaults.getDefaultAreaConfig();
    areaChartConfig.bindto = '#areaChart';
    areaChartConfig.data = {
      columns: areaChartDataColumns,
      type: 'area-spline'
    };
    var areaChart = c3.generate(areaChartConfig);

    //single area chart
    var singleAreaChartDataColumns = [
      ['data2', 140, 100, 150, 205, 145, 50]
    ];

    var singleAreaChartConfig = c3ChartDefaults.getDefaultSingleAreaConfig();
    singleAreaChartConfig.bindto = '#singleAreaChart';
    singleAreaChartConfig.data = {
      columns: singleAreaChartDataColumns,
      type: 'area-spline'
    };
    var singleAreaChart = c3.generate(singleAreaChartConfig);
  }

});
