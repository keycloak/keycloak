describe('c3', function () {
    'use strict';

    var c3 = window.c3;

    it('exists', function () {
        expect(c3).not.toBeNull();
        expect(typeof c3).toBe('object');
    });
});
