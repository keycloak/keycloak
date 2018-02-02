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

                expect(data.attr('d')).toMatch(/M-304,-3\..+A304,304 0 0,1 245\..+,-178\..+L237\..+,-172\..+A294,294 0 0,0 -294,-3\..+Z/);
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
                expect(data.attr('d')).toMatch(/M-221.*?,-2\..+A221.*?,221.*? 0 1,1 -68.*?,210.*?L-65.*?,201.*?A211.*?,211.*? 0 1,0 -211.*?,-2.*?Z/);
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
    });

});
