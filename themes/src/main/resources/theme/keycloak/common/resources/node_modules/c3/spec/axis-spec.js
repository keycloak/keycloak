describe('c3 chart axis', function () {
    'use strict';

    var chart;

    var args = {
        data: {
            columns: [
                ['data1', 30, 200, 100, 400, 150, 250],
                ['data2', 50, 20, 10, 40, 15, 25],
                ['data3', 150, 120, 110, 140, 115, 125]
            ]
        },
        axis: {
            y: {
                tick: {
                    values: null,
                    count: undefined
                }
            },
            y2: {
                tick: {
                    values: null,
                    count: undefined
                }
            }
        }
    };

    beforeEach(function (done) {
        chart = window.initChart(chart, args, done);
    });

    describe('axis.y.tick.count', function () {

        describe('with only 1 tick on y axis', function () {
            beforeAll(function(){
                args.axis.y.tick.count = 1;
            });

            it('should have only 1 tick on y axis', function () {
                var ticksSize = d3.select('.c3-axis-y').selectAll('g.tick').size();
                expect(ticksSize).toBe(1);
            });
        });

        describe('with 2 ticks on y axis', function () {
            beforeAll(function(){
                args.axis.y.tick.count = 2;
            });

            it('should have 2 ticks on y axis', function () {
                var ticksSize = d3.select('.c3-axis-y').selectAll('g.tick').size();
                expect(ticksSize).toBe(2);
            });
        });

        describe('with 3 ticks on y axis', function () {
            beforeAll(function(){
                args.axis.y.tick.count = 3;
            });

            it('should have 3 ticks on y axis', function () {
                var ticksSize = d3.select('.c3-axis-y').selectAll('g.tick').size();
                expect(ticksSize).toBe(3);
            });
        });
    });

    describe('axis.y.tick.values', function () {

        var values = [100, 500];

        describe('with only 2 ticks on y axis', function () {
            beforeAll(function(){
                args.axis.y.tick.values = values;
            });

            it('should have only 2 tick on y axis', function () {
                var ticksSize = d3.select('.c3-axis-y').selectAll('g.tick').size();
                expect(ticksSize).toBe(2);
            });

            it('should have specified tick texts', function () {
                d3.select('.c3-axis-y').selectAll('g.tick').each(function (d, i) {
                    var text = d3.select(this).select('text').text();
                    expect(+text).toBe(values[i]);
                });
            });
        });
    });

    describe('axis y timeseries', function () {
        beforeAll(function () {
            args = {
                data: {
                    columns: [
                        ["times", 60000, 120000, 180000, 240000]
                    ]
                },
                axis: {
                    y: {
                        type : 'timeseries',
                        tick: {
                            time: {
                            }
                        }
                    }
                }
            };
        });

        it('should have 7 ticks on y axis', function () {
            var ticksSize = d3.select('.c3-axis-y').selectAll('g.tick').size();
            expect(ticksSize).toBe(7); // the count starts at initial value and increments by the set interval
        });

        it('should have specified 30 second intervals', function () {
            var prevValue;
            d3.select('.c3-axis-y').selectAll('g.tick').each(function (d, i) {
                if (i !== 0) {
                    var result = d - prevValue;
                    expect(result).toEqual(30000); // expressed in milliseconds
                }
                prevValue = d;
            });
        });

        describe('with axis.y.time', function () {
            beforeAll(function(){
                args.axis.y.tick.time = {
                    value : 'seconds',
                    interval : 60
                };
            });

            it('should have 4 ticks on y axis', function () {
                var ticksSize = d3.select('.c3-axis-y').selectAll('g.tick').size();
                expect(ticksSize).toBe(4); // the count starts at initial value and increments by the set interval
            });

            it('should have specified 60 second intervals', function () {
                var prevValue;
                d3.select('.c3-axis-y').selectAll('g.tick').each(function (d, i) {
                    if (i !== 0) {
                        var result = d - prevValue;
                        expect(result).toEqual(60000); // expressed in milliseconds
                    }
                    prevValue = d;
                });
            });
        });
    });

    describe('axis.x.tick.values', function () {
        describe('function is provided', function () {
            var tickGenerator = function () {
                var values = [];
                for (var i = 0; i <= 300; i += 50) {
                    values.push(i);
                }
                return values;
            };
            beforeEach(function () {
                args.axis.x = {
                    tick: {
                        values: tickGenerator
                    }
                };
                chart = window.c3.generate(args);
                window.generatedTicks = tickGenerator(); // This should be removed from window
            });

            it('should use function to generate ticks', function () {
                d3.select('.c3-axis-x').selectAll('g.tick').each(function (d, i) {
                    var tick = d3.select(this).select('text').text();
                    expect(+tick).toBe(window.generatedTicks[i]);
                });
            });
        });
    });

    describe('axis.x.tick.width', function () {

        describe('indexed x axis and y/y2 axis', function () {

            describe('not rotated', function () {

                beforeAll(function () {
                    args = {
                        data: {
                            columns: [
                                ['data1', 30, 200, 100, 400, 150, 250],
                                ['data2', 50, 20, 10, 40, 15, 25]
                            ],
                            axes: {
                                data2: 'y2'
                            }
                        },
                        axis: {
                            y2: {
                                show: true
                            }
                        }
                    };
                });

                it('should construct indexed x axis properly', function () {
                    var ticks = chart.internal.main.select('.c3-axis-x').selectAll('g.tick'),
                        expectedX = '0',
                        expectedDy = '.71em';
                    expect(ticks.size()).toBe(6);
                    ticks.each(function (d, i) {
                        var tspans = d3.select(this).selectAll('tspan');
                        expect(tspans.size()).toBe(1);
                        tspans.each(function () {
                            var tspan = d3.select(this);
                            expect(tspan.text()).toBe(i + '');
                            expect(tspan.attr('x')).toBe(expectedX);
                            expect(tspan.attr('dy')).toBe(expectedDy);
                        });
                    });
                });

                describe('should set axis.x.tick.format', function () {
                    beforeAll(function(){
                        args.axis.x = {
                            tick: {
                                format: function () {
                                    return 'very long tick text on x axis';
                                }
                            }
                        };
                    });

                    it('should split x axis tick text to multiple lines', function () {
                        var ticks = chart.internal.main.select('.c3-axis-x').selectAll('g.tick'),
                            expectedTexts = ['very long tick text on x', 'axis'],
                            expectedX = '0';
                        expect(ticks.size()).toBe(6);
                        ticks.each(function () {
                            var tspans = d3.select(this).selectAll('tspan');
                            expect(tspans.size()).toBe(2);
                            tspans.each(function (d, i) {
                                var tspan = d3.select(this);
                                expect(tspan.text()).toBe(expectedTexts[i]);
                                expect(tspan.attr('x')).toBe(expectedX);
                                if (i === 0) {
                                    expect(tspan.attr('dy')).toBe('.71em');
                                } else {
                                    expect(tspan.attr('dy')).toBeGreaterThan(8);
                                }
                            });
                        });
                    });

                    it('should construct y axis properly', function () {
                        var ticks = chart.internal.main.select('.c3-axis-y').selectAll('g.tick'),
                            expectedX = '-9',
                            expectedDy = '3';
                        expect(ticks.size()).toBe(9);
                        ticks.each(function (d) {
                            var tspans = d3.select(this).selectAll('tspan');
                            expect(tspans.size()).toBe(1);
                            tspans.each(function () {
                                var tspan = d3.select(this);
                                expect(tspan.text()).toBe(d + '');
                                expect(tspan.attr('x')).toBe(expectedX);
                                expect(tspan.attr('dy')).toBe(expectedDy);
                            });
                        });
                    });

                    it('should construct y2 axis properly', function () {
                        var ticks = chart.internal.main.select('.c3-axis-y2').selectAll('g.tick'),
                            expectedX = '9',
                            expectedDy = '3';
                        expect(ticks.size()).toBe(9);
                        ticks.each(function (d) {
                            var tspans = d3.select(this).selectAll('tspan');
                            expect(tspans.size()).toBe(1);
                            tspans.each(function () {
                                var tspan = d3.select(this);
                                expect(tspan.text()).toBe(d + '');
                                expect(tspan.attr('x')).toBe(expectedX);
                                expect(tspan.attr('dy')).toBe(expectedDy);
                            });
                        });
                    });
                });

                describe('should set big values in y', function () {
                    beforeAll(function(){
                        args.data.columns = [
                            ['data1', 3000000000000000, 200, 100, 400, 150, 250],
                            ['data2', 50, 20, 10, 40, 15, 25]
                        ];
                    });

                    it('should not split y axis tick text to multiple lines', function () {
                        var ticks = chart.internal.main.select('.c3-axis-y2').selectAll('g.tick');
                        ticks.each(function () {
                            var tspans = d3.select(this).selectAll('tspan');
                            expect(tspans.size()).toBe(1);
                        });
                    });
                });
            });

            describe('rotated', function () {

                beforeAll(function () {
                    args.axis.rotated = true;
                });

                it('should split x axis tick text to multiple lines', function () {
                    var ticks = chart.internal.main.select('.c3-axis-x').selectAll('g.tick'),
                        expectedTexts = ['very long tick text on', 'x axis'],
                        expectedX = '-9';
                    expect(ticks.size()).toBe(6);
                    ticks.each(function () {
                        var tspans = d3.select(this).selectAll('tspan');
                        expect(tspans.size()).toBe(2);
                        tspans.each(function (d, i) {
                            var tspan = d3.select(this);
                            expect(tspan.text()).toBe(expectedTexts[i]);
                            expect(tspan.attr('x')).toBe(expectedX);
                            if (i === 0) {
                                expect(tspan.attr('dy')).toBeLessThan(0);
                            } else {
                                expect(tspan.attr('dy')).toBeGreaterThan(9);
                            }
                        });
                    });
                });

                it('should not split y axis tick text to multiple lines', function () {
                    var ticks = chart.internal.main.select('.c3-axis-y').selectAll('g.tick'),
                        expectedTexts = [
                            '0',
                            '500000000000000',
                            '1000000000000000',
                            '1500000000000000',
                            '2000000000000000',
                            '2500000000000000',
                            '3000000000000000'
                        ],
                        expectedX = '0',
                        expectedDy = '.71em';
                    expect(ticks.size()).toBe(7);
                    ticks.each(function (d, i) {
                        var tspans = d3.select(this).selectAll('tspan');
                        expect(tspans.size()).toBe(1);
                        tspans.each(function () {
                            var tspan = d3.select(this);
                            expect(tspan.text()).toBe(expectedTexts[i]);
                            expect(tspan.attr('x')).toBe(expectedX);
                            expect(tspan.attr('dy')).toBe(expectedDy);
                        });
                    });
                });

            });
        });

        describe('category axis', function () {

            describe('not rotated', function () {

                beforeAll(function () {
                    args = {
                        data: {
                            x: 'x',
                            columns: [
                                ['x', 'this is a very long tick text on category axis', 'cat1', 'cat2', 'cat3', 'cat4', 'cat5'],
                                ['data1', 30, 200, 100, 400, 150, 250],
                                ['data2', 50, 20, 10, 40, 15, 25]
                            ]
                        },
                        axis: {
                            x: {
                                type: 'category'
                            }
                        }
                    };
                });

                it('should locate ticks properly', function () {
                    var ticks = chart.internal.main.select('.c3-axis-x').selectAll('g.tick');
                    ticks.each(function (d, i) {
                        var tspans = d3.select(this).selectAll('tspan'),
                            expectedX = '0',
                            expectedDy = '.71em';
                        if (i > 0) { // i === 0 should be checked in next test
                            expect(tspans.size()).toBe(1);
                            tspans.each(function () {
                                var tspan = d3.select(this);
                                expect(tspan.attr('x')).toBe(expectedX);
                                expect(tspan.attr('dy')).toBe(expectedDy);
                            });
                        }
                    });
                });

                xit('should split tick text properly', function () {
                    var tick = chart.internal.main.select('.c3-axis-x').select('g.tick'),
                        tspans = tick.selectAll('tspan'),
                        expectedTickTexts = [
                            'this is a very long',
                            'tick text on category',
                            'axis'
                        ],
                        expectedX = '0';
                    expect(tspans.size()).toBe(3);
                    tspans.each(function (d, i) {
                        var tspan = d3.select(this);
                        expect(tspan.text()).toBe(expectedTickTexts[i]);
                        expect(tspan.attr('x')).toBe(expectedX);
                        // unable to define pricise number because it differs depends on environment..
                        if (i === 0) {
                            expect(tspan.attr('dy')).toBe('.71em');
                        } else {
                            expect(tspan.attr('dy')).toBeGreaterThan(8);
                        }
                    });
                });
            });

            describe('rotated', function () {

                beforeAll(function () {
                    args.axis.rotated = true;
                });

                it('should locate ticks on rotated axis properly', function () {
                    var ticks = chart.internal.main.select('.c3-axis-x').selectAll('g.tick');
                    ticks.each(function (d, i) {
                        var tspans = d3.select(this).selectAll('tspan'),
                            expectedX = '-9',
                            expectedDy = '3';
                        if (i > 0) { // i === 0 should be checked in next test
                            expect(tspans.size()).toBe(1);
                            tspans.each(function () {
                                var tspan = d3.select(this);
                                expect(tspan.attr('x')).toBe(expectedX);
                                expect(tspan.attr('dy')).toBe(expectedDy);
                            });
                        }
                    });
                });

                it('should split tick text on rotated axis properly', function () {
                    var tick = chart.internal.main.select('.c3-axis-x').select('g.tick'),
                        tspans = tick.selectAll('tspan'),
                        expectedTickTexts = [
                            'this is a very long',
                            'tick text on category',
                            'axis'
                        ],
                        expectedX = '-9';
                    expect(tspans.size()).toBe(3);
                    tspans.each(function (d, i) {
                        var tspan = d3.select(this);
                        expect(tspan.text()).toBe(expectedTickTexts[i]);
                        expect(tspan.attr('x')).toBe(expectedX);
                        // unable to define pricise number because it differs depends on environment..
                        if (i === 0) {
                            expect(tspan.attr('dy')).toBeLessThan(0);
                        } else {
                            expect(tspan.attr('dy')).toBeGreaterThan(8);
                        }
                    });
                });

            });

            describe('option used', function () {

                describe('as null', function () {

                    beforeAll(function () { //'without split ticks',
                        args.axis.x.tick = {
                            multiline: false
                        };
                    });

                    it('should split x tick', function () {
                        var tick = chart.internal.main.select('.c3-axis-x').select('g.tick'),
                            tspans = tick.selectAll('tspan');
                        expect(tspans.size()).toBe(1);
                    });

                });

                describe('as value', function () {

                    beforeAll(function () { // 'without split ticks',
                        args.axis.x.tick = {
                            width: 150
                        };
                    });

                    it('should split x tick to 2 lines properly', function () {
                        var tick = chart.internal.main.select('.c3-axis-x').select('g.tick'),
                            tspans = tick.selectAll('tspan'),
                            expectedTickTexts = [
                                'this is a very long tick text on',
                                'category axis'
                            ],
                            expectedX = '-9';
                        expect(tspans.size()).toBe(2);
                        tspans.each(function (d, i) {
                            var tspan = d3.select(this);
                            expect(tspan.text()).toBe(expectedTickTexts[i]);
                            expect(tspan.attr('x')).toBe(expectedX);
                            // unable to define pricise number because it differs depends on environment..
                            if (i === 0) {
                                expect(tspan.attr('dy')).toBeLessThan(0);
                            } else {
                                expect(tspan.attr('dy')).toBeGreaterThan(8);
                            }
                        });
                    });
                });

                describe('with multilineMax', function() {
                    beforeAll(function() {
                        args.axis.x.tick = {
                            multiline: true,
                            multilineMax: 2,
                        };
                    });

                    it('should ellipsify x tick properly', function() {
                        var tick = chart.internal.main.select('.c3-axis-x').select('g.tick');
                        var tspans = tick.selectAll('tspan');
                        var expectedTickText = [
                            'this is a very long',
                            'tick text on categ...',
                        ];

                        expect(tspans.size()).toBe(2);

                        tspans.each(function (d, i) {
                            var tspan = d3.select(this);
                            expect(tspan.text()).toBe(expectedTickText[i]);
                        });
                    });
                });
            });
        });

        describe('with axis.x.tick.format', function () {

            beforeAll(function () { // 'with axis.x.tick.format',
                args.axis.x.tick.format = function () {
                    return ['this is a very long tick text', 'on category axis'];
                };
            });

            it('should have multiline tick text', function () {
                var tick = chart.internal.main.select('.c3-axis-x').select('g.tick'),
                    tspans = tick.selectAll('tspan'),
                    expectedTickTexts = ['this is a very long tick text', 'on category axis'];
                expect(tspans.size()).toBe(2);
                tspans.each(function (d, i) {
                    var tspan = d3.select(this);
                    expect(tspan.text()).toBe(expectedTickTexts[i]);
                });
            });

        });
    });

    describe('axis.x.tick.rotate', function () {

        describe('not rotated', function () {

            beforeAll(function () {
                args = {
                    data: {
                        x: 'x',
                        columns: [
                            ['x', 'category 1', 'category 2', 'category 3', 'category 4', 'category 5', 'category 6'],
                            ['data1', 30, 200, 100, 400, 150, 250],
                            ['data2', 50, 20, 10, 40, 15, 25]
                        ]
                    },
                    axis: {
                        x: {
                            type: 'category',
                            tick: {
                                rotate: 60
                            }
                        }
                    }
                };
            });

            it('should rotate tick texts', function () {
                chart.internal.main.selectAll('.c3-axis-x g.tick').each(function () {
                    var tick = d3.select(this),
                        text = tick.select('text'),
                        tspan = text.select('tspan');
                    expect(text.attr('transform')).toBe('rotate(60)');
                    expect(text.attr('y')).toBe('1.5');
                    expect(tspan.attr('dx')).toBe('6.928203230275509');
                });
            });

            it('should have automatically calculated x axis height', function () {
                var box = chart.internal.main.select('.c3-axis-x').node().getBoundingClientRect(),
                    height = chart.internal.getHorizontalAxisHeight('x');
                expect(box.height).toBeGreaterThan(50);
                expect(height).toBeCloseTo(76, -1.3); // @TODO make this test better
            });

        });
    });

    describe('axis.y.tick.rotate', function () {

        describe('not rotated', function () {

            beforeAll(function () {
                args = {
                    data: {
                        columns: [
                            ['data1', 30, 200, 100, 400, 150, 250, 100, 600],
                            ['data2', 50, 20, 10, 40, 15, 25],
                        ]
                    },
                    axis: {
                        rotated: true,
                        y: {
                            tick: {
                                rotate: 45
                            }
                        }
                    }
                };
            });

            it('should rotate tick texts', function () {
                chart.internal.main.selectAll('.c3-axis-y g.tick').each(function () {
                    var tick = d3.select(this),
                        text = tick.select('text'),
                        tspan = text.select('tspan');
                    expect(text.attr('transform')).toBe('rotate(45)');
                    expect(text.attr('y')).toBe('4');
                    expect(tspan.attr('dx')).toBeCloseTo('5.6', 0);
                });
            });

            it('should have automatically calculated y axis width', function () {
                var box = chart.internal.main.select('.c3-axis-y').node().getBoundingClientRect();
                expect(box.width).toBeCloseTo(590, 1);
            });

        });
    });

    describe('axis.x.tick.fit', function () {

        describe('axis.x.tick.fit = true', function () {

            beforeAll(function () { // 'should set args for indexed data',
                args = {
                    data: {
                        columns: [
                            ['data1', 30, 200, 100, 400, 150, 250],
                            ['data2', 50, 20, 10, 40, 15, 25],
                            ['data3', 150, 120, 110, 140, 115, 125]
                        ]
                    }
                };
            });

            it('should show fitted ticks on indexed data', function () {
                var ticks = chart.internal.main.selectAll('.c3-axis-x g.tick');
                expect(ticks.size()).toBe(6);
            });

            describe('should set args for x-based data', function () {
                beforeAll(function(){
                    args = {
                        data: {
                            x: 'x',
                            columns: [
                                ['x', 10, 20, 100, 110, 200, 1000],
                                ['data1', 30, 200, 100, 400, 150, 250],
                                ['data2', 50, 20, 10, 40, 15, 25],
                                ['data3', 150, 120, 110, 140, 115, 125]
                            ]
                        }
                    };
                });

                it('should show fitted ticks on indexed data', function () {
                    var ticks = chart.internal.main.selectAll('.c3-axis-x g.tick');
                    expect(ticks.size()).toBe(6);
                });

                it('should show fitted ticks after hide and show', function () {
                    chart.hide();
                    chart.show();
                    var ticks = chart.internal.main.selectAll('.c3-axis-x g.tick');
                    expect(ticks.size()).toBe(6);
                });
            });
        });

        describe('axis.x.tick.fit = false', function () {

            describe('should set args for indexed data', function () {
                beforeAll(function(){
                    args = {
                        data: {
                            columns: [
                                ['data1', 30, 200, 100, 400, 150, 250],
                                ['data2', 50, 20, 10, 40, 15, 25],
                                ['data3', 150, 120, 110, 140, 115, 125]
                            ]
                        },
                        axis: {
                            x: {
                                tick: {
                                    fit: false
                                }
                            }
                        }
                    };
                });

                it('should show fitted ticks on indexed data', function () {
                    var ticks = chart.internal.main.selectAll('.c3-axis-x g.tick');
                    expect(ticks.size()).toBe(11);
                });
            });


            describe('should set args for x-based data', function () {
                beforeAll(function(){
                    args.data = {
                        x: 'x',
                        columns: [
                            ['x', 10, 20, 100, 110, 200, 1000],
                            ['data1', 30, 200, 100, 400, 150, 250],
                            ['data2', 50, 20, 10, 40, 15, 25],
                            ['data3', 150, 120, 110, 140, 115, 125]
                        ]
                    };
                });

                it('should show fitted ticks on indexed data', function () {
                    var ticks = chart.internal.main.selectAll('.c3-axis-x g.tick');
                    expect(ticks.size()).toBe(10);
                });

                it('should show fitted ticks after hide and show', function () {
                    chart.hide();
                    chart.show();
                    var ticks = chart.internal.main.selectAll('.c3-axis-x g.tick');
                    expect(ticks.size()).toBe(10);
                });
            });
        });
    });

    describe('axis.y.inner', function () {

        beforeAll(function () {
            args = {
                data: {
                    columns: [
                        ['data1', 30, 200, 100, 400, 150, 250],
                        ['data2', 50, 20, 10, 40, 15, 25]
                    ]
                },
                axis: {
                    y: {
                        inner: false
                    }
                }
            };
        });

        it('should not have inner y axis', function () {
            var paddingLeft = chart.internal.getCurrentPaddingLeft(),
                tickTexts = chart.internal.main.selectAll('.c3-axis-y g.tick text');
            expect(paddingLeft).toBeGreaterThan(19);
            tickTexts.each(function () {
                expect(+d3.select(this).attr('x')).toBeLessThan(0);
            });
        });

        describe('with inner y axis', function () {
            beforeAll(function(){
                args.axis.y.inner = true;
            });

            it('should have inner y axis', function () {
                var paddingLeft = chart.internal.getCurrentPaddingLeft(),
                    tickTexts = chart.internal.main.selectAll('.c3-axis-y g.tick text');
                expect(paddingLeft).toBe(1);
                tickTexts.each(function () {
                    expect(+d3.select(this).attr('x')).toBeGreaterThan(0);
                });
            });
        });
    });

    describe('axis.y2.inner', function () {

        beforeAll(function () {
            args = {
                data: {
                    columns: [
                        ['data1', 30, 200, 100, 400, 150, 250],
                        ['data2', 50, 20, 10, 40, 15, 25]
                    ]
                },
                axis: {
                    y2: {
                        show: true,
                        inner: false
                    }
                }
            };
        });

        it('should not have inner y axis', function () {
            var paddingRight = chart.internal.getCurrentPaddingRight(),
                tickTexts = chart.internal.main.selectAll('.c3-axis-2y g.tick text');
            expect(paddingRight).toBeGreaterThan(19);
            tickTexts.each(function () {
                expect(+d3.select(this).attr('x')).toBeGreaterThan(0);
            });
        });

        describe('with inner y axis', function () {
            beforeAll(function(){
                args.axis.y2.inner = true;
            });

            it('should have inner y axis', function () {
                var paddingRight = chart.internal.getCurrentPaddingRight(),
                    tickTexts = chart.internal.main.selectAll('.c3-axis-2y g.tick text');
                expect(paddingRight).toBe(2);
                tickTexts.each(function () {
                    expect(+d3.select(this).attr('x')).toBeLessThan(0);
                });
            });
        });
    });

});
