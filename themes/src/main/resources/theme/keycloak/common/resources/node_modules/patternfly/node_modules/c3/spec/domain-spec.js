describe('c3 chart domain', function () {
    'use strict';

    var chart;

    var args = {
        data: {
            columns: [
                ['data1', 30, 200, 100, 400, 150, 250],
                ['data2', 50, 20, 10, 40, 15, 25]
            ]
        },
        axis: {
            y: {},
            y2: {}
        }
    };

    beforeEach(function (done) {
        chart = window.initChart(chart, args, done);
    });

    describe('axis.y.min', function () {

        describe('should change axis.y.min to -100', function () {
            beforeAll(function(){
                args.axis.y.min = -100;
            });

            it('should be set properly when smaller than max of data', function () {
                var domain = chart.internal.y.domain();
                expect(domain[0]).toBe(-150);
                expect(domain[1]).toBe(450);
            });
        });

        describe('should change axis.y.min to 500', function () {
            beforeAll(function(){
                args.axis.y.min = 500;
            });

            it('should be set properly when bigger than max of data', function () {
                var domain = chart.internal.y.domain();
                expect(domain[0]).toBe(499);
                expect(domain[1]).toBe(511);
            });
        });

        afterAll(function(){
            args.axis.y.min = undefined;
        });

    });

    describe('axis.y.max', function () {
        describe('should change axis.y.max to 1000', function () {
            beforeAll(function(){
                args.axis.y.max = 1000;
            });

            it('should be set properly when bigger than min of data', function () {
                var domain = chart.internal.y.domain();
                expect(domain[0]).toBe(-89);
                expect(domain[1]).toBe(1099);
            });
        });

        describe('should change axis.y.max to 0', function () {
            beforeAll(function(){
                args.axis.y.max = 0;
            });

            it('should be set properly when smaller than min of data', function () {
                var domain = chart.internal.y.domain();
                expect(domain[0]).toBe(-11);
                expect(domain[1]).toBe(1);
            });
        });
    });

    describe('axis.y.padding', function () {

        describe('should change axis.y.max to 1000', function () {
            beforeAll(function(){
                args = {
                    data: {
                        columns: [
                            ['data1', 10, 20, 10, 40, 15, 25],
                            ['data2', 50, 40, 30, 45, 25, 45]
                        ]
                    },
                    axis: {
                        y: {
                            padding: 200,
                        }
                    }
                };
            });

            it('should be set properly when bigger than min of data', function () {
                var domain = chart.internal.y.domain();
                expect(domain[0]).toBeCloseTo(-9, -1);
                expect(domain[1]).toBeCloseTo(69, -1);
            });
        });


        describe('should change axis.y.max to 1000 with top/bottom padding', function () {
            beforeAll(function(){
                args = {
                    data: {
                        columns: [
                            ['data1', 10, 20, 10, 40, 15, 25],
                            ['data2', 50, 40, 30, 45, 25, 45]
                        ]
                    },
                    axis: {
                        y: {
                            padding: {
                                top: 200,
                                bottom: 200
                            }
                        }
                    }
                };
            });

            it('should be set properly when bigger than min of data', function () {
                var domain = chart.internal.y.domain();
                expect(domain[0]).toBeCloseTo(-9, -1);
                expect(domain[1]).toBeCloseTo(69, -1);
            });
        });
    });
});
