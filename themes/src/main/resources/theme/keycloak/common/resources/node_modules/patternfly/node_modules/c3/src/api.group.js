import { c3_chart_fn } from './core';
import { isUndefined } from './util';

c3_chart_fn.groups = function (groups) {
    var $$ = this.internal, config = $$.config;
    if (isUndefined(groups)) { return config.data_groups; }
    config.data_groups = groups;
    $$.redraw();
    return config.data_groups;
};
