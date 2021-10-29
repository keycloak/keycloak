describe('c3 api axis', function () {
    'use strict';

    var chart, args;

    beforeEach(function (done) {
        chart = window.initChart(chart, args, done);
    });

    describe('axis.labels', function () {

        beforeAll(function () {
            args = {
                data: {
                    columns: [
                        ['data1', 30, 200, 100],
                        ['data2', 50, 20, 10]
                    ],
                    axes: {
                        data1: 'y',
                        data2: 'y2'
                    }
                },
                axis: {
                    y: {
                        label: 'Y Axis Label'
                    },
                    y2: {
                        show: true,
                        label: 'Y2 Axis Label'
                    }
                }
            };
        });

        it('updates y axis label', function () {
            chart.axis.labels({y: 'New Y Axis Label'});
            var label = d3.select('.c3-axis-y-label');
            expect(label.text()).toBe('New Y Axis Label');
            expect(label.attr('dx')).toBe('-0.5em');
            expect(label.attr('dy')).toBe('1.2em');
        });

        it('updates y axis label', function () {
            chart.axis.labels({y2: 'New Y2 Axis Label'});
            var label = d3.select('.c3-axis-y2-label');
            expect(label.text()).toBe('New Y2 Axis Label');
            expect(label.attr('dx')).toBe('-0.5em');
            expect(label.attr('dy')).toBe('-0.5em');
        });

    });
});
