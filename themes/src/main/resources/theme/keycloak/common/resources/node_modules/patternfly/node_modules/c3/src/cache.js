import { c3_chart_internal_fn } from './core';

c3_chart_internal_fn.hasCaches = function (ids) {
    for (var i = 0; i < ids.length; i++) {
        if (! (ids[i] in this.cache)) { return false; }
    }
    return true;
};
c3_chart_internal_fn.addCache = function (id, target) {
    this.cache[id] = this.cloneTarget(target);
};
c3_chart_internal_fn.getCaches = function (ids) {
    var targets = [], i;
    for (i = 0; i < ids.length; i++) {
        if (ids[i] in this.cache) { targets.push(this.cloneTarget(this.cache[ids[i]])); }
    }
    return targets;
};
