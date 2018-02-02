describe('c3 api region', function () {
    'use strict';

    var chart, args;

    beforeEach(function (done) {
        chart = window.initChart(chart, args, done);
    });

    describe('api.region', function () {

        beforeAll(function () {
            args = {
                data: {
                    columns: [
                        ['data1', 30, 200, 100, 400, 150, 250],
                    ]
                },
                regions: [
                    {
                        axis: 'y',
                        start: 300,
                        end: 400,
                        class: 'green',
                    },
                    {
                        axis: 'y',
                        start: 0,
                        end: 100,
                        class: 'green',
                    }
                ]
            };
        });

        it('updates regions', function (done) {
            var main = chart.internal.main,
                expectedRegions = [
                    {
                        axis: 'y',
                        start: 250,
                        end: 350,
                        class: 'red'
                    },
                    {
                        axis: 'y',
                        start: 25,
                        end: 75,
                        class: 'red'
                    }
                ],
                regions;

            // Call regions API
            chart.regions(expectedRegions);
            setTimeout(function () {
                regions = main.selectAll('.c3-region');
                expect(regions.size()).toBe(expectedRegions.length);

                regions.each(function (d, i) {
                    var region = d3.select(this),
                        rect = region.select('rect'),
                        y = +rect.attr('y'),
                        height = +rect.attr('height'),
                        expectedClass = 'red',
                        unexpectedClass = 'green',
                        expectedStart = Math.round(chart.internal.y(expectedRegions[i].start)),
                        expectedEnd = Math.round(chart.internal.y(expectedRegions[i].end)),
                        expectedY = expectedEnd,
                        expectedHeight = expectedStart - expectedEnd;
                    expect(y).toBeCloseTo(expectedY, -1);
                    expect(height).toBeCloseTo(expectedHeight, -1);
                    expect(region.classed(expectedClass)).toBeTruthy();
                    expect(region.classed(unexpectedClass)).toBeFalsy();
                });
            }, 500);

            setTimeout(function () {
                done();
            }, 1000);
        });
    });

    describe('api.region.add', function () {

        beforeAll(function () {
            args = {
                data: {
                    columns: [
                        ['data1', 30, 200, 100, 400, 150, 250],
                    ]
                },
                regions: [
                    {
                        axis: 'y',
                        start: 300,
                        end: 400,
                        class: 'green',
                    },
                    {
                        axis: 'y',
                        start: 0,
                        end: 100,
                        class: 'green',
                    }
                ]
            };
        });

        it('should add regions', function (done) {
            var main = chart.internal.main,
                expectedRegions = [
                    {
                        axis: 'y',
                        start: 300,
                        end: 400,
                        class: 'green',
                    },
                    {
                        axis: 'y',
                        start: 0,
                        end: 100,
                        class: 'green',
                    },
                    {
                        axis: 'y',
                        start: 250,
                        end: 350,
                        class: 'red'
                    },
                    {
                        axis: 'y',
                        start: 25,
                        end: 75,
                        class: 'red'
                    }
                ],
                expectedClasses = [
                    'green',
                    'green',
                    'red',
                    'red',
                ],
                regions;

            // Call regions API
            chart.regions(expectedRegions);
            setTimeout(function () {
                regions = main.selectAll('.c3-region');
                expect(regions.size()).toBe(expectedRegions.length);

                regions.each(function (d, i) {
                    var region = d3.select(this),
                        rect = region.select('rect'),
                        y = +rect.attr('y'),
                        height = +rect.attr('height'),
                        expectedClass = expectedClasses[i],
                        expectedStart = Math.round(chart.internal.y(expectedRegions[i].start)),
                        expectedEnd = Math.round(chart.internal.y(expectedRegions[i].end)),
                        expectedY = expectedEnd,
                        expectedHeight = expectedStart - expectedEnd;
                    expect(y).toBeCloseTo(expectedY, -1);
                    expect(height).toBeCloseTo(expectedHeight, -1);
                    expect(region.classed(expectedClass)).toBeTruthy();
                });
            }, 500);

            setTimeout(function () {
                done();
            }, 1000);
        });
    });

    describe('api.region.remove', function () {

        beforeAll(function () {
            args = {
                data: {
                    columns: [
                        ['data1', 30, 200, 100, 400, 150, 250],
                    ]
                },
                regions: [
                    {
                        axis: 'y',
                        start: 300,
                        end: 400,
                        class: 'green',
                    },
                    {
                        axis: 'y',
                        start: 0,
                        end: 100,
                        class: 'green',
                    },
                    {
                        axis: 'y',
                        start: 250,
                        end: 350,
                        class: 'red'
                    },
                ]
            };
        });

        it('should remove regions', function (done) {
            var main = chart.internal.main,
                expectedRegions = [
                    {
                        axis: 'y',
                        start: 250,
                        end: 350,
                        class: 'red'
                    },
                ],
                expectedClasses = ['red'],
                regions;

            // Call regions API
            chart.regions(expectedRegions);
            setTimeout(function () {
                regions = main.selectAll('.c3-region');
                expect(regions.size()).toBe(expectedRegions.length);

                regions.each(function (d, i) {
                    var region = d3.select(this),
                        rect = region.select('rect'),
                        y = +rect.attr('y'),
                        height = +rect.attr('height'),
                        expectedClass = expectedClasses[i],
                        expectedStart = Math.round(chart.internal.y(expectedRegions[i].start)),
                        expectedEnd = Math.round(chart.internal.y(expectedRegions[i].end)),
                        expectedY = expectedEnd,
                        expectedHeight = expectedStart - expectedEnd;
                    expect(y).toBeCloseTo(expectedY, -1);
                    expect(height).toBeCloseTo(expectedHeight, -1);
                    expect(region.classed(expectedClass)).toBeTruthy();
                });
            }, 500);

            setTimeout(function () {
                done();
            }, 1000);
        });
    });

});
