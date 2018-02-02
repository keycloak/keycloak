import c3 from '../src';

const $$ = c3.chart.internal.fn;

describe('$$.convertColumnsToData', () => {
    it('converts column data to normalized data', () => {
        const data = $$.convertColumnsToData([
            ["cat1", "a", "b", "c", "d"],
            ["data1", 30, 200, 100, 400],
            ["cat2", "b", "a", "c", "d", "e", "f"],
            ["data2", 400, 60, 200, 800, 10, 10]
        ]);

        expect(data).toEqual([{
            cat1: 'a',
            data1: 30,
            cat2: 'b',
            data2: 400
        }, {
            cat1: 'b',
            data1: 200,
            cat2: 'a',
            data2: 60
        }, {
            cat1: 'c',
            data1: 100,
            cat2: 'c',
            data2: 200
        }, {
            cat1: 'd',
            data1: 400,
            cat2: 'd',
            data2: 800
        }, {
            cat2: 'e',
            data2: 10
        }, {
            cat2: 'f',
            data2: 10
        }]);
    });

    it('throws when the column data contains undefined', () => {
        expect(() => $$.convertColumnsToData([
            ["cat1", "a", "b", "c", "d"],
            ["data1", undefined]
        ])).toThrowError(Error, /Source data is missing a component/);
    });
});

describe('$$.convertRowsToData', () => {
    it('converts the row data to normalized data', () => {
        const data = $$.convertRowsToData([
            ['data1', 'data2', 'data3'],
            [90, 120, 300],
            [40, 160, 240],
            [50, 200, 290],
            [120, 160, 230],
            [80, 130, 300],
            [90, 220, 320]
        ]);

        expect(data).toEqual([{
            data1: 90,
            data2: 120,
            data3: 300
        }, {
            data1: 40,
            data2: 160,
            data3: 240
        }, {
            data1: 50,
            data2: 200,
            data3: 290
        }, {
            data1: 120,
            data2: 160,
            data3: 230
        }, {
            data1: 80,
            data2: 130,
            data3: 300
        }, {
            data1: 90,
            data2: 220,
            data3: 320
        }]);
    });

    it('throws when the row data contains undefined', () => {
        expect(() => $$.convertRowsToData([
            ['data1', 'data2', 'data3'],
            [40, 160, 240],
            [90, 120, undefined]
        ])).toThrowError(Error, /Source data is missing a component/);
    });
});
