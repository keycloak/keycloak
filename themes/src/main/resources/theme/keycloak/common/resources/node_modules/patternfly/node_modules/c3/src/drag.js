import CLASS from './class';
import { c3_chart_internal_fn } from './core';
import { getPathBox } from './util';

c3_chart_internal_fn.drag = function (mouse) {
    var $$ = this, config = $$.config, main = $$.main, d3 = $$.d3;
    var sx, sy, mx, my, minX, maxX, minY, maxY;

    if ($$.hasArcType()) { return; }
    if (! config.data_selection_enabled) { return; } // do nothing if not selectable
    if (config.zoom_enabled && ! $$.zoom.altDomain) { return; } // skip if zoomable because of conflict drag dehavior
    if (!config.data_selection_multiple) { return; } // skip when single selection because drag is used for multiple selection

    sx = $$.dragStart[0];
    sy = $$.dragStart[1];
    mx = mouse[0];
    my = mouse[1];
    minX = Math.min(sx, mx);
    maxX = Math.max(sx, mx);
    minY = (config.data_selection_grouped) ? $$.margin.top : Math.min(sy, my);
    maxY = (config.data_selection_grouped) ? $$.height : Math.max(sy, my);

    main.select('.' + CLASS.dragarea)
        .attr('x', minX)
        .attr('y', minY)
        .attr('width', maxX - minX)
        .attr('height', maxY - minY);
    // TODO: binary search when multiple xs
    main.selectAll('.' + CLASS.shapes).selectAll('.' + CLASS.shape)
        .filter(function (d) { return config.data_selection_isselectable(d); })
        .each(function (d, i) {
            var shape = d3.select(this),
                isSelected = shape.classed(CLASS.SELECTED),
                isIncluded = shape.classed(CLASS.INCLUDED),
                _x, _y, _w, _h, toggle, isWithin = false, box;
            if (shape.classed(CLASS.circle)) {
                _x = shape.attr("cx") * 1;
                _y = shape.attr("cy") * 1;
                toggle = $$.togglePoint;
                isWithin = minX < _x && _x < maxX && minY < _y && _y < maxY;
            }
            else if (shape.classed(CLASS.bar)) {
                box = getPathBox(this);
                _x = box.x;
                _y = box.y;
                _w = box.width;
                _h = box.height;
                toggle = $$.togglePath;
                isWithin = !(maxX < _x || _x + _w < minX) && !(maxY < _y || _y + _h < minY);
            } else {
                // line/area selection not supported yet
                return;
            }
            if (isWithin ^ isIncluded) {
                shape.classed(CLASS.INCLUDED, !isIncluded);
                // TODO: included/unincluded callback here
                shape.classed(CLASS.SELECTED, !isSelected);
                toggle.call($$, !isSelected, shape, d, i);
            }
        });
};

c3_chart_internal_fn.dragstart = function (mouse) {
    var $$ = this, config = $$.config;
    if ($$.hasArcType()) { return; }
    if (! config.data_selection_enabled) { return; } // do nothing if not selectable
    $$.dragStart = mouse;
    $$.main.select('.' + CLASS.chart).append('rect')
        .attr('class', CLASS.dragarea)
        .style('opacity', 0.1);
    $$.dragging = true;
};

c3_chart_internal_fn.dragend = function () {
    var $$ = this, config = $$.config;
    if ($$.hasArcType()) { return; }
    if (! config.data_selection_enabled) { return; } // do nothing if not selectable
    $$.main.select('.' + CLASS.dragarea)
        .transition().duration(100)
        .style('opacity', 0)
        .remove();
    $$.main.selectAll('.' + CLASS.shape)
        .classed(CLASS.INCLUDED, false);
    $$.dragging = false;
};
