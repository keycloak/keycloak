import { c3_chart_fn } from './core';

c3_chart_fn.x = function (x) {
    var $$ = this.internal;
    if (arguments.length) {
        $$.updateTargetX($$.data.targets, x);
        $$.redraw({withUpdateOrgXDomain: true, withUpdateXDomain: true});
    }
    return $$.data.xs;
};
c3_chart_fn.xs = function (xs) {
    var $$ = this.internal;
    if (arguments.length) {
        $$.updateTargetXs($$.data.targets, xs);
        $$.redraw({withUpdateOrgXDomain: true, withUpdateXDomain: true});
    }
    return $$.data.xs;
};
