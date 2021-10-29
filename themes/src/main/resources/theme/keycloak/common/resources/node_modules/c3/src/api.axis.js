import { c3_chart_fn } from './core';
import { isValue, isDefined } from './util';

c3_chart_fn.axis = function () {};
c3_chart_fn.axis.labels = function (labels) {
    var $$ = this.internal;
    if (arguments.length) {
        Object.keys(labels).forEach(function (axisId) {
            $$.axis.setLabelText(axisId, labels[axisId]);
        });
        $$.axis.updateLabels();
    }
    // TODO: return some values?
};
c3_chart_fn.axis.max = function (max) {
    var $$ = this.internal, config = $$.config;
    if (arguments.length) {
        if (typeof max === 'object') {
            if (isValue(max.x)) { config.axis_x_max = max.x; }
            if (isValue(max.y)) { config.axis_y_max = max.y; }
            if (isValue(max.y2)) { config.axis_y2_max = max.y2; }
        } else {
            config.axis_y_max = config.axis_y2_max = max;
        }
        $$.redraw({withUpdateOrgXDomain: true, withUpdateXDomain: true});
    } else {
        return {
            x: config.axis_x_max,
            y: config.axis_y_max,
            y2: config.axis_y2_max
        };
    }
};
c3_chart_fn.axis.min = function (min) {
    var $$ = this.internal, config = $$.config;
    if (arguments.length) {
        if (typeof min === 'object') {
            if (isValue(min.x)) { config.axis_x_min = min.x; }
            if (isValue(min.y)) { config.axis_y_min = min.y; }
            if (isValue(min.y2)) { config.axis_y2_min = min.y2; }
        } else {
            config.axis_y_min = config.axis_y2_min = min;
        }
        $$.redraw({withUpdateOrgXDomain: true, withUpdateXDomain: true});
    } else {
        return {
            x: config.axis_x_min,
            y: config.axis_y_min,
            y2: config.axis_y2_min
        };
    }
};
c3_chart_fn.axis.range = function (range) {
    if (arguments.length) {
        if (isDefined(range.max)) { this.axis.max(range.max); }
        if (isDefined(range.min)) { this.axis.min(range.min); }
    } else {
        return {
            max: this.axis.max(),
            min: this.axis.min()
        };
    }
};
