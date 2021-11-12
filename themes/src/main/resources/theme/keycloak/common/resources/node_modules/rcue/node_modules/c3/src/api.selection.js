import CLASS from './class';
import { c3_chart_fn } from './core';
import { isDefined } from './util';

c3_chart_fn.selected = function (targetId) {
    var $$ = this.internal, d3 = $$.d3;
    return d3.merge(
        $$.main.selectAll('.' + CLASS.shapes + $$.getTargetSelectorSuffix(targetId)).selectAll('.' + CLASS.shape)
            .filter(function () { return d3.select(this).classed(CLASS.SELECTED); })
            .map(function (d) { return d.map(function (d) { var data = d.__data__; return data.data ? data.data : data; }); })
    );
};
c3_chart_fn.select = function (ids, indices, resetOther) {
    var $$ = this.internal, d3 = $$.d3, config = $$.config;
    if (! config.data_selection_enabled) { return; }
    $$.main.selectAll('.' + CLASS.shapes).selectAll('.' + CLASS.shape).each(function (d, i) {
        var shape = d3.select(this), id = d.data ? d.data.id : d.id,
            toggle = $$.getToggle(this, d).bind($$),
            isTargetId = config.data_selection_grouped || !ids || ids.indexOf(id) >= 0,
            isTargetIndex = !indices || indices.indexOf(i) >= 0,
            isSelected = shape.classed(CLASS.SELECTED);
        // line/area selection not supported yet
        if (shape.classed(CLASS.line) || shape.classed(CLASS.area)) {
            return;
        }
        if (isTargetId && isTargetIndex) {
            if (config.data_selection_isselectable(d) && !isSelected) {
                toggle(true, shape.classed(CLASS.SELECTED, true), d, i);
            }
        } else if (isDefined(resetOther) && resetOther) {
            if (isSelected) {
                toggle(false, shape.classed(CLASS.SELECTED, false), d, i);
            }
        }
    });
};
c3_chart_fn.unselect = function (ids, indices) {
    var $$ = this.internal, d3 = $$.d3, config = $$.config;
    if (! config.data_selection_enabled) { return; }
    $$.main.selectAll('.' + CLASS.shapes).selectAll('.' + CLASS.shape).each(function (d, i) {
        var shape = d3.select(this), id = d.data ? d.data.id : d.id,
            toggle = $$.getToggle(this, d).bind($$),
            isTargetId = config.data_selection_grouped || !ids || ids.indexOf(id) >= 0,
            isTargetIndex = !indices || indices.indexOf(i) >= 0,
            isSelected = shape.classed(CLASS.SELECTED);
        // line/area selection not supported yet
        if (shape.classed(CLASS.line) || shape.classed(CLASS.area)) {
            return;
        }
        if (isTargetId && isTargetIndex) {
            if (config.data_selection_isselectable(d)) {
                if (isSelected) {
                    toggle(false, shape.classed(CLASS.SELECTED, false), d, i);
                }
            }
        }
    });
};
