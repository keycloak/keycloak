describe('c3 chart legend', function () {
    'use strict';

    var chart, args;

    beforeEach(function (done) {
        chart = window.initChart(chart, args, done);
    });

    describe('legend when multiple charts rendered', function () {

        beforeAll(function () {
            args = {
                data: {
                    columns: [
                        ['data1', 30],
                        ['data2', 50],
                        ['data3', 100]
                    ]
                }
            };
        });

        describe('long data names', function () {
            beforeAll(function(){
                args = {
                    data: {
                        columns: [
                            ['long data name 1', 30],
                            ['long data name 2', 50],
                            ['long data name 3', 50],
                        ]
                    }
                };
            });

            it('should have properly computed legend width', function () {
                var expectedLeft = [148, 226, 384],
                    expectedWidth = [118, 118, 108];
                d3.selectAll('.c3-legend-item').each(function (d, i) {
                    var rect = d3.select(this).node().getBoundingClientRect();
                    expect(rect.left).toBeCloseTo(expectedLeft[i], -2);
                    expect(rect.width).toBeCloseTo(expectedWidth[i], -2);
                });
            });
        });
    });

    describe('legend position', function () {

        beforeAll(function () {
            args = {
                data: {
                    columns: [
                        ['data1', 30, 200, 100, 400, 150, 250],
                        ['data2', 50, 20, 10, 40, 15, 25]
                    ]
                }
            };
        });

        it('should be located on the center of chart', function () {
            var box = chart.internal.legend.node().getBoundingClientRect();
            expect(box.left + box.right).toBe(638);
        });

    });

    describe('legend as inset', function () {

        describe('should change the legend to "inset" successfully', function () {
            beforeAll(function(){
                args = {
                    data: {
                        columns: [
                            ['data1', 30, 200, 100, 400, 150, 250],
                            ['data2', 50, 20, 10, 40, 15, 25]
                        ]
                    },
                    legend: {
                        position: 'inset',
                        inset: {
                            step: null
                        }
                    }
                };
            });

            it('should be positioned properly', function () {
                var box = d3.select('.c3-legend-background').node().getBoundingClientRect();
                expect(box.top).toBe(5.5);
                expect(box.left).toBeGreaterThan(30);
            });

            it('should have automatically calculated height', function () {
                var box = d3.select('.c3-legend-background').node().getBoundingClientRect();
                expect(box.height).toBe(48);
            });
        });

        describe('should change the legend step to 1 successfully', function () {
            beforeAll(function(){
                args.legend.inset.step = 1;
            });

            it('should have automatically calculated height', function () {
                var box = d3.select('.c3-legend-background').node().getBoundingClientRect();
                expect(box.height).toBe(28);
            });
        });

        describe('should change the legend step to 2 successfully', function () {
            beforeAll(function(){
                args.legend.inset.step = 2;
            });

            it('should have automatically calculated height', function () {
                var box = d3.select('.c3-legend-background').node().getBoundingClientRect();
                expect(box.height).toBe(48);
            });
        });

        describe('with only one series', function () {
            beforeAll(function(){
                args = {
                    data: {
                        columns: [
                            ['data1', 30, 200, 100, 400, 150, 250],
                        ]
                    },
                    legend: {
                        position: 'inset'
                    }
                };
            });

            it('should locate legend properly', function () {
                var box = d3.select('.c3-legend-background').node().getBoundingClientRect();
                expect(box.height).toBe(28);
                expect(box.width).toBeGreaterThan(64);
            });
        });
    });

    describe('legend.hide', function () {

        beforeAll(function () {
            args = {
                data: {
                    columns: [
                        ['data1', 30, 200, 100, 400, 150, 250],
                        ['data2', 130, 100, 200, 100, 250, 150]
                    ]
                },
                legend: {
                    hide: true
                }
            };
        });

        it('should not show legends', function () {
            d3.selectAll('.c3-legend-item').each(function () {
                expect(d3.select(this).style('visibility')).toBe('hidden');
            });
        });

        describe('hidden legend', function () {
            beforeAll(function(){
                args = {
                    data: {
                        columns: [
                            ['data1', 30, 200, 100, 400, 150, 250],
                            ['data2', 130, 100, 200, 100, 250, 150]
                        ]
                    },
                    legend: {
                        hide: 'data2'
                    }
                };
            });

            it('should not show legends', function () {
                expect(d3.select('.c3-legend-item-data1').style('visibility')).toBe('visible');
                expect(d3.select('.c3-legend-item-data2').style('visibility')).toBe('hidden');
            });
        });

    });

    describe('legend.show', function () {

        beforeAll(function () {
            args = {
                data: {
                    columns: [
                        ['data1', 30, 200, 100, 400, 150, 250],
                        ['data2', 130, 100, 200, 100, 250, 150]
                    ]
                },
                legend: {
                    show: false
                }
            };
        });

        it('should not initially have rendered any legend items', function () {
            expect(d3.selectAll('.c3-legend-item').empty()).toBe(true);
        });

        it('allows us to show the legend on showLegend call', function () {
            chart.legend.show();
            d3.selectAll('.c3-legend-item').each(function () {
                expect(d3.select(this).style('visibility')).toBe('visible');
                // This selects all the children, but we expect it to be empty
                expect(d3.select(this).selectAll("*").length).not.toEqual(0);
            });
        });

    });

    describe('custom legend size', function() {
        beforeAll(function () {
            args = {
                data: {
                    columns: [
                        ['data1', 30, 200, 100, 400, 150, 250],
                        ['data2', 130, 100, 200, 100, 250, 150]
                    ]
                },
                legend: {
                    item: {
                        tile: {
                            width: 15,
                            height: 2
                        }
                    }
                }
            };
        });

        it('renders the legend item with the correct width and height', function () {
            d3.selectAll('.c3-legend-item-tile').each(function () {
                expect(d3.select(this).style('stroke-width')).toBe(args.legend.item.tile.height + 'px');
                var tileWidth = d3.select(this).attr('x2') - d3.select(this).attr('x1');
                expect(tileWidth).toBe(args.legend.item.tile.width);
            });
        });
    });

    describe('custom legend padding', function() {
        beforeAll(function () {
            args = {
                data: {
                    columns: [
                        ['padded1', 30, 200, 100, 400, 150, 250],
                        ['padded2', 130, 100, 200, 100, 250, 150]
                    ]
                },
                legend: {
                    padding: 10
                }
            };
        });

        it('renders the correct amount of padding on the legend element', function () {
            d3.selectAll('.c3-legend-item-padded1 .c3-legend-item-tile, .c3-legend-item-padded2 .c3-legend-item-tile').each(function (el, index) {
                var itemWidth = d3.select(this).node().parentNode.getBBox().width,
                    textBoxWidth = d3.select(d3.select(this).node().parentNode).select('text').node().getBBox().width,
                    tileWidth = 17, // default value is 10, plus 7 more for padding @TODO verify this, seems PhantomJS@^2 adds another 1px to each side
                    expectedWidth = textBoxWidth + tileWidth + (index ? 0 : 10) + args.legend.padding;

                expect(itemWidth).toBe(expectedWidth);
            });
        });
    });

    describe('legend item tile coloring with color_treshold', function () {
        beforeAll(function () {
            args = {
                data: {
                    columns: [
                        ['padded1', 100],
                        ['padded2', 90],
                        ['padded3', 50],
                        ['padded4', 20]
                    ]
                },
                type: 'gauge',
                color: {
                    pattern: ['#FF0000', '#F97600', '#F6C600', '#60B044'],
                    threshold: {
                        values: [30, 80, 95]
                    }
                }
            };
        });

        // espacially for gauges with multiple arcs to have the same coloring between legend tiles, tooltip tiles and arc
        it('selects the color from color_pattern if color_treshold is given', function () {
            var tileColor = [];
            d3.selectAll('.c3-legend-item-tile').each(function () {
                tileColor.push(d3.select(this).style('stroke'));
            });
            expect(tileColor[0]).toBe('rgb(96, 176, 68)');
            expect(tileColor[1]).toBe('rgb(246, 198, 0)');
            expect(tileColor[2]).toBe('rgb(249, 118, 0)');
            expect(tileColor[3]).toBe('rgb(255, 0, 0)');
        });
    });

    describe('legend item tile coloring without color_treshold', function () {
        beforeAll(function () {
            args = {
                data: {
                    columns: [
                        ['padded1', 100],
                        ['padded2', 90],
                        ['padded3', 50],
                        ['padded4', 20]
                    ],
                    colors: {
                        'padded1': '#60b044',
                        'padded4': '#8b008b'
                    }
                },
                type: 'gauge'
            };
        });

        it('selects the color from data_colors, data_color or default', function () {
            var tileColor = [];
            d3.selectAll('.c3-legend-item-tile').each(function () {
                tileColor.push(d3.select(this).style('stroke'));
            });
            expect(tileColor[0]).toBe('rgb(96, 176, 68)');
            expect(tileColor[1]).toBe('rgb(31, 119, 180)');
            expect(tileColor[2]).toBe('rgb(255, 127, 14)');
            expect(tileColor[3]).toBe('rgb(139, 0, 139)');
        });
    });
});
