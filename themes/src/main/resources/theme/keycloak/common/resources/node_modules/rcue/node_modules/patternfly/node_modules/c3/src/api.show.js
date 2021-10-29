import { c3_chart_fn } from './core';

c3_chart_fn.show = function (targetIds, options) {
    var $$ = this.internal, targets;

    targetIds = $$.mapToTargetIds(targetIds);
    options = options || {};

    $$.removeHiddenTargetIds(targetIds);
    targets = $$.svg.selectAll($$.selectorTargets(targetIds));

    targets.transition()
        .style('opacity', 1, 'important')
        .call($$.endall, function () {
            targets.style('opacity', null).style('opacity', 1);
        });

    if (options.withLegend) {
        $$.showLegend(targetIds);
    }

    $$.redraw({withUpdateOrgXDomain: true, withUpdateXDomain: true, withLegend: true});
};

c3_chart_fn.hide = function (targetIds, options) {
    var $$ = this.internal, targets;

    targetIds = $$.mapToTargetIds(targetIds);
    options = options || {};

    $$.addHiddenTargetIds(targetIds);
    targets = $$.svg.selectAll($$.selectorTargets(targetIds));

    targets.transition()
        .style('opacity', 0, 'important')
        .call($$.endall, function () {
            targets.style('opacity', null).style('opacity', 0);
        });

    if (options.withLegend) {
        $$.hideLegend(targetIds);
    }

    $$.redraw({withUpdateOrgXDomain: true, withUpdateXDomain: true, withLegend: true});
};

c3_chart_fn.toggle = function (targetIds, options) {
    var that = this, $$ = this.internal;
    $$.mapToTargetIds(targetIds).forEach(function (targetId) {
        $$.isTargetToShow(targetId) ? that.hide(targetId, options) : that.show(targetId, options);
    });
};
