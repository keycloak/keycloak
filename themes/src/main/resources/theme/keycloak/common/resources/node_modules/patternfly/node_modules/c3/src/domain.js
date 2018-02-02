import { c3_chart_internal_fn } from './core';
import { isValue, isDefined, diffDomain, notEmpty } from './util';

c3_chart_internal_fn.getYDomainMin = function (targets) {
    var $$ = this, config = $$.config,
        ids = $$.mapToIds(targets), ys = $$.getValuesAsIdKeyed(targets),
        j, k, baseId, idsInGroup, id, hasNegativeValue;
    if (config.data_groups.length > 0) {
        hasNegativeValue = $$.hasNegativeValueInTargets(targets);
        for (j = 0; j < config.data_groups.length; j++) {
            // Determine baseId
            idsInGroup = config.data_groups[j].filter(function (id) { return ids.indexOf(id) >= 0; });
            if (idsInGroup.length === 0) { continue; }
            baseId = idsInGroup[0];
            // Consider negative values
            if (hasNegativeValue && ys[baseId]) {
                ys[baseId].forEach(function (v, i) {
                    ys[baseId][i] = v < 0 ? v : 0;
                });
            }
            // Compute min
            for (k = 1; k < idsInGroup.length; k++) {
                id = idsInGroup[k];
                if (! ys[id]) { continue; }
                ys[id].forEach(function (v, i) {
                    if ($$.axis.getId(id) === $$.axis.getId(baseId) && ys[baseId] && !(hasNegativeValue && +v > 0)) {
                        ys[baseId][i] += +v;
                    }
                });
            }
        }
    }
    return $$.d3.min(Object.keys(ys).map(function (key) { return $$.d3.min(ys[key]); }));
};
c3_chart_internal_fn.getYDomainMax = function (targets) {
    var $$ = this, config = $$.config,
        ids = $$.mapToIds(targets), ys = $$.getValuesAsIdKeyed(targets),
        j, k, baseId, idsInGroup, id, hasPositiveValue;
    if (config.data_groups.length > 0) {
        hasPositiveValue = $$.hasPositiveValueInTargets(targets);
        for (j = 0; j < config.data_groups.length; j++) {
            // Determine baseId
            idsInGroup = config.data_groups[j].filter(function (id) { return ids.indexOf(id) >= 0; });
            if (idsInGroup.length === 0) { continue; }
            baseId = idsInGroup[0];
            // Consider positive values
            if (hasPositiveValue && ys[baseId]) {
                ys[baseId].forEach(function (v, i) {
                    ys[baseId][i] = v > 0 ? v : 0;
                });
            }
            // Compute max
            for (k = 1; k < idsInGroup.length; k++) {
                id = idsInGroup[k];
                if (! ys[id]) { continue; }
                ys[id].forEach(function (v, i) {
                    if ($$.axis.getId(id) === $$.axis.getId(baseId) && ys[baseId] && !(hasPositiveValue && +v < 0)) {
                        ys[baseId][i] += +v;
                    }
                });
            }
        }
    }
    return $$.d3.max(Object.keys(ys).map(function (key) { return $$.d3.max(ys[key]); }));
};
c3_chart_internal_fn.getYDomain = function (targets, axisId, xDomain) {
    var $$ = this, config = $$.config,
        targetsByAxisId = targets.filter(function (t) { return $$.axis.getId(t.id) === axisId; }),
        yTargets = xDomain ? $$.filterByXDomain(targetsByAxisId, xDomain) : targetsByAxisId,
        yMin = axisId === 'y2' ? config.axis_y2_min : config.axis_y_min,
        yMax = axisId === 'y2' ? config.axis_y2_max : config.axis_y_max,
        yDomainMin = $$.getYDomainMin(yTargets),
        yDomainMax = $$.getYDomainMax(yTargets),
        domain, domainLength, padding, padding_top, padding_bottom,
        center = axisId === 'y2' ? config.axis_y2_center : config.axis_y_center,
        yDomainAbs, lengths, diff, ratio, isAllPositive, isAllNegative,
        isZeroBased = ($$.hasType('bar', yTargets) && config.bar_zerobased) || ($$.hasType('area', yTargets) && config.area_zerobased),
        isInverted = axisId === 'y2' ? config.axis_y2_inverted : config.axis_y_inverted,
        showHorizontalDataLabel = $$.hasDataLabel() && config.axis_rotated,
        showVerticalDataLabel = $$.hasDataLabel() && !config.axis_rotated;

    // MEMO: avoid inverting domain unexpectedly
    yDomainMin = isValue(yMin) ? yMin : isValue(yMax) ? (yDomainMin < yMax ? yDomainMin : yMax - 10) : yDomainMin;
    yDomainMax = isValue(yMax) ? yMax : isValue(yMin) ? (yMin < yDomainMax ? yDomainMax : yMin + 10) : yDomainMax;

    if (yTargets.length === 0) { // use current domain if target of axisId is none
        return axisId === 'y2' ? $$.y2.domain() : $$.y.domain();
    }
    if (isNaN(yDomainMin)) { // set minimum to zero when not number
        yDomainMin = 0;
    }
    if (isNaN(yDomainMax)) { // set maximum to have same value as yDomainMin
        yDomainMax = yDomainMin;
    }
    if (yDomainMin === yDomainMax) {
        yDomainMin < 0 ? yDomainMax = 0 : yDomainMin = 0;
    }
    isAllPositive = yDomainMin >= 0 && yDomainMax >= 0;
    isAllNegative = yDomainMin <= 0 && yDomainMax <= 0;

    // Cancel zerobased if axis_*_min / axis_*_max specified
    if ((isValue(yMin) && isAllPositive) || (isValue(yMax) && isAllNegative)) {
        isZeroBased = false;
    }

    // Bar/Area chart should be 0-based if all positive|negative
    if (isZeroBased) {
        if (isAllPositive) { yDomainMin = 0; }
        if (isAllNegative) { yDomainMax = 0; }
    }

    domainLength = Math.abs(yDomainMax - yDomainMin);
    padding = padding_top = padding_bottom = domainLength * 0.1;

    if (typeof center !== 'undefined') {
        yDomainAbs = Math.max(Math.abs(yDomainMin), Math.abs(yDomainMax));
        yDomainMax = center + yDomainAbs;
        yDomainMin = center - yDomainAbs;
    }
    // add padding for data label
    if (showHorizontalDataLabel) {
        lengths = $$.getDataLabelLength(yDomainMin, yDomainMax, 'width');
        diff = diffDomain($$.y.range());
        ratio = [lengths[0] / diff, lengths[1] / diff];
        padding_top += domainLength * (ratio[1] / (1 - ratio[0] - ratio[1]));
        padding_bottom += domainLength * (ratio[0] / (1 - ratio[0] - ratio[1]));
    } else if (showVerticalDataLabel) {
        lengths = $$.getDataLabelLength(yDomainMin, yDomainMax, 'height');
        padding_top += $$.axis.convertPixelsToAxisPadding(lengths[1], domainLength);
        padding_bottom += $$.axis.convertPixelsToAxisPadding(lengths[0], domainLength);
    }
    if (axisId === 'y' && notEmpty(config.axis_y_padding)) {
        padding_top = $$.axis.getPadding(config.axis_y_padding, 'top', padding_top, domainLength);
        padding_bottom = $$.axis.getPadding(config.axis_y_padding, 'bottom', padding_bottom, domainLength);
    }
    if (axisId === 'y2' && notEmpty(config.axis_y2_padding)) {
        padding_top = $$.axis.getPadding(config.axis_y2_padding, 'top', padding_top, domainLength);
        padding_bottom = $$.axis.getPadding(config.axis_y2_padding, 'bottom', padding_bottom, domainLength);
    }
    // Bar/Area chart should be 0-based if all positive|negative
    if (isZeroBased) {
        if (isAllPositive) { padding_bottom = yDomainMin; }
        if (isAllNegative) { padding_top = -yDomainMax; }
    }
    domain = [yDomainMin - padding_bottom, yDomainMax + padding_top];
    return isInverted ? domain.reverse() : domain;
};
c3_chart_internal_fn.getXDomainMin = function (targets) {
    var $$ = this, config = $$.config;
    return isDefined(config.axis_x_min) ?
        ($$.isTimeSeries() ? this.parseDate(config.axis_x_min) : config.axis_x_min) :
    $$.d3.min(targets, function (t) { return $$.d3.min(t.values, function (v) { return v.x; }); });
};
c3_chart_internal_fn.getXDomainMax = function (targets) {
    var $$ = this, config = $$.config;
    return isDefined(config.axis_x_max) ?
        ($$.isTimeSeries() ? this.parseDate(config.axis_x_max) : config.axis_x_max) :
    $$.d3.max(targets, function (t) { return $$.d3.max(t.values, function (v) { return v.x; }); });
};
c3_chart_internal_fn.getXDomainPadding = function (domain) {
    var $$ = this, config = $$.config,
        diff = domain[1] - domain[0],
        maxDataCount, padding, paddingLeft, paddingRight;
    if ($$.isCategorized()) {
        padding = 0;
    } else if ($$.hasType('bar')) {
        maxDataCount = $$.getMaxDataCount();
        padding = maxDataCount > 1 ? (diff / (maxDataCount - 1)) / 2 : 0.5;
    } else {
        padding = diff * 0.01;
    }
    if (typeof config.axis_x_padding === 'object' && notEmpty(config.axis_x_padding)) {
        paddingLeft = isValue(config.axis_x_padding.left) ? config.axis_x_padding.left : padding;
        paddingRight = isValue(config.axis_x_padding.right) ? config.axis_x_padding.right : padding;
    } else if (typeof config.axis_x_padding === 'number') {
        paddingLeft = paddingRight = config.axis_x_padding;
    } else {
        paddingLeft = paddingRight = padding;
    }
    return {left: paddingLeft, right: paddingRight};
};
c3_chart_internal_fn.getXDomain = function (targets) {
    var $$ = this,
        xDomain = [$$.getXDomainMin(targets), $$.getXDomainMax(targets)],
        firstX = xDomain[0], lastX = xDomain[1],
        padding = $$.getXDomainPadding(xDomain),
        min = 0, max = 0;
    // show center of x domain if min and max are the same
    if ((firstX - lastX) === 0 && !$$.isCategorized()) {
        if ($$.isTimeSeries()) {
            firstX = new Date(firstX.getTime() * 0.5);
            lastX = new Date(lastX.getTime() * 1.5);
        } else {
            firstX = firstX === 0 ? 1 : (firstX * 0.5);
            lastX = lastX === 0 ? -1 : (lastX * 1.5);
        }
    }
    if (firstX || firstX === 0) {
        min = $$.isTimeSeries() ? new Date(firstX.getTime() - padding.left) : firstX - padding.left;
    }
    if (lastX || lastX === 0) {
        max = $$.isTimeSeries() ? new Date(lastX.getTime() + padding.right) : lastX + padding.right;
    }
    return [min, max];
};
c3_chart_internal_fn.updateXDomain = function (targets, withUpdateXDomain, withUpdateOrgXDomain, withTrim, domain) {
    var $$ = this, config = $$.config;

    if (withUpdateOrgXDomain) {
        $$.x.domain(domain ? domain : $$.d3.extent($$.getXDomain(targets)));
        $$.orgXDomain = $$.x.domain();
        if (config.zoom_enabled) { $$.zoom.scale($$.x).updateScaleExtent(); }
        $$.subX.domain($$.x.domain());
        if ($$.brush) { $$.brush.scale($$.subX); }
    }
    if (withUpdateXDomain) {
        $$.x.domain(domain ? domain : (!$$.brush || $$.brush.empty()) ? $$.orgXDomain : $$.brush.extent());
        if (config.zoom_enabled) { $$.zoom.scale($$.x).updateScaleExtent(); }
    }

    // Trim domain when too big by zoom mousemove event
    if (withTrim) { $$.x.domain($$.trimXDomain($$.x.orgDomain())); }

    return $$.x.domain();
};
c3_chart_internal_fn.trimXDomain = function (domain) {
    var zoomDomain = this.getZoomDomain(),
        min = zoomDomain[0], max = zoomDomain[1];
    if (domain[0] <= min) {
        domain[1] = +domain[1] + (min - domain[0]);
        domain[0] = min;
    }
    if (max <= domain[1]) {
        domain[0] = +domain[0] - (domain[1] - max);
        domain[1] = max;
    }
    return domain;
};
