describe('c3 chart data', function () {
    'use strict';

    var chart, args;

    beforeEach(function (done) {
        chart = window.initChart(chart, args, done);
    });

    describe('load json', function () {

        beforeAll(function () {
            args = {
                data: {
                    json: {
                        data1: [30, 20, 50],
                        data2: [200, 130, 90]
                    }
                }
            };
        });

        it('should draw correctly', function () {
            var expectedCx = [6, 299, 593],
                expectedCy = [371, 391, 332];
            d3.selectAll('.c3-circles-data1 .c3-circle').each(function (d, i) {
                var circle = d3.select(this);
                expect(+circle.attr('cx')).toBeCloseTo(expectedCx[i], 0);
                expect(+circle.attr('cy')).toBeCloseTo(expectedCy[i], 0);
            });
        });

        describe('more data', function(){
            beforeAll(function () {
                args = {
                    data: {
                        json: [{
                            "date": "2014-06-03",
                            "443": "3000",
                            "995": "500"
                        }, {
                            "date": "2014-06-04",
                            "443": "1000"
                        }, {
                            "date": "2014-06-05",
                            "443": "5000",
                            "995": "1000"
                        }],
                        keys: {
                            x: 'date',
                            value: [ "443", "995" ]
                        }
                    },
                    axis: {
                        x: {
                            type: "category"
                        }
                    }
                };
            });

            it('should draw correctly', function () {
                var expectedCx = {443: [98, 294, 490], 995: [98, 294, 490]},
                    expectedCy = {443: [194, 351, 36], 995: [391, 430, 351]};
                d3.selectAll('.c3-circles-443 .c3-circle').each(function (d, i) {
                    var circle = d3.select(this);
                    expect(+circle.attr('cx')).toBeCloseTo(expectedCx[443][i], 0);
                    expect(+circle.attr('cy')).toBeCloseTo(expectedCy[443][i], 0);
                });
                d3.selectAll('.c3-circles-995 .c3-circle').each(function (d, i) {
                    var circle = d3.select(this);
                    expect(+circle.attr('cx')).toBeCloseTo(expectedCx[995][i], 0);
                    expect(+circle.attr('cy')).toBeCloseTo(expectedCy[995][i], 0);
                });
            });
        });

        describe('with nested JSON args', function () {
            beforeAll(function(){
                args = {
                    data: {
                        json: [{
                            "date": "2014-06-03",
                            "443": "3000",
                            "995": {"996": "500"},
                            "112": ["600"],
                            "223": [{"224": "100"}],
                            "334": [[],[{"335": "300"}]],
                            "556": {"557" : {"558" : ["1000"]}}
                        }, {
                            "date": "2014-06-04",
                            "443": "1000",
                            "112": ["700"],
                            "223": [{"224": "200"}],
                            "556": {"557" : {"558" : ["2000"]}}
                        }, {
                            "date": "2014-06-05",
                            "995": {"996": "1000"},
                            "112": ["800"],
                            "223": [{"224": "300"}],
                            "443": "5000",
                            "334": [[],[{"335": "500"}]],
                            "556": {"557" : {"558" : ["3000"]}}
                        }],
                        keys: {
                            x: 'date',
                            value: [ "443","995.996","112[0]","223[0].224","334[1][0].335","556.557.558[0]"]
                        }
                    },
                    axis: {
                        x: {
                            type: "category"
                        }
                    }
                };
            });

            it('should draw nested JSON correctly', function () {
                var expectedCx = [98, 294, 490],
                    expectedCy = {
                        443: [181, 326, 36],
                        995: [362, 398, 326],
                        112: [354, 347, 340],
                        223: [391, 383, 376],
                        334: [376, 398, 362],
                        556: [326, 253, 181]
                    };

                d3.selectAll('.c3-circles-443 .c3-circle').each(function (d, i) {
                    var circle = d3.select(this);
                    expect(+circle.attr('cx')).toBeCloseTo(expectedCx[i], 0);
                    expect(+circle.attr('cy')).toBeCloseTo(expectedCy[443][i], 0);
                });

                d3.selectAll('.c3-circles-995-996 .c3-circle').each(function (d, i) {
                    var circle = d3.select(this);
                    expect(+circle.attr('cx')).toBeCloseTo(expectedCx[i], 0);
                    expect(+circle.attr('cy')).toBeCloseTo(expectedCy[995][i], 0);
                });

                d3.selectAll('.c3-circles-112-0- .c3-circle').each(function (d, i) {
                    var circle = d3.select(this);
                    expect(+circle.attr('cx')).toBeCloseTo(expectedCx[i], 0);
                    expect(+circle.attr('cy')).toBeCloseTo(expectedCy[112][i], 0);
                });

                d3.selectAll('.c3-circles-223-0--224 .c3-circle').each(function (d, i) {
                    var circle = d3.select(this);
                    expect(+circle.attr('cx')).toBeCloseTo(expectedCx[i], 0);
                    expect(+circle.attr('cy')).toBeCloseTo(expectedCy[223][i], 0);
                });

                d3.selectAll('.c3-circles-334-1--0--335 .c3-circle').each(function (d, i) {
                    var circle = d3.select(this);
                    expect(+circle.attr('cx')).toBeCloseTo(expectedCx[i], 0);
                    expect(+circle.attr('cy')).toBeCloseTo(expectedCy[334][i], 0);
                });

                d3.selectAll('.c3-circles-556-557-558-0- .c3-circle').each(function (d, i) {
                    var circle = d3.select(this);
                    expect(+circle.attr('cx')).toBeCloseTo(expectedCx[i], 0);
                    expect(+circle.attr('cy')).toBeCloseTo(expectedCy[556][i], 0);
                });
            });
        });
    });

    describe('function in data.order', function () {
        beforeAll(function () {
            args = {
                data: {
                    columns: [
                        ['data1', 30, 200, 100, 400, 150, 250],
                        ['data2', 50, 20, 10, 40, 15, 25],
                        ['data3', 150, 120, 110, 140, 115, 125]
                    ],
                    order: function () {
                        return 0;
                    }
                }
            };
        });

        it('should return false in isOrderAsc and isOrderDesc functions', function () {
            expect(chart.internal.isOrderAsc() || chart.internal.isOrderDesc()).toBe(false);
        });
    });

    describe('addHiddenTargetIds if not already hidden', function () {
        it('should update args', function () {
            args = {
                data: {
                    columns: [
                        ['data1', 30, 200, 100, 400, 150, 250],
                        ['data2', 150, 120, 110, 140, 115, 125]
                    ]
                }
            };
            expect(true).toBeTruthy();
        });

        it('length of hiddenTargetIds should not change if same key added twice', function () {
            chart.internal.addHiddenTargetIds('data1');
            expect(chart.internal.hiddenTargetIds.length).toBe(1);
            chart.internal.addHiddenTargetIds('data1');
            expect(chart.internal.hiddenTargetIds.length).toBe(1);
            chart.hide('data1');
            expect(chart.internal.hiddenTargetIds.length).toBe(1);
            chart.internal.addHiddenTargetIds('data2');
            expect(chart.internal.hiddenTargetIds.length).toBe(2);
            chart.show();
            chart.hide(['data1', 'data2']);
            expect(chart.internal.hiddenTargetIds.length).toBe(2);
            chart.show();
            chart.hide();
            expect(chart.internal.hiddenTargetIds.length).toBe(2);
        });
    });

    describe('addHiddenLegendIds if not already hidden', function () {
        it('should update args', function () {
            args = {
                data: {
                    columns: [
                        ['data1', 30, 200, 100, 400, 150, 250],
                        ['data2', 150, 120, 110, 140, 115, 125]
                    ]
                }
            };
            expect(true).toBeTruthy();
        });

        it('length of hiddenLegendIds should not change if same key added twice', function () {
            chart.internal.addHiddenLegendIds('data1');
            expect(chart.internal.hiddenLegendIds.length).toBe(1);
            chart.internal.addHiddenLegendIds('data1');
            expect(chart.internal.hiddenLegendIds.length).toBe(1);
            chart.hide('data1', {withLegend: true});
            expect(chart.internal.hiddenLegendIds.length).toBe(1);
            chart.hide('data2', {withLegend: true});
            expect(chart.internal.hiddenLegendIds.length).toBe(2);
            chart.show(['data1', 'data2'], {withLegend: true});
            chart.hide(['data1', 'data2'], {withLegend: true});
            expect(chart.internal.hiddenLegendIds.length).toBe(2);
        });
    });

    describe('data.xs', function () {

        beforeAll(function () {
            args = {
                data: {
                    columns: [
                        ['data1', 30, 200, 100, 400, 150, 250],
                        ['data2', 50, 20, 10, 40, 15, 25],
                        ['data3', 150, 120, 110, 140, 115, 125]
                    ],
                }
            };
        });

        describe('normal x', function () {

            it('should have correct number of xs for each', function () {
                expect(Object.keys(chart.internal.data.xs).length).toBe(3);
                expect(chart.internal.data.xs.data1.length).toBe(6);
                expect(chart.internal.data.xs.data2.length).toBe(6);
                expect(chart.internal.data.xs.data3.length).toBe(6);
            });

            it('should have integer index as x', function () {
                for (var i = 0; i < chart.internal.data.xs.data3.length; i++) {
                    expect(chart.internal.data.xs.data1[i]).toBe(i);
                    expect(chart.internal.data.xs.data2[i]).toBe(i);
                    expect(chart.internal.data.xs.data3[i]).toBe(i);
                }
            });

        });

        describe('timeseries x', function () {
            describe('without xFormat', function () {

                beforeAll(function () {
                    args = {
                        data: {
                            x : 'date',
                            columns: [
                                ['date', '2013-01-01', '2013-01-02', '2013-01-03'],
                                ['data1', 30, 200, 100],
                                ['data2', 130, 300, 200]
                            ]
                        },
                        axis : {
                            x : {
                                type : 'timeseries'
                            }
                        }
                    };
                });

                it('should have correct number of xs', function () {
                    expect(Object.keys(chart.internal.data.xs).length).toBe(2);
                    expect(chart.internal.data.xs.data1.length).toBe(3);
                    expect(chart.internal.data.xs.data2.length).toBe(3);
                });

                it('should have Date object as x', function () {
                    var xs = chart.internal.data.xs;
                    expect(+xs.data1[0]).toBe(+new Date(2013, 0, 1, 0, 0, 0));
                    expect(+xs.data1[1]).toBe(+new Date(2013, 0, 2, 0, 0, 0));
                    expect(+xs.data1[2]).toBe(+new Date(2013, 0, 3, 0, 0, 0));
                    expect(+xs.data2[0]).toBe(+new Date(2013, 0, 1, 0, 0, 0));
                    expect(+xs.data2[1]).toBe(+new Date(2013, 0, 2, 0, 0, 0));
                    expect(+xs.data2[2]).toBe(+new Date(2013, 0, 3, 0, 0, 0));
                });
            });

            describe('with xFormat', function () {
                describe('timeseries x with xFormat', function () {
                    beforeAll(function () {
                        args = {
                            data: {
                                x : 'date',
                                xFormat: '%Y%m%d',
                                columns: [
                                    ['date', '20130101', '20130102', '20130103'],
                                    ['data1', 30, 200, 100],
                                    ['data2', 130, 300, 200]
                                ]
                            },
                            axis : {
                                x : {
                                    type : 'timeseries'
                                }
                            }
                        };
                    });

                    it('should have correct number of xs', function () {
                        expect(Object.keys(chart.internal.data.xs).length).toBe(2);
                        expect(chart.internal.data.xs.data1.length).toBe(3);
                        expect(chart.internal.data.xs.data2.length).toBe(3);
                    });

                    it('should have Date object as x', function () {
                        var xs = chart.internal.data.xs;
                        expect(+xs.data1[0]).toBe(+new Date(2013, 0, 1, 0, 0, 0));
                        expect(+xs.data1[1]).toBe(+new Date(2013, 0, 2, 0, 0, 0));
                        expect(+xs.data1[2]).toBe(+new Date(2013, 0, 3, 0, 0, 0));
                        expect(+xs.data2[0]).toBe(+new Date(2013, 0, 1, 0, 0, 0));
                        expect(+xs.data2[1]).toBe(+new Date(2013, 0, 2, 0, 0, 0));
                        expect(+xs.data2[2]).toBe(+new Date(2013, 0, 3, 0, 0, 0));
                    });
                });
            });
        });

        describe('milliseconds timeseries x', function () {

            describe('as date string', function () {
                beforeAll(function () {
                    args = {
                        data: {
                            x : 'date',
                            xFormat: '%Y-%m-%d %H:%M:%S.%L',
                            columns: [
                                ['date', "2014-05-20 17:25:00.123", "2014-05-20 17:30:00.345"],
                                ['data1', 30, 200],
                                ['data2', 130, 300]
                            ]
                        },
                        axis: {
                            x: {
                                type: 'timeseries',
                                tick: {
                                    format: '%Y-%m-%d %H:%M:%S.%L',
                                    multiline: false
                                }
                            }
                        }
                    };
                });

                it('should have correct number of xs', function () {
                    expect(Object.keys(chart.internal.data.xs).length).toBe(2);
                    expect(chart.internal.data.xs.data1.length).toBe(2);
                    expect(chart.internal.data.xs.data2.length).toBe(2);
                });

                it('should have Date object as x', function () {
                    var xs = chart.internal.data.xs;
                    expect(+xs.data1[0]).toBe(+new Date(2014, 4, 20, 17, 25, 0, 123));
                    expect(+xs.data1[1]).toBe(+new Date(2014, 4, 20, 17, 30, 0, 345));
                    expect(+xs.data2[0]).toBe(+new Date(2014, 4, 20, 17, 25, 0, 123));
                    expect(+xs.data2[1]).toBe(+new Date(2014, 4, 20, 17, 30, 0, 345));
                });

                it('should have milliseconds tick format', function () {
                    var expected = ["2014-05-20 17:25:00.123", "2014-05-20 17:30:00.345"];
                    chart.internal.main.selectAll('.c3-axis-x g.tick text').each(function (d, i) {
                        expect(d3.select(this).text()).toBe(expected[i]);
                    });
                });

            });

            describe('as unixtime number', function () {

                beforeAll(function () {
                    args = {
                        data: {
                            x : 'date',
                            columns: [
                                ['date', 1417622461123, 1417622522345],
                                ['data1', 30, 200],
                                ['data2', 130, 300]
                            ]
                        },
                        axis: {
                            x: {
                                type: 'timeseries',
                                tick: {
                                    format: '%Y-%m-%d %H:%M:%S.%L'
                                }
                            }
                        }
                    };
                });

                it('should have correct number of xs', function () {
                    expect(Object.keys(chart.internal.data.xs).length).toBe(2);
                    expect(chart.internal.data.xs.data1.length).toBe(2);
                    expect(chart.internal.data.xs.data2.length).toBe(2);
                });

                it('should have Date object as x', function () {
                    var xs = chart.internal.data.xs;
                    expect(+xs.data1[0]).toBe(1417622461123);
                    expect(+xs.data1[1]).toBe(1417622522345);
                    expect(+xs.data2[0]).toBe(1417622461123);
                    expect(+xs.data2[1]).toBe(1417622522345);
                });
            });

        });

    });

    describe('data.label', function () {

        describe('on line chart', function () {

            beforeAll(function () {
                args = {
                    data: {
                        columns: [
                            ['data1', 1030, 2200, 2100],
                            ['data2', 1150, 2010, 1200],
                            ['data3', -1150, -2010, -1200],
                            ['data4', -1030, -2200, -2100],
                        ],
                        type: 'line',
                        labels: true,
                    }
                };
            });

            it('should locate data labels in correct position', function () {
                var expectedTextY = {
                    data1: [128, 38, 46],
                    data2: [119, 53, 115],
                    data3: [311, 377, 315],
                    data4: [302, 392, 384],
                };
                var expectedTextX = {
                    data1: [6, 294, 583],
                    data2: [6, 294, 583],
                    data3: [6, 294, 583],
                    data4: [6, 294, 583],
                };
                Object.keys(expectedTextY).forEach(function (key) {
                    d3.selectAll('.c3-texts-' + key + ' text.c3-text').each(function (d, i) {
                        var text = d3.select(this);
                        expect(+text.attr('y')).toBeCloseTo(expectedTextY[key][i], -2);
                        expect(+text.attr('x')).toBeCloseTo(expectedTextX[key][i], -2);
                    });
                });
            });

            describe('with stacked', function () {
                beforeAll(function(){
                    args.data.groups = [['data1', 'data2'], ['data3', 'data4']];
                });

                it('should locate data labels in correct position', function () {
                    var expectedTextY = {
                        data1: [120, 38, 75],
                        data2: [161, 127, 159],
                        data3: [269, 303, 271],
                        data4: [310, 392, 355],
                    };
                    var expectedTextX = {
                        data1: [6, 294, 583],
                        data2: [6, 294, 583],
                        data3: [6, 294, 583],
                        data4: [6, 294, 583],
                    };
                    Object.keys(expectedTextY).forEach(function (key) {
                        d3.selectAll('.c3-texts-' + key + ' text.c3-text').each(function (d, i) {
                            var text = d3.select(this);
                            expect(+text.attr('y')).toBeCloseTo(expectedTextY[key][i], -2);
                            expect(+text.attr('x')).toBeCloseTo(expectedTextX[key][i], -2);
                        });
                    });
                });
            });
        });

        describe('on area chart', function () {

            beforeAll(function () {
                args = {
                    data: {
                        columns: [
                            ['data1', 1030, 2200, 2100],
                            ['data2', 1150, 2010, 1200],
                            ['data3', -1150, -2010, -1200],
                            ['data4', -1030, -2200, -2100],
                        ],
                        type: 'area',
                        labels: true,
                    }
                };
            });

            it('should locate data labels in correct position', function () {
                var expectedTextY = {
                    data1: [128, 38, 46],
                    data2: [119, 53, 115],
                    data3: [311, 377, 315],
                    data4: [302, 392, 384],
                };
                var expectedTextX = {
                    data1: [6, 294, 583],
                    data2: [6, 294, 583],
                    data3: [6, 294, 583],
                    data4: [6, 294, 583],
                };
                Object.keys(expectedTextY).forEach(function (key) {
                    d3.selectAll('.c3-texts-' + key + ' text.c3-text').each(function (d, i) {
                        var text = d3.select(this);
                        expect(+text.attr('y')).toBeCloseTo(expectedTextY[key][i], -2);
                        expect(+text.attr('x')).toBeCloseTo(expectedTextX[key][i], -2);
                    });
                });
            });

            describe('with stacked', function () {
                beforeAll(function(){
                    args.data.groups = [['data1', 'data2'], ['data3', 'data4']];
                });

                it('should locate data labels in correct position', function () {
                    var expectedTextY = {
                        data1: [120, 38, 75],
                        data2: [161, 127, 159],
                        data3: [269, 303, 271],
                        data4: [310, 392, 355],
                    };
                    var expectedTextX = {
                        data1: [6, 294, 583],
                        data2: [6, 294, 583],
                        data3: [6, 294, 583],
                        data4: [6, 294, 583],
                    };
                    Object.keys(expectedTextY).forEach(function (key) {
                        d3.selectAll('.c3-texts-' + key + ' text.c3-text').each(function (d, i) {
                            var text = d3.select(this);
                            expect(+text.attr('y')).toBeCloseTo(expectedTextY[key][i], -2);
                            expect(+text.attr('x')).toBeCloseTo(expectedTextX[key][i], -2);
                        });
                    });
                });
            });

        });

        describe('on bar chart', function () {

            beforeAll(function () {
                args = {
                    data: {
                        columns: [
                            ['data1', 1030, 2200, 2100],
                            ['data2', 1150, 2010, 1200],
                            ['data3', -1150, -2010, -1200],
                            ['data4', -1030, -2200, -2100],
                        ],
                        type: 'bar',
                        labels: true,
                    }
                };
            });

            it('should locate data labels in correct position', function () {
                var expectedTextY = {
                    data1: [128, 38, 46],
                    data2: [119, 53, 115],
                    data3: [311, 377, 315],
                    data4: [302, 392, 384],
                };
                var expectedTextX = {
                    data1: [53, 249, 445],
                    data2: [83, 279, 475],
                    data3: [112, 308, 504],
                    data4: [142, 338, 534],
                };
                Object.keys(expectedTextY).forEach(function (key) {
                    d3.selectAll('.c3-texts-' + key + ' text.c3-text').each(function (d, i) {
                        var text = d3.select(this);
                        expect(+text.attr('y')).toBeCloseTo(expectedTextY[key][i], -2);
                        expect(+text.attr('x')).toBeCloseTo(expectedTextX[key][i], -2);
                    });
                });
            });

            describe('with stacked', function () {
                beforeAll(function(){
                    args.data.groups = [['data1', 'data2'], ['data3', 'data4']];
                });

                it('should locate data labels in correct position', function () {
                    var expectedTextY = {
                        data1: [120, 38, 75],
                        data2: [161, 127, 159],
                        data3: [269, 303, 271],
                        data4: [310, 392, 355],
                    };
                    var expectedTextX = {
                        data1: [68.6, 264, 460],
                        data2: [68.6, 264, 460],
                        data3: [127, 323, 519],
                        data4: [127, 323, 519],
                    };
                    Object.keys(expectedTextY).forEach(function (key) {
                        d3.selectAll('.c3-texts-' + key + ' text.c3-text').each(function (d, i) {
                            var text = d3.select(this);
                            expect(+text.attr('y')).toBeCloseTo(expectedTextY[key][i], -2);
                            expect(+text.attr('x')).toBeCloseTo(expectedTextX[key][i], -2);
                        });
                    });
                });
            });
        });

        describe('for all targets', function () {

            describe('with data label for all data', function () {
                beforeAll(function(){
                    args = {
                        data: {
                            columns: [
                                ['data1', 100, 200, 100, 400, 150, 250],
                                ['data2', 10, 20, 10, 40, 15, 25],
                                ['data3', 1000, 2000, 1000, 4000, 1500, 2500]
                            ],
                            labels: true
                        }
                    };
                });

                it('should have data labels on all data', function () {
                    d3.selectAll('.c3-texts-data1 text').each(function (d, i) {
                        expect(d3.select(this).text()).toBe(args.data.columns[0][i + 1] + '');
                    });
                    d3.selectAll('.c3-texts-data2 text').each(function (d, i) {
                        expect(d3.select(this).text()).toBe(args.data.columns[1][i + 1] + '');
                    });
                    d3.selectAll('.c3-texts-data3 text').each(function (d, i) {
                        expect(d3.select(this).text()).toBe(args.data.columns[2][i + 1] + '');
                    });
                });
            });
        });

        describe('for each target', function () {

            describe('as true', function () {

                describe('with data label for only data1', function () {
                    beforeAll(function(){
                        args = {
                            data: {
                                columns: [
                                    ['data1', 100, 200, 100, 400, 150, 250],
                                    ['data2', 10, 20, 10, 40, 15, 25],
                                    ['data3', 1000, 2000, 1000, 4000, 1500, 2500]
                                ],
                                labels: {
                                    format: {
                                        data1: true
                                    }
                                }
                            }
                        };
                    });

                    it('should have data labels on all data', function () {
                        d3.selectAll('.c3-texts-data1 text').each(function (d, i) {
                            expect(d3.select(this).text()).toBe(args.data.columns[0][i + 1] + '');
                        });
                        d3.selectAll('.c3-texts-data2 text').each(function () {
                            expect(d3.select(this).text()).toBe('');
                        });
                        d3.selectAll('.c3-texts-data3 text').each(function () {
                            expect(d3.select(this).text()).toBe('');
                        });
                    });
                });
            });

            describe('as function', function () {

                describe('with data label for only data1', function () {
                    beforeAll(function(){
                        args = {
                            data: {
                                columns: [
                                    ['data1', 100, 200, 100, 400, 150, 250],
                                    ['data2', 10, 20, 10, 40, 15, 25],
                                    ['data3', 1000, 2000, 1000, 4000, 1500, 2500]
                                ],
                                labels: {
                                    format: {
                                        data1: d3.format('$')
                                    }
                                }
                            }
                        };
                    });

                    it('should have data labels on all data', function () {
                        d3.selectAll('.c3-texts-data1 text').each(function (d, i) {
                            expect(d3.select(this).text()).toBe('$' + args.data.columns[0][i + 1]);
                        });
                        d3.selectAll('.c3-texts-data2 text').each(function () {
                            expect(d3.select(this).text()).toBe('');
                        });
                        d3.selectAll('.c3-texts-data3 text').each(function () {
                            expect(d3.select(this).text()).toBe('');
                        });
                    });
                });
            });

        });

        describe('with small values', function () {

            describe('with data label', function () {
                beforeAll(function(){
                    args = {
                        data: {
                            columns: [
                                ['data1', 0.03, 0.2, 0.1, 0.4, 0.15, 0.250]
                            ],
                            labels: true
                        }
                    };
                });

                it('should have proper y domain', function () {
                    var domain = chart.internal.y.domain();
                    expect(domain[0]).toBeCloseTo(-0.02);
                    expect(domain[1]).toBeCloseTo(0.45);
                });
            });
        });

        describe('with positive values and null', function () {

            describe('on not rotated axis', function () {

                beforeAll(function () {
                    args = {
                        data: {
                            columns: [
                                ['data1', 190, 200, 190, null],
                            ],
                            type: 'bar',
                            labels: {
                                format: function (v) {
                                    if (v === null) {
                                        return 'Not Applicable';
                                    }
                                    return d3.format('$')(v);
                                }
                            }
                        }
                    };
                });

                it('should have y domain with proper padding', function () {
                    var domain = chart.internal.y.domain();
                    expect(domain[0]).toBeCloseTo(0, -1);
                    expect(domain[1]).toBeCloseTo(227, -1);
                });

                it('should locate labels above each data point', function () {
                    var texts = chart.internal.main.selectAll('.c3-texts-data1 text'),
                        expectedYs = [67, 49, 67, 423],
                        expectedXs = [74, 221, 368, 515];
                    texts.each(function (d, i) {
                        var text = d3.select(this);
                        expect(+text.attr('y')).toBeCloseTo(expectedYs[i], -2);
                        expect(+text.attr('x')).toBeCloseTo(expectedXs[i], -2);
                    });
                });

                describe('data type line', function () {
                    beforeAll(function(){
                        args.data.type = 'line';
                    });

                    it('should have y domain with proper padding', function () {
                        var domain = chart.internal.y.domain();
                        expect(domain[0]).toBeCloseTo(189, -1);
                        expect(domain[1]).toBeCloseTo(201, -1);
                    });

                    it('should locate labels above each data point', function () {
                        var texts = chart.internal.main.selectAll('.c3-texts-data1 text'),
                            expectedYs = [375, 40, 375, 422],
                            expectedXs = [6, 198, 391, 583];
                        texts.each(function (d, i) {
                            var text = d3.select(this);
                            expect(+text.attr('y')).toBeCloseTo(expectedYs[i], -2);
                            expect(+text.attr('x')).toBeCloseTo(expectedXs[i], -2);
                        });
                    });
                });
            });

            describe('on rotated axis', function () {

                describe('data type bar', function () {
                    beforeAll(function(){
                        args.data.type = 'bar';
                        args.axis = {
                            rotated: true
                        };
                    });

                    it('should have y domain with proper padding', function () {
                        var domain = chart.internal.y.domain();
                        expect(domain[0]).toBeCloseTo(0, -1);
                        expect(domain[1]).toBeCloseTo(231, -1);
                    });

                    it('should locate labels above each data point', function () {
                        var texts = chart.internal.main.selectAll('.c3-texts-data1 text'),
                            expectedYs = [57, 163, 269, 375],
                            expectedXs = [490, 516, 490, 4];
                        texts.each(function (d, i) {
                            var text = d3.select(this);
                            expect(+text.attr('y')).toBeCloseTo(expectedYs[i], -2);
                            expect(+text.attr('x')).toBeCloseTo(expectedXs[i], -2);
                        });
                    });
                });

                describe('data type line', function () {
                    beforeAll(function(){
                        args.data.type = 'line';
                    });

                    it('should have y domain with proper padding', function () {
                        var domain = chart.internal.y.domain();
                        expect(domain[0]).toBeCloseTo(188, -1);
                        expect(domain[1]).toBeCloseTo(202, -1);
                    });

                    it('should locate labels above each data point', function () {
                        var texts = chart.internal.main.selectAll('.c3-texts-data1 text'),
                            expectedYs = [9, 147, 286, 424],
                            expectedXs = [76, 526, 76, 4];
                        texts.each(function (d, i) {
                            var text = d3.select(this);
                            expect(+text.attr('y')).toBeCloseTo(expectedYs[i], -2);
                            expect(+text.attr('x')).toBeCloseTo(expectedXs[i], -2);
                        });
                    });
                });
            });

        });

        describe('with negative values and null', function () {

            describe('on not rotated axis', function () {

                describe('type bar', function () {
                    beforeAll(function(){
                        args = {
                            data: {
                                columns: [
                                    ['data1', -190, 0, -190, null],
                                ],
                                type: 'bar',
                                labels: {
                                    format: function (v) {
                                        if (v === null) {
                                            return 'Not Applicable';
                                        }
                                        return d3.format('$')(v);
                                    }
                                }
                            }
                        };
                    });

                    it('should have y domain with proper padding', function () {
                        var domain = chart.internal.y.domain();
                        expect(domain[0]).toBeCloseTo(-215, -1);
                        expect(domain[1]).toBeCloseTo(0, -1);
                    });

                    it('should locate labels above each data point', function () {
                        var texts = chart.internal.main.selectAll('.c3-texts-data1 text'),
                            expectedYs = [368, 12, 368, 12],
                            expectedXs = [74, 221, 368, 515];
                        texts.each(function (d, i) {
                            var text = d3.select(this);
                            expect(+text.attr('y')).toBeCloseTo(expectedYs[i], -2);
                            expect(+text.attr('x')).toBeCloseTo(expectedXs[i], -2);
                        });
                    });
                });

                describe('data type line', function () {
                    beforeAll(function(){
                        args.data.type = 'line';
                    });

                    it('should have y domain with proper padding', function () {
                        var domain = chart.internal.y.domain();
                        expect(domain[0]).toBeCloseTo(-215, -1);
                        expect(domain[1]).toBeCloseTo(25, -1);
                    });

                    it('should locate labels above each data point', function () {
                        var texts = chart.internal.main.selectAll('.c3-texts-data1 text'),
                            expectedYs = [395, 60, 395, 12],
                            expectedXs = [6, 198, 391, 583];
                        texts.each(function (d, i) {
                            var text = d3.select(this);
                            expect(+text.attr('y')).toBeCloseTo(expectedYs[i], -2);
                            expect(+text.attr('x')).toBeCloseTo(expectedXs[i], -2);
                        });
                    });
                });
            });

            describe('on rotated axis', function () {

                describe('data type bar', function () {
                    beforeAll(function(){
                        args.data.type = 'bar';
                        args.axis = {
                            rotated: true
                        };
                    });

                    it('should have y domain with proper padding', function () {
                        var domain = chart.internal.y.domain();
                        expect(domain[0]).toBeCloseTo(-220, -1);
                        expect(domain[1]).toBeCloseTo(0, -1);
                    });

                    it('should locate labels above each data point', function () {
                        var texts = chart.internal.main.selectAll('.c3-texts-data1 text'),
                            expectedYs = [57, 163, 269, 375],
                            expectedXs = [103, 594, 103, 526];
                        texts.each(function (d, i) {
                            var text = d3.select(this);
                            expect(+text.attr('y')).toBeCloseTo(expectedYs[i], -2);
                            expect(+text.attr('x')).toBeCloseTo(expectedXs[i], -2);
                        });
                    });
                });

                describe('data type line', function () {
                    beforeAll(function(){
                        args.data.type = 'line';
                    });

                    it('should have y domain with proper padding', function () {
                        var domain = chart.internal.y.domain();
                        expect(domain[0]).toBeCloseTo(-220, -1);
                        expect(domain[1]).toBeCloseTo(24, -1);
                    });

                    it('should locate labels above each data point', function () {
                        var texts = chart.internal.main.selectAll('.c3-texts-data1 text'),
                            expectedYs = [9, 147, 286, 424],
                            expectedXs = [67, 537, 67, 526];
                        texts.each(function (d, i) {
                            var text = d3.select(this);
                            expect(+text.attr('y')).toBeCloseTo(expectedYs[i], -2);
                            expect(+text.attr('x')).toBeCloseTo(expectedXs[i], -2);
                        });
                    });
                });
            });

        });

        describe('with positive and negative values and null', function () {

            describe('on non rotated axis', function () {

                describe('data type bar', function () {
                    beforeAll(function(){
                        args = {
                            data: {
                                columns: [
                                    ['data1', -190, 200, 190, null],
                                ],
                                type: 'bar',
                                labels: {
                                    format: function (v) {
                                        if (v === null) {
                                            return 'Not Applicable';
                                        }
                                        return d3.format('$')(v);
                                    }
                                }
                            }
                        };
                    });

                    it('should have y domain with proper padding', function () {
                        var domain = chart.internal.y.domain();
                        expect(domain[0]).toBeCloseTo(-243, -1);
                        expect(domain[1]).toBeCloseTo(253, -1);
                    });

                    it('should locate labels above each data point', function () {
                        var texts = chart.internal.main.selectAll('.c3-texts-data1 text'),
                            expectedYs = [392, 43, 52, 215],
                            expectedXs = [74, 221, 368, 515];
                        texts.each(function (d, i) {
                            var text = d3.select(this);
                            expect(+text.attr('y')).toBeCloseTo(expectedYs[i], -2);
                            expect(+text.attr('x')).toBeCloseTo(expectedXs[i], -2);
                        });
                    });
                });

                describe('data type line', function () {
                    beforeAll(function(){
                        args.data.type = 'line';
                    });

                    it('should have y domain with proper padding', function () {
                        var domain = chart.internal.y.domain();
                        expect(domain[0]).toBeCloseTo(-243, -1);
                        expect(domain[1]).toBeCloseTo(253, -1);
                    });

                    it('should locate labels above each data point', function () {
                        var texts = chart.internal.main.selectAll('.c3-texts-data1 text'),
                            expectedYs = [392, 40, 49, 212],
                            expectedXs = [6, 198, 391, 583];
                        texts.each(function (d, i) {
                            var text = d3.select(this);
                            expect(+text.attr('y')).toBeCloseTo(expectedYs[i], -2);
                            expect(+text.attr('x')).toBeCloseTo(expectedXs[i], -2);
                        });
                    });
                });
            });

            describe('on rotated axis', function () {

                describe('data type bar', function () {
                    beforeAll(function(){
                        args.data.type = 'bar';
                        args.axis = {
                            rotated: true
                        };
                    });

                    it('should have y domain with proper padding', function () {
                        var domain = chart.internal.y.domain();
                        expect(domain[0]).toBeCloseTo(-253, -1);
                        expect(domain[1]).toBeCloseTo(260, -1);
                    });

                    it('should locate labels above each data point', function () {
                        var texts = chart.internal.main.selectAll('.c3-texts-data1 text'),
                            expectedYs = [57, 163, 269, 375],
                            expectedXs = [69, 525, 513, 295];
                        texts.each(function (d, i) {
                            var text = d3.select(this);
                            expect(+text.attr('y')).toBeCloseTo(expectedYs[i], -2);
                            expect(+text.attr('x')).toBeCloseTo(expectedXs[i], -2);
                        });
                    });
                });

                describe('data type line', function () {
                    beforeAll(function(){
                        args.data.type = 'line';
                    });

                    it('should have y domain with proper padding', function () {
                        var domain = chart.internal.y.domain();
                        expect(domain[0]).toBeCloseTo(-253, -1);
                        expect(domain[1]).toBeCloseTo(260, -1);
                    });

                    it('should locate labels above each data point', function () {
                        var texts = chart.internal.main.selectAll('.c3-texts-data1 text'),
                            expectedYs = [9, 147, 286, 424],
                            expectedXs = [67, 527, 515, 297];
                        texts.each(function (d, i) {
                            var text = d3.select(this);
                            expect(+text.attr('y')).toBeCloseTo(expectedYs[i], -2);
                            expect(+text.attr('x')).toBeCloseTo(expectedXs[i], -2);
                        });
                    });
                });
            });

        });

        describe('with positive grouped values', function () {

            describe('on non rotated axis', function () {

                describe('data type bar', function () {
                    beforeAll(function(){
                        args = {
                            data: {
                                columns: [
                                    ['data1', 30, 200, 100, 500],
                                    ['data2', 50, 20, 10, 40],
                                    ['data3', 250, 220, 210, 240],
                                ],
                                groups: [['data1', 'data2', 'data3']],
                                labels: true,
                                type: 'bar',
                            }
                        };
                    });

                    it('should have y domain with proper padding', function () {
                        var domain = chart.internal.y.domain();
                        expect(domain[0]).toBeCloseTo(0, -1);
                        expect(domain[1]).toBeCloseTo(885, -1);
                    });

                    it('should locate labels above each data point', function () {
                        var texts = chart.internal.main.selectAll('.c3-texts-data1 text'),
                            expectedYs = [385, 317, 370, 164],
                            expectedXs = [74, 221, 368, 515];
                        texts.each(function (d, i) {
                            var text = d3.select(this);
                            expect(+text.attr('y')).toBeCloseTo(expectedYs[i], -2);
                            expect(+text.attr('x')).toBeCloseTo(expectedXs[i], -2);
                        });
                    });
                });

                describe('data type line', function () {
                    beforeAll(function(){
                        args.data.type = 'line';
                    });

                    it('should have y domain with proper padding', function () {
                        var domain = chart.internal.y.domain();
                        expect(domain[0]).toBeCloseTo(-94, -1);
                        expect(domain[1]).toBeCloseTo(884, -1);
                    });

                    it('should locate labels above each data point', function () {
                        var texts = chart.internal.main.selectAll('.c3-texts-data1 text'),
                            expectedYs = [344, 284, 331, 144],
                            expectedXs = [6, 198, 391, 583];
                        texts.each(function (d, i) {
                            var text = d3.select(this);
                            expect(+text.attr('y')).toBeCloseTo(expectedYs[i], -2);
                            expect(+text.attr('x')).toBeCloseTo(expectedXs[i], -2);
                        });
                    });
                });
            });

            describe('on rotated axis', function () {

                describe('data type bar', function () {
                    beforeAll(function(){
                        args.data.type = 'bar';
                        args.axis = {
                            rotated: true
                        };
                    });

                    it('should have y domain with proper padding', function () {
                        var domain = chart.internal.y.domain();
                        expect(domain[0]).toBeCloseTo(0, -1);
                        expect(domain[1]).toBeCloseTo(888, -1.2);
                    });

                    it('should locate labels above each data point', function () {
                        var texts = chart.internal.main.selectAll('.c3-texts-data1 text'),
                            expectedYs = [57, 163, 269, 375],
                            expectedXs = [57, 150, 77, 363];
                        texts.each(function (d, i) {
                            var text = d3.select(this);
                            expect(+text.attr('y')).toBeCloseTo(expectedYs[i], -2);
                            expect(+text.attr('x')).toBeCloseTo(expectedXs[i], -2);
                        });
                    });
                });

                describe('data type line', function () {
                    beforeAll(function(){
                        args.data.type = 'line';
                    });

                    it('should have y domain with proper padding', function () {
                        var domain = chart.internal.y.domain();
                        expect(domain[0]).toBeCloseTo(-87, -1);
                        expect(domain[1]).toBeCloseTo(887, -1.2);
                    });

                    it('should locate labels above each data point', function () {
                        var texts = chart.internal.main.selectAll('.c3-texts-data1 text'),
                            expectedYs = [9, 147, 286, 424],
                            expectedXs = [107, 192, 125, 386];
                        texts.each(function (d, i) {
                            var text = d3.select(this);
                            expect(+text.attr('y')).toBeCloseTo(expectedYs[i], -2);
                            expect(+text.attr('x')).toBeCloseTo(expectedXs[i], -2);
                        });
                    });
                });
            });

        });

        describe('with negative grouped values', function () {

            describe('on non rotated axis', function () {

                describe('data type bar', function () {
                    beforeAll(function(){
                        args = {
                            data: {
                                columns: [
                                    ['data1', -30, -200, -100, -500],
                                    ['data2', -50, -20, -10, -40],
                                    ['data3', -250, -220, -210, -240]
                                ],
                                groups: [['data1', 'data2', 'data3']],
                                labels: true,
                                type: 'bar',
                            }
                        };
                    });

                    it('should have y domain with proper padding', function () {
                        var domain = chart.internal.y.domain();
                        expect(domain[0]).toBeCloseTo(-885, -1);
                        expect(domain[1]).toBeCloseTo(0, -1);
                    });

                    it('should locate labels above each data point', function () {
                        var texts = chart.internal.main.selectAll('.c3-texts-data1 text'),
                            expectedYs = [51, 118, 65, 272],
                            expectedXs = [74, 221, 368, 515];
                        texts.each(function (d, i) {
                            var text = d3.select(this);
                            expect(+text.attr('y')).toBeCloseTo(expectedYs[i], -2);
                            expect(+text.attr('x')).toBeCloseTo(expectedXs[i], -2);
                        });
                    });
                });

                describe('data type line', function () {
                    beforeAll(function(){
                        args.data.type = 'line';
                    });

                    it('should have y domain with proper padding', function () {
                        var domain = chart.internal.y.domain();
                        expect(domain[0]).toBeCloseTo(-884, -1);
                        expect(domain[1]).toBeCloseTo(94, -1);
                    });

                    it('should locate labels above each data point', function () {
                        var texts = chart.internal.main.selectAll('.c3-texts-data1 text'),
                            expectedYs = [88, 149, 101, 288],
                            expectedXs = [6, 198, 391, 583];
                        texts.each(function (d, i) {
                            var text = d3.select(this);
                            expect(+text.attr('y')).toBeCloseTo(expectedYs[i], -2);
                            expect(+text.attr('x')).toBeCloseTo(expectedXs[i], -2);
                        });
                    });
                });
            });

            describe('on rotated axis', function () {

                describe('data type bar', function () {
                    beforeAll(function(){
                        args.data.type = 'bar';
                        args.axis = {
                            rotated: true
                        };
                    });

                    it('should have y domain with proper padding', function () {
                        var domain = chart.internal.y.domain();
                        expect(domain[0]).toBeCloseTo(-899, -1);
                        expect(domain[1]).toBeCloseTo(0, -1);
                    });

                    it('should locate labels above each data point', function () {
                        var texts = chart.internal.main.selectAll('.c3-texts-data1 text'),
                            expectedYs = [57, 163, 269, 375],
                            expectedXs = [533, 440, 513, 230];
                        texts.each(function (d, i) {
                            var text = d3.select(this);
                            expect(+text.attr('y')).toBeCloseTo(expectedYs[i], -2);
                            expect(+text.attr('x')).toBeCloseTo(expectedXs[i], -2);
                        });
                    });
                });

                describe('data type line', function () {
                    beforeAll(function(){
                        args.data.type = 'line';
                    });

                    it('should have y domain with proper padding', function () {
                        var domain = chart.internal.y.domain();
                        expect(domain[0]).toBeCloseTo(-893, -1);
                        expect(domain[1]).toBeCloseTo(93, -1);
                    });

                    it('should locate labels above each data point', function () {
                        var texts = chart.internal.main.selectAll('.c3-texts-data1 text'),
                            expectedYs = [9, 147, 286, 424],
                            expectedXs = [480, 397, 462, 205];
                        texts.each(function (d, i) {
                            var text = d3.select(this);
                            expect(+text.attr('y')).toBeCloseTo(expectedYs[i], -2);
                            expect(+text.attr('x')).toBeCloseTo(expectedXs[i], -2);
                        });
                    });
                });
            });

        });

    });

});
