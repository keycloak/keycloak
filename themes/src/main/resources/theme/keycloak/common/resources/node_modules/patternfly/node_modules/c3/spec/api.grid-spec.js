describe('c3 api grid', function () {
    'use strict';

    var chart, args;

    beforeEach(function (done) {
        chart = window.initChart(chart, args, done);
    });

    describe('ygrid.add and ygrid.remove', function () {

        beforeAll(function () {
            args = {
                data: {
                    columns: [
                        ['data1', 30, 200, 100, 400, 150, 250]
                    ]
                }
            };
        });

        it('updates y grids', function (done) {
            var main = chart.internal.main,
                expectedGrids = [
                    {
                        value: 100,
                        text: 'Pressure Low'
                    },
                    {
                        value: 200,
                        text: 'Pressure High'
                    }
                ],
                grids;

            // Call ygrids.add
            chart.ygrids.add(expectedGrids);
            setTimeout(function () {
                grids = main.selectAll('.c3-ygrid-line');
                expect(grids.size()).toBe(expectedGrids.length);
                grids.each(function (d, i) {
                    var y = +d3.select(this).select('line').attr('y1'),
                        text = d3.select(this).select('text').text(),
                        expectedY = Math.round(chart.internal.y(expectedGrids[i].value)),
                        expectedText = expectedGrids[i].text;
                    expect(y).toBe(expectedY);
                    expect(text).toBe(expectedText);
                });

                // Call ygrids.remove
                chart.ygrids.remove(expectedGrids);
                setTimeout(function () {
                    grids = main.selectAll('.c3-ygrid-line');
                    expect(grids.size()).toBe(0);
                }, 500);

            }, 500);

            setTimeout(function () {
                done();
            }, 1200);
        });

        it('updates x ygrids even if zoomed', function (done) {
            var main = chart.internal.main,
                expectedGrids = [
                    {
                        value: 0,
                        text: 'Pressure Low'
                    },
                    {
                        value: 1,
                        text: 'Pressure High'
                    }
                ],
                grids, domain;

            chart.zoom([0, 2]);
            setTimeout(function () {

                // Call xgrids
                chart.xgrids(expectedGrids);
                setTimeout(function () {
                    grids = main.selectAll('.c3-xgrid-line');
                    expect(grids.size()).toBe(expectedGrids.length);
                    grids.each(function (d, i) {
                        var x = +d3.select(this).select('line').attr('x1'),
                            text = d3.select(this).select('text').text(),
                            expectedX = Math.round(chart.internal.x(expectedGrids[i].value)),
                            expectedText = expectedGrids[i].text;
                        expect(x).toBe(expectedX);
                        expect(text).toBe(expectedText);
                    });

                    // check if it was not rescaled
                    domain = chart.internal.y.domain();
                    expect(domain[0]).toBeLessThan(0);
                    expect(domain[1]).toBeGreaterThan(400);

                    // Call xgrids.remove
                    chart.xgrids.remove(expectedGrids);
                    setTimeout(function () {
                        grids = main.selectAll('.c3-xgrid-line');
                        expect(grids.size()).toBe(0);
                    }, 500); // for xgrids.remove()

                }, 500); // for xgrids()

            }, 500); // for zoom

            setTimeout(function () {
                done();
            }, 1700);
        });

    });

});
