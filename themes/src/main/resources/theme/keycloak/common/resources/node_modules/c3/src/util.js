import { c3_chart_internal_fn } from './core';

export var isValue = function (v) {
    return v || v === 0;
};
export var isFunction = function (o) {
    return typeof o === 'function';
};
export var isArray = function (o) {
    return Array.isArray(o);
};
export var isString = function (o) {
    return typeof o === 'string';
};
export var isUndefined = function (v) {
return typeof v === 'undefined';
};
export var isDefined = function (v) {
    return typeof v !== 'undefined';
};
export var ceil10 = function (v) {
    return Math.ceil(v / 10) * 10;
};
export var asHalfPixel = function (n) {
    return Math.ceil(n) + 0.5;
};
export var diffDomain = function (d) {
    return d[1] - d[0];
};
export var isEmpty = function (o) {
    return typeof o === 'undefined' || o === null || (isString(o) && o.length === 0) || (typeof o === 'object' && Object.keys(o).length === 0);
};
export var notEmpty = function (o) {
    return !c3_chart_internal_fn.isEmpty(o);
};
export var getOption = function (options, key, defaultValue) {
    return isDefined(options[key]) ? options[key] : defaultValue;
};
export var hasValue = function (dict, value) {
    var found = false;
    Object.keys(dict).forEach(function (key) {
        if (dict[key] === value) { found = true; }
    });
    return found;
};
export var sanitise = function (str) {
    return typeof str === 'string' ? str.replace(/</g, '&lt;').replace(/>/g, '&gt;') : str;
};
export var getPathBox = function (path) {
    var box = path.getBoundingClientRect(),
        items = [path.pathSegList.getItem(0), path.pathSegList.getItem(1)],
        minX = items[0].x, minY = Math.min(items[0].y, items[1].y);
    return {x: minX, y: minY, width: box.width, height: box.height};
};
