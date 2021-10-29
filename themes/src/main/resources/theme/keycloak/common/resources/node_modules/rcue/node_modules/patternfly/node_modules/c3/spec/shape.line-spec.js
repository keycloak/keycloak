import { parseSvgPath } from './svg-helper';

describe('c3 chart shape line', function () {
    'use strict';

    var chart, args;

    beforeEach(function (done) {
        chart = window.initChart(chart, args, done);
    });

    describe('shape-rendering for line chart', function () {

        beforeAll(function () {
            args = {
                data: {
                    columns: [
                        ['data1', 30, 200, 100, 400, -150, 250],
                        ['data2', 50, 20, 10, 40, 15, 25],
                        ['data3', -150, 120, 110, 140, 115, 125]
                    ],
                    type: 'line'
                }
            };
        });

        it("Should render the lines correctly", function(done) {
             setTimeout(function () {
                var target = chart.internal.main.select('.c3-chart-line.c3-target-data1');
                var commands = parseSvgPath( target.select('.c3-line-data1').attr('d'));
                expect(commands.length).toBe(6);
                done();
            }, 500);
        });

        it("should not have shape-rendering when it's line chart", function () {
            d3.selectAll('.c3-line').each(function () {
                var style = d3.select(this).style('shape-rendering');
                expect(style).toBe('auto');
            });
        });

        describe('should change to step chart', function () {
            beforeAll(function(){
                args.data.type = 'step';
            });

            it("should have shape-rendering = crispedges when it's step chart", function () {
                d3.selectAll('.c3-line').each(function () {
                    var style = d3.select(this).style('shape-rendering').toLowerCase();
                    expect(style).toBe('crispedges');
                });
            });
        });

        describe('should change to spline chart', function () {
            beforeAll(function(){
                args.data.type = 'spline';
            });

            it('should use cardinal interpolation by default', function () {
                expect(chart.internal.config.spline_interpolation_type).toBe('cardinal');
            });
        });

    });

    describe('point.show option', function () {

        describe('should change args to include null data', function () {
            beforeAll(function(){
                args = {
                    data: {
                        columns: [
                            ['data1', 30, null, 100, 400, -150, 250],
                            ['data2', 50, 20, 10, 40, 15, 25],
                            ['data3', -150, 120, 110, 140, 115, 125]
                        ],
                        type: 'line'
                    }
                };
            });

            it('should not show the circle for null', function (done) {
                setTimeout(function () {
                    var target = chart.internal.main.select('.c3-chart-line.c3-target-data1');
                    expect(+target.select('.c3-circle-0').style('opacity')).toBe(1);
                    expect(+target.select('.c3-circle-1').style('opacity')).toBe(0);
                    expect(+target.select('.c3-circle-2').style('opacity')).toBe(1);
                    done();
                }, 500);
            });

            it('should not draw a line segment for null data', function(done) {
                setTimeout(function () {
                    var target = chart.internal.main.select('.c3-chart-line.c3-target-data1');
                    var commands = parseSvgPath( target.select('.c3-line-data1').attr('d'));
                    var segments = 0;
                    for(var i = 0; i < commands.length; i++) {
                        (commands[i].command === 'L') ? segments++ : null;
                    }
                    expect(segments).toBe(3);
                    done();
                }, 500);
            });

            // it('should change args to include null data on scatter plot', function () {
            //     args = {
            //         data: {
            //             columns: [
            //                 ['data1', 30, null, 100, 400, -150, 250],
            //                 ['data2', 50, 20, 10, 40, 15, 25],
            //                 ['data3', -150, 120, 110, 140, 115, 125]
            //             ],
            //             type: 'scatter'
            //         }
            //     };
            //     expect(true).toBeTruthy();
            // });

            // it('should not show the circle for null', function (done) {
            //     setTimeout(function () {
            //         var target = chart.internal.main.select('.c3-chart-line.c3-target-data1');
            //         expect(+target.select('.c3-circle-0').style('opacity')).toBe(0.5);
            //         expect(+target.select('.c3-circle-1').style('opacity')).toBe(0);
            //         expect(+target.select('.c3-circle-2').style('opacity')).toBe(0.5);
            //         done();
            //     }, 500);
            // });
        });

        describe('should allow passing a function', function() {
            beforeAll(function(){
                args = {
                    data: {
                        columns: [
                            ['data1', 30, 50, 100]
                        ],
                        type: 'line'
                    },
                    point: {
                        show: function(d) {
                            return d.value > 50;
                        }
                    }
                };
            });

            it('should show point if function returns true', function() {
                var target = chart.internal.main.select('.c3-chart-line.c3-target-data1');
                expect(+target.select('.c3-circle-0').style('opacity')).toBe(0);
                expect(+target.select('.c3-circle-1').style('opacity')).toBe(0);
                expect(+target.select('.c3-circle-2').style('opacity')).toBe(1);
            });
        });
    });

    describe('spline.interpolation option', function () {

        beforeAll(function () {
            args = {
                data: {
                    columns: [
                        ['data1', 30, 200, 100, 400, -150, 250],
                        ['data2', 50, 20, 10, 40, 15, 25],
                        ['data3', -150, 120, 110, 140, 115, 125]
                    ],
                    type: 'spline'
                },
                spline: {
                    interpolation: {
                        type: 'monotone'
                    }
                }
            };
        });

        it('updates interpolation function', function() {
            expect(chart.internal.getInterpolate(chart.data()[0])).toBe('monotone');
        });

        describe('should not use a non-valid interpolation', function () {
            beforeAll(function(){
                args.spline.interpolation.type = 'foo';
            });

            it('should use cardinal interpolation when given option is not valid', function() {
                expect(chart.internal.getInterpolate(chart.data()[0])).toBe('cardinal');
            });
        });
    });

});
