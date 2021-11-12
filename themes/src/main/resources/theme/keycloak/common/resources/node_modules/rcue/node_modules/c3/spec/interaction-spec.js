describe('c3 chart interaction', function () {
    'use strict';

    var chart, args;

    beforeEach(function (done) {
        chart = window.initChart(chart, args, done);
    });

    describe('generate event rects', function () {

        describe('custom x', function () {

            beforeAll(function () {
                args = {
                    data: {
                        x: 'x',
                        columns: [
                            ['x', 0, 1000, 3000, 10000],
                            ['data', 10, 10, 10, 10]
                        ],
                        type: 'bar'
                    }
                };
            });

            it('should have 4 event rects properly', function () {
                var lefts = [78, 138, 205.5, 407.5],
                    widths = [60, 67.5, 202, 194];
                d3.selectAll('.c3-event-rect').each(function (d, i) {
                    var box = d3.select(this).node().getBoundingClientRect();
                    expect(box.left).toBeCloseTo(lefts[i], -2);
                    expect(box.width).toBeCloseTo(widths[i], -2);
                });
            });

            describe('should generate bar chart with only one data', function () {
                beforeAll(function(){
                    args = {
                        data: {
                            x: 'x',
                            columns: [
                                ['x', 0],
                                ['data', 10]
                            ],
                            type: 'bar'
                        }
                    };
                });

                it('should have 1 event rects properly', function () {
                    var eventRects = d3.selectAll('.c3-event-rect');
                    expect(eventRects.size()).toBe(1);
                    eventRects.each(function () {
                        var box = d3.select(this).node().getBoundingClientRect();
                        expect(box.left).toBeCloseTo(40.5, -2);
                        expect(box.width).toBeCloseTo(598, -2);
                    });
                });
            });
        });

        describe('timeseries', function () {
            beforeAll(function () {
                args = {
                    data: {
                        x: 'x',
                        columns: [
                            ['x', '20140101', '20140201', '20140210',  '20140301'],
                            ['data', 10, 10, 10, 10]
                        ]
                    }
                };
            });

            it('should have 4 event rects properly', function () {
                var lefts = [43.5, 193, 353, 500],
                    widths = [149.5, 160, 147, 136];
                d3.selectAll('.c3-event-rect').each(function (d, i) {
                    var box = d3.select(this).node().getBoundingClientRect();
                    expect(box.left).toBeCloseTo(lefts[i], -2);
                    expect(box.width).toBeCloseTo(widths[i], -2);
                });

            });

            describe('should generate line chart with only 1 data timeseries', function () {
                beforeAll(function(){
                    args = {
                        data: {
                            x: 'x',
                            columns: [
                                ['x', '20140101'],
                                ['data', 10]
                            ]
                        }
                    };
                });

                it('should have 1 event rects properly', function () {
                    var eventRects = d3.selectAll('.c3-event-rect');
                    expect(eventRects.size()).toBe(1);
                    eventRects.each(function () {
                        var box = d3.select(this).node().getBoundingClientRect();
                        expect(box.left).toBeCloseTo(40.5, -2);
                        expect(box.width).toBeCloseTo(598, -2);
                    });
                });
            });
        });
    });
});
