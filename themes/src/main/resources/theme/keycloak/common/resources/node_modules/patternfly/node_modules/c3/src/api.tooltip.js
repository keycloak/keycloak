import { c3_chart_fn } from './core';
import { isValue } from './util';

c3_chart_fn.tooltip = function () {};
c3_chart_fn.tooltip.show = function (args) {
    var $$ = this.internal, index, mouse;

    // determine mouse position on the chart
    if (args.mouse) {
        mouse = args.mouse;
    }

    // determine focus data
    if (args.data) {
        if ($$.isMultipleX()) {
            // if multiple xs, target point will be determined by mouse
            mouse = [$$.x(args.data.x), $$.getYScale(args.data.id)(args.data.value)];
            index = null;
        } else {
            // TODO: when tooltip_grouped = false
            index = isValue(args.data.index) ? args.data.index : $$.getIndexByX(args.data.x);
        }
    }
    else if (typeof args.x !== 'undefined') {
        index = $$.getIndexByX(args.x);
    }
    else if (typeof args.index !== 'undefined') {
        index = args.index;
    }

    // emulate mouse events to show
    $$.dispatchEvent('mouseover', index, mouse);
    $$.dispatchEvent('mousemove', index, mouse);

    $$.config.tooltip_onshow.call($$, args.data);
};
c3_chart_fn.tooltip.hide = function () {
    // TODO: get target data by checking the state of focus
    this.internal.dispatchEvent('mouseout', 0);

    this.internal.config.tooltip_onhide.call(this);
};
