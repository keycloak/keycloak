describe('c3 chart types', function () {
    'use strict';

    var chart, args;

    beforeEach(function (done) {
        chart = window.initChart(chart, args, done);
    });

    describe('internal.hasArcType', function () {

        describe('with data', function () {

            beforeAll(function () {
                args = {
                    data: {
                        columns: [
                            ['data1', 30, 200, 100, 400, 150, 250],
                            ['data2', 50, 20, 10, 40, 15, 25],
                            ['data3', 150, 120, 110, 140, 115, 125]
                        ],
                        type: 'pie'
                    }
                };
            });

            it('should return true', function () {
                expect(chart.internal.hasArcType()).toBeTruthy();
            });

            describe('should change chart type to "bar"', function () {
                beforeAll(function(){
                    args.data.type = 'bar';
                });

                it('should return false', function () {
                    expect(chart.internal.hasArcType()).toBeFalsy();
                });
            });

        });

        describe('with empty data', function () {

            beforeAll(function () {
                args = {
                    data: {
                        columns: [],
                        type: 'pie'
                    }
                };
            });

            it('should return true', function () {
                expect(chart.internal.hasArcType()).toBeTruthy();
            });

            describe('should change chart type to "bar"', function () {
                beforeAll(function(){
                    args.data.type = 'bar';
                });

                it('should return false', function () {
                    expect(chart.internal.hasArcType()).toBeFalsy();
                });
            });

        });

    });

    describe('internal.hasType', function () {

        beforeAll(function () {
            args = {
                data: {
                    columns: [
                        ['data1', 30, 200, 100, 400, 150, 250],
                        ['data2', 50, 20, 10, 40, 15, 25],
                        ['data3', 150, 120, 110, 140, 115, 125]
                    ],
                    type: 'pie'
                }
            };
        });

        it('should return true for "pie" type', function () {
            expect(chart.internal.hasType('pie')).toBeTruthy();
        });

        it('should return false for "line" type', function () {
            expect(chart.internal.hasType('line')).toBeFalsy();
        });

        it('should return false for "bar" type', function () {
            expect(chart.internal.hasType('bar')).toBeFalsy();
        });

        describe('should unload successfully', function () {
            beforeAll(function(){
                chart.unload([]);
            });

            it('should return true for "pie" type even if no data', function () {
                expect(chart.internal.hasType('pie')).toBeTruthy();
            });

            it('should return false for "line" type even if no data', function () {
                expect(chart.internal.hasType('line')).toBeFalsy();
            });

            it('should return false for "bar" type even if no data', function () {
                expect(chart.internal.hasType('bar')).toBeFalsy();
            });

            describe('should change chart type to "bar" successfully', function () {
                beforeAll(function(){
                    args.data.type = 'bar';
                });

                it('should return false for "pie" type even if no data', function () {
                    expect(chart.internal.hasType('pie')).toBeFalsy();
                });

                it('should return false for "line" type even if no data', function () {
                    expect(chart.internal.hasType('line')).toBeFalsy();
                });

                it('should return true for "bar" type even if no data', function () {
                    expect(chart.internal.hasType('bar')).toBeTruthy();
                });
            });
        });

    });

});
