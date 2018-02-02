import { c3_chart_internal_fn } from './core';

c3_chart_internal_fn.isSafari = function () {
    var ua = window.navigator.userAgent;
    return ua.indexOf('Safari') >= 0 && ua.indexOf('Chrome') < 0;
};
c3_chart_internal_fn.isChrome = function () {
    var ua = window.navigator.userAgent;
    return ua.indexOf('Chrome') >= 0;
};
