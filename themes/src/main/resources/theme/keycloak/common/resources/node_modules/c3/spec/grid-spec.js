describe('c3 chart grid', function () {
    'use strict';

    var chart, args;

    beforeEach(function (done) {
        chart = window.initChart(chart, args, done);
    });

    describe('y grid show', function () {

        beforeAll(function () {
            args = {
                data: {
                    columns: [
                        ['data1', 30, 200, 100, 400, 150, 250]
                    ]
                },
                axis: {
                    y: {
                        tick: {
                        }
                    }
                },
                grid: {
                    y: {
                        show: false
                    }
                }
            };
        });

        it('should not show y grids', function () {
            expect(chart.internal.main.select('.c3-ygrids').size()).toBe(0);
        });

        describe('with y grids', function () {
            beforeAll(function(){
                args.grid.y.show = true;
            });

            it('should show y grids', function () {
                var ygrids = chart.internal.main.select('.c3-ygrids');
                expect(ygrids.size()).toBe(1);
                expect(ygrids.selectAll('.c3-ygrid').size()).toBe(9);
            });
        });

        describe('with only 3 y grids', function () {
            beforeAll(function(){
                args.grid.y.ticks = 3;
            });

            it('should show only 3 y grids', function () {
                var ygrids = chart.internal.main.select('.c3-ygrids');
                expect(ygrids.size()).toBe(1);
                expect(ygrids.selectAll('.c3-ygrid').size()).toBe(3);
            });
        });

        describe('with y grids depending on y axis ticks', function () {
            beforeAll(function(){
                args.axis.y.tick.count = 5;
            });

            it('should show grids depending on y axis ticks', function () {
                var ygrids = chart.internal.main.select('.c3-ygrids'),
                    expectedYs = [];
                ygrids.selectAll('.c3-ygrid').each(function (d, i) {
                    expectedYs[i] = +d3.select(this).attr('y1');
                });
                expect(ygrids.size()).toBe(1);
                expect(ygrids.selectAll('.c3-ygrid').size()).toBe(5);
                chart.internal.main.select('.c3-axis-y').selectAll('.tick').each(function (d, i) {
                    var t = d3.transform(d3.select(this).attr('transform'));
                    expect(t.translate[1]).toBe(expectedYs[i]);
                });
            });
        });
    });

    describe('y grid lines', function () {

        describe('position', function () {

            beforeAll(function () {
                args = {
                    data: {
                        columns: [
                            ['data1', 10, 200, 100, 400, 150, 250]
                        ]
                    },
                    grid: {
                        y: {
                            lines: [
                                {value: 30, text: 'Label 30', position: 'start'},
                                {value: 145, text: 'Label 145', position: 'middle'},
                                {value: 225, text: 'Label 225'}
                            ]
                        }
                    }
                };
            });

            it('should show 3 grid lines', function () {
                expect(chart.internal.main.selectAll('.c3-ygrid-lines .c3-ygrid-line').size()).toBe(3);
            });

            it('should locate grid lines properly', function () {
                var lines = chart.internal.main.selectAll('.c3-ygrid-lines .c3-ygrid-line'),
                    expectedY1s = [373, 268, 196];
                lines.each(function (d, i) {
                    var y1 = d3.select(this).select('line').attr('y1');
                    expect(y1).toBeCloseTo(expectedY1s[i], -2);
                });
            });

            it('should locate grid texts properly', function () {
                var lines = chart.internal.main.selectAll('.c3-ygrid-lines .c3-ygrid-line'),
                    expectedPositions = ['start', 'middle', 'end'],
                    expectedDxs = [4, 0, -4];
                lines.each(function (d, i) {
                    var text = d3.select(this).select('text'),
                        textAnchor = text.attr('text-anchor'),
                        dx = text.attr('dx');
                    expect(textAnchor).toBe(expectedPositions[i]);
                    expect(+dx).toBe(expectedDxs[i]);
                });
            });

            describe('three gridlines', function () {
                beforeAll(function(){
                    args = {
                        data: {
                            columns: [
                                ['data1', 10, 200, 100, 400, 150, 250]
                            ]
                        },
                        axis: {
                            rotated: true
                        },
                        grid: {
                            y: {
                                lines: [
                                    {value: 30, text: 'Label 30', position: 'start'},
                                    {value: 145, text: 'Label 145', position: 'middle'},
                                    {value: 225, text: 'Label 225'}
                                ]
                            }
                        }
                    };
                });

                it('should show 3 grid lines', function () {
                    expect(chart.internal.main.selectAll('.c3-ygrid-lines .c3-ygrid-line').size()).toBe(3);
                });

                it('should locate grid lines properly', function () {
                    var lines = chart.internal.main.selectAll('.c3-ygrid-lines .c3-ygrid-line'),
                        expectedX1s = [75, 220, 321];
                    lines.each(function (d, i) {
                        var x1 = d3.select(this).select('line').attr('x1');
                        expect(x1).toBeCloseTo(expectedX1s[i], -2);
                    });
                });

                it('should locate grid texts properly', function () {
                    var lines = chart.internal.main.selectAll('.c3-ygrid-lines .c3-ygrid-line'),
                        expectedPositions = ['start', 'middle', 'end'],
                        expectedDxs = [4, 0, -4];
                    lines.each(function (d, i) {
                        var text = d3.select(this).select('text'),
                            textAnchor = text.attr('text-anchor'),
                            dx = text.attr('dx');
                        expect(textAnchor).toBe(expectedPositions[i]);
                        expect(+dx).toBe(expectedDxs[i]);
                    });
                });
            });
        });
    });

    describe('x grid lines', function () {

        describe('position', function () {

            beforeAll(function () { // 'should have correct height',
                args = {
                    data: {
                        columns: [
                            ['data1', 30, 200, 100, 400],
                        ]
                    },
                    grid: {
                        x: {
                            lines: [
                                {value: 1, text: 'Label 1', position: 'start'},
                                {value: 2, text: 'Label 2', position: 'middle'},
                                {value: 3, text: 'Label 3'},
                            ]
                        }
                    },
                };
            });

            it('should show 3 grid lines', function () {
                expect(chart.internal.main.selectAll('.c3-xgrid-lines .c3-xgrid-line').size()).toBe(3);
            });

            it('should locate grid lines properly', function () {
                var lines = chart.internal.main.selectAll('.c3-xgrid-lines .c3-xgrid-line'),
                    expectedX1s = [202, 397, 593];
                lines.each(function (d, i) {
                    var x1 = d3.select(this).select('line').attr('x1');
                    expect(x1).toBeCloseTo(expectedX1s[i], -2);
                });
            });

            it('should locate grid texts properly', function () {
                var lines = chart.internal.main.selectAll('.c3-xgrid-lines .c3-xgrid-line'),
                    expectedPositions = ['start', 'middle', 'end'],
                    expectedDxs = [4, 0, -4];
                lines.each(function (d, i) {
                    var text = d3.select(this).select('text'),
                        textAnchor = text.attr('text-anchor'),
                        dx = text.attr('dx');
                    expect(textAnchor).toBe(expectedPositions[i]);
                    expect(+dx).toBe(expectedDxs[i]);
                });
            });

            describe('three grid lines', function () {
                beforeAll(function(){
                    args = {
                        data: {
                            columns: [
                                ['data1', 30, 200, 100, 400],
                            ]
                        },
                        axis: {
                            rotated: true
                        },
                        grid: {
                            x: {
                                lines: [
                                    {value: 1, text: 'Label 1', position: 'start'},
                                    {value: 2, text: 'Label 2', position: 'middle'},
                                    {value: 3, text: 'Label 3'},
                                ]
                            }
                        },
                    };
                });

                it('should show 3 grid lines', function () {
                    expect(chart.internal.main.selectAll('.c3-xgrid-lines .c3-xgrid-line').size()).toBe(3);
                });

                it('should locate grid lines properly', function () {
                    var lines = chart.internal.main.selectAll('.c3-xgrid-lines .c3-xgrid-line'),
                        expectedY1s = [144, 283, 421];
                    lines.each(function (d, i) {
                        var y1 = d3.select(this).select('line').attr('y1');
                        expect(y1).toBeCloseTo(expectedY1s[i], -2);
                    });
                });

                it('should locate grid texts properly', function () {
                    var lines = chart.internal.main.selectAll('.c3-xgrid-lines .c3-xgrid-line'),
                        expectedPositions = ['start', 'middle', 'end'],
                        expectedDxs = [4, 0, -4];
                    lines.each(function (d, i) {
                        var text = d3.select(this).select('text'),
                            textAnchor = text.attr('text-anchor'),
                            dx = text.attr('dx');
                        expect(textAnchor).toBe(expectedPositions[i]);
                        expect(+dx).toBe(expectedDxs[i]);
                    });
                });
            });
        });

        describe('with padding.top', function () {

            describe('should have correct height', function () {
                beforeAll(function(){
                    args = {
                        data: {
                            columns: [
                                ['data1', 30, 200, 100, 400],
                            ]
                        },
                        grid: {
                            x: {
                                lines: [
                                    {value: 3, text: 'Label 3'}
                                ]
                            }
                        },
                        padding: {
                            top: 50
                        }
                    };
                });

                it('should show x grid lines', function () {
                    var lines = chart.internal.main.select('.c3-xgrid-lines .c3-xgrid-line'),
                        expectedX1 = 593,
                        expectedText = ['Label 3'];
                    lines.each(function (id, i) {
                        var line = d3.select(this),
                            l = line.select('line'),
                            t = line.select('text');
                        expect(+l.attr('x1')).toBeCloseTo(expectedX1, -2);
                        expect(t.text()).toBe(expectedText[i]);
                    });
                });
            });
        });

        describe('on category axis', function () {

            beforeAll(function () {
                args = {
                    data: {
                        x: 'x',
                        columns: [
                            ['x', 'a', 'b', 'c', 'd'],
                            ['data1', 30, 200, 100, 400],
                        ]
                    },
                    axis: {
                        x: {
                            type: 'category'
                        }
                    },
                    grid: {
                        x: {
                            lines: [
                                {value: 3, text: 'Label 3'},
                                {value: 'a', text: 'Label a'}
                            ]
                        }
                    }
                };
            });

            it('should show x grid lines', function () {
                var lines = chart.internal.main.selectAll('.c3-xgrid-lines .c3-xgrid-line'),
                    expectedX1 = [515, 74],
                    expectedText = ['Label 3', 'Label a'];
                lines.each(function (id, i) {
                    var line = d3.select(this),
                        l = line.select('line'),
                        t = line.select('text');
                    expect(+l.attr('x1')).toBeCloseTo(expectedX1[i], -2);
                    expect(t.text()).toBe(expectedText[i]);
                });
            });

        });
    });

});
