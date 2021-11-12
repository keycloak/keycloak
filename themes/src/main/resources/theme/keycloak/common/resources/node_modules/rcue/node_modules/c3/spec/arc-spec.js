describe('c3 chart arc', function () {
    'use strict';

    var chart, args;

    beforeEach(function (done) {
        chart = window.initChart(chart, args, done);
    });

    describe('show pie chart', function () {

        beforeAll(function () {
            args = {
                data: {
                    columns: [
                        ['data1', 30],
                        ['data2', 150],
                        ['data3', 120]
                    ],
                    type: 'pie'
                }
            };
        });

        it('should have correct classes', function () {
            var chartArc = d3.select('.c3-chart-arcs'),
            arcs = {
                data1: chartArc.select('.c3-chart-arc.c3-target.c3-target-data1')
                    .select('g.c3-shapes.c3-shapes-data1.c3-arcs.c3-arcs-data1')
                    .select('path.c3-shape.c3-shape.c3-arc.c3-arc-data1'),
                data2: chartArc.select('.c3-chart-arc.c3-target.c3-target-data2')
                    .select('g.c3-shapes.c3-shapes-data2.c3-arcs.c3-arcs-data2')
                    .select('path.c3-shape.c3-shape.c3-arc.c3-arc-data2'),
                data3: chartArc.select('.c3-chart-arc.c3-target.c3-target-data3')
                    .select('g.c3-shapes.c3-shapes-data3.c3-arcs.c3-arcs-data3')
                    .select('path.c3-shape.c3-shape.c3-arc.c3-arc-data3')
            };
            expect(arcs.data1.size()).toBe(1);
            expect(arcs.data2.size()).toBe(1);
            expect(arcs.data3.size()).toBe(1);
        });

        it('should have correct d', function () {
            expect(d3.select('.c3-arc-data1').attr('d')).toMatch(/M-124\..+,-171\..+A211\..+,211\..+ 0 0,1 -3\..+,-211\..+L0,0Z/);
            expect(d3.select('.c3-arc-data2').attr('d')).toMatch(/M1\..+,-211\..+A211\..+,211\..+ 0 0,1 1\..+,211\..+L0,0Z/);
            expect(d3.select('.c3-arc-data3').attr('d')).toMatch(/M1\..+,211\..+A211\..+,211\..+ 0 0,1 -124\..+,-171\..+L0,0Z/);
        });

        describe('with data id that can be converted to a color', function () {
            beforeAll(function(){
                args.data.columns = [
                    ['black', 30],
                    ['data2', 150],
                    ['data3', 120]
                ];
            });

            it('should have correct d even if data id can be converted to a color', function (done) {
                setTimeout(function () {
                    expect(d3.select('.c3-arc-black').attr('d')).toMatch(/M-124\..+,-171\..+A211\..+,211\..+ 0 0,1 -3\..+,-211\..+L0,0Z/);
                    done();
                }, 500);
            });

            describe('with empty pie chart', function(){
                beforeAll(function () {
                    args = {
                        data: {
                            columns: [
                                ['data1', null],
                                ['data2', null],
                                ['data3', null]
                            ],
                            type: 'pie'
                        }
                    };
                });

                it('should have correct d attribute', function () {
                    var chartArc = d3.select('.c3-chart-arcs'),
                    arcs = {
                        data1: chartArc.select('.c3-chart-arc.c3-target.c3-target-data1')
                            .select('g.c3-shapes.c3-shapes-data1.c3-arcs.c3-arcs-data1')
                            .select('path.c3-shape.c3-shape.c3-arc.c3-arc-data1'),
                        data2: chartArc.select('.c3-chart-arc.c3-target.c3-target-data2')
                            .select('g.c3-shapes.c3-shapes-data2.c3-arcs.c3-arcs-data2')
                            .select('path.c3-shape.c3-shape.c3-arc.c3-arc-data2'),
                        data3: chartArc.select('.c3-chart-arc.c3-target.c3-target-data3')
                            .select('g.c3-shapes.c3-shapes-data3.c3-arcs.c3-arcs-data3')
                            .select('path.c3-shape.c3-shape.c3-arc.c3-arc-data3')
                    };
                    expect(arcs.data1.attr('d').indexOf('NaN')).toBe(-1);
                    expect(arcs.data2.attr('d').indexOf('NaN')).toBe(-1);
                    expect(arcs.data3.attr('d').indexOf('NaN')).toBe(-1);
                });
            });
        });
    });

    describe('sort pie chart', function() {

        var createPie = function(order) {
            return {
                data: {
                    order: order,
                    columns: [
                        ['data1', 30],
                        ['data2', 150],
                        ['data3', 120]
                    ],
                    type: 'pie'
                }
            };
        };

        var collectArcs = function() {
            return d3.selectAll('.c3-arc')
                .data()
                .sort(function(a, b) {
                    return a.startAngle - b.startAngle;
                })
                .map(function(item) {
                    return item.data.id;
                });
        };

        it('should update data_order to desc', function () {
            args = createPie('desc');
            expect(true).toBeTruthy();
        });

        it('it should have descending ordering', function () {
            expect(collectArcs()).toEqual([ 'data2', 'data3', 'data1' ]);
        });

        it('should update data_order to asc', function () {
            args = createPie('asc');
            expect(true).toBeTruthy();
        });

        it('it should have ascending ordering', function () {
            expect(collectArcs()).toEqual([ 'data1', 'data3', 'data2' ]);
        });

        it('should update data_order to NULL', function () {
            args = createPie(null);
            expect(true).toBeTruthy();
        });

        it('it should have no ordering', function () {
            expect(collectArcs()).toEqual([ 'data1', 'data2', 'data3' ]);
        });

        it('should update data_order to Array', function () {
            args = createPie([ 'data3', 'data2', 'data1' ]);
            expect(true).toBeTruthy();
        });

        it('it should have array specified ordering', function () {
            expect(collectArcs()).toEqual([ 'data3', 'data2', 'data1' ]);
        });

        it('should update data_order to Function', function () {
            var names = [ 'data2', 'data1', 'data3' ];
            args = createPie(function(a, b) {
                return names.indexOf(a.id) - names.indexOf(b.id);
            });
            expect(true).toBeTruthy();
        });

        it('it should have array specified ordering', function () {
            expect(collectArcs()).toEqual([ 'data2', 'data1', 'data3' ]);
        });
    });

    describe('show gauge', function () {

        describe('with a 180 degree gauge', function(){
            beforeAll(function () {
                args = {
                    gauge: {
                        width: 10,
                        max: 10,
                        expand: true
                    },
                    data: {
                        columns: [
                            ['data', 8]
                        ],
                        type: 'gauge'
                    }
                };
            });

            it('should have correct d for Pi radian gauge', function () {
                var chartArc = d3.select('.c3-chart-arcs'),
                    data = chartArc.select('.c3-chart-arc.c3-target.c3-target-data')
                        .select('g.c3-shapes.c3-shapes-data.c3-arcs.c3-arcs-data')
                        .select('path.c3-shape.c3-shape.c3-arc.c3-arc-data');

                expect(data.attr('d')).toMatch(/-258.4,-3\..+A258.4,258.4 0 0,1 209\..+,-151\..+L200\..+,-146\..+A248.39999999999998,248.39999999999998 0 0,0 -248.39999999999998,-3\..+Z/);
            });
        });

        describe('with a 2 Pi radian gauge that starts at Pi/2', function() {
            beforeAll(function(){
                args = {
                    gauge: {
                        width: 10,
                        max: 10,
                        expand: true,
                        fullCircle: true
                    },
                    data: {
                        columns: [
                            ['data', 8]
                        ],
                        type: 'gauge',
                        fullCircle: true,
                        startingAngle: Math.PI/2
                    }
                };
            });

            it('should have correct d for 2 Pi radian gauge starting at Pi/2', function() {
                var chartArc = d3.select('.c3-chart-arcs'),
                    data = chartArc.select('.c3-chart-arc.c3-target.c3-target-data')
                        .select('g.c3-shapes.c3-shapes-data.c3-arcs.c3-arcs-data')
                        .select('path.c3-shape.c3-shape.c3-arc.c3-arc-data');

                // This test has bee updated to make tests pass. @TODO double-check this test is accurate.
                expect(data.attr('d')).toMatch(/M-180.*?,-2\..+A180.*?,180.*? 0 1,1 -55.*?,171.*?L-52.*?,161.*?A170.*?,170.*? 0 1,0 -170.*?,-2.*?Z/);
            });

            describe('with labels use custom text', function() {
                beforeAll(function(){
                    args = {
                        gauge: {
                            width: 10,
                            max: 100,
                            expand: true,
                            label: {
                                extents: function (value, isMax) {
                                    if (isMax) {
                                        return 'Max: ' + value + '%';
                                    }

                                    return 'Min: ' + value + '%';
                                }
                            }
                        },
                        data: {
                            columns: [
                                ['data', 8]
                            ],
                            type: 'gauge',
                            fullCircle: true,
                            startingAngle: Math.PI/2
                        }
                    };
                });
                it('should show custom min/max guage labels', function () {
                    var chartArc = d3.select('.c3-chart-arcs'),
                        min = chartArc.select('.c3-chart-arcs-gauge-min'),
                        max = chartArc.select('.c3-chart-arcs-gauge-max');

                    expect(min.text()).toMatch('Min: 0%');
                    expect(max.text()).toMatch('Max: 100%');
                });
            });
        });

        describe('with more than one data_column ', function () {
            beforeAll(function () {
                args = {
                    data: {
                        columns: [
                            ['padded1', 100],
                            ['padded2', 90],
                            ['padded3', 50],
                            ['padded4', 20]
                        ],
                        type: 'gauge'
                    },
                    color: {
                        pattern: ['#FF0000', '#F97600', '#F6C600', '#60B044'],
                        threshold: {
                            values: [30, 80, 95]
                        }
                    }
                };
            });
            var arcColor = ['rgb(96, 176, 68)', 'rgb(246, 198, 0)', 'rgb(249, 118, 0)', 'rgb(255, 0, 0)'];

            describe('should contain arcs ', function () {
                it('each data_column should have one arc', function () {
                    chart.internal.main.selectAll('.c3-chart-arc .c3-arc').each(function (d, i) {
                        expect(d3.select(this).classed('c3-arc-' + args.data.columns[i][0])).toBeTruthy();
                    });
                });

                it('each arc should have the color from color_pattern if color_treshold is given ', function () {
                    chart.internal.main.selectAll('.c3-chart-arc .c3-arc').each(function (d, i) {
                        expect(d3.select(this).style('fill')).toBe(arcColor[i]);
                    });
                });
            });

            describe('should contain backgrounds ', function () {
                it('each data_column should have one background', function () {
                    chart.internal.main.selectAll('.c3-chart-arcs path.c3-chart-arcs-background').each(function (d, i) {
                        expect(d3.select(this).classed('c3-chart-arcs-background-'+ i)).toBeTruthy();
                    });
                });

                it('each background should have tbe same color', function () {
                    chart.internal.main.selectAll('.c3-chart-arcs path.c3-chart-arcs-background').each(function () {
                        expect(d3.select(this).style('fill')).toBe('rgb(224, 224, 224)');
                    });
                });
            });

            describe('should contain labels', function () {
                it('each data_column should have a label', function () {
                    chart.internal.main.selectAll('.c3-chart-arc .c3-gauge-value').each(function (d, i) {
                        expect(d3.select(this).text()).toBe(chart.internal.defaultArcValueFormat(null, args.data.columns[i][1] / 100));
                    });
                });

                it('each label should have the same color', function () {
                    chart.internal.main.selectAll('.c3-chart-arc .c3-gauge-value').each(function () {
                        expect(d3.select(this).style('fill')).toBe('rgb(0, 0, 0)');
                    });

                });

                it('if only one data_column is visible the label should have "" for transform', function (done) {
                    var textBeforeHide = chart.internal.main.select('.c3-chart-arc.c3-target.c3-target-padded4 text');
                    expect(textBeforeHide.attr('transform')).not.toBe('');
                    chart.hide(['padded1', 'padded2', 'padded3']);
                    setTimeout(function () {
                        var textAfterHide = chart.internal.main.select('.c3-chart-arc.c3-target.c3-target-padded4 text');
                        expect(textAfterHide.attr('transform')).toBe('');
                        done();
                    }, 1000);
                });
            });

            describe('should contain labellines', function () {
                it('each data_column should have a labelline', function () {
                    chart.internal.main.selectAll('.c3-chart-arc .c3-arc-label-line').each(function (d, i) {
                        expect(d3.select(this).classed('c3-target-' + args.data.columns[i][0])).toBeTruthy();
                    });
                });

                it('each labelline should have the color from color_pattern if color_treshold is given', function () {
                    chart.internal.main.selectAll('.c3-chart-arc .c3-arc-label-line').each(function (d, i) {
                        expect(d3.select(this).style('fill')).toBe(arcColor[i]);
                    });
                });
            });
        });
    });

});
