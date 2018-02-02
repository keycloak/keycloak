(function (global, factory) {
	typeof exports === 'object' && typeof module !== 'undefined' ? module.exports = factory() :
	typeof define === 'function' && define.amd ? define(factory) :
	(global.c3 = factory());
}(this, (function () { 'use strict';

var CLASS = {
    target: 'c3-target',
    chart: 'c3-chart',
    chartLine: 'c3-chart-line',
    chartLines: 'c3-chart-lines',
    chartBar: 'c3-chart-bar',
    chartBars: 'c3-chart-bars',
    chartText: 'c3-chart-text',
    chartTexts: 'c3-chart-texts',
    chartArc: 'c3-chart-arc',
    chartArcs: 'c3-chart-arcs',
    chartArcsTitle: 'c3-chart-arcs-title',
    chartArcsBackground: 'c3-chart-arcs-background',
    chartArcsGaugeUnit: 'c3-chart-arcs-gauge-unit',
    chartArcsGaugeMax: 'c3-chart-arcs-gauge-max',
    chartArcsGaugeMin: 'c3-chart-arcs-gauge-min',
    selectedCircle: 'c3-selected-circle',
    selectedCircles: 'c3-selected-circles',
    eventRect: 'c3-event-rect',
    eventRects: 'c3-event-rects',
    eventRectsSingle: 'c3-event-rects-single',
    eventRectsMultiple: 'c3-event-rects-multiple',
    zoomRect: 'c3-zoom-rect',
    brush: 'c3-brush',
    focused: 'c3-focused',
    defocused: 'c3-defocused',
    region: 'c3-region',
    regions: 'c3-regions',
    title: 'c3-title',
    tooltipContainer: 'c3-tooltip-container',
    tooltip: 'c3-tooltip',
    tooltipName: 'c3-tooltip-name',
    shape: 'c3-shape',
    shapes: 'c3-shapes',
    line: 'c3-line',
    lines: 'c3-lines',
    bar: 'c3-bar',
    bars: 'c3-bars',
    circle: 'c3-circle',
    circles: 'c3-circles',
    arc: 'c3-arc',
    arcs: 'c3-arcs',
    area: 'c3-area',
    areas: 'c3-areas',
    empty: 'c3-empty',
    text: 'c3-text',
    texts: 'c3-texts',
    gaugeValue: 'c3-gauge-value',
    grid: 'c3-grid',
    gridLines: 'c3-grid-lines',
    xgrid: 'c3-xgrid',
    xgrids: 'c3-xgrids',
    xgridLine: 'c3-xgrid-line',
    xgridLines: 'c3-xgrid-lines',
    xgridFocus: 'c3-xgrid-focus',
    ygrid: 'c3-ygrid',
    ygrids: 'c3-ygrids',
    ygridLine: 'c3-ygrid-line',
    ygridLines: 'c3-ygrid-lines',
    axis: 'c3-axis',
    axisX: 'c3-axis-x',
    axisXLabel: 'c3-axis-x-label',
    axisY: 'c3-axis-y',
    axisYLabel: 'c3-axis-y-label',
    axisY2: 'c3-axis-y2',
    axisY2Label: 'c3-axis-y2-label',
    legendBackground: 'c3-legend-background',
    legendItem: 'c3-legend-item',
    legendItemEvent: 'c3-legend-item-event',
    legendItemTile: 'c3-legend-item-tile',
    legendItemHidden: 'c3-legend-item-hidden',
    legendItemFocused: 'c3-legend-item-focused',
    dragarea: 'c3-dragarea',
    EXPANDED: '_expanded_',
    SELECTED: '_selected_',
    INCLUDED: '_included_'
};

var _typeof = typeof Symbol === "function" && typeof Symbol.iterator === "symbol" ? function (obj) {
  return typeof obj;
} : function (obj) {
  return obj && typeof Symbol === "function" && obj.constructor === Symbol && obj !== Symbol.prototype ? "symbol" : typeof obj;
};











var classCallCheck = function (instance, Constructor) {
  if (!(instance instanceof Constructor)) {
    throw new TypeError("Cannot call a class as a function");
  }
};











var inherits = function (subClass, superClass) {
  if (typeof superClass !== "function" && superClass !== null) {
    throw new TypeError("Super expression must either be null or a function, not " + typeof superClass);
  }

  subClass.prototype = Object.create(superClass && superClass.prototype, {
    constructor: {
      value: subClass,
      enumerable: false,
      writable: true,
      configurable: true
    }
  });
  if (superClass) Object.setPrototypeOf ? Object.setPrototypeOf(subClass, superClass) : subClass.__proto__ = superClass;
};











var possibleConstructorReturn = function (self, call) {
  if (!self) {
    throw new ReferenceError("this hasn't been initialised - super() hasn't been called");
  }

  return call && (typeof call === "object" || typeof call === "function") ? call : self;
};

var isValue = function isValue(v) {
    return v || v === 0;
};
var isFunction = function isFunction(o) {
    return typeof o === 'function';
};
var isArray = function isArray(o) {
    return Array.isArray(o);
};
var isString = function isString(o) {
    return typeof o === 'string';
};
var isUndefined = function isUndefined(v) {
    return typeof v === 'undefined';
};
var isDefined = function isDefined(v) {
    return typeof v !== 'undefined';
};
var ceil10 = function ceil10(v) {
    return Math.ceil(v / 10) * 10;
};
var asHalfPixel = function asHalfPixel(n) {
    return Math.ceil(n) + 0.5;
};
var diffDomain = function diffDomain(d) {
    return d[1] - d[0];
};
var isEmpty = function isEmpty(o) {
    return typeof o === 'undefined' || o === null || isString(o) && o.length === 0 || (typeof o === 'undefined' ? 'undefined' : _typeof(o)) === 'object' && Object.keys(o).length === 0;
};
var notEmpty = function notEmpty(o) {
    return !c3_chart_internal_fn.isEmpty(o);
};
var getOption = function getOption(options, key, defaultValue) {
    return isDefined(options[key]) ? options[key] : defaultValue;
};
var hasValue = function hasValue(dict, value) {
    var found = false;
    Object.keys(dict).forEach(function (key) {
        if (dict[key] === value) {
            found = true;
        }
    });
    return found;
};
var sanitise = function sanitise(str) {
    return typeof str === 'string' ? str.replace(/</g, '&lt;').replace(/>/g, '&gt;') : str;
};
var getPathBox = function getPathBox(path) {
    var box = path.getBoundingClientRect(),
        items = [path.pathSegList.getItem(0), path.pathSegList.getItem(1)],
        minX = items[0].x,
        minY = Math.min(items[0].y, items[1].y);
    return { x: minX, y: minY, width: box.width, height: box.height };
};

var c3_axis_fn;
var c3_axis_internal_fn;

function AxisInternal(component, params) {
    var internal = this;
    internal.component = component;
    internal.params = params || {};

    internal.d3 = component.d3;
    internal.scale = internal.d3.scale.linear();
    internal.range;
    internal.orient = "bottom";
    internal.innerTickSize = 6;
    internal.outerTickSize = this.params.withOuterTick ? 6 : 0;
    internal.tickPadding = 3;
    internal.tickValues = null;
    internal.tickFormat;
    internal.tickArguments;

    internal.tickOffset = 0;
    internal.tickCulling = true;
    internal.tickCentered;
    internal.tickTextCharSize;
    internal.tickTextRotate = internal.params.tickTextRotate;
    internal.tickLength;

    internal.axis = internal.generateAxis();
}
c3_axis_internal_fn = AxisInternal.prototype;

c3_axis_internal_fn.axisX = function (selection, x, tickOffset) {
    selection.attr("transform", function (d) {
        return "translate(" + Math.ceil(x(d) + tickOffset) + ", 0)";
    });
};
c3_axis_internal_fn.axisY = function (selection, y) {
    selection.attr("transform", function (d) {
        return "translate(0," + Math.ceil(y(d)) + ")";
    });
};
c3_axis_internal_fn.scaleExtent = function (domain) {
    var start = domain[0],
        stop = domain[domain.length - 1];
    return start < stop ? [start, stop] : [stop, start];
};
c3_axis_internal_fn.generateTicks = function (scale) {
    var internal = this;
    var i,
        domain,
        ticks = [];
    if (scale.ticks) {
        return scale.ticks.apply(scale, internal.tickArguments);
    }
    domain = scale.domain();
    for (i = Math.ceil(domain[0]); i < domain[1]; i++) {
        ticks.push(i);
    }
    if (ticks.length > 0 && ticks[0] > 0) {
        ticks.unshift(ticks[0] - (ticks[1] - ticks[0]));
    }
    return ticks;
};
c3_axis_internal_fn.copyScale = function () {
    var internal = this;
    var newScale = internal.scale.copy(),
        domain;
    if (internal.params.isCategory) {
        domain = internal.scale.domain();
        newScale.domain([domain[0], domain[1] - 1]);
    }
    return newScale;
};
c3_axis_internal_fn.textFormatted = function (v) {
    var internal = this,
        formatted = internal.tickFormat ? internal.tickFormat(v) : v;
    return typeof formatted !== 'undefined' ? formatted : '';
};
c3_axis_internal_fn.updateRange = function () {
    var internal = this;
    internal.range = internal.scale.rangeExtent ? internal.scale.rangeExtent() : internal.scaleExtent(internal.scale.range());
    return internal.range;
};
c3_axis_internal_fn.updateTickTextCharSize = function (tick) {
    var internal = this;
    if (internal.tickTextCharSize) {
        return internal.tickTextCharSize;
    }
    var size = {
        h: 11.5,
        w: 5.5
    };
    tick.select('text').text(function (d) {
        return internal.textFormatted(d);
    }).each(function (d) {
        var box = this.getBoundingClientRect(),
            text = internal.textFormatted(d),
            h = box.height,
            w = text ? box.width / text.length : undefined;
        if (h && w) {
            size.h = h;
            size.w = w;
        }
    }).text('');
    internal.tickTextCharSize = size;
    return size;
};
c3_axis_internal_fn.transitionise = function (selection) {
    return this.params.withoutTransition ? selection : this.d3.transition(selection);
};
c3_axis_internal_fn.isVertical = function () {
    return this.orient === 'left' || this.orient === 'right';
};
c3_axis_internal_fn.tspanData = function (d, i, ticks, scale) {
    var internal = this;
    var splitted = internal.params.tickMultiline ? internal.splitTickText(d, ticks, scale) : [].concat(internal.textFormatted(d));
    return splitted.map(function (s) {
        return { index: i, splitted: s, length: splitted.length };
    });
};
c3_axis_internal_fn.splitTickText = function (d, ticks, scale) {
    var internal = this,
        tickText = internal.textFormatted(d),
        maxWidth = internal.params.tickWidth,
        subtext,
        spaceIndex,
        textWidth,
        splitted = [];

    if (Object.prototype.toString.call(tickText) === "[object Array]") {
        return tickText;
    }

    if (!maxWidth || maxWidth <= 0) {
        maxWidth = internal.isVertical() ? 95 : internal.params.isCategory ? Math.ceil(scale(ticks[1]) - scale(ticks[0])) - 12 : 110;
    }

    function split(splitted, text) {
        spaceIndex = undefined;
        for (var i = 1; i < text.length; i++) {
            if (text.charAt(i) === ' ') {
                spaceIndex = i;
            }
            subtext = text.substr(0, i + 1);
            textWidth = internal.tickTextCharSize.w * subtext.length;
            // if text width gets over tick width, split by space index or crrent index
            if (maxWidth < textWidth) {
                return split(splitted.concat(text.substr(0, spaceIndex ? spaceIndex : i)), text.slice(spaceIndex ? spaceIndex + 1 : i));
            }
        }
        return splitted.concat(text);
    }

    return split(splitted, tickText + "");
};
c3_axis_internal_fn.updateTickLength = function () {
    var internal = this;
    internal.tickLength = Math.max(internal.innerTickSize, 0) + internal.tickPadding;
};
c3_axis_internal_fn.lineY2 = function (d) {
    var internal = this,
        tickPosition = internal.scale(d) + (internal.tickCentered ? 0 : internal.tickOffset);
    return internal.range[0] < tickPosition && tickPosition < internal.range[1] ? internal.innerTickSize : 0;
};
c3_axis_internal_fn.textY = function () {
    var internal = this,
        rotate = internal.tickTextRotate;
    return rotate ? 11.5 - 2.5 * (rotate / 15) * (rotate > 0 ? 1 : -1) : internal.tickLength;
};
c3_axis_internal_fn.textTransform = function () {
    var internal = this,
        rotate = internal.tickTextRotate;
    return rotate ? "rotate(" + rotate + ")" : "";
};
c3_axis_internal_fn.textTextAnchor = function () {
    var internal = this,
        rotate = internal.tickTextRotate;
    return rotate ? rotate > 0 ? "start" : "end" : "middle";
};
c3_axis_internal_fn.tspanDx = function () {
    var internal = this,
        rotate = internal.tickTextRotate;
    return rotate ? 8 * Math.sin(Math.PI * (rotate / 180)) : 0;
};
c3_axis_internal_fn.tspanDy = function (d, i) {
    var internal = this,
        dy = internal.tickTextCharSize.h;
    if (i === 0) {
        if (internal.isVertical()) {
            dy = -((d.length - 1) * (internal.tickTextCharSize.h / 2) - 3);
        } else {
            dy = ".71em";
        }
    }
    return dy;
};

c3_axis_internal_fn.generateAxis = function () {
    var internal = this,
        d3 = internal.d3,
        params = internal.params;
    function axis(g) {
        g.each(function () {
            var g = axis.g = d3.select(this);

            var scale0 = this.__chart__ || internal.scale,
                scale1 = this.__chart__ = internal.copyScale();

            var ticks = internal.tickValues ? internal.tickValues : internal.generateTicks(scale1),
                tick = g.selectAll(".tick").data(ticks, scale1),
                tickEnter = tick.enter().insert("g", ".domain").attr("class", "tick").style("opacity", 1e-6),

            // MEMO: No exit transition. The reason is this transition affects max tick width calculation because old tick will be included in the ticks.
            tickExit = tick.exit().remove(),
                tickUpdate = internal.transitionise(tick).style("opacity", 1),
                tickTransform,
                tickX,
                tickY;

            if (params.isCategory) {
                internal.tickOffset = Math.ceil((scale1(1) - scale1(0)) / 2);
                tickX = internal.tickCentered ? 0 : internal.tickOffset;
                tickY = internal.tickCentered ? internal.tickOffset : 0;
            } else {
                internal.tickOffset = tickX = 0;
            }

            tickEnter.append("line");
            tickEnter.append("text");

            internal.updateRange();
            internal.updateTickLength();
            internal.updateTickTextCharSize(g.select('.tick'));

            var lineUpdate = tickUpdate.select("line"),
                textUpdate = tickUpdate.select("text"),
                tspanUpdate = tick.select("text").selectAll('tspan').data(function (d, i) {
                return internal.tspanData(d, i, ticks, scale1);
            });

            tspanUpdate.enter().append('tspan');
            tspanUpdate.exit().remove();
            tspanUpdate.text(function (d) {
                return d.splitted;
            });

            var path = g.selectAll(".domain").data([0]),
                pathUpdate = (path.enter().append("path").attr("class", "domain"), internal.transitionise(path));

            // TODO: each attr should be one function and change its behavior by internal.orient, probably
            switch (internal.orient) {
                case "bottom":
                    {
                        tickTransform = internal.axisX;
                        lineUpdate.attr("x1", tickX).attr("x2", tickX).attr("y2", function (d, i) {
                            return internal.lineY2(d, i);
                        });
                        textUpdate.attr("x", 0).attr("y", function (d, i) {
                            return internal.textY(d, i);
                        }).attr("transform", function (d, i) {
                            return internal.textTransform(d, i);
                        }).style("text-anchor", function (d, i) {
                            return internal.textTextAnchor(d, i);
                        });
                        tspanUpdate.attr('x', 0).attr("dy", function (d, i) {
                            return internal.tspanDy(d, i);
                        }).attr('dx', function (d, i) {
                            return internal.tspanDx(d, i);
                        });
                        pathUpdate.attr("d", "M" + internal.range[0] + "," + internal.outerTickSize + "V0H" + internal.range[1] + "V" + internal.outerTickSize);
                        break;
                    }
                case "top":
                    {
                        // TODO: rotated tick text
                        tickTransform = internal.axisX;
                        lineUpdate.attr("x2", 0).attr("y2", -internal.innerTickSize);
                        textUpdate.attr("x", 0).attr("y", -internal.tickLength).style("text-anchor", "middle");
                        tspanUpdate.attr('x', 0).attr("dy", "0em");
                        pathUpdate.attr("d", "M" + internal.range[0] + "," + -internal.outerTickSize + "V0H" + internal.range[1] + "V" + -internal.outerTickSize);
                        break;
                    }
                case "left":
                    {
                        tickTransform = internal.axisY;
                        lineUpdate.attr("x2", -internal.innerTickSize).attr("y1", tickY).attr("y2", tickY);
                        textUpdate.attr("x", -internal.tickLength).attr("y", internal.tickOffset).style("text-anchor", "end");
                        tspanUpdate.attr('x', -internal.tickLength).attr("dy", function (d, i) {
                            return internal.tspanDy(d, i);
                        });
                        pathUpdate.attr("d", "M" + -internal.outerTickSize + "," + internal.range[0] + "H0V" + internal.range[1] + "H" + -internal.outerTickSize);
                        break;
                    }
                case "right":
                    {
                        tickTransform = internal.axisY;
                        lineUpdate.attr("x2", internal.innerTickSize).attr("y2", 0);
                        textUpdate.attr("x", internal.tickLength).attr("y", 0).style("text-anchor", "start");
                        tspanUpdate.attr('x', internal.tickLength).attr("dy", function (d, i) {
                            return internal.tspanDy(d, i);
                        });
                        pathUpdate.attr("d", "M" + internal.outerTickSize + "," + internal.range[0] + "H0V" + internal.range[1] + "H" + internal.outerTickSize);
                        break;
                    }
            }
            if (scale1.rangeBand) {
                var x = scale1,
                    dx = x.rangeBand() / 2;
                scale0 = scale1 = function scale1(d) {
                    return x(d) + dx;
                };
            } else if (scale0.rangeBand) {
                scale0 = scale1;
            } else {
                tickExit.call(tickTransform, scale1, internal.tickOffset);
            }
            tickEnter.call(tickTransform, scale0, internal.tickOffset);
            tickUpdate.call(tickTransform, scale1, internal.tickOffset);
        });
    }
    axis.scale = function (x) {
        if (!arguments.length) {
            return internal.scale;
        }
        internal.scale = x;
        return axis;
    };
    axis.orient = function (x) {
        if (!arguments.length) {
            return internal.orient;
        }
        internal.orient = x in { top: 1, right: 1, bottom: 1, left: 1 } ? x + "" : "bottom";
        return axis;
    };
    axis.tickFormat = function (format) {
        if (!arguments.length) {
            return internal.tickFormat;
        }
        internal.tickFormat = format;
        return axis;
    };
    axis.tickCentered = function (isCentered) {
        if (!arguments.length) {
            return internal.tickCentered;
        }
        internal.tickCentered = isCentered;
        return axis;
    };
    axis.tickOffset = function () {
        return internal.tickOffset;
    };
    axis.tickInterval = function () {
        var interval, length;
        if (params.isCategory) {
            interval = internal.tickOffset * 2;
        } else {
            length = axis.g.select('path.domain').node().getTotalLength() - internal.outerTickSize * 2;
            interval = length / axis.g.selectAll('line').size();
        }
        return interval === Infinity ? 0 : interval;
    };
    axis.ticks = function () {
        if (!arguments.length) {
            return internal.tickArguments;
        }
        internal.tickArguments = arguments;
        return axis;
    };
    axis.tickCulling = function (culling) {
        if (!arguments.length) {
            return internal.tickCulling;
        }
        internal.tickCulling = culling;
        return axis;
    };
    axis.tickValues = function (x) {
        if (typeof x === 'function') {
            internal.tickValues = function () {
                return x(internal.scale.domain());
            };
        } else {
            if (!arguments.length) {
                return internal.tickValues;
            }
            internal.tickValues = x;
        }
        return axis;
    };
    return axis;
};

var Axis = function (_Component) {
    inherits(Axis, _Component);

    function Axis(owner) {
        classCallCheck(this, Axis);

        var fn = {
            fn: c3_axis_fn,
            internal: {
                fn: c3_axis_internal_fn
            }
        };

        var _this = possibleConstructorReturn(this, (Axis.__proto__ || Object.getPrototypeOf(Axis)).call(this, owner, 'axis', fn));

        _this.d3 = owner.d3;
        _this.internal = AxisInternal;
        return _this;
    }

    return Axis;
}(Component);

c3_axis_fn = Axis.prototype;

c3_axis_fn.init = function init() {
    var $$ = this.owner,
        config = $$.config,
        main = $$.main;
    $$.axes.x = main.append("g").attr("class", CLASS.axis + ' ' + CLASS.axisX).attr("clip-path", $$.clipPathForXAxis).attr("transform", $$.getTranslate('x')).style("visibility", config.axis_x_show ? 'visible' : 'hidden');
    $$.axes.x.append("text").attr("class", CLASS.axisXLabel).attr("transform", config.axis_rotated ? "rotate(-90)" : "").style("text-anchor", this.textAnchorForXAxisLabel.bind(this));
    $$.axes.y = main.append("g").attr("class", CLASS.axis + ' ' + CLASS.axisY).attr("clip-path", config.axis_y_inner ? "" : $$.clipPathForYAxis).attr("transform", $$.getTranslate('y')).style("visibility", config.axis_y_show ? 'visible' : 'hidden');
    $$.axes.y.append("text").attr("class", CLASS.axisYLabel).attr("transform", config.axis_rotated ? "" : "rotate(-90)").style("text-anchor", this.textAnchorForYAxisLabel.bind(this));

    $$.axes.y2 = main.append("g").attr("class", CLASS.axis + ' ' + CLASS.axisY2
    // clip-path?
    ).attr("transform", $$.getTranslate('y2')).style("visibility", config.axis_y2_show ? 'visible' : 'hidden');
    $$.axes.y2.append("text").attr("class", CLASS.axisY2Label).attr("transform", config.axis_rotated ? "" : "rotate(-90)").style("text-anchor", this.textAnchorForY2AxisLabel.bind(this));
};
c3_axis_fn.getXAxis = function getXAxis(scale, orient, tickFormat, tickValues, withOuterTick, withoutTransition, withoutRotateTickText) {
    var $$ = this.owner,
        config = $$.config,
        axisParams = {
        isCategory: $$.isCategorized(),
        withOuterTick: withOuterTick,
        tickMultiline: config.axis_x_tick_multiline,
        tickWidth: config.axis_x_tick_width,
        tickTextRotate: withoutRotateTickText ? 0 : config.axis_x_tick_rotate,
        withoutTransition: withoutTransition
    },
        axis = new this.internal(this, axisParams).axis.scale(scale).orient(orient);

    if ($$.isTimeSeries() && tickValues && typeof tickValues !== "function") {
        tickValues = tickValues.map(function (v) {
            return $$.parseDate(v);
        });
    }

    // Set tick
    axis.tickFormat(tickFormat).tickValues(tickValues);
    if ($$.isCategorized()) {
        axis.tickCentered(config.axis_x_tick_centered);
        if (isEmpty(config.axis_x_tick_culling)) {
            config.axis_x_tick_culling = false;
        }
    }

    return axis;
};
c3_axis_fn.updateXAxisTickValues = function updateXAxisTickValues(targets, axis) {
    var $$ = this.owner,
        config = $$.config,
        tickValues;
    if (config.axis_x_tick_fit || config.axis_x_tick_count) {
        tickValues = this.generateTickValues($$.mapTargetsToUniqueXs(targets), config.axis_x_tick_count, $$.isTimeSeries());
    }
    if (axis) {
        axis.tickValues(tickValues);
    } else {
        $$.xAxis.tickValues(tickValues);
        $$.subXAxis.tickValues(tickValues);
    }
    return tickValues;
};
c3_axis_fn.getYAxis = function getYAxis(scale, orient, tickFormat, tickValues, withOuterTick, withoutTransition, withoutRotateTickText) {
    var $$ = this.owner,
        config = $$.config,
        axisParams = {
        withOuterTick: withOuterTick,
        withoutTransition: withoutTransition,
        tickTextRotate: withoutRotateTickText ? 0 : config.axis_y_tick_rotate
    },
        axis = new this.internal(this, axisParams).axis.scale(scale).orient(orient).tickFormat(tickFormat);
    if ($$.isTimeSeriesY()) {
        axis.ticks($$.d3.time[config.axis_y_tick_time_value], config.axis_y_tick_time_interval);
    } else {
        axis.tickValues(tickValues);
    }
    return axis;
};
c3_axis_fn.getId = function getId(id) {
    var config = this.owner.config;
    return id in config.data_axes ? config.data_axes[id] : 'y';
};
c3_axis_fn.getXAxisTickFormat = function getXAxisTickFormat() {
    var $$ = this.owner,
        config = $$.config,
        format = $$.isTimeSeries() ? $$.defaultAxisTimeFormat : $$.isCategorized() ? $$.categoryName : function (v) {
        return v < 0 ? v.toFixed(0) : v;
    };
    if (config.axis_x_tick_format) {
        if (isFunction(config.axis_x_tick_format)) {
            format = config.axis_x_tick_format;
        } else if ($$.isTimeSeries()) {
            format = function format(date) {
                return date ? $$.axisTimeFormat(config.axis_x_tick_format)(date) : "";
            };
        }
    }
    return isFunction(format) ? function (v) {
        return format.call($$, v);
    } : format;
};
c3_axis_fn.getTickValues = function getTickValues(tickValues, axis) {
    return tickValues ? tickValues : axis ? axis.tickValues() : undefined;
};
c3_axis_fn.getXAxisTickValues = function getXAxisTickValues() {
    return this.getTickValues(this.owner.config.axis_x_tick_values, this.owner.xAxis);
};
c3_axis_fn.getYAxisTickValues = function getYAxisTickValues() {
    return this.getTickValues(this.owner.config.axis_y_tick_values, this.owner.yAxis);
};
c3_axis_fn.getY2AxisTickValues = function getY2AxisTickValues() {
    return this.getTickValues(this.owner.config.axis_y2_tick_values, this.owner.y2Axis);
};
c3_axis_fn.getLabelOptionByAxisId = function getLabelOptionByAxisId(axisId) {
    var $$ = this.owner,
        config = $$.config,
        option;
    if (axisId === 'y') {
        option = config.axis_y_label;
    } else if (axisId === 'y2') {
        option = config.axis_y2_label;
    } else if (axisId === 'x') {
        option = config.axis_x_label;
    }
    return option;
};
c3_axis_fn.getLabelText = function getLabelText(axisId) {
    var option = this.getLabelOptionByAxisId(axisId);
    return isString(option) ? option : option ? option.text : null;
};
c3_axis_fn.setLabelText = function setLabelText(axisId, text) {
    var $$ = this.owner,
        config = $$.config,
        option = this.getLabelOptionByAxisId(axisId);
    if (isString(option)) {
        if (axisId === 'y') {
            config.axis_y_label = text;
        } else if (axisId === 'y2') {
            config.axis_y2_label = text;
        } else if (axisId === 'x') {
            config.axis_x_label = text;
        }
    } else if (option) {
        option.text = text;
    }
};
c3_axis_fn.getLabelPosition = function getLabelPosition(axisId, defaultPosition) {
    var option = this.getLabelOptionByAxisId(axisId),
        position = option && (typeof option === 'undefined' ? 'undefined' : _typeof(option)) === 'object' && option.position ? option.position : defaultPosition;
    return {
        isInner: position.indexOf('inner') >= 0,
        isOuter: position.indexOf('outer') >= 0,
        isLeft: position.indexOf('left') >= 0,
        isCenter: position.indexOf('center') >= 0,
        isRight: position.indexOf('right') >= 0,
        isTop: position.indexOf('top') >= 0,
        isMiddle: position.indexOf('middle') >= 0,
        isBottom: position.indexOf('bottom') >= 0
    };
};
c3_axis_fn.getXAxisLabelPosition = function getXAxisLabelPosition() {
    return this.getLabelPosition('x', this.owner.config.axis_rotated ? 'inner-top' : 'inner-right');
};
c3_axis_fn.getYAxisLabelPosition = function getYAxisLabelPosition() {
    return this.getLabelPosition('y', this.owner.config.axis_rotated ? 'inner-right' : 'inner-top');
};
c3_axis_fn.getY2AxisLabelPosition = function getY2AxisLabelPosition() {
    return this.getLabelPosition('y2', this.owner.config.axis_rotated ? 'inner-right' : 'inner-top');
};
c3_axis_fn.getLabelPositionById = function getLabelPositionById(id) {
    return id === 'y2' ? this.getY2AxisLabelPosition() : id === 'y' ? this.getYAxisLabelPosition() : this.getXAxisLabelPosition();
};
c3_axis_fn.textForXAxisLabel = function textForXAxisLabel() {
    return this.getLabelText('x');
};
c3_axis_fn.textForYAxisLabel = function textForYAxisLabel() {
    return this.getLabelText('y');
};
c3_axis_fn.textForY2AxisLabel = function textForY2AxisLabel() {
    return this.getLabelText('y2');
};
c3_axis_fn.xForAxisLabel = function xForAxisLabel(forHorizontal, position) {
    var $$ = this.owner;
    if (forHorizontal) {
        return position.isLeft ? 0 : position.isCenter ? $$.width / 2 : $$.width;
    } else {
        return position.isBottom ? -$$.height : position.isMiddle ? -$$.height / 2 : 0;
    }
};
c3_axis_fn.dxForAxisLabel = function dxForAxisLabel(forHorizontal, position) {
    if (forHorizontal) {
        return position.isLeft ? "0.5em" : position.isRight ? "-0.5em" : "0";
    } else {
        return position.isTop ? "-0.5em" : position.isBottom ? "0.5em" : "0";
    }
};
c3_axis_fn.textAnchorForAxisLabel = function textAnchorForAxisLabel(forHorizontal, position) {
    if (forHorizontal) {
        return position.isLeft ? 'start' : position.isCenter ? 'middle' : 'end';
    } else {
        return position.isBottom ? 'start' : position.isMiddle ? 'middle' : 'end';
    }
};
c3_axis_fn.xForXAxisLabel = function xForXAxisLabel() {
    return this.xForAxisLabel(!this.owner.config.axis_rotated, this.getXAxisLabelPosition());
};
c3_axis_fn.xForYAxisLabel = function xForYAxisLabel() {
    return this.xForAxisLabel(this.owner.config.axis_rotated, this.getYAxisLabelPosition());
};
c3_axis_fn.xForY2AxisLabel = function xForY2AxisLabel() {
    return this.xForAxisLabel(this.owner.config.axis_rotated, this.getY2AxisLabelPosition());
};
c3_axis_fn.dxForXAxisLabel = function dxForXAxisLabel() {
    return this.dxForAxisLabel(!this.owner.config.axis_rotated, this.getXAxisLabelPosition());
};
c3_axis_fn.dxForYAxisLabel = function dxForYAxisLabel() {
    return this.dxForAxisLabel(this.owner.config.axis_rotated, this.getYAxisLabelPosition());
};
c3_axis_fn.dxForY2AxisLabel = function dxForY2AxisLabel() {
    return this.dxForAxisLabel(this.owner.config.axis_rotated, this.getY2AxisLabelPosition());
};
c3_axis_fn.dyForXAxisLabel = function dyForXAxisLabel() {
    var $$ = this.owner,
        config = $$.config,
        position = this.getXAxisLabelPosition();
    if (config.axis_rotated) {
        return position.isInner ? "1.2em" : -25 - this.getMaxTickWidth('x');
    } else {
        return position.isInner ? "-0.5em" : config.axis_x_height ? config.axis_x_height - 10 : "3em";
    }
};
c3_axis_fn.dyForYAxisLabel = function dyForYAxisLabel() {
    var $$ = this.owner,
        position = this.getYAxisLabelPosition();
    if ($$.config.axis_rotated) {
        return position.isInner ? "-0.5em" : "3em";
    } else {
        return position.isInner ? "1.2em" : -10 - ($$.config.axis_y_inner ? 0 : this.getMaxTickWidth('y') + 10);
    }
};
c3_axis_fn.dyForY2AxisLabel = function dyForY2AxisLabel() {
    var $$ = this.owner,
        position = this.getY2AxisLabelPosition();
    if ($$.config.axis_rotated) {
        return position.isInner ? "1.2em" : "-2.2em";
    } else {
        return position.isInner ? "-0.5em" : 15 + ($$.config.axis_y2_inner ? 0 : this.getMaxTickWidth('y2') + 15);
    }
};
c3_axis_fn.textAnchorForXAxisLabel = function textAnchorForXAxisLabel() {
    var $$ = this.owner;
    return this.textAnchorForAxisLabel(!$$.config.axis_rotated, this.getXAxisLabelPosition());
};
c3_axis_fn.textAnchorForYAxisLabel = function textAnchorForYAxisLabel() {
    var $$ = this.owner;
    return this.textAnchorForAxisLabel($$.config.axis_rotated, this.getYAxisLabelPosition());
};
c3_axis_fn.textAnchorForY2AxisLabel = function textAnchorForY2AxisLabel() {
    var $$ = this.owner;
    return this.textAnchorForAxisLabel($$.config.axis_rotated, this.getY2AxisLabelPosition());
};
c3_axis_fn.getMaxTickWidth = function getMaxTickWidth(id, withoutRecompute) {
    var $$ = this.owner,
        config = $$.config,
        maxWidth = 0,
        targetsToShow,
        scale,
        axis,
        dummy,
        svg;
    if (withoutRecompute && $$.currentMaxTickWidths[id]) {
        return $$.currentMaxTickWidths[id];
    }
    if ($$.svg) {
        targetsToShow = $$.filterTargetsToShow($$.data.targets);
        if (id === 'y') {
            scale = $$.y.copy().domain($$.getYDomain(targetsToShow, 'y'));
            axis = this.getYAxis(scale, $$.yOrient, config.axis_y_tick_format, $$.yAxisTickValues, false, true, true);
        } else if (id === 'y2') {
            scale = $$.y2.copy().domain($$.getYDomain(targetsToShow, 'y2'));
            axis = this.getYAxis(scale, $$.y2Orient, config.axis_y2_tick_format, $$.y2AxisTickValues, false, true, true);
        } else {
            scale = $$.x.copy().domain($$.getXDomain(targetsToShow));
            axis = this.getXAxis(scale, $$.xOrient, $$.xAxisTickFormat, $$.xAxisTickValues, false, true, true);
            this.updateXAxisTickValues(targetsToShow, axis);
        }
        dummy = $$.d3.select('body').append('div').classed('c3', true);
        svg = dummy.append("svg").style('visibility', 'hidden').style('position', 'fixed').style('top', 0).style('left', 0), svg.append('g').call(axis).each(function () {
            $$.d3.select(this).selectAll('text').each(function () {
                var box = this.getBoundingClientRect();
                if (maxWidth < box.width) {
                    maxWidth = box.width;
                }
            });
            dummy.remove();
        });
    }
    $$.currentMaxTickWidths[id] = maxWidth <= 0 ? $$.currentMaxTickWidths[id] : maxWidth;
    return $$.currentMaxTickWidths[id];
};

c3_axis_fn.updateLabels = function updateLabels(withTransition) {
    var $$ = this.owner;
    var axisXLabel = $$.main.select('.' + CLASS.axisX + ' .' + CLASS.axisXLabel),
        axisYLabel = $$.main.select('.' + CLASS.axisY + ' .' + CLASS.axisYLabel),
        axisY2Label = $$.main.select('.' + CLASS.axisY2 + ' .' + CLASS.axisY2Label);
    (withTransition ? axisXLabel.transition() : axisXLabel).attr("x", this.xForXAxisLabel.bind(this)).attr("dx", this.dxForXAxisLabel.bind(this)).attr("dy", this.dyForXAxisLabel.bind(this)).text(this.textForXAxisLabel.bind(this));
    (withTransition ? axisYLabel.transition() : axisYLabel).attr("x", this.xForYAxisLabel.bind(this)).attr("dx", this.dxForYAxisLabel.bind(this)).attr("dy", this.dyForYAxisLabel.bind(this)).text(this.textForYAxisLabel.bind(this));
    (withTransition ? axisY2Label.transition() : axisY2Label).attr("x", this.xForY2AxisLabel.bind(this)).attr("dx", this.dxForY2AxisLabel.bind(this)).attr("dy", this.dyForY2AxisLabel.bind(this)).text(this.textForY2AxisLabel.bind(this));
};
c3_axis_fn.getPadding = function getPadding(padding, key, defaultValue, domainLength) {
    var p = typeof padding === 'number' ? padding : padding[key];
    if (!isValue(p)) {
        return defaultValue;
    }
    if (padding.unit === 'ratio') {
        return padding[key] * domainLength;
    }
    // assume padding is pixels if unit is not specified
    return this.convertPixelsToAxisPadding(p, domainLength);
};
c3_axis_fn.convertPixelsToAxisPadding = function convertPixelsToAxisPadding(pixels, domainLength) {
    var $$ = this.owner,
        length = $$.config.axis_rotated ? $$.width : $$.height;
    return domainLength * (pixels / length);
};
c3_axis_fn.generateTickValues = function generateTickValues(values, tickCount, forTimeSeries) {
    var tickValues = values,
        targetCount,
        start,
        end,
        count,
        interval,
        i,
        tickValue;
    if (tickCount) {
        targetCount = isFunction(tickCount) ? tickCount() : tickCount;
        // compute ticks according to tickCount
        if (targetCount === 1) {
            tickValues = [values[0]];
        } else if (targetCount === 2) {
            tickValues = [values[0], values[values.length - 1]];
        } else if (targetCount > 2) {
            count = targetCount - 2;
            start = values[0];
            end = values[values.length - 1];
            interval = (end - start) / (count + 1);
            // re-construct unique values
            tickValues = [start];
            for (i = 0; i < count; i++) {
                tickValue = +start + interval * (i + 1);
                tickValues.push(forTimeSeries ? new Date(tickValue) : tickValue);
            }
            tickValues.push(end);
        }
    }
    if (!forTimeSeries) {
        tickValues = tickValues.sort(function (a, b) {
            return a - b;
        });
    }
    return tickValues;
};
c3_axis_fn.generateTransitions = function generateTransitions(duration) {
    var $$ = this.owner,
        axes = $$.axes;
    return {
        axisX: duration ? axes.x.transition().duration(duration) : axes.x,
        axisY: duration ? axes.y.transition().duration(duration) : axes.y,
        axisY2: duration ? axes.y2.transition().duration(duration) : axes.y2,
        axisSubX: duration ? axes.subx.transition().duration(duration) : axes.subx
    };
};
c3_axis_fn.redraw = function redraw(transitions, isHidden) {
    var $$ = this.owner;
    $$.axes.x.style("opacity", isHidden ? 0 : 1);
    $$.axes.y.style("opacity", isHidden ? 0 : 1);
    $$.axes.y2.style("opacity", isHidden ? 0 : 1);
    $$.axes.subx.style("opacity", isHidden ? 0 : 1);
    transitions.axisX.call($$.xAxis);
    transitions.axisY.call($$.yAxis);
    transitions.axisY2.call($$.y2Axis);
    transitions.axisSubX.call($$.subXAxis);
};

var c3$1 = { version: "0.4.18" };

var c3_chart_fn;
var c3_chart_internal_fn;

function Component(owner, componentKey, fn) {
    this.owner = owner;
    c3$1.chart.internal[componentKey] = fn;
}

function Chart(config) {
    var $$ = this.internal = new ChartInternal(this);
    $$.loadConfig(config);

    $$.beforeInit(config);
    $$.init();
    $$.afterInit(config);

    // bind "this" to nested API
    (function bindThis(fn, target, argThis) {
        Object.keys(fn).forEach(function (key) {
            target[key] = fn[key].bind(argThis);
            if (Object.keys(fn[key]).length > 0) {
                bindThis(fn[key], target[key], argThis);
            }
        });
    })(c3_chart_fn, this, this);
}

function ChartInternal(api) {
    var $$ = this;
    $$.d3 = window.d3 ? window.d3 : typeof require !== 'undefined' ? require("d3") : undefined;
    $$.api = api;
    $$.config = $$.getDefaultConfig();
    $$.data = {};
    $$.cache = {};
    $$.axes = {};
}

c3$1.generate = function (config) {
    return new Chart(config);
};

c3$1.chart = {
    fn: Chart.prototype,
    internal: {
        fn: ChartInternal.prototype
    }
};
c3_chart_fn = c3$1.chart.fn;
c3_chart_internal_fn = c3$1.chart.internal.fn;

c3_chart_internal_fn.beforeInit = function () {
    // can do something
};
c3_chart_internal_fn.afterInit = function () {
    // can do something
};
c3_chart_internal_fn.init = function () {
    var $$ = this,
        config = $$.config;

    $$.initParams();

    if (config.data_url) {
        $$.convertUrlToData(config.data_url, config.data_mimeType, config.data_headers, config.data_keys, $$.initWithData);
    } else if (config.data_json) {
        $$.initWithData($$.convertJsonToData(config.data_json, config.data_keys));
    } else if (config.data_rows) {
        $$.initWithData($$.convertRowsToData(config.data_rows));
    } else if (config.data_columns) {
        $$.initWithData($$.convertColumnsToData(config.data_columns));
    } else {
        throw Error('url or json or rows or columns is required.');
    }
};

c3_chart_internal_fn.initParams = function () {
    var $$ = this,
        d3 = $$.d3,
        config = $$.config;

    // MEMO: clipId needs to be unique because it conflicts when multiple charts exist
    $$.clipId = "c3-" + +new Date() + '-clip', $$.clipIdForXAxis = $$.clipId + '-xaxis', $$.clipIdForYAxis = $$.clipId + '-yaxis', $$.clipIdForGrid = $$.clipId + '-grid', $$.clipIdForSubchart = $$.clipId + '-subchart', $$.clipPath = $$.getClipPath($$.clipId), $$.clipPathForXAxis = $$.getClipPath($$.clipIdForXAxis), $$.clipPathForYAxis = $$.getClipPath($$.clipIdForYAxis);
    $$.clipPathForGrid = $$.getClipPath($$.clipIdForGrid), $$.clipPathForSubchart = $$.getClipPath($$.clipIdForSubchart), $$.dragStart = null;
    $$.dragging = false;
    $$.flowing = false;
    $$.cancelClick = false;
    $$.mouseover = false;
    $$.transiting = false;

    $$.color = $$.generateColor();
    $$.levelColor = $$.generateLevelColor();

    $$.dataTimeFormat = config.data_xLocaltime ? d3.time.format : d3.time.format.utc;
    $$.axisTimeFormat = config.axis_x_localtime ? d3.time.format : d3.time.format.utc;
    $$.defaultAxisTimeFormat = $$.axisTimeFormat.multi([[".%L", function (d) {
        return d.getMilliseconds();
    }], [":%S", function (d) {
        return d.getSeconds();
    }], ["%I:%M", function (d) {
        return d.getMinutes();
    }], ["%I %p", function (d) {
        return d.getHours();
    }], ["%-m/%-d", function (d) {
        return d.getDay() && d.getDate() !== 1;
    }], ["%-m/%-d", function (d) {
        return d.getDate() !== 1;
    }], ["%-m/%-d", function (d) {
        return d.getMonth();
    }], ["%Y/%-m/%-d", function () {
        return true;
    }]]);

    $$.hiddenTargetIds = [];
    $$.hiddenLegendIds = [];
    $$.focusedTargetIds = [];
    $$.defocusedTargetIds = [];

    $$.xOrient = config.axis_rotated ? "left" : "bottom";
    $$.yOrient = config.axis_rotated ? config.axis_y_inner ? "top" : "bottom" : config.axis_y_inner ? "right" : "left";
    $$.y2Orient = config.axis_rotated ? config.axis_y2_inner ? "bottom" : "top" : config.axis_y2_inner ? "left" : "right";
    $$.subXOrient = config.axis_rotated ? "left" : "bottom";

    $$.isLegendRight = config.legend_position === 'right';
    $$.isLegendInset = config.legend_position === 'inset';
    $$.isLegendTop = config.legend_inset_anchor === 'top-left' || config.legend_inset_anchor === 'top-right';
    $$.isLegendLeft = config.legend_inset_anchor === 'top-left' || config.legend_inset_anchor === 'bottom-left';
    $$.legendStep = 0;
    $$.legendItemWidth = 0;
    $$.legendItemHeight = 0;

    $$.currentMaxTickWidths = {
        x: 0,
        y: 0,
        y2: 0
    };

    $$.rotated_padding_left = 30;
    $$.rotated_padding_right = config.axis_rotated && !config.axis_x_show ? 0 : 30;
    $$.rotated_padding_top = 5;

    $$.withoutFadeIn = {};

    $$.intervalForObserveInserted = undefined;

    $$.axes.subx = d3.selectAll([]); // needs when excluding subchart.js
};

c3_chart_internal_fn.initChartElements = function () {
    if (this.initBar) {
        this.initBar();
    }
    if (this.initLine) {
        this.initLine();
    }
    if (this.initArc) {
        this.initArc();
    }
    if (this.initGauge) {
        this.initGauge();
    }
    if (this.initText) {
        this.initText();
    }
};

c3_chart_internal_fn.initWithData = function (data) {
    var $$ = this,
        d3 = $$.d3,
        config = $$.config;
    var defs,
        main,
        binding = true;

    $$.axis = new Axis($$);

    if ($$.initPie) {
        $$.initPie();
    }
    if ($$.initBrush) {
        $$.initBrush();
    }
    if ($$.initZoom) {
        $$.initZoom();
    }

    if (!config.bindto) {
        $$.selectChart = d3.selectAll([]);
    } else if (typeof config.bindto.node === 'function') {
        $$.selectChart = config.bindto;
    } else {
        $$.selectChart = d3.select(config.bindto);
    }
    if ($$.selectChart.empty()) {
        $$.selectChart = d3.select(document.createElement('div')).style('opacity', 0);
        $$.observeInserted($$.selectChart);
        binding = false;
    }
    $$.selectChart.html("").classed("c3", true);

    // Init data as targets
    $$.data.xs = {};
    $$.data.targets = $$.convertDataToTargets(data);

    if (config.data_filter) {
        $$.data.targets = $$.data.targets.filter(config.data_filter);
    }

    // Set targets to hide if needed
    if (config.data_hide) {
        $$.addHiddenTargetIds(config.data_hide === true ? $$.mapToIds($$.data.targets) : config.data_hide);
    }
    if (config.legend_hide) {
        $$.addHiddenLegendIds(config.legend_hide === true ? $$.mapToIds($$.data.targets) : config.legend_hide);
    }

    // when gauge, hide legend // TODO: fix
    if ($$.hasType('gauge')) {
        config.legend_show = false;
    }

    // Init sizes and scales
    $$.updateSizes();
    $$.updateScales();

    // Set domains for each scale
    $$.x.domain(d3.extent($$.getXDomain($$.data.targets)));
    $$.y.domain($$.getYDomain($$.data.targets, 'y'));
    $$.y2.domain($$.getYDomain($$.data.targets, 'y2'));
    $$.subX.domain($$.x.domain());
    $$.subY.domain($$.y.domain());
    $$.subY2.domain($$.y2.domain());

    // Save original x domain for zoom update
    $$.orgXDomain = $$.x.domain();

    // Set initialized scales to brush and zoom
    if ($$.brush) {
        $$.brush.scale($$.subX);
    }
    if (config.zoom_enabled) {
        $$.zoom.scale($$.x);
    }

    /*-- Basic Elements --*/

    // Define svgs
    $$.svg = $$.selectChart.append("svg").style("overflow", "hidden").on('mouseenter', function () {
        return config.onmouseover.call($$);
    }).on('mouseleave', function () {
        return config.onmouseout.call($$);
    });

    if ($$.config.svg_classname) {
        $$.svg.attr('class', $$.config.svg_classname);
    }

    // Define defs
    defs = $$.svg.append("defs");
    $$.clipChart = $$.appendClip(defs, $$.clipId);
    $$.clipXAxis = $$.appendClip(defs, $$.clipIdForXAxis);
    $$.clipYAxis = $$.appendClip(defs, $$.clipIdForYAxis);
    $$.clipGrid = $$.appendClip(defs, $$.clipIdForGrid);
    $$.clipSubchart = $$.appendClip(defs, $$.clipIdForSubchart);
    $$.updateSvgSize();

    // Define regions
    main = $$.main = $$.svg.append("g").attr("transform", $$.getTranslate('main'));

    if ($$.initSubchart) {
        $$.initSubchart();
    }
    if ($$.initTooltip) {
        $$.initTooltip();
    }
    if ($$.initLegend) {
        $$.initLegend();
    }
    if ($$.initTitle) {
        $$.initTitle();
    }

    /*-- Main Region --*/

    // text when empty
    main.append("text").attr("class", CLASS.text + ' ' + CLASS.empty).attr("text-anchor", "middle" // horizontal centering of text at x position in all browsers.
    ).attr("dominant-baseline", "middle"); // vertical centering of text at y position in all browsers, except IE.

    // Regions
    $$.initRegion();

    // Grids
    $$.initGrid();

    // Define g for chart area
    main.append('g').attr("clip-path", $$.clipPath).attr('class', CLASS.chart);

    // Grid lines
    if (config.grid_lines_front) {
        $$.initGridLines();
    }

    // Cover whole with rects for events
    $$.initEventRect();

    // Define g for chart
    $$.initChartElements();

    // if zoom privileged, insert rect to forefront
    // TODO: is this needed?
    main.insert('rect', config.zoom_privileged ? null : 'g.' + CLASS.regions).attr('class', CLASS.zoomRect).attr('width', $$.width).attr('height', $$.height).style('opacity', 0).on("dblclick.zoom", null);

    // Set default extent if defined
    if (config.axis_x_extent) {
        $$.brush.extent($$.getDefaultExtent());
    }

    // Add Axis
    $$.axis.init();

    // Set targets
    $$.updateTargets($$.data.targets);

    // Draw with targets
    if (binding) {
        $$.updateDimension();
        $$.config.oninit.call($$);
        $$.redraw({
            withTransition: false,
            withTransform: true,
            withUpdateXDomain: true,
            withUpdateOrgXDomain: true,
            withTransitionForAxis: false
        });
    }

    // Bind resize event
    $$.bindResize();

    // export element of the chart
    $$.api.element = $$.selectChart.node();
};

c3_chart_internal_fn.smoothLines = function (el, type) {
    var $$ = this;
    if (type === 'grid') {
        el.each(function () {
            var g = $$.d3.select(this),
                x1 = g.attr('x1'),
                x2 = g.attr('x2'),
                y1 = g.attr('y1'),
                y2 = g.attr('y2');
            g.attr({
                'x1': Math.ceil(x1),
                'x2': Math.ceil(x2),
                'y1': Math.ceil(y1),
                'y2': Math.ceil(y2)
            });
        });
    }
};

c3_chart_internal_fn.updateSizes = function () {
    var $$ = this,
        config = $$.config;
    var legendHeight = $$.legend ? $$.getLegendHeight() : 0,
        legendWidth = $$.legend ? $$.getLegendWidth() : 0,
        legendHeightForBottom = $$.isLegendRight || $$.isLegendInset ? 0 : legendHeight,
        hasArc = $$.hasArcType(),
        xAxisHeight = config.axis_rotated || hasArc ? 0 : $$.getHorizontalAxisHeight('x'),
        subchartHeight = config.subchart_show && !hasArc ? config.subchart_size_height + xAxisHeight : 0;

    $$.currentWidth = $$.getCurrentWidth();
    $$.currentHeight = $$.getCurrentHeight();

    // for main
    $$.margin = config.axis_rotated ? {
        top: $$.getHorizontalAxisHeight('y2') + $$.getCurrentPaddingTop(),
        right: hasArc ? 0 : $$.getCurrentPaddingRight(),
        bottom: $$.getHorizontalAxisHeight('y') + legendHeightForBottom + $$.getCurrentPaddingBottom(),
        left: subchartHeight + (hasArc ? 0 : $$.getCurrentPaddingLeft())
    } : {
        top: 4 + $$.getCurrentPaddingTop(), // for top tick text
        right: hasArc ? 0 : $$.getCurrentPaddingRight(),
        bottom: xAxisHeight + subchartHeight + legendHeightForBottom + $$.getCurrentPaddingBottom(),
        left: hasArc ? 0 : $$.getCurrentPaddingLeft()
    };

    // for subchart
    $$.margin2 = config.axis_rotated ? {
        top: $$.margin.top,
        right: NaN,
        bottom: 20 + legendHeightForBottom,
        left: $$.rotated_padding_left
    } : {
        top: $$.currentHeight - subchartHeight - legendHeightForBottom,
        right: NaN,
        bottom: xAxisHeight + legendHeightForBottom,
        left: $$.margin.left
    };

    // for legend
    $$.margin3 = {
        top: 0,
        right: NaN,
        bottom: 0,
        left: 0
    };
    if ($$.updateSizeForLegend) {
        $$.updateSizeForLegend(legendHeight, legendWidth);
    }

    $$.width = $$.currentWidth - $$.margin.left - $$.margin.right;
    $$.height = $$.currentHeight - $$.margin.top - $$.margin.bottom;
    if ($$.width < 0) {
        $$.width = 0;
    }
    if ($$.height < 0) {
        $$.height = 0;
    }

    $$.width2 = config.axis_rotated ? $$.margin.left - $$.rotated_padding_left - $$.rotated_padding_right : $$.width;
    $$.height2 = config.axis_rotated ? $$.height : $$.currentHeight - $$.margin2.top - $$.margin2.bottom;
    if ($$.width2 < 0) {
        $$.width2 = 0;
    }
    if ($$.height2 < 0) {
        $$.height2 = 0;
    }

    // for arc
    $$.arcWidth = $$.width - ($$.isLegendRight ? legendWidth + 10 : 0);
    $$.arcHeight = $$.height - ($$.isLegendRight ? 0 : 10);
    if ($$.hasType('gauge') && !config.gauge_fullCircle) {
        $$.arcHeight += $$.height - $$.getGaugeLabelHeight();
    }
    if ($$.updateRadius) {
        $$.updateRadius();
    }

    if ($$.isLegendRight && hasArc) {
        $$.margin3.left = $$.arcWidth / 2 + $$.radiusExpanded * 1.1;
    }
};

c3_chart_internal_fn.updateTargets = function (targets) {
    var $$ = this;

    /*-- Main --*/

    //-- Text --//
    $$.updateTargetsForText(targets);

    //-- Bar --//
    $$.updateTargetsForBar(targets);

    //-- Line --//
    $$.updateTargetsForLine(targets);

    //-- Arc --//
    if ($$.hasArcType() && $$.updateTargetsForArc) {
        $$.updateTargetsForArc(targets);
    }

    /*-- Sub --*/

    if ($$.updateTargetsForSubchart) {
        $$.updateTargetsForSubchart(targets);
    }

    // Fade-in each chart
    $$.showTargets();
};
c3_chart_internal_fn.showTargets = function () {
    var $$ = this;
    $$.svg.selectAll('.' + CLASS.target).filter(function (d) {
        return $$.isTargetToShow(d.id);
    }).transition().duration($$.config.transition_duration).style("opacity", 1);
};

c3_chart_internal_fn.redraw = function (options, transitions) {
    var $$ = this,
        main = $$.main,
        d3 = $$.d3,
        config = $$.config;
    var areaIndices = $$.getShapeIndices($$.isAreaType),
        barIndices = $$.getShapeIndices($$.isBarType),
        lineIndices = $$.getShapeIndices($$.isLineType);
    var withY, withSubchart, withTransition, withTransitionForExit, withTransitionForAxis, withTransform, withUpdateXDomain, withUpdateOrgXDomain, withTrimXDomain, withLegend, withEventRect, withDimension, withUpdateXAxis;
    var hideAxis = $$.hasArcType();
    var drawArea, drawBar, drawLine, xForText, yForText;
    var duration, durationForExit, durationForAxis;
    var waitForDraw, flow;
    var targetsToShow = $$.filterTargetsToShow($$.data.targets),
        tickValues,
        i,
        intervalForCulling,
        xDomainForZoom;
    var xv = $$.xv.bind($$),
        cx,
        cy;

    options = options || {};
    withY = getOption(options, "withY", true);
    withSubchart = getOption(options, "withSubchart", true);
    withTransition = getOption(options, "withTransition", true);
    withTransform = getOption(options, "withTransform", false);
    withUpdateXDomain = getOption(options, "withUpdateXDomain", false);
    withUpdateOrgXDomain = getOption(options, "withUpdateOrgXDomain", false);
    withTrimXDomain = getOption(options, "withTrimXDomain", true);
    withUpdateXAxis = getOption(options, "withUpdateXAxis", withUpdateXDomain);
    withLegend = getOption(options, "withLegend", false);
    withEventRect = getOption(options, "withEventRect", true);
    withDimension = getOption(options, "withDimension", true);
    withTransitionForExit = getOption(options, "withTransitionForExit", withTransition);
    withTransitionForAxis = getOption(options, "withTransitionForAxis", withTransition);

    duration = withTransition ? config.transition_duration : 0;
    durationForExit = withTransitionForExit ? duration : 0;
    durationForAxis = withTransitionForAxis ? duration : 0;

    transitions = transitions || $$.axis.generateTransitions(durationForAxis);

    // update legend and transform each g
    if (withLegend && config.legend_show) {
        $$.updateLegend($$.mapToIds($$.data.targets), options, transitions);
    } else if (withDimension) {
        // need to update dimension (e.g. axis.y.tick.values) because y tick values should change
        // no need to update axis in it because they will be updated in redraw()
        $$.updateDimension(true);
    }

    // MEMO: needed for grids calculation
    if ($$.isCategorized() && targetsToShow.length === 0) {
        $$.x.domain([0, $$.axes.x.selectAll('.tick').size()]);
    }

    if (targetsToShow.length) {
        $$.updateXDomain(targetsToShow, withUpdateXDomain, withUpdateOrgXDomain, withTrimXDomain);
        if (!config.axis_x_tick_values) {
            tickValues = $$.axis.updateXAxisTickValues(targetsToShow);
        }
    } else {
        $$.xAxis.tickValues([]);
        $$.subXAxis.tickValues([]);
    }

    if (config.zoom_rescale && !options.flow) {
        xDomainForZoom = $$.x.orgDomain();
    }

    $$.y.domain($$.getYDomain(targetsToShow, 'y', xDomainForZoom));
    $$.y2.domain($$.getYDomain(targetsToShow, 'y2', xDomainForZoom));

    if (!config.axis_y_tick_values && config.axis_y_tick_count) {
        $$.yAxis.tickValues($$.axis.generateTickValues($$.y.domain(), config.axis_y_tick_count));
    }
    if (!config.axis_y2_tick_values && config.axis_y2_tick_count) {
        $$.y2Axis.tickValues($$.axis.generateTickValues($$.y2.domain(), config.axis_y2_tick_count));
    }

    // axes
    $$.axis.redraw(transitions, hideAxis);

    // Update axis label
    $$.axis.updateLabels(withTransition);

    // show/hide if manual culling needed
    if ((withUpdateXDomain || withUpdateXAxis) && targetsToShow.length) {
        if (config.axis_x_tick_culling && tickValues) {
            for (i = 1; i < tickValues.length; i++) {
                if (tickValues.length / i < config.axis_x_tick_culling_max) {
                    intervalForCulling = i;
                    break;
                }
            }
            $$.svg.selectAll('.' + CLASS.axisX + ' .tick text').each(function (e) {
                var index = tickValues.indexOf(e);
                if (index >= 0) {
                    d3.select(this).style('display', index % intervalForCulling ? 'none' : 'block');
                }
            });
        } else {
            $$.svg.selectAll('.' + CLASS.axisX + ' .tick text').style('display', 'block');
        }
    }

    // setup drawer - MEMO: these must be called after axis updated
    drawArea = $$.generateDrawArea ? $$.generateDrawArea(areaIndices, false) : undefined;
    drawBar = $$.generateDrawBar ? $$.generateDrawBar(barIndices) : undefined;
    drawLine = $$.generateDrawLine ? $$.generateDrawLine(lineIndices, false) : undefined;
    xForText = $$.generateXYForText(areaIndices, barIndices, lineIndices, true);
    yForText = $$.generateXYForText(areaIndices, barIndices, lineIndices, false);

    // Update sub domain
    if (withY) {
        $$.subY.domain($$.getYDomain(targetsToShow, 'y'));
        $$.subY2.domain($$.getYDomain(targetsToShow, 'y2'));
    }

    // xgrid focus
    $$.updateXgridFocus();

    // Data empty label positioning and text.
    main.select("text." + CLASS.text + '.' + CLASS.empty).attr("x", $$.width / 2).attr("y", $$.height / 2).text(config.data_empty_label_text).transition().style('opacity', targetsToShow.length ? 0 : 1);

    // grid
    $$.updateGrid(duration);

    // rect for regions
    $$.updateRegion(duration);

    // bars
    $$.updateBar(durationForExit);

    // lines, areas and cricles
    $$.updateLine(durationForExit);
    $$.updateArea(durationForExit);
    $$.updateCircle();

    // text
    if ($$.hasDataLabel()) {
        $$.updateText(durationForExit);
    }

    // title
    if ($$.redrawTitle) {
        $$.redrawTitle();
    }

    // arc
    if ($$.redrawArc) {
        $$.redrawArc(duration, durationForExit, withTransform);
    }

    // subchart
    if ($$.redrawSubchart) {
        $$.redrawSubchart(withSubchart, transitions, duration, durationForExit, areaIndices, barIndices, lineIndices);
    }

    // circles for select
    main.selectAll('.' + CLASS.selectedCircles).filter($$.isBarType.bind($$)).selectAll('circle').remove();

    // event rects will redrawn when flow called
    if (config.interaction_enabled && !options.flow && withEventRect) {
        $$.redrawEventRect();
        if ($$.updateZoom) {
            $$.updateZoom();
        }
    }

    // update circleY based on updated parameters
    $$.updateCircleY();

    // generate circle x/y functions depending on updated params
    cx = ($$.config.axis_rotated ? $$.circleY : $$.circleX).bind($$);
    cy = ($$.config.axis_rotated ? $$.circleX : $$.circleY).bind($$);

    if (options.flow) {
        flow = $$.generateFlow({
            targets: targetsToShow,
            flow: options.flow,
            duration: options.flow.duration,
            drawBar: drawBar,
            drawLine: drawLine,
            drawArea: drawArea,
            cx: cx,
            cy: cy,
            xv: xv,
            xForText: xForText,
            yForText: yForText
        });
    }

    if ((duration || flow) && $$.isTabVisible()) {
        // Only use transition if tab visible. See #938.
        // transition should be derived from one transition
        d3.transition().duration(duration).each(function () {
            var transitionsToWait = [];

            // redraw and gather transitions
            [$$.redrawBar(drawBar, true), $$.redrawLine(drawLine, true), $$.redrawArea(drawArea, true), $$.redrawCircle(cx, cy, true), $$.redrawText(xForText, yForText, options.flow, true), $$.redrawRegion(true), $$.redrawGrid(true)].forEach(function (transitions) {
                transitions.forEach(function (transition) {
                    transitionsToWait.push(transition);
                });
            });

            // Wait for end of transitions to call flow and onrendered callback
            waitForDraw = $$.generateWait();
            transitionsToWait.forEach(function (t) {
                waitForDraw.add(t);
            });
        }).call(waitForDraw, function () {
            if (flow) {
                flow();
            }
            if (config.onrendered) {
                config.onrendered.call($$);
            }
        });
    } else {
        $$.redrawBar(drawBar);
        $$.redrawLine(drawLine);
        $$.redrawArea(drawArea);
        $$.redrawCircle(cx, cy);
        $$.redrawText(xForText, yForText, options.flow);
        $$.redrawRegion();
        $$.redrawGrid();
        if (config.onrendered) {
            config.onrendered.call($$);
        }
    }

    // update fadein condition
    $$.mapToIds($$.data.targets).forEach(function (id) {
        $$.withoutFadeIn[id] = true;
    });
};

c3_chart_internal_fn.updateAndRedraw = function (options) {
    var $$ = this,
        config = $$.config,
        transitions;
    options = options || {};
    // same with redraw
    options.withTransition = getOption(options, "withTransition", true);
    options.withTransform = getOption(options, "withTransform", false);
    options.withLegend = getOption(options, "withLegend", false);
    // NOT same with redraw
    options.withUpdateXDomain = true;
    options.withUpdateOrgXDomain = true;
    options.withTransitionForExit = false;
    options.withTransitionForTransform = getOption(options, "withTransitionForTransform", options.withTransition);
    // MEMO: this needs to be called before updateLegend and it means this ALWAYS needs to be called)
    $$.updateSizes();
    // MEMO: called in updateLegend in redraw if withLegend
    if (!(options.withLegend && config.legend_show)) {
        transitions = $$.axis.generateTransitions(options.withTransitionForAxis ? config.transition_duration : 0);
        // Update scales
        $$.updateScales();
        $$.updateSvgSize();
        // Update g positions
        $$.transformAll(options.withTransitionForTransform, transitions);
    }
    // Draw with new sizes & scales
    $$.redraw(options, transitions);
};
c3_chart_internal_fn.redrawWithoutRescale = function () {
    this.redraw({
        withY: false,
        withSubchart: false,
        withEventRect: false,
        withTransitionForAxis: false
    });
};

c3_chart_internal_fn.isTimeSeries = function () {
    return this.config.axis_x_type === 'timeseries';
};
c3_chart_internal_fn.isCategorized = function () {
    return this.config.axis_x_type.indexOf('categor') >= 0;
};
c3_chart_internal_fn.isCustomX = function () {
    var $$ = this,
        config = $$.config;
    return !$$.isTimeSeries() && (config.data_x || notEmpty(config.data_xs));
};

c3_chart_internal_fn.isTimeSeriesY = function () {
    return this.config.axis_y_type === 'timeseries';
};

c3_chart_internal_fn.getTranslate = function (target) {
    var $$ = this,
        config = $$.config,
        x,
        y;
    if (target === 'main') {
        x = asHalfPixel($$.margin.left);
        y = asHalfPixel($$.margin.top);
    } else if (target === 'context') {
        x = asHalfPixel($$.margin2.left);
        y = asHalfPixel($$.margin2.top);
    } else if (target === 'legend') {
        x = $$.margin3.left;
        y = $$.margin3.top;
    } else if (target === 'x') {
        x = 0;
        y = config.axis_rotated ? 0 : $$.height;
    } else if (target === 'y') {
        x = 0;
        y = config.axis_rotated ? $$.height : 0;
    } else if (target === 'y2') {
        x = config.axis_rotated ? 0 : $$.width;
        y = config.axis_rotated ? 1 : 0;
    } else if (target === 'subx') {
        x = 0;
        y = config.axis_rotated ? 0 : $$.height2;
    } else if (target === 'arc') {
        x = $$.arcWidth / 2;
        y = $$.arcHeight / 2;
    }
    return "translate(" + x + "," + y + ")";
};
c3_chart_internal_fn.initialOpacity = function (d) {
    return d.value !== null && this.withoutFadeIn[d.id] ? 1 : 0;
};
c3_chart_internal_fn.initialOpacityForCircle = function (d) {
    return d.value !== null && this.withoutFadeIn[d.id] ? this.opacityForCircle(d) : 0;
};
c3_chart_internal_fn.opacityForCircle = function (d) {
    var isPointShouldBeShown = isFunction(this.config.point_show) ? this.config.point_show(d) : this.config.point_show;
    var opacity = isPointShouldBeShown ? 1 : 0;
    return isValue(d.value) ? this.isScatterType(d) ? 0.5 : opacity : 0;
};
c3_chart_internal_fn.opacityForText = function () {
    return this.hasDataLabel() ? 1 : 0;
};
c3_chart_internal_fn.xx = function (d) {
    return d ? this.x(d.x) : null;
};
c3_chart_internal_fn.xv = function (d) {
    var $$ = this,
        value = d.value;
    if ($$.isTimeSeries()) {
        value = $$.parseDate(d.value);
    } else if ($$.isCategorized() && typeof d.value === 'string') {
        value = $$.config.axis_x_categories.indexOf(d.value);
    }
    return Math.ceil($$.x(value));
};
c3_chart_internal_fn.yv = function (d) {
    var $$ = this,
        yScale = d.axis && d.axis === 'y2' ? $$.y2 : $$.y;
    return Math.ceil(yScale(d.value));
};
c3_chart_internal_fn.subxx = function (d) {
    return d ? this.subX(d.x) : null;
};

c3_chart_internal_fn.transformMain = function (withTransition, transitions) {
    var $$ = this,
        xAxis,
        yAxis,
        y2Axis;
    if (transitions && transitions.axisX) {
        xAxis = transitions.axisX;
    } else {
        xAxis = $$.main.select('.' + CLASS.axisX);
        if (withTransition) {
            xAxis = xAxis.transition();
        }
    }
    if (transitions && transitions.axisY) {
        yAxis = transitions.axisY;
    } else {
        yAxis = $$.main.select('.' + CLASS.axisY);
        if (withTransition) {
            yAxis = yAxis.transition();
        }
    }
    if (transitions && transitions.axisY2) {
        y2Axis = transitions.axisY2;
    } else {
        y2Axis = $$.main.select('.' + CLASS.axisY2);
        if (withTransition) {
            y2Axis = y2Axis.transition();
        }
    }
    (withTransition ? $$.main.transition() : $$.main).attr("transform", $$.getTranslate('main'));
    xAxis.attr("transform", $$.getTranslate('x'));
    yAxis.attr("transform", $$.getTranslate('y'));
    y2Axis.attr("transform", $$.getTranslate('y2'));
    $$.main.select('.' + CLASS.chartArcs).attr("transform", $$.getTranslate('arc'));
};
c3_chart_internal_fn.transformAll = function (withTransition, transitions) {
    var $$ = this;
    $$.transformMain(withTransition, transitions);
    if ($$.config.subchart_show) {
        $$.transformContext(withTransition, transitions);
    }
    if ($$.legend) {
        $$.transformLegend(withTransition);
    }
};

c3_chart_internal_fn.updateSvgSize = function () {
    var $$ = this,
        brush = $$.svg.select(".c3-brush .background");
    $$.svg.attr('width', $$.currentWidth).attr('height', $$.currentHeight);
    $$.svg.selectAll(['#' + $$.clipId, '#' + $$.clipIdForGrid]).select('rect').attr('width', $$.width).attr('height', $$.height);
    $$.svg.select('#' + $$.clipIdForXAxis).select('rect').attr('x', $$.getXAxisClipX.bind($$)).attr('y', $$.getXAxisClipY.bind($$)).attr('width', $$.getXAxisClipWidth.bind($$)).attr('height', $$.getXAxisClipHeight.bind($$));
    $$.svg.select('#' + $$.clipIdForYAxis).select('rect').attr('x', $$.getYAxisClipX.bind($$)).attr('y', $$.getYAxisClipY.bind($$)).attr('width', $$.getYAxisClipWidth.bind($$)).attr('height', $$.getYAxisClipHeight.bind($$));
    $$.svg.select('#' + $$.clipIdForSubchart).select('rect').attr('width', $$.width).attr('height', brush.size() ? brush.attr('height') : 0);
    $$.svg.select('.' + CLASS.zoomRect).attr('width', $$.width).attr('height', $$.height);
    // MEMO: parent div's height will be bigger than svg when <!DOCTYPE html>
    $$.selectChart.style('max-height', $$.currentHeight + "px");
};

c3_chart_internal_fn.updateDimension = function (withoutAxis) {
    var $$ = this;
    if (!withoutAxis) {
        if ($$.config.axis_rotated) {
            $$.axes.x.call($$.xAxis);
            $$.axes.subx.call($$.subXAxis);
        } else {
            $$.axes.y.call($$.yAxis);
            $$.axes.y2.call($$.y2Axis);
        }
    }
    $$.updateSizes();
    $$.updateScales();
    $$.updateSvgSize();
    $$.transformAll(false);
};

c3_chart_internal_fn.observeInserted = function (selection) {
    var $$ = this,
        observer;
    if (typeof MutationObserver === 'undefined') {
        window.console.error("MutationObserver not defined.");
        return;
    }
    observer = new MutationObserver(function (mutations) {
        mutations.forEach(function (mutation) {
            if (mutation.type === 'childList' && mutation.previousSibling) {
                observer.disconnect();
                // need to wait for completion of load because size calculation requires the actual sizes determined after that completion
                $$.intervalForObserveInserted = window.setInterval(function () {
                    // parentNode will NOT be null when completed
                    if (selection.node().parentNode) {
                        window.clearInterval($$.intervalForObserveInserted);
                        $$.updateDimension();
                        if ($$.brush) {
                            $$.brush.update();
                        }
                        $$.config.oninit.call($$);
                        $$.redraw({
                            withTransform: true,
                            withUpdateXDomain: true,
                            withUpdateOrgXDomain: true,
                            withTransition: false,
                            withTransitionForTransform: false,
                            withLegend: true
                        });
                        selection.transition().style('opacity', 1);
                    }
                }, 10);
            }
        });
    });
    observer.observe(selection.node(), { attributes: true, childList: true, characterData: true });
};

c3_chart_internal_fn.bindResize = function () {
    var $$ = this,
        config = $$.config;

    $$.resizeFunction = $$.generateResize();

    $$.resizeFunction.add(function () {
        config.onresize.call($$);
    });
    if (config.resize_auto) {
        $$.resizeFunction.add(function () {
            if ($$.resizeTimeout !== undefined) {
                window.clearTimeout($$.resizeTimeout);
            }
            $$.resizeTimeout = window.setTimeout(function () {
                delete $$.resizeTimeout;
                $$.api.flush();
            }, 100);
        });
    }
    $$.resizeFunction.add(function () {
        config.onresized.call($$);
    });

    if (window.attachEvent) {
        window.attachEvent('onresize', $$.resizeFunction);
    } else if (window.addEventListener) {
        window.addEventListener('resize', $$.resizeFunction, false);
    } else {
        // fallback to this, if this is a very old browser
        var wrapper = window.onresize;
        if (!wrapper) {
            // create a wrapper that will call all charts
            wrapper = $$.generateResize();
        } else if (!wrapper.add || !wrapper.remove) {
            // there is already a handler registered, make sure we call it too
            wrapper = $$.generateResize();
            wrapper.add(window.onresize);
        }
        // add this graph to the wrapper, we will be removed if the user calls destroy
        wrapper.add($$.resizeFunction);
        window.onresize = wrapper;
    }
};

c3_chart_internal_fn.generateResize = function () {
    var resizeFunctions = [];
    function callResizeFunctions() {
        resizeFunctions.forEach(function (f) {
            f();
        });
    }
    callResizeFunctions.add = function (f) {
        resizeFunctions.push(f);
    };
    callResizeFunctions.remove = function (f) {
        for (var i = 0; i < resizeFunctions.length; i++) {
            if (resizeFunctions[i] === f) {
                resizeFunctions.splice(i, 1);
                break;
            }
        }
    };
    return callResizeFunctions;
};

c3_chart_internal_fn.endall = function (transition, callback) {
    var n = 0;
    transition.each(function () {
        ++n;
    }).each("end", function () {
        if (! --n) {
            callback.apply(this, arguments);
        }
    });
};
c3_chart_internal_fn.generateWait = function () {
    var transitionsToWait = [],
        f = function f(transition, callback) {
        var timer = setInterval(function () {
            var done = 0;
            transitionsToWait.forEach(function (t) {
                if (t.empty()) {
                    done += 1;
                    return;
                }
                try {
                    t.transition();
                } catch (e) {
                    done += 1;
                }
            });
            if (done === transitionsToWait.length) {
                clearInterval(timer);
                if (callback) {
                    callback();
                }
            }
        }, 10);
    };
    f.add = function (transition) {
        transitionsToWait.push(transition);
    };
    return f;
};

c3_chart_internal_fn.parseDate = function (date) {
    var $$ = this,
        parsedDate;
    if (date instanceof Date) {
        parsedDate = date;
    } else if (typeof date === 'string') {
        parsedDate = $$.dataTimeFormat($$.config.data_xFormat).parse(date);
    } else if ((typeof date === 'undefined' ? 'undefined' : _typeof(date)) === 'object') {
        parsedDate = new Date(+date);
    } else if (typeof date === 'number' && !isNaN(date)) {
        parsedDate = new Date(+date);
    }
    if (!parsedDate || isNaN(+parsedDate)) {
        window.console.error("Failed to parse x '" + date + "' to Date object");
    }
    return parsedDate;
};

c3_chart_internal_fn.isTabVisible = function () {
    var hidden;
    if (typeof document.hidden !== "undefined") {
        // Opera 12.10 and Firefox 18 and later support
        hidden = "hidden";
    } else if (typeof document.mozHidden !== "undefined") {
        hidden = "mozHidden";
    } else if (typeof document.msHidden !== "undefined") {
        hidden = "msHidden";
    } else if (typeof document.webkitHidden !== "undefined") {
        hidden = "webkitHidden";
    }

    return document[hidden] ? false : true;
};

c3_chart_internal_fn.isValue = isValue;
c3_chart_internal_fn.isFunction = isFunction;
c3_chart_internal_fn.isString = isString;
c3_chart_internal_fn.isUndefined = isUndefined;
c3_chart_internal_fn.isDefined = isDefined;
c3_chart_internal_fn.ceil10 = ceil10;
c3_chart_internal_fn.asHalfPixel = asHalfPixel;
c3_chart_internal_fn.diffDomain = diffDomain;
c3_chart_internal_fn.isEmpty = isEmpty;
c3_chart_internal_fn.notEmpty = notEmpty;
c3_chart_internal_fn.notEmpty = notEmpty;
c3_chart_internal_fn.getOption = getOption;
c3_chart_internal_fn.hasValue = hasValue;
c3_chart_internal_fn.sanitise = sanitise;
c3_chart_internal_fn.getPathBox = getPathBox;
c3_chart_internal_fn.CLASS = CLASS;

/* jshint ignore:start */

// PhantomJS doesn't have support for Function.prototype.bind, which has caused confusion. Use
// this polyfill to avoid the confusion.
// https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Function/bind#Polyfill

if (!Function.prototype.bind) {
    Function.prototype.bind = function (oThis) {
        if (typeof this !== 'function') {
            // closest thing possible to the ECMAScript 5
            // internal IsCallable function
            throw new TypeError('Function.prototype.bind - what is trying to be bound is not callable');
        }

        var aArgs = Array.prototype.slice.call(arguments, 1),
            fToBind = this,
            fNOP = function fNOP() {},
            fBound = function fBound() {
            return fToBind.apply(this instanceof fNOP ? this : oThis, aArgs.concat(Array.prototype.slice.call(arguments)));
        };

        fNOP.prototype = this.prototype;
        fBound.prototype = new fNOP();

        return fBound;
    };
}

// SVGPathSeg API polyfill
// https://github.com/progers/pathseg
//
// This is a drop-in replacement for the SVGPathSeg and SVGPathSegList APIs that were removed from
// SVG2 (https://lists.w3.org/Archives/Public/www-svg/2015Jun/0044.html), including the latest spec
// changes which were implemented in Firefox 43 and Chrome 46.

(function () {
    "use strict";

    if (!("SVGPathSeg" in window)) {
        // Spec: http://www.w3.org/TR/SVG11/single-page.html#paths-InterfaceSVGPathSeg
        window.SVGPathSeg = function (type, typeAsLetter, owningPathSegList) {
            this.pathSegType = type;
            this.pathSegTypeAsLetter = typeAsLetter;
            this._owningPathSegList = owningPathSegList;
        };

        window.SVGPathSeg.prototype.classname = "SVGPathSeg";

        window.SVGPathSeg.PATHSEG_UNKNOWN = 0;
        window.SVGPathSeg.PATHSEG_CLOSEPATH = 1;
        window.SVGPathSeg.PATHSEG_MOVETO_ABS = 2;
        window.SVGPathSeg.PATHSEG_MOVETO_REL = 3;
        window.SVGPathSeg.PATHSEG_LINETO_ABS = 4;
        window.SVGPathSeg.PATHSEG_LINETO_REL = 5;
        window.SVGPathSeg.PATHSEG_CURVETO_CUBIC_ABS = 6;
        window.SVGPathSeg.PATHSEG_CURVETO_CUBIC_REL = 7;
        window.SVGPathSeg.PATHSEG_CURVETO_QUADRATIC_ABS = 8;
        window.SVGPathSeg.PATHSEG_CURVETO_QUADRATIC_REL = 9;
        window.SVGPathSeg.PATHSEG_ARC_ABS = 10;
        window.SVGPathSeg.PATHSEG_ARC_REL = 11;
        window.SVGPathSeg.PATHSEG_LINETO_HORIZONTAL_ABS = 12;
        window.SVGPathSeg.PATHSEG_LINETO_HORIZONTAL_REL = 13;
        window.SVGPathSeg.PATHSEG_LINETO_VERTICAL_ABS = 14;
        window.SVGPathSeg.PATHSEG_LINETO_VERTICAL_REL = 15;
        window.SVGPathSeg.PATHSEG_CURVETO_CUBIC_SMOOTH_ABS = 16;
        window.SVGPathSeg.PATHSEG_CURVETO_CUBIC_SMOOTH_REL = 17;
        window.SVGPathSeg.PATHSEG_CURVETO_QUADRATIC_SMOOTH_ABS = 18;
        window.SVGPathSeg.PATHSEG_CURVETO_QUADRATIC_SMOOTH_REL = 19;

        // Notify owning PathSegList on any changes so they can be synchronized back to the path element.
        window.SVGPathSeg.prototype._segmentChanged = function () {
            if (this._owningPathSegList) this._owningPathSegList.segmentChanged(this);
        };

        window.SVGPathSegClosePath = function (owningPathSegList) {
            window.SVGPathSeg.call(this, window.SVGPathSeg.PATHSEG_CLOSEPATH, "z", owningPathSegList);
        };
        window.SVGPathSegClosePath.prototype = Object.create(window.SVGPathSeg.prototype);
        window.SVGPathSegClosePath.prototype.toString = function () {
            return "[object SVGPathSegClosePath]";
        };
        window.SVGPathSegClosePath.prototype._asPathString = function () {
            return this.pathSegTypeAsLetter;
        };
        window.SVGPathSegClosePath.prototype.clone = function () {
            return new window.SVGPathSegClosePath(undefined);
        };

        window.SVGPathSegMovetoAbs = function (owningPathSegList, x, y) {
            window.SVGPathSeg.call(this, window.SVGPathSeg.PATHSEG_MOVETO_ABS, "M", owningPathSegList);
            this._x = x;
            this._y = y;
        };
        window.SVGPathSegMovetoAbs.prototype = Object.create(window.SVGPathSeg.prototype);
        window.SVGPathSegMovetoAbs.prototype.toString = function () {
            return "[object SVGPathSegMovetoAbs]";
        };
        window.SVGPathSegMovetoAbs.prototype._asPathString = function () {
            return this.pathSegTypeAsLetter + " " + this._x + " " + this._y;
        };
        window.SVGPathSegMovetoAbs.prototype.clone = function () {
            return new window.SVGPathSegMovetoAbs(undefined, this._x, this._y);
        };
        Object.defineProperty(window.SVGPathSegMovetoAbs.prototype, "x", { get: function get() {
                return this._x;
            }, set: function set(x) {
                this._x = x;this._segmentChanged();
            }, enumerable: true });
        Object.defineProperty(window.SVGPathSegMovetoAbs.prototype, "y", { get: function get() {
                return this._y;
            }, set: function set(y) {
                this._y = y;this._segmentChanged();
            }, enumerable: true });

        window.SVGPathSegMovetoRel = function (owningPathSegList, x, y) {
            window.SVGPathSeg.call(this, window.SVGPathSeg.PATHSEG_MOVETO_REL, "m", owningPathSegList);
            this._x = x;
            this._y = y;
        };
        window.SVGPathSegMovetoRel.prototype = Object.create(window.SVGPathSeg.prototype);
        window.SVGPathSegMovetoRel.prototype.toString = function () {
            return "[object SVGPathSegMovetoRel]";
        };
        window.SVGPathSegMovetoRel.prototype._asPathString = function () {
            return this.pathSegTypeAsLetter + " " + this._x + " " + this._y;
        };
        window.SVGPathSegMovetoRel.prototype.clone = function () {
            return new window.SVGPathSegMovetoRel(undefined, this._x, this._y);
        };
        Object.defineProperty(window.SVGPathSegMovetoRel.prototype, "x", { get: function get() {
                return this._x;
            }, set: function set(x) {
                this._x = x;this._segmentChanged();
            }, enumerable: true });
        Object.defineProperty(window.SVGPathSegMovetoRel.prototype, "y", { get: function get() {
                return this._y;
            }, set: function set(y) {
                this._y = y;this._segmentChanged();
            }, enumerable: true });

        window.SVGPathSegLinetoAbs = function (owningPathSegList, x, y) {
            window.SVGPathSeg.call(this, window.SVGPathSeg.PATHSEG_LINETO_ABS, "L", owningPathSegList);
            this._x = x;
            this._y = y;
        };
        window.SVGPathSegLinetoAbs.prototype = Object.create(window.SVGPathSeg.prototype);
        window.SVGPathSegLinetoAbs.prototype.toString = function () {
            return "[object SVGPathSegLinetoAbs]";
        };
        window.SVGPathSegLinetoAbs.prototype._asPathString = function () {
            return this.pathSegTypeAsLetter + " " + this._x + " " + this._y;
        };
        window.SVGPathSegLinetoAbs.prototype.clone = function () {
            return new window.SVGPathSegLinetoAbs(undefined, this._x, this._y);
        };
        Object.defineProperty(window.SVGPathSegLinetoAbs.prototype, "x", { get: function get() {
                return this._x;
            }, set: function set(x) {
                this._x = x;this._segmentChanged();
            }, enumerable: true });
        Object.defineProperty(window.SVGPathSegLinetoAbs.prototype, "y", { get: function get() {
                return this._y;
            }, set: function set(y) {
                this._y = y;this._segmentChanged();
            }, enumerable: true });

        window.SVGPathSegLinetoRel = function (owningPathSegList, x, y) {
            window.SVGPathSeg.call(this, window.SVGPathSeg.PATHSEG_LINETO_REL, "l", owningPathSegList);
            this._x = x;
            this._y = y;
        };
        window.SVGPathSegLinetoRel.prototype = Object.create(window.SVGPathSeg.prototype);
        window.SVGPathSegLinetoRel.prototype.toString = function () {
            return "[object SVGPathSegLinetoRel]";
        };
        window.SVGPathSegLinetoRel.prototype._asPathString = function () {
            return this.pathSegTypeAsLetter + " " + this._x + " " + this._y;
        };
        window.SVGPathSegLinetoRel.prototype.clone = function () {
            return new window.SVGPathSegLinetoRel(undefined, this._x, this._y);
        };
        Object.defineProperty(window.SVGPathSegLinetoRel.prototype, "x", { get: function get() {
                return this._x;
            }, set: function set(x) {
                this._x = x;this._segmentChanged();
            }, enumerable: true });
        Object.defineProperty(window.SVGPathSegLinetoRel.prototype, "y", { get: function get() {
                return this._y;
            }, set: function set(y) {
                this._y = y;this._segmentChanged();
            }, enumerable: true });

        window.SVGPathSegCurvetoCubicAbs = function (owningPathSegList, x, y, x1, y1, x2, y2) {
            window.SVGPathSeg.call(this, window.SVGPathSeg.PATHSEG_CURVETO_CUBIC_ABS, "C", owningPathSegList);
            this._x = x;
            this._y = y;
            this._x1 = x1;
            this._y1 = y1;
            this._x2 = x2;
            this._y2 = y2;
        };
        window.SVGPathSegCurvetoCubicAbs.prototype = Object.create(window.SVGPathSeg.prototype);
        window.SVGPathSegCurvetoCubicAbs.prototype.toString = function () {
            return "[object SVGPathSegCurvetoCubicAbs]";
        };
        window.SVGPathSegCurvetoCubicAbs.prototype._asPathString = function () {
            return this.pathSegTypeAsLetter + " " + this._x1 + " " + this._y1 + " " + this._x2 + " " + this._y2 + " " + this._x + " " + this._y;
        };
        window.SVGPathSegCurvetoCubicAbs.prototype.clone = function () {
            return new window.SVGPathSegCurvetoCubicAbs(undefined, this._x, this._y, this._x1, this._y1, this._x2, this._y2);
        };
        Object.defineProperty(window.SVGPathSegCurvetoCubicAbs.prototype, "x", { get: function get() {
                return this._x;
            }, set: function set(x) {
                this._x = x;this._segmentChanged();
            }, enumerable: true });
        Object.defineProperty(window.SVGPathSegCurvetoCubicAbs.prototype, "y", { get: function get() {
                return this._y;
            }, set: function set(y) {
                this._y = y;this._segmentChanged();
            }, enumerable: true });
        Object.defineProperty(window.SVGPathSegCurvetoCubicAbs.prototype, "x1", { get: function get() {
                return this._x1;
            }, set: function set(x1) {
                this._x1 = x1;this._segmentChanged();
            }, enumerable: true });
        Object.defineProperty(window.SVGPathSegCurvetoCubicAbs.prototype, "y1", { get: function get() {
                return this._y1;
            }, set: function set(y1) {
                this._y1 = y1;this._segmentChanged();
            }, enumerable: true });
        Object.defineProperty(window.SVGPathSegCurvetoCubicAbs.prototype, "x2", { get: function get() {
                return this._x2;
            }, set: function set(x2) {
                this._x2 = x2;this._segmentChanged();
            }, enumerable: true });
        Object.defineProperty(window.SVGPathSegCurvetoCubicAbs.prototype, "y2", { get: function get() {
                return this._y2;
            }, set: function set(y2) {
                this._y2 = y2;this._segmentChanged();
            }, enumerable: true });

        window.SVGPathSegCurvetoCubicRel = function (owningPathSegList, x, y, x1, y1, x2, y2) {
            window.SVGPathSeg.call(this, window.SVGPathSeg.PATHSEG_CURVETO_CUBIC_REL, "c", owningPathSegList);
            this._x = x;
            this._y = y;
            this._x1 = x1;
            this._y1 = y1;
            this._x2 = x2;
            this._y2 = y2;
        };
        window.SVGPathSegCurvetoCubicRel.prototype = Object.create(window.SVGPathSeg.prototype);
        window.SVGPathSegCurvetoCubicRel.prototype.toString = function () {
            return "[object SVGPathSegCurvetoCubicRel]";
        };
        window.SVGPathSegCurvetoCubicRel.prototype._asPathString = function () {
            return this.pathSegTypeAsLetter + " " + this._x1 + " " + this._y1 + " " + this._x2 + " " + this._y2 + " " + this._x + " " + this._y;
        };
        window.SVGPathSegCurvetoCubicRel.prototype.clone = function () {
            return new window.SVGPathSegCurvetoCubicRel(undefined, this._x, this._y, this._x1, this._y1, this._x2, this._y2);
        };
        Object.defineProperty(window.SVGPathSegCurvetoCubicRel.prototype, "x", { get: function get() {
                return this._x;
            }, set: function set(x) {
                this._x = x;this._segmentChanged();
            }, enumerable: true });
        Object.defineProperty(window.SVGPathSegCurvetoCubicRel.prototype, "y", { get: function get() {
                return this._y;
            }, set: function set(y) {
                this._y = y;this._segmentChanged();
            }, enumerable: true });
        Object.defineProperty(window.SVGPathSegCurvetoCubicRel.prototype, "x1", { get: function get() {
                return this._x1;
            }, set: function set(x1) {
                this._x1 = x1;this._segmentChanged();
            }, enumerable: true });
        Object.defineProperty(window.SVGPathSegCurvetoCubicRel.prototype, "y1", { get: function get() {
                return this._y1;
            }, set: function set(y1) {
                this._y1 = y1;this._segmentChanged();
            }, enumerable: true });
        Object.defineProperty(window.SVGPathSegCurvetoCubicRel.prototype, "x2", { get: function get() {
                return this._x2;
            }, set: function set(x2) {
                this._x2 = x2;this._segmentChanged();
            }, enumerable: true });
        Object.defineProperty(window.SVGPathSegCurvetoCubicRel.prototype, "y2", { get: function get() {
                return this._y2;
            }, set: function set(y2) {
                this._y2 = y2;this._segmentChanged();
            }, enumerable: true });

        window.SVGPathSegCurvetoQuadraticAbs = function (owningPathSegList, x, y, x1, y1) {
            window.SVGPathSeg.call(this, window.SVGPathSeg.PATHSEG_CURVETO_QUADRATIC_ABS, "Q", owningPathSegList);
            this._x = x;
            this._y = y;
            this._x1 = x1;
            this._y1 = y1;
        };
        window.SVGPathSegCurvetoQuadraticAbs.prototype = Object.create(window.SVGPathSeg.prototype);
        window.SVGPathSegCurvetoQuadraticAbs.prototype.toString = function () {
            return "[object SVGPathSegCurvetoQuadraticAbs]";
        };
        window.SVGPathSegCurvetoQuadraticAbs.prototype._asPathString = function () {
            return this.pathSegTypeAsLetter + " " + this._x1 + " " + this._y1 + " " + this._x + " " + this._y;
        };
        window.SVGPathSegCurvetoQuadraticAbs.prototype.clone = function () {
            return new window.SVGPathSegCurvetoQuadraticAbs(undefined, this._x, this._y, this._x1, this._y1);
        };
        Object.defineProperty(window.SVGPathSegCurvetoQuadraticAbs.prototype, "x", { get: function get() {
                return this._x;
            }, set: function set(x) {
                this._x = x;this._segmentChanged();
            }, enumerable: true });
        Object.defineProperty(window.SVGPathSegCurvetoQuadraticAbs.prototype, "y", { get: function get() {
                return this._y;
            }, set: function set(y) {
                this._y = y;this._segmentChanged();
            }, enumerable: true });
        Object.defineProperty(window.SVGPathSegCurvetoQuadraticAbs.prototype, "x1", { get: function get() {
                return this._x1;
            }, set: function set(x1) {
                this._x1 = x1;this._segmentChanged();
            }, enumerable: true });
        Object.defineProperty(window.SVGPathSegCurvetoQuadraticAbs.prototype, "y1", { get: function get() {
                return this._y1;
            }, set: function set(y1) {
                this._y1 = y1;this._segmentChanged();
            }, enumerable: true });

        window.SVGPathSegCurvetoQuadraticRel = function (owningPathSegList, x, y, x1, y1) {
            window.SVGPathSeg.call(this, window.SVGPathSeg.PATHSEG_CURVETO_QUADRATIC_REL, "q", owningPathSegList);
            this._x = x;
            this._y = y;
            this._x1 = x1;
            this._y1 = y1;
        };
        window.SVGPathSegCurvetoQuadraticRel.prototype = Object.create(window.SVGPathSeg.prototype);
        window.SVGPathSegCurvetoQuadraticRel.prototype.toString = function () {
            return "[object SVGPathSegCurvetoQuadraticRel]";
        };
        window.SVGPathSegCurvetoQuadraticRel.prototype._asPathString = function () {
            return this.pathSegTypeAsLetter + " " + this._x1 + " " + this._y1 + " " + this._x + " " + this._y;
        };
        window.SVGPathSegCurvetoQuadraticRel.prototype.clone = function () {
            return new window.SVGPathSegCurvetoQuadraticRel(undefined, this._x, this._y, this._x1, this._y1);
        };
        Object.defineProperty(window.SVGPathSegCurvetoQuadraticRel.prototype, "x", { get: function get() {
                return this._x;
            }, set: function set(x) {
                this._x = x;this._segmentChanged();
            }, enumerable: true });
        Object.defineProperty(window.SVGPathSegCurvetoQuadraticRel.prototype, "y", { get: function get() {
                return this._y;
            }, set: function set(y) {
                this._y = y;this._segmentChanged();
            }, enumerable: true });
        Object.defineProperty(window.SVGPathSegCurvetoQuadraticRel.prototype, "x1", { get: function get() {
                return this._x1;
            }, set: function set(x1) {
                this._x1 = x1;this._segmentChanged();
            }, enumerable: true });
        Object.defineProperty(window.SVGPathSegCurvetoQuadraticRel.prototype, "y1", { get: function get() {
                return this._y1;
            }, set: function set(y1) {
                this._y1 = y1;this._segmentChanged();
            }, enumerable: true });

        window.SVGPathSegArcAbs = function (owningPathSegList, x, y, r1, r2, angle, largeArcFlag, sweepFlag) {
            window.SVGPathSeg.call(this, window.SVGPathSeg.PATHSEG_ARC_ABS, "A", owningPathSegList);
            this._x = x;
            this._y = y;
            this._r1 = r1;
            this._r2 = r2;
            this._angle = angle;
            this._largeArcFlag = largeArcFlag;
            this._sweepFlag = sweepFlag;
        };
        window.SVGPathSegArcAbs.prototype = Object.create(window.SVGPathSeg.prototype);
        window.SVGPathSegArcAbs.prototype.toString = function () {
            return "[object SVGPathSegArcAbs]";
        };
        window.SVGPathSegArcAbs.prototype._asPathString = function () {
            return this.pathSegTypeAsLetter + " " + this._r1 + " " + this._r2 + " " + this._angle + " " + (this._largeArcFlag ? "1" : "0") + " " + (this._sweepFlag ? "1" : "0") + " " + this._x + " " + this._y;
        };
        window.SVGPathSegArcAbs.prototype.clone = function () {
            return new window.SVGPathSegArcAbs(undefined, this._x, this._y, this._r1, this._r2, this._angle, this._largeArcFlag, this._sweepFlag);
        };
        Object.defineProperty(window.SVGPathSegArcAbs.prototype, "x", { get: function get() {
                return this._x;
            }, set: function set(x) {
                this._x = x;this._segmentChanged();
            }, enumerable: true });
        Object.defineProperty(window.SVGPathSegArcAbs.prototype, "y", { get: function get() {
                return this._y;
            }, set: function set(y) {
                this._y = y;this._segmentChanged();
            }, enumerable: true });
        Object.defineProperty(window.SVGPathSegArcAbs.prototype, "r1", { get: function get() {
                return this._r1;
            }, set: function set(r1) {
                this._r1 = r1;this._segmentChanged();
            }, enumerable: true });
        Object.defineProperty(window.SVGPathSegArcAbs.prototype, "r2", { get: function get() {
                return this._r2;
            }, set: function set(r2) {
                this._r2 = r2;this._segmentChanged();
            }, enumerable: true });
        Object.defineProperty(window.SVGPathSegArcAbs.prototype, "angle", { get: function get() {
                return this._angle;
            }, set: function set(angle) {
                this._angle = angle;this._segmentChanged();
            }, enumerable: true });
        Object.defineProperty(window.SVGPathSegArcAbs.prototype, "largeArcFlag", { get: function get() {
                return this._largeArcFlag;
            }, set: function set(largeArcFlag) {
                this._largeArcFlag = largeArcFlag;this._segmentChanged();
            }, enumerable: true });
        Object.defineProperty(window.SVGPathSegArcAbs.prototype, "sweepFlag", { get: function get() {
                return this._sweepFlag;
            }, set: function set(sweepFlag) {
                this._sweepFlag = sweepFlag;this._segmentChanged();
            }, enumerable: true });

        window.SVGPathSegArcRel = function (owningPathSegList, x, y, r1, r2, angle, largeArcFlag, sweepFlag) {
            window.SVGPathSeg.call(this, window.SVGPathSeg.PATHSEG_ARC_REL, "a", owningPathSegList);
            this._x = x;
            this._y = y;
            this._r1 = r1;
            this._r2 = r2;
            this._angle = angle;
            this._largeArcFlag = largeArcFlag;
            this._sweepFlag = sweepFlag;
        };
        window.SVGPathSegArcRel.prototype = Object.create(window.SVGPathSeg.prototype);
        window.SVGPathSegArcRel.prototype.toString = function () {
            return "[object SVGPathSegArcRel]";
        };
        window.SVGPathSegArcRel.prototype._asPathString = function () {
            return this.pathSegTypeAsLetter + " " + this._r1 + " " + this._r2 + " " + this._angle + " " + (this._largeArcFlag ? "1" : "0") + " " + (this._sweepFlag ? "1" : "0") + " " + this._x + " " + this._y;
        };
        window.SVGPathSegArcRel.prototype.clone = function () {
            return new window.SVGPathSegArcRel(undefined, this._x, this._y, this._r1, this._r2, this._angle, this._largeArcFlag, this._sweepFlag);
        };
        Object.defineProperty(window.SVGPathSegArcRel.prototype, "x", { get: function get() {
                return this._x;
            }, set: function set(x) {
                this._x = x;this._segmentChanged();
            }, enumerable: true });
        Object.defineProperty(window.SVGPathSegArcRel.prototype, "y", { get: function get() {
                return this._y;
            }, set: function set(y) {
                this._y = y;this._segmentChanged();
            }, enumerable: true });
        Object.defineProperty(window.SVGPathSegArcRel.prototype, "r1", { get: function get() {
                return this._r1;
            }, set: function set(r1) {
                this._r1 = r1;this._segmentChanged();
            }, enumerable: true });
        Object.defineProperty(window.SVGPathSegArcRel.prototype, "r2", { get: function get() {
                return this._r2;
            }, set: function set(r2) {
                this._r2 = r2;this._segmentChanged();
            }, enumerable: true });
        Object.defineProperty(window.SVGPathSegArcRel.prototype, "angle", { get: function get() {
                return this._angle;
            }, set: function set(angle) {
                this._angle = angle;this._segmentChanged();
            }, enumerable: true });
        Object.defineProperty(window.SVGPathSegArcRel.prototype, "largeArcFlag", { get: function get() {
                return this._largeArcFlag;
            }, set: function set(largeArcFlag) {
                this._largeArcFlag = largeArcFlag;this._segmentChanged();
            }, enumerable: true });
        Object.defineProperty(window.SVGPathSegArcRel.prototype, "sweepFlag", { get: function get() {
                return this._sweepFlag;
            }, set: function set(sweepFlag) {
                this._sweepFlag = sweepFlag;this._segmentChanged();
            }, enumerable: true });

        window.SVGPathSegLinetoHorizontalAbs = function (owningPathSegList, x) {
            window.SVGPathSeg.call(this, window.SVGPathSeg.PATHSEG_LINETO_HORIZONTAL_ABS, "H", owningPathSegList);
            this._x = x;
        };
        window.SVGPathSegLinetoHorizontalAbs.prototype = Object.create(window.SVGPathSeg.prototype);
        window.SVGPathSegLinetoHorizontalAbs.prototype.toString = function () {
            return "[object SVGPathSegLinetoHorizontalAbs]";
        };
        window.SVGPathSegLinetoHorizontalAbs.prototype._asPathString = function () {
            return this.pathSegTypeAsLetter + " " + this._x;
        };
        window.SVGPathSegLinetoHorizontalAbs.prototype.clone = function () {
            return new window.SVGPathSegLinetoHorizontalAbs(undefined, this._x);
        };
        Object.defineProperty(window.SVGPathSegLinetoHorizontalAbs.prototype, "x", { get: function get() {
                return this._x;
            }, set: function set(x) {
                this._x = x;this._segmentChanged();
            }, enumerable: true });

        window.SVGPathSegLinetoHorizontalRel = function (owningPathSegList, x) {
            window.SVGPathSeg.call(this, window.SVGPathSeg.PATHSEG_LINETO_HORIZONTAL_REL, "h", owningPathSegList);
            this._x = x;
        };
        window.SVGPathSegLinetoHorizontalRel.prototype = Object.create(window.SVGPathSeg.prototype);
        window.SVGPathSegLinetoHorizontalRel.prototype.toString = function () {
            return "[object SVGPathSegLinetoHorizontalRel]";
        };
        window.SVGPathSegLinetoHorizontalRel.prototype._asPathString = function () {
            return this.pathSegTypeAsLetter + " " + this._x;
        };
        window.SVGPathSegLinetoHorizontalRel.prototype.clone = function () {
            return new window.SVGPathSegLinetoHorizontalRel(undefined, this._x);
        };
        Object.defineProperty(window.SVGPathSegLinetoHorizontalRel.prototype, "x", { get: function get() {
                return this._x;
            }, set: function set(x) {
                this._x = x;this._segmentChanged();
            }, enumerable: true });

        window.SVGPathSegLinetoVerticalAbs = function (owningPathSegList, y) {
            window.SVGPathSeg.call(this, window.SVGPathSeg.PATHSEG_LINETO_VERTICAL_ABS, "V", owningPathSegList);
            this._y = y;
        };
        window.SVGPathSegLinetoVerticalAbs.prototype = Object.create(window.SVGPathSeg.prototype);
        window.SVGPathSegLinetoVerticalAbs.prototype.toString = function () {
            return "[object SVGPathSegLinetoVerticalAbs]";
        };
        window.SVGPathSegLinetoVerticalAbs.prototype._asPathString = function () {
            return this.pathSegTypeAsLetter + " " + this._y;
        };
        window.SVGPathSegLinetoVerticalAbs.prototype.clone = function () {
            return new window.SVGPathSegLinetoVerticalAbs(undefined, this._y);
        };
        Object.defineProperty(window.SVGPathSegLinetoVerticalAbs.prototype, "y", { get: function get() {
                return this._y;
            }, set: function set(y) {
                this._y = y;this._segmentChanged();
            }, enumerable: true });

        window.SVGPathSegLinetoVerticalRel = function (owningPathSegList, y) {
            window.SVGPathSeg.call(this, window.SVGPathSeg.PATHSEG_LINETO_VERTICAL_REL, "v", owningPathSegList);
            this._y = y;
        };
        window.SVGPathSegLinetoVerticalRel.prototype = Object.create(window.SVGPathSeg.prototype);
        window.SVGPathSegLinetoVerticalRel.prototype.toString = function () {
            return "[object SVGPathSegLinetoVerticalRel]";
        };
        window.SVGPathSegLinetoVerticalRel.prototype._asPathString = function () {
            return this.pathSegTypeAsLetter + " " + this._y;
        };
        window.SVGPathSegLinetoVerticalRel.prototype.clone = function () {
            return new window.SVGPathSegLinetoVerticalRel(undefined, this._y);
        };
        Object.defineProperty(window.SVGPathSegLinetoVerticalRel.prototype, "y", { get: function get() {
                return this._y;
            }, set: function set(y) {
                this._y = y;this._segmentChanged();
            }, enumerable: true });

        window.SVGPathSegCurvetoCubicSmoothAbs = function (owningPathSegList, x, y, x2, y2) {
            window.SVGPathSeg.call(this, window.SVGPathSeg.PATHSEG_CURVETO_CUBIC_SMOOTH_ABS, "S", owningPathSegList);
            this._x = x;
            this._y = y;
            this._x2 = x2;
            this._y2 = y2;
        };
        window.SVGPathSegCurvetoCubicSmoothAbs.prototype = Object.create(window.SVGPathSeg.prototype);
        window.SVGPathSegCurvetoCubicSmoothAbs.prototype.toString = function () {
            return "[object SVGPathSegCurvetoCubicSmoothAbs]";
        };
        window.SVGPathSegCurvetoCubicSmoothAbs.prototype._asPathString = function () {
            return this.pathSegTypeAsLetter + " " + this._x2 + " " + this._y2 + " " + this._x + " " + this._y;
        };
        window.SVGPathSegCurvetoCubicSmoothAbs.prototype.clone = function () {
            return new window.SVGPathSegCurvetoCubicSmoothAbs(undefined, this._x, this._y, this._x2, this._y2);
        };
        Object.defineProperty(window.SVGPathSegCurvetoCubicSmoothAbs.prototype, "x", { get: function get() {
                return this._x;
            }, set: function set(x) {
                this._x = x;this._segmentChanged();
            }, enumerable: true });
        Object.defineProperty(window.SVGPathSegCurvetoCubicSmoothAbs.prototype, "y", { get: function get() {
                return this._y;
            }, set: function set(y) {
                this._y = y;this._segmentChanged();
            }, enumerable: true });
        Object.defineProperty(window.SVGPathSegCurvetoCubicSmoothAbs.prototype, "x2", { get: function get() {
                return this._x2;
            }, set: function set(x2) {
                this._x2 = x2;this._segmentChanged();
            }, enumerable: true });
        Object.defineProperty(window.SVGPathSegCurvetoCubicSmoothAbs.prototype, "y2", { get: function get() {
                return this._y2;
            }, set: function set(y2) {
                this._y2 = y2;this._segmentChanged();
            }, enumerable: true });

        window.SVGPathSegCurvetoCubicSmoothRel = function (owningPathSegList, x, y, x2, y2) {
            window.SVGPathSeg.call(this, window.SVGPathSeg.PATHSEG_CURVETO_CUBIC_SMOOTH_REL, "s", owningPathSegList);
            this._x = x;
            this._y = y;
            this._x2 = x2;
            this._y2 = y2;
        };
        window.SVGPathSegCurvetoCubicSmoothRel.prototype = Object.create(window.SVGPathSeg.prototype);
        window.SVGPathSegCurvetoCubicSmoothRel.prototype.toString = function () {
            return "[object SVGPathSegCurvetoCubicSmoothRel]";
        };
        window.SVGPathSegCurvetoCubicSmoothRel.prototype._asPathString = function () {
            return this.pathSegTypeAsLetter + " " + this._x2 + " " + this._y2 + " " + this._x + " " + this._y;
        };
        window.SVGPathSegCurvetoCubicSmoothRel.prototype.clone = function () {
            return new window.SVGPathSegCurvetoCubicSmoothRel(undefined, this._x, this._y, this._x2, this._y2);
        };
        Object.defineProperty(window.SVGPathSegCurvetoCubicSmoothRel.prototype, "x", { get: function get() {
                return this._x;
            }, set: function set(x) {
                this._x = x;this._segmentChanged();
            }, enumerable: true });
        Object.defineProperty(window.SVGPathSegCurvetoCubicSmoothRel.prototype, "y", { get: function get() {
                return this._y;
            }, set: function set(y) {
                this._y = y;this._segmentChanged();
            }, enumerable: true });
        Object.defineProperty(window.SVGPathSegCurvetoCubicSmoothRel.prototype, "x2", { get: function get() {
                return this._x2;
            }, set: function set(x2) {
                this._x2 = x2;this._segmentChanged();
            }, enumerable: true });
        Object.defineProperty(window.SVGPathSegCurvetoCubicSmoothRel.prototype, "y2", { get: function get() {
                return this._y2;
            }, set: function set(y2) {
                this._y2 = y2;this._segmentChanged();
            }, enumerable: true });

        window.SVGPathSegCurvetoQuadraticSmoothAbs = function (owningPathSegList, x, y) {
            window.SVGPathSeg.call(this, window.SVGPathSeg.PATHSEG_CURVETO_QUADRATIC_SMOOTH_ABS, "T", owningPathSegList);
            this._x = x;
            this._y = y;
        };
        window.SVGPathSegCurvetoQuadraticSmoothAbs.prototype = Object.create(window.SVGPathSeg.prototype);
        window.SVGPathSegCurvetoQuadraticSmoothAbs.prototype.toString = function () {
            return "[object SVGPathSegCurvetoQuadraticSmoothAbs]";
        };
        window.SVGPathSegCurvetoQuadraticSmoothAbs.prototype._asPathString = function () {
            return this.pathSegTypeAsLetter + " " + this._x + " " + this._y;
        };
        window.SVGPathSegCurvetoQuadraticSmoothAbs.prototype.clone = function () {
            return new window.SVGPathSegCurvetoQuadraticSmoothAbs(undefined, this._x, this._y);
        };
        Object.defineProperty(window.SVGPathSegCurvetoQuadraticSmoothAbs.prototype, "x", { get: function get() {
                return this._x;
            }, set: function set(x) {
                this._x = x;this._segmentChanged();
            }, enumerable: true });
        Object.defineProperty(window.SVGPathSegCurvetoQuadraticSmoothAbs.prototype, "y", { get: function get() {
                return this._y;
            }, set: function set(y) {
                this._y = y;this._segmentChanged();
            }, enumerable: true });

        window.SVGPathSegCurvetoQuadraticSmoothRel = function (owningPathSegList, x, y) {
            window.SVGPathSeg.call(this, window.SVGPathSeg.PATHSEG_CURVETO_QUADRATIC_SMOOTH_REL, "t", owningPathSegList);
            this._x = x;
            this._y = y;
        };
        window.SVGPathSegCurvetoQuadraticSmoothRel.prototype = Object.create(window.SVGPathSeg.prototype);
        window.SVGPathSegCurvetoQuadraticSmoothRel.prototype.toString = function () {
            return "[object SVGPathSegCurvetoQuadraticSmoothRel]";
        };
        window.SVGPathSegCurvetoQuadraticSmoothRel.prototype._asPathString = function () {
            return this.pathSegTypeAsLetter + " " + this._x + " " + this._y;
        };
        window.SVGPathSegCurvetoQuadraticSmoothRel.prototype.clone = function () {
            return new window.SVGPathSegCurvetoQuadraticSmoothRel(undefined, this._x, this._y);
        };
        Object.defineProperty(window.SVGPathSegCurvetoQuadraticSmoothRel.prototype, "x", { get: function get() {
                return this._x;
            }, set: function set(x) {
                this._x = x;this._segmentChanged();
            }, enumerable: true });
        Object.defineProperty(window.SVGPathSegCurvetoQuadraticSmoothRel.prototype, "y", { get: function get() {
                return this._y;
            }, set: function set(y) {
                this._y = y;this._segmentChanged();
            }, enumerable: true });

        // Add createSVGPathSeg* functions to window.SVGPathElement.
        // Spec: http://www.w3.org/TR/SVG11/single-page.html#paths-Interfacewindow.SVGPathElement.
        window.SVGPathElement.prototype.createSVGPathSegClosePath = function () {
            return new window.SVGPathSegClosePath(undefined);
        };
        window.SVGPathElement.prototype.createSVGPathSegMovetoAbs = function (x, y) {
            return new window.SVGPathSegMovetoAbs(undefined, x, y);
        };
        window.SVGPathElement.prototype.createSVGPathSegMovetoRel = function (x, y) {
            return new window.SVGPathSegMovetoRel(undefined, x, y);
        };
        window.SVGPathElement.prototype.createSVGPathSegLinetoAbs = function (x, y) {
            return new window.SVGPathSegLinetoAbs(undefined, x, y);
        };
        window.SVGPathElement.prototype.createSVGPathSegLinetoRel = function (x, y) {
            return new window.SVGPathSegLinetoRel(undefined, x, y);
        };
        window.SVGPathElement.prototype.createSVGPathSegCurvetoCubicAbs = function (x, y, x1, y1, x2, y2) {
            return new window.SVGPathSegCurvetoCubicAbs(undefined, x, y, x1, y1, x2, y2);
        };
        window.SVGPathElement.prototype.createSVGPathSegCurvetoCubicRel = function (x, y, x1, y1, x2, y2) {
            return new window.SVGPathSegCurvetoCubicRel(undefined, x, y, x1, y1, x2, y2);
        };
        window.SVGPathElement.prototype.createSVGPathSegCurvetoQuadraticAbs = function (x, y, x1, y1) {
            return new window.SVGPathSegCurvetoQuadraticAbs(undefined, x, y, x1, y1);
        };
        window.SVGPathElement.prototype.createSVGPathSegCurvetoQuadraticRel = function (x, y, x1, y1) {
            return new window.SVGPathSegCurvetoQuadraticRel(undefined, x, y, x1, y1);
        };
        window.SVGPathElement.prototype.createSVGPathSegArcAbs = function (x, y, r1, r2, angle, largeArcFlag, sweepFlag) {
            return new window.SVGPathSegArcAbs(undefined, x, y, r1, r2, angle, largeArcFlag, sweepFlag);
        };
        window.SVGPathElement.prototype.createSVGPathSegArcRel = function (x, y, r1, r2, angle, largeArcFlag, sweepFlag) {
            return new window.SVGPathSegArcRel(undefined, x, y, r1, r2, angle, largeArcFlag, sweepFlag);
        };
        window.SVGPathElement.prototype.createSVGPathSegLinetoHorizontalAbs = function (x) {
            return new window.SVGPathSegLinetoHorizontalAbs(undefined, x);
        };
        window.SVGPathElement.prototype.createSVGPathSegLinetoHorizontalRel = function (x) {
            return new window.SVGPathSegLinetoHorizontalRel(undefined, x);
        };
        window.SVGPathElement.prototype.createSVGPathSegLinetoVerticalAbs = function (y) {
            return new window.SVGPathSegLinetoVerticalAbs(undefined, y);
        };
        window.SVGPathElement.prototype.createSVGPathSegLinetoVerticalRel = function (y) {
            return new window.SVGPathSegLinetoVerticalRel(undefined, y);
        };
        window.SVGPathElement.prototype.createSVGPathSegCurvetoCubicSmoothAbs = function (x, y, x2, y2) {
            return new window.SVGPathSegCurvetoCubicSmoothAbs(undefined, x, y, x2, y2);
        };
        window.SVGPathElement.prototype.createSVGPathSegCurvetoCubicSmoothRel = function (x, y, x2, y2) {
            return new window.SVGPathSegCurvetoCubicSmoothRel(undefined, x, y, x2, y2);
        };
        window.SVGPathElement.prototype.createSVGPathSegCurvetoQuadraticSmoothAbs = function (x, y) {
            return new window.SVGPathSegCurvetoQuadraticSmoothAbs(undefined, x, y);
        };
        window.SVGPathElement.prototype.createSVGPathSegCurvetoQuadraticSmoothRel = function (x, y) {
            return new window.SVGPathSegCurvetoQuadraticSmoothRel(undefined, x, y);
        };

        if (!("getPathSegAtLength" in window.SVGPathElement.prototype)) {
            // Add getPathSegAtLength to SVGPathElement.
            // Spec: https://www.w3.org/TR/SVG11/single-page.html#paths-__svg__SVGPathElement__getPathSegAtLength
            // This polyfill requires SVGPathElement.getTotalLength to implement the distance-along-a-path algorithm.
            window.SVGPathElement.prototype.getPathSegAtLength = function (distance) {
                if (distance === undefined || !isFinite(distance)) throw "Invalid arguments.";

                var measurementElement = document.createElementNS("http://www.w3.org/2000/svg", "path");
                measurementElement.setAttribute("d", this.getAttribute("d"));
                var lastPathSegment = measurementElement.pathSegList.numberOfItems - 1;

                // If the path is empty, return 0.
                if (lastPathSegment <= 0) return 0;

                do {
                    measurementElement.pathSegList.removeItem(lastPathSegment);
                    if (distance > measurementElement.getTotalLength()) break;
                    lastPathSegment--;
                } while (lastPathSegment > 0);
                return lastPathSegment;
            };
        }
    }

    if (!("SVGPathSegList" in window)) {
        // Spec: http://www.w3.org/TR/SVG11/single-page.html#paths-InterfaceSVGPathSegList
        window.SVGPathSegList = function (pathElement) {
            this._pathElement = pathElement;
            this._list = this._parsePath(this._pathElement.getAttribute("d"));

            // Use a MutationObserver to catch changes to the path's "d" attribute.
            this._mutationObserverConfig = { "attributes": true, "attributeFilter": ["d"] };
            this._pathElementMutationObserver = new MutationObserver(this._updateListFromPathMutations.bind(this));
            this._pathElementMutationObserver.observe(this._pathElement, this._mutationObserverConfig);
        };

        window.SVGPathSegList.prototype.classname = "SVGPathSegList";

        Object.defineProperty(window.SVGPathSegList.prototype, "numberOfItems", {
            get: function get() {
                this._checkPathSynchronizedToList();
                return this._list.length;
            },
            enumerable: true
        });

        // Add the pathSegList accessors to window.SVGPathElement.
        // Spec: http://www.w3.org/TR/SVG11/single-page.html#paths-InterfaceSVGAnimatedPathData
        Object.defineProperty(window.SVGPathElement.prototype, "pathSegList", {
            get: function get() {
                if (!this._pathSegList) this._pathSegList = new window.SVGPathSegList(this);
                return this._pathSegList;
            },
            enumerable: true
        });
        // FIXME: The following are not implemented and simply return window.SVGPathElement.pathSegList.
        Object.defineProperty(window.SVGPathElement.prototype, "normalizedPathSegList", { get: function get() {
                return this.pathSegList;
            }, enumerable: true });
        Object.defineProperty(window.SVGPathElement.prototype, "animatedPathSegList", { get: function get() {
                return this.pathSegList;
            }, enumerable: true });
        Object.defineProperty(window.SVGPathElement.prototype, "animatedNormalizedPathSegList", { get: function get() {
                return this.pathSegList;
            }, enumerable: true });

        // Process any pending mutations to the path element and update the list as needed.
        // This should be the first call of all public functions and is needed because
        // MutationObservers are not synchronous so we can have pending asynchronous mutations.
        window.SVGPathSegList.prototype._checkPathSynchronizedToList = function () {
            this._updateListFromPathMutations(this._pathElementMutationObserver.takeRecords());
        };

        window.SVGPathSegList.prototype._updateListFromPathMutations = function (mutationRecords) {
            if (!this._pathElement) return;
            var hasPathMutations = false;
            mutationRecords.forEach(function (record) {
                if (record.attributeName == "d") hasPathMutations = true;
            });
            if (hasPathMutations) this._list = this._parsePath(this._pathElement.getAttribute("d"));
        };

        // Serialize the list and update the path's 'd' attribute.
        window.SVGPathSegList.prototype._writeListToPath = function () {
            this._pathElementMutationObserver.disconnect();
            this._pathElement.setAttribute("d", window.SVGPathSegList._pathSegArrayAsString(this._list));
            this._pathElementMutationObserver.observe(this._pathElement, this._mutationObserverConfig);
        };

        // When a path segment changes the list needs to be synchronized back to the path element.
        window.SVGPathSegList.prototype.segmentChanged = function (pathSeg) {
            this._writeListToPath();
        };

        window.SVGPathSegList.prototype.clear = function () {
            this._checkPathSynchronizedToList();

            this._list.forEach(function (pathSeg) {
                pathSeg._owningPathSegList = null;
            });
            this._list = [];
            this._writeListToPath();
        };

        window.SVGPathSegList.prototype.initialize = function (newItem) {
            this._checkPathSynchronizedToList();

            this._list = [newItem];
            newItem._owningPathSegList = this;
            this._writeListToPath();
            return newItem;
        };

        window.SVGPathSegList.prototype._checkValidIndex = function (index) {
            if (isNaN(index) || index < 0 || index >= this.numberOfItems) throw "INDEX_SIZE_ERR";
        };

        window.SVGPathSegList.prototype.getItem = function (index) {
            this._checkPathSynchronizedToList();

            this._checkValidIndex(index);
            return this._list[index];
        };

        window.SVGPathSegList.prototype.insertItemBefore = function (newItem, index) {
            this._checkPathSynchronizedToList();

            // Spec: If the index is greater than or equal to numberOfItems, then the new item is appended to the end of the list.
            if (index > this.numberOfItems) index = this.numberOfItems;
            if (newItem._owningPathSegList) {
                // SVG2 spec says to make a copy.
                newItem = newItem.clone();
            }
            this._list.splice(index, 0, newItem);
            newItem._owningPathSegList = this;
            this._writeListToPath();
            return newItem;
        };

        window.SVGPathSegList.prototype.replaceItem = function (newItem, index) {
            this._checkPathSynchronizedToList();

            if (newItem._owningPathSegList) {
                // SVG2 spec says to make a copy.
                newItem = newItem.clone();
            }
            this._checkValidIndex(index);
            this._list[index] = newItem;
            newItem._owningPathSegList = this;
            this._writeListToPath();
            return newItem;
        };

        window.SVGPathSegList.prototype.removeItem = function (index) {
            this._checkPathSynchronizedToList();

            this._checkValidIndex(index);
            var item = this._list[index];
            this._list.splice(index, 1);
            this._writeListToPath();
            return item;
        };

        window.SVGPathSegList.prototype.appendItem = function (newItem) {
            this._checkPathSynchronizedToList();

            if (newItem._owningPathSegList) {
                // SVG2 spec says to make a copy.
                newItem = newItem.clone();
            }
            this._list.push(newItem);
            newItem._owningPathSegList = this;
            // TODO: Optimize this to just append to the existing attribute.
            this._writeListToPath();
            return newItem;
        };

        window.SVGPathSegList._pathSegArrayAsString = function (pathSegArray) {
            var string = "";
            var first = true;
            pathSegArray.forEach(function (pathSeg) {
                if (first) {
                    first = false;
                    string += pathSeg._asPathString();
                } else {
                    string += " " + pathSeg._asPathString();
                }
            });
            return string;
        };

        // This closely follows SVGPathParser::parsePath from Source/core/svg/SVGPathParser.cpp.
        window.SVGPathSegList.prototype._parsePath = function (string) {
            if (!string || string.length == 0) return [];

            var owningPathSegList = this;

            var Builder = function Builder() {
                this.pathSegList = [];
            };

            Builder.prototype.appendSegment = function (pathSeg) {
                this.pathSegList.push(pathSeg);
            };

            var Source = function Source(string) {
                this._string = string;
                this._currentIndex = 0;
                this._endIndex = this._string.length;
                this._previousCommand = window.SVGPathSeg.PATHSEG_UNKNOWN;

                this._skipOptionalSpaces();
            };

            Source.prototype._isCurrentSpace = function () {
                var character = this._string[this._currentIndex];
                return character <= " " && (character == " " || character == "\n" || character == "\t" || character == "\r" || character == "\f");
            };

            Source.prototype._skipOptionalSpaces = function () {
                while (this._currentIndex < this._endIndex && this._isCurrentSpace()) {
                    this._currentIndex++;
                }return this._currentIndex < this._endIndex;
            };

            Source.prototype._skipOptionalSpacesOrDelimiter = function () {
                if (this._currentIndex < this._endIndex && !this._isCurrentSpace() && this._string.charAt(this._currentIndex) != ",") return false;
                if (this._skipOptionalSpaces()) {
                    if (this._currentIndex < this._endIndex && this._string.charAt(this._currentIndex) == ",") {
                        this._currentIndex++;
                        this._skipOptionalSpaces();
                    }
                }
                return this._currentIndex < this._endIndex;
            };

            Source.prototype.hasMoreData = function () {
                return this._currentIndex < this._endIndex;
            };

            Source.prototype.peekSegmentType = function () {
                var lookahead = this._string[this._currentIndex];
                return this._pathSegTypeFromChar(lookahead);
            };

            Source.prototype._pathSegTypeFromChar = function (lookahead) {
                switch (lookahead) {
                    case "Z":
                    case "z":
                        return window.SVGPathSeg.PATHSEG_CLOSEPATH;
                    case "M":
                        return window.SVGPathSeg.PATHSEG_MOVETO_ABS;
                    case "m":
                        return window.SVGPathSeg.PATHSEG_MOVETO_REL;
                    case "L":
                        return window.SVGPathSeg.PATHSEG_LINETO_ABS;
                    case "l":
                        return window.SVGPathSeg.PATHSEG_LINETO_REL;
                    case "C":
                        return window.SVGPathSeg.PATHSEG_CURVETO_CUBIC_ABS;
                    case "c":
                        return window.SVGPathSeg.PATHSEG_CURVETO_CUBIC_REL;
                    case "Q":
                        return window.SVGPathSeg.PATHSEG_CURVETO_QUADRATIC_ABS;
                    case "q":
                        return window.SVGPathSeg.PATHSEG_CURVETO_QUADRATIC_REL;
                    case "A":
                        return window.SVGPathSeg.PATHSEG_ARC_ABS;
                    case "a":
                        return window.SVGPathSeg.PATHSEG_ARC_REL;
                    case "H":
                        return window.SVGPathSeg.PATHSEG_LINETO_HORIZONTAL_ABS;
                    case "h":
                        return window.SVGPathSeg.PATHSEG_LINETO_HORIZONTAL_REL;
                    case "V":
                        return window.SVGPathSeg.PATHSEG_LINETO_VERTICAL_ABS;
                    case "v":
                        return window.SVGPathSeg.PATHSEG_LINETO_VERTICAL_REL;
                    case "S":
                        return window.SVGPathSeg.PATHSEG_CURVETO_CUBIC_SMOOTH_ABS;
                    case "s":
                        return window.SVGPathSeg.PATHSEG_CURVETO_CUBIC_SMOOTH_REL;
                    case "T":
                        return window.SVGPathSeg.PATHSEG_CURVETO_QUADRATIC_SMOOTH_ABS;
                    case "t":
                        return window.SVGPathSeg.PATHSEG_CURVETO_QUADRATIC_SMOOTH_REL;
                    default:
                        return window.SVGPathSeg.PATHSEG_UNKNOWN;
                }
            };

            Source.prototype._nextCommandHelper = function (lookahead, previousCommand) {
                // Check for remaining coordinates in the current command.
                if ((lookahead == "+" || lookahead == "-" || lookahead == "." || lookahead >= "0" && lookahead <= "9") && previousCommand != window.SVGPathSeg.PATHSEG_CLOSEPATH) {
                    if (previousCommand == window.SVGPathSeg.PATHSEG_MOVETO_ABS) return window.SVGPathSeg.PATHSEG_LINETO_ABS;
                    if (previousCommand == window.SVGPathSeg.PATHSEG_MOVETO_REL) return window.SVGPathSeg.PATHSEG_LINETO_REL;
                    return previousCommand;
                }
                return window.SVGPathSeg.PATHSEG_UNKNOWN;
            };

            Source.prototype.initialCommandIsMoveTo = function () {
                // If the path is empty it is still valid, so return true.
                if (!this.hasMoreData()) return true;
                var command = this.peekSegmentType();
                // Path must start with moveTo.
                return command == window.SVGPathSeg.PATHSEG_MOVETO_ABS || command == window.SVGPathSeg.PATHSEG_MOVETO_REL;
            };

            // Parse a number from an SVG path. This very closely follows genericParseNumber(...) from Source/core/svg/SVGParserUtilities.cpp.
            // Spec: http://www.w3.org/TR/SVG11/single-page.html#paths-PathDataBNF
            Source.prototype._parseNumber = function () {
                var exponent = 0;
                var integer = 0;
                var frac = 1;
                var decimal = 0;
                var sign = 1;
                var expsign = 1;

                var startIndex = this._currentIndex;

                this._skipOptionalSpaces();

                // Read the sign.
                if (this._currentIndex < this._endIndex && this._string.charAt(this._currentIndex) == "+") this._currentIndex++;else if (this._currentIndex < this._endIndex && this._string.charAt(this._currentIndex) == "-") {
                    this._currentIndex++;
                    sign = -1;
                }

                if (this._currentIndex == this._endIndex || (this._string.charAt(this._currentIndex) < "0" || this._string.charAt(this._currentIndex) > "9") && this._string.charAt(this._currentIndex) != ".")
                    // The first character of a number must be one of [0-9+-.].
                    return undefined;

                // Read the integer part, build right-to-left.
                var startIntPartIndex = this._currentIndex;
                while (this._currentIndex < this._endIndex && this._string.charAt(this._currentIndex) >= "0" && this._string.charAt(this._currentIndex) <= "9") {
                    this._currentIndex++;
                } // Advance to first non-digit.

                if (this._currentIndex != startIntPartIndex) {
                    var scanIntPartIndex = this._currentIndex - 1;
                    var multiplier = 1;
                    while (scanIntPartIndex >= startIntPartIndex) {
                        integer += multiplier * (this._string.charAt(scanIntPartIndex--) - "0");
                        multiplier *= 10;
                    }
                }

                // Read the decimals.
                if (this._currentIndex < this._endIndex && this._string.charAt(this._currentIndex) == ".") {
                    this._currentIndex++;

                    // There must be a least one digit following the .
                    if (this._currentIndex >= this._endIndex || this._string.charAt(this._currentIndex) < "0" || this._string.charAt(this._currentIndex) > "9") return undefined;
                    while (this._currentIndex < this._endIndex && this._string.charAt(this._currentIndex) >= "0" && this._string.charAt(this._currentIndex) <= "9") {
                        frac *= 10;
                        decimal += (this._string.charAt(this._currentIndex) - "0") / frac;
                        this._currentIndex += 1;
                    }
                }

                // Read the exponent part.
                if (this._currentIndex != startIndex && this._currentIndex + 1 < this._endIndex && (this._string.charAt(this._currentIndex) == "e" || this._string.charAt(this._currentIndex) == "E") && this._string.charAt(this._currentIndex + 1) != "x" && this._string.charAt(this._currentIndex + 1) != "m") {
                    this._currentIndex++;

                    // Read the sign of the exponent.
                    if (this._string.charAt(this._currentIndex) == "+") {
                        this._currentIndex++;
                    } else if (this._string.charAt(this._currentIndex) == "-") {
                        this._currentIndex++;
                        expsign = -1;
                    }

                    // There must be an exponent.
                    if (this._currentIndex >= this._endIndex || this._string.charAt(this._currentIndex) < "0" || this._string.charAt(this._currentIndex) > "9") return undefined;

                    while (this._currentIndex < this._endIndex && this._string.charAt(this._currentIndex) >= "0" && this._string.charAt(this._currentIndex) <= "9") {
                        exponent *= 10;
                        exponent += this._string.charAt(this._currentIndex) - "0";
                        this._currentIndex++;
                    }
                }

                var number = integer + decimal;
                number *= sign;

                if (exponent) number *= Math.pow(10, expsign * exponent);

                if (startIndex == this._currentIndex) return undefined;

                this._skipOptionalSpacesOrDelimiter();

                return number;
            };

            Source.prototype._parseArcFlag = function () {
                if (this._currentIndex >= this._endIndex) return undefined;
                var flag = false;
                var flagChar = this._string.charAt(this._currentIndex++);
                if (flagChar == "0") flag = false;else if (flagChar == "1") flag = true;else return undefined;

                this._skipOptionalSpacesOrDelimiter();
                return flag;
            };

            Source.prototype.parseSegment = function () {
                var lookahead = this._string[this._currentIndex];
                var command = this._pathSegTypeFromChar(lookahead);
                if (command == window.SVGPathSeg.PATHSEG_UNKNOWN) {
                    // Possibly an implicit command. Not allowed if this is the first command.
                    if (this._previousCommand == window.SVGPathSeg.PATHSEG_UNKNOWN) return null;
                    command = this._nextCommandHelper(lookahead, this._previousCommand);
                    if (command == window.SVGPathSeg.PATHSEG_UNKNOWN) return null;
                } else {
                    this._currentIndex++;
                }

                this._previousCommand = command;

                switch (command) {
                    case window.SVGPathSeg.PATHSEG_MOVETO_REL:
                        return new window.SVGPathSegMovetoRel(owningPathSegList, this._parseNumber(), this._parseNumber());
                    case window.SVGPathSeg.PATHSEG_MOVETO_ABS:
                        return new window.SVGPathSegMovetoAbs(owningPathSegList, this._parseNumber(), this._parseNumber());
                    case window.SVGPathSeg.PATHSEG_LINETO_REL:
                        return new window.SVGPathSegLinetoRel(owningPathSegList, this._parseNumber(), this._parseNumber());
                    case window.SVGPathSeg.PATHSEG_LINETO_ABS:
                        return new window.SVGPathSegLinetoAbs(owningPathSegList, this._parseNumber(), this._parseNumber());
                    case window.SVGPathSeg.PATHSEG_LINETO_HORIZONTAL_REL:
                        return new window.SVGPathSegLinetoHorizontalRel(owningPathSegList, this._parseNumber());
                    case window.SVGPathSeg.PATHSEG_LINETO_HORIZONTAL_ABS:
                        return new window.SVGPathSegLinetoHorizontalAbs(owningPathSegList, this._parseNumber());
                    case window.SVGPathSeg.PATHSEG_LINETO_VERTICAL_REL:
                        return new window.SVGPathSegLinetoVerticalRel(owningPathSegList, this._parseNumber());
                    case window.SVGPathSeg.PATHSEG_LINETO_VERTICAL_ABS:
                        return new window.SVGPathSegLinetoVerticalAbs(owningPathSegList, this._parseNumber());
                    case window.SVGPathSeg.PATHSEG_CLOSEPATH:
                        this._skipOptionalSpaces();
                        return new window.SVGPathSegClosePath(owningPathSegList);
                    case window.SVGPathSeg.PATHSEG_CURVETO_CUBIC_REL:
                        var points = { x1: this._parseNumber(), y1: this._parseNumber(), x2: this._parseNumber(), y2: this._parseNumber(), x: this._parseNumber(), y: this._parseNumber() };
                        return new window.SVGPathSegCurvetoCubicRel(owningPathSegList, points.x, points.y, points.x1, points.y1, points.x2, points.y2);
                    case window.SVGPathSeg.PATHSEG_CURVETO_CUBIC_ABS:
                        var points = { x1: this._parseNumber(), y1: this._parseNumber(), x2: this._parseNumber(), y2: this._parseNumber(), x: this._parseNumber(), y: this._parseNumber() };
                        return new window.SVGPathSegCurvetoCubicAbs(owningPathSegList, points.x, points.y, points.x1, points.y1, points.x2, points.y2);
                    case window.SVGPathSeg.PATHSEG_CURVETO_CUBIC_SMOOTH_REL:
                        var points = { x2: this._parseNumber(), y2: this._parseNumber(), x: this._parseNumber(), y: this._parseNumber() };
                        return new window.SVGPathSegCurvetoCubicSmoothRel(owningPathSegList, points.x, points.y, points.x2, points.y2);
                    case window.SVGPathSeg.PATHSEG_CURVETO_CUBIC_SMOOTH_ABS:
                        var points = { x2: this._parseNumber(), y2: this._parseNumber(), x: this._parseNumber(), y: this._parseNumber() };
                        return new window.SVGPathSegCurvetoCubicSmoothAbs(owningPathSegList, points.x, points.y, points.x2, points.y2);
                    case window.SVGPathSeg.PATHSEG_CURVETO_QUADRATIC_REL:
                        var points = { x1: this._parseNumber(), y1: this._parseNumber(), x: this._parseNumber(), y: this._parseNumber() };
                        return new window.SVGPathSegCurvetoQuadraticRel(owningPathSegList, points.x, points.y, points.x1, points.y1);
                    case window.SVGPathSeg.PATHSEG_CURVETO_QUADRATIC_ABS:
                        var points = { x1: this._parseNumber(), y1: this._parseNumber(), x: this._parseNumber(), y: this._parseNumber() };
                        return new window.SVGPathSegCurvetoQuadraticAbs(owningPathSegList, points.x, points.y, points.x1, points.y1);
                    case window.SVGPathSeg.PATHSEG_CURVETO_QUADRATIC_SMOOTH_REL:
                        return new window.SVGPathSegCurvetoQuadraticSmoothRel(owningPathSegList, this._parseNumber(), this._parseNumber());
                    case window.SVGPathSeg.PATHSEG_CURVETO_QUADRATIC_SMOOTH_ABS:
                        return new window.SVGPathSegCurvetoQuadraticSmoothAbs(owningPathSegList, this._parseNumber(), this._parseNumber());
                    case window.SVGPathSeg.PATHSEG_ARC_REL:
                        var points = { x1: this._parseNumber(), y1: this._parseNumber(), arcAngle: this._parseNumber(), arcLarge: this._parseArcFlag(), arcSweep: this._parseArcFlag(), x: this._parseNumber(), y: this._parseNumber() };
                        return new window.SVGPathSegArcRel(owningPathSegList, points.x, points.y, points.x1, points.y1, points.arcAngle, points.arcLarge, points.arcSweep);
                    case window.SVGPathSeg.PATHSEG_ARC_ABS:
                        var points = { x1: this._parseNumber(), y1: this._parseNumber(), arcAngle: this._parseNumber(), arcLarge: this._parseArcFlag(), arcSweep: this._parseArcFlag(), x: this._parseNumber(), y: this._parseNumber() };
                        return new window.SVGPathSegArcAbs(owningPathSegList, points.x, points.y, points.x1, points.y1, points.arcAngle, points.arcLarge, points.arcSweep);
                    default:
                        throw "Unknown path seg type.";
                }
            };

            var builder = new Builder();
            var source = new Source(string);

            if (!source.initialCommandIsMoveTo()) return [];
            while (source.hasMoreData()) {
                var pathSeg = source.parseSegment();
                if (!pathSeg) return [];
                builder.appendSegment(pathSeg);
            }

            return builder.pathSegList;
        };
    }
})();

/* jshint ignore:end */

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
    var $$ = this.internal,
        config = $$.config;
    if (arguments.length) {
        if ((typeof max === 'undefined' ? 'undefined' : _typeof(max)) === 'object') {
            if (isValue(max.x)) {
                config.axis_x_max = max.x;
            }
            if (isValue(max.y)) {
                config.axis_y_max = max.y;
            }
            if (isValue(max.y2)) {
                config.axis_y2_max = max.y2;
            }
        } else {
            config.axis_y_max = config.axis_y2_max = max;
        }
        $$.redraw({ withUpdateOrgXDomain: true, withUpdateXDomain: true });
    } else {
        return {
            x: config.axis_x_max,
            y: config.axis_y_max,
            y2: config.axis_y2_max
        };
    }
};
c3_chart_fn.axis.min = function (min) {
    var $$ = this.internal,
        config = $$.config;
    if (arguments.length) {
        if ((typeof min === 'undefined' ? 'undefined' : _typeof(min)) === 'object') {
            if (isValue(min.x)) {
                config.axis_x_min = min.x;
            }
            if (isValue(min.y)) {
                config.axis_y_min = min.y;
            }
            if (isValue(min.y2)) {
                config.axis_y2_min = min.y2;
            }
        } else {
            config.axis_y_min = config.axis_y2_min = min;
        }
        $$.redraw({ withUpdateOrgXDomain: true, withUpdateXDomain: true });
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
        if (isDefined(range.max)) {
            this.axis.max(range.max);
        }
        if (isDefined(range.min)) {
            this.axis.min(range.min);
        }
    } else {
        return {
            max: this.axis.max(),
            min: this.axis.min()
        };
    }
};

c3_chart_fn.category = function (i, category) {
    var $$ = this.internal,
        config = $$.config;
    if (arguments.length > 1) {
        config.axis_x_categories[i] = category;
        $$.redraw();
    }
    return config.axis_x_categories[i];
};
c3_chart_fn.categories = function (categories) {
    var $$ = this.internal,
        config = $$.config;
    if (!arguments.length) {
        return config.axis_x_categories;
    }
    config.axis_x_categories = categories;
    $$.redraw();
    return config.axis_x_categories;
};

c3_chart_fn.resize = function (size) {
    var $$ = this.internal,
        config = $$.config;
    config.size_width = size ? size.width : null;
    config.size_height = size ? size.height : null;
    this.flush();
};

c3_chart_fn.flush = function () {
    var $$ = this.internal;
    $$.updateAndRedraw({ withLegend: true, withTransition: false, withTransitionForTransform: false });
};

c3_chart_fn.destroy = function () {
    var $$ = this.internal;

    window.clearInterval($$.intervalForObserveInserted);

    if ($$.resizeTimeout !== undefined) {
        window.clearTimeout($$.resizeTimeout);
    }

    if (window.detachEvent) {
        window.detachEvent('onresize', $$.resizeFunction);
    } else if (window.removeEventListener) {
        window.removeEventListener('resize', $$.resizeFunction);
    } else {
        var wrapper = window.onresize;
        // check if no one else removed our wrapper and remove our resizeFunction from it
        if (wrapper && wrapper.add && wrapper.remove) {
            wrapper.remove($$.resizeFunction);
        }
    }

    $$.selectChart.classed('c3', false).html("");

    // MEMO: this is needed because the reference of some elements will not be released, then memory leak will happen.
    Object.keys($$).forEach(function (key) {
        $$[key] = null;
    });

    return null;
};

// TODO: fix
c3_chart_fn.color = function (id) {
    var $$ = this.internal;
    return $$.color(id); // more patterns
};

c3_chart_fn.data = function (targetIds) {
    var targets = this.internal.data.targets;
    return typeof targetIds === 'undefined' ? targets : targets.filter(function (t) {
        return [].concat(targetIds).indexOf(t.id) >= 0;
    });
};
c3_chart_fn.data.shown = function (targetIds) {
    return this.internal.filterTargetsToShow(this.data(targetIds));
};
c3_chart_fn.data.values = function (targetId) {
    var targets,
        values = null;
    if (targetId) {
        targets = this.data(targetId);
        values = targets[0] ? targets[0].values.map(function (d) {
            return d.value;
        }) : null;
    }
    return values;
};
c3_chart_fn.data.names = function (names) {
    this.internal.clearLegendItemTextBoxCache();
    return this.internal.updateDataAttributes('names', names);
};
c3_chart_fn.data.colors = function (colors) {
    return this.internal.updateDataAttributes('colors', colors);
};
c3_chart_fn.data.axes = function (axes) {
    return this.internal.updateDataAttributes('axes', axes);
};

c3_chart_fn.flow = function (args) {
    var $$ = this.internal,
        targets,
        data,
        notfoundIds = [],
        orgDataCount = $$.getMaxDataCount(),
        dataCount,
        domain,
        baseTarget,
        baseValue,
        length = 0,
        tail = 0,
        diff,
        to;

    if (args.json) {
        data = $$.convertJsonToData(args.json, args.keys);
    } else if (args.rows) {
        data = $$.convertRowsToData(args.rows);
    } else if (args.columns) {
        data = $$.convertColumnsToData(args.columns);
    } else {
        return;
    }
    targets = $$.convertDataToTargets(data, true);

    // Update/Add data
    $$.data.targets.forEach(function (t) {
        var found = false,
            i,
            j;
        for (i = 0; i < targets.length; i++) {
            if (t.id === targets[i].id) {
                found = true;

                if (t.values[t.values.length - 1]) {
                    tail = t.values[t.values.length - 1].index + 1;
                }
                length = targets[i].values.length;

                for (j = 0; j < length; j++) {
                    targets[i].values[j].index = tail + j;
                    if (!$$.isTimeSeries()) {
                        targets[i].values[j].x = tail + j;
                    }
                }
                t.values = t.values.concat(targets[i].values);

                targets.splice(i, 1);
                break;
            }
        }
        if (!found) {
            notfoundIds.push(t.id);
        }
    });

    // Append null for not found targets
    $$.data.targets.forEach(function (t) {
        var i, j;
        for (i = 0; i < notfoundIds.length; i++) {
            if (t.id === notfoundIds[i]) {
                tail = t.values[t.values.length - 1].index + 1;
                for (j = 0; j < length; j++) {
                    t.values.push({
                        id: t.id,
                        index: tail + j,
                        x: $$.isTimeSeries() ? $$.getOtherTargetX(tail + j) : tail + j,
                        value: null
                    });
                }
            }
        }
    });

    // Generate null values for new target
    if ($$.data.targets.length) {
        targets.forEach(function (t) {
            var i,
                missing = [];
            for (i = $$.data.targets[0].values[0].index; i < tail; i++) {
                missing.push({
                    id: t.id,
                    index: i,
                    x: $$.isTimeSeries() ? $$.getOtherTargetX(i) : i,
                    value: null
                });
            }
            t.values.forEach(function (v) {
                v.index += tail;
                if (!$$.isTimeSeries()) {
                    v.x += tail;
                }
            });
            t.values = missing.concat(t.values);
        });
    }
    $$.data.targets = $$.data.targets.concat(targets); // add remained

    // check data count because behavior needs to change when it's only one
    dataCount = $$.getMaxDataCount();
    baseTarget = $$.data.targets[0];
    baseValue = baseTarget.values[0];

    // Update length to flow if needed
    if (isDefined(args.to)) {
        length = 0;
        to = $$.isTimeSeries() ? $$.parseDate(args.to) : args.to;
        baseTarget.values.forEach(function (v) {
            if (v.x < to) {
                length++;
            }
        });
    } else if (isDefined(args.length)) {
        length = args.length;
    }

    // If only one data, update the domain to flow from left edge of the chart
    if (!orgDataCount) {
        if ($$.isTimeSeries()) {
            if (baseTarget.values.length > 1) {
                diff = baseTarget.values[baseTarget.values.length - 1].x - baseValue.x;
            } else {
                diff = baseValue.x - $$.getXDomain($$.data.targets)[0];
            }
        } else {
            diff = 1;
        }
        domain = [baseValue.x - diff, baseValue.x];
        $$.updateXDomain(null, true, true, false, domain);
    } else if (orgDataCount === 1) {
        if ($$.isTimeSeries()) {
            diff = (baseTarget.values[baseTarget.values.length - 1].x - baseValue.x) / 2;
            domain = [new Date(+baseValue.x - diff), new Date(+baseValue.x + diff)];
            $$.updateXDomain(null, true, true, false, domain);
        }
    }

    // Set targets
    $$.updateTargets($$.data.targets);

    // Redraw with new targets
    $$.redraw({
        flow: {
            index: baseValue.index,
            length: length,
            duration: isValue(args.duration) ? args.duration : $$.config.transition_duration,
            done: args.done,
            orgDataCount: orgDataCount
        },
        withLegend: true,
        withTransition: orgDataCount > 1,
        withTrimXDomain: false,
        withUpdateXAxis: true
    });
};

c3_chart_internal_fn.generateFlow = function (args) {
    var $$ = this,
        config = $$.config,
        d3 = $$.d3;

    return function () {
        var targets = args.targets,
            flow = args.flow,
            drawBar = args.drawBar,
            drawLine = args.drawLine,
            drawArea = args.drawArea,
            cx = args.cx,
            cy = args.cy,
            xv = args.xv,
            xForText = args.xForText,
            yForText = args.yForText,
            duration = args.duration;

        var translateX,
            scaleX = 1,
            transform,
            flowIndex = flow.index,
            flowLength = flow.length,
            flowStart = $$.getValueOnIndex($$.data.targets[0].values, flowIndex),
            flowEnd = $$.getValueOnIndex($$.data.targets[0].values, flowIndex + flowLength),
            orgDomain = $$.x.domain(),
            domain,
            durationForFlow = flow.duration || duration,
            done = flow.done || function () {},
            wait = $$.generateWait();

        var xgrid = $$.xgrid || d3.selectAll([]),
            xgridLines = $$.xgridLines || d3.selectAll([]),
            mainRegion = $$.mainRegion || d3.selectAll([]),
            mainText = $$.mainText || d3.selectAll([]),
            mainBar = $$.mainBar || d3.selectAll([]),
            mainLine = $$.mainLine || d3.selectAll([]),
            mainArea = $$.mainArea || d3.selectAll([]),
            mainCircle = $$.mainCircle || d3.selectAll([]);

        // set flag
        $$.flowing = true;

        // remove head data after rendered
        $$.data.targets.forEach(function (d) {
            d.values.splice(0, flowLength);
        });

        // update x domain to generate axis elements for flow
        domain = $$.updateXDomain(targets, true, true);
        // update elements related to x scale
        if ($$.updateXGrid) {
            $$.updateXGrid(true);
        }

        // generate transform to flow
        if (!flow.orgDataCount) {
            // if empty
            if ($$.data.targets[0].values.length !== 1) {
                translateX = $$.x(orgDomain[0]) - $$.x(domain[0]);
            } else {
                if ($$.isTimeSeries()) {
                    flowStart = $$.getValueOnIndex($$.data.targets[0].values, 0);
                    flowEnd = $$.getValueOnIndex($$.data.targets[0].values, $$.data.targets[0].values.length - 1);
                    translateX = $$.x(flowStart.x) - $$.x(flowEnd.x);
                } else {
                    translateX = diffDomain(domain) / 2;
                }
            }
        } else if (flow.orgDataCount === 1 || (flowStart && flowStart.x) === (flowEnd && flowEnd.x)) {
            translateX = $$.x(orgDomain[0]) - $$.x(domain[0]);
        } else {
            if ($$.isTimeSeries()) {
                translateX = $$.x(orgDomain[0]) - $$.x(domain[0]);
            } else {
                translateX = $$.x(flowStart.x) - $$.x(flowEnd.x);
            }
        }
        scaleX = diffDomain(orgDomain) / diffDomain(domain);
        transform = 'translate(' + translateX + ',0) scale(' + scaleX + ',1)';

        $$.hideXGridFocus();

        d3.transition().ease('linear').duration(durationForFlow).each(function () {
            wait.add($$.axes.x.transition().call($$.xAxis));
            wait.add(mainBar.transition().attr('transform', transform));
            wait.add(mainLine.transition().attr('transform', transform));
            wait.add(mainArea.transition().attr('transform', transform));
            wait.add(mainCircle.transition().attr('transform', transform));
            wait.add(mainText.transition().attr('transform', transform));
            wait.add(mainRegion.filter($$.isRegionOnX).transition().attr('transform', transform));
            wait.add(xgrid.transition().attr('transform', transform));
            wait.add(xgridLines.transition().attr('transform', transform));
        }).call(wait, function () {
            var i,
                shapes = [],
                texts = [],
                eventRects = [];

            // remove flowed elements
            if (flowLength) {
                for (i = 0; i < flowLength; i++) {
                    shapes.push('.' + CLASS.shape + '-' + (flowIndex + i));
                    texts.push('.' + CLASS.text + '-' + (flowIndex + i));
                    eventRects.push('.' + CLASS.eventRect + '-' + (flowIndex + i));
                }
                $$.svg.selectAll('.' + CLASS.shapes).selectAll(shapes).remove();
                $$.svg.selectAll('.' + CLASS.texts).selectAll(texts).remove();
                $$.svg.selectAll('.' + CLASS.eventRects).selectAll(eventRects).remove();
                $$.svg.select('.' + CLASS.xgrid).remove();
            }

            // draw again for removing flowed elements and reverting attr
            xgrid.attr('transform', null).attr($$.xgridAttr);
            xgridLines.attr('transform', null);
            xgridLines.select('line').attr("x1", config.axis_rotated ? 0 : xv).attr("x2", config.axis_rotated ? $$.width : xv);
            xgridLines.select('text').attr("x", config.axis_rotated ? $$.width : 0).attr("y", xv);
            mainBar.attr('transform', null).attr("d", drawBar);
            mainLine.attr('transform', null).attr("d", drawLine);
            mainArea.attr('transform', null).attr("d", drawArea);
            mainCircle.attr('transform', null).attr("cx", cx).attr("cy", cy);
            mainText.attr('transform', null).attr('x', xForText).attr('y', yForText).style('fill-opacity', $$.opacityForText.bind($$));
            mainRegion.attr('transform', null);
            mainRegion.select('rect').filter($$.isRegionOnX).attr("x", $$.regionX.bind($$)).attr("width", $$.regionWidth.bind($$));

            if (config.interaction_enabled) {
                $$.redrawEventRect();
            }

            // callback for end of flow
            done();

            $$.flowing = false;
        });
    };
};

c3_chart_fn.focus = function (targetIds) {
    var $$ = this.internal,
        candidates;

    targetIds = $$.mapToTargetIds(targetIds);
    candidates = $$.svg.selectAll($$.selectorTargets(targetIds.filter($$.isTargetToShow, $$))), this.revert();
    this.defocus();
    candidates.classed(CLASS.focused, true).classed(CLASS.defocused, false);
    if ($$.hasArcType()) {
        $$.expandArc(targetIds);
    }
    $$.toggleFocusLegend(targetIds, true);

    $$.focusedTargetIds = targetIds;
    $$.defocusedTargetIds = $$.defocusedTargetIds.filter(function (id) {
        return targetIds.indexOf(id) < 0;
    });
};

c3_chart_fn.defocus = function (targetIds) {
    var $$ = this.internal,
        candidates;

    targetIds = $$.mapToTargetIds(targetIds);
    candidates = $$.svg.selectAll($$.selectorTargets(targetIds.filter($$.isTargetToShow, $$))), candidates.classed(CLASS.focused, false).classed(CLASS.defocused, true);
    if ($$.hasArcType()) {
        $$.unexpandArc(targetIds);
    }
    $$.toggleFocusLegend(targetIds, false);

    $$.focusedTargetIds = $$.focusedTargetIds.filter(function (id) {
        return targetIds.indexOf(id) < 0;
    });
    $$.defocusedTargetIds = targetIds;
};

c3_chart_fn.revert = function (targetIds) {
    var $$ = this.internal,
        candidates;

    targetIds = $$.mapToTargetIds(targetIds);
    candidates = $$.svg.selectAll($$.selectorTargets(targetIds)); // should be for all targets

    candidates.classed(CLASS.focused, false).classed(CLASS.defocused, false);
    if ($$.hasArcType()) {
        $$.unexpandArc(targetIds);
    }
    if ($$.config.legend_show) {
        $$.showLegend(targetIds.filter($$.isLegendToShow.bind($$)));
        $$.legend.selectAll($$.selectorLegends(targetIds)).filter(function () {
            return $$.d3.select(this).classed(CLASS.legendItemFocused);
        }).classed(CLASS.legendItemFocused, false);
    }

    $$.focusedTargetIds = [];
    $$.defocusedTargetIds = [];
};

c3_chart_fn.xgrids = function (grids) {
    var $$ = this.internal,
        config = $$.config;
    if (!grids) {
        return config.grid_x_lines;
    }
    config.grid_x_lines = grids;
    $$.redrawWithoutRescale();
    return config.grid_x_lines;
};
c3_chart_fn.xgrids.add = function (grids) {
    var $$ = this.internal;
    return this.xgrids($$.config.grid_x_lines.concat(grids ? grids : []));
};
c3_chart_fn.xgrids.remove = function (params) {
    // TODO: multiple
    var $$ = this.internal;
    $$.removeGridLines(params, true);
};

c3_chart_fn.ygrids = function (grids) {
    var $$ = this.internal,
        config = $$.config;
    if (!grids) {
        return config.grid_y_lines;
    }
    config.grid_y_lines = grids;
    $$.redrawWithoutRescale();
    return config.grid_y_lines;
};
c3_chart_fn.ygrids.add = function (grids) {
    var $$ = this.internal;
    return this.ygrids($$.config.grid_y_lines.concat(grids ? grids : []));
};
c3_chart_fn.ygrids.remove = function (params) {
    // TODO: multiple
    var $$ = this.internal;
    $$.removeGridLines(params, false);
};

c3_chart_fn.groups = function (groups) {
    var $$ = this.internal,
        config = $$.config;
    if (isUndefined(groups)) {
        return config.data_groups;
    }
    config.data_groups = groups;
    $$.redraw();
    return config.data_groups;
};

c3_chart_fn.legend = function () {};
c3_chart_fn.legend.show = function (targetIds) {
    var $$ = this.internal;
    $$.showLegend($$.mapToTargetIds(targetIds));
    $$.updateAndRedraw({ withLegend: true });
};
c3_chart_fn.legend.hide = function (targetIds) {
    var $$ = this.internal;
    $$.hideLegend($$.mapToTargetIds(targetIds));
    $$.updateAndRedraw({ withLegend: true });
};

c3_chart_fn.load = function (args) {
    var $$ = this.internal,
        config = $$.config;
    // update xs if specified
    if (args.xs) {
        $$.addXs(args.xs);
    }
    // update names if exists
    if ('names' in args) {
        c3_chart_fn.data.names.bind(this)(args.names);
    }
    // update classes if exists
    if ('classes' in args) {
        Object.keys(args.classes).forEach(function (id) {
            config.data_classes[id] = args.classes[id];
        });
    }
    // update categories if exists
    if ('categories' in args && $$.isCategorized()) {
        config.axis_x_categories = args.categories;
    }
    // update axes if exists
    if ('axes' in args) {
        Object.keys(args.axes).forEach(function (id) {
            config.data_axes[id] = args.axes[id];
        });
    }
    // update colors if exists
    if ('colors' in args) {
        Object.keys(args.colors).forEach(function (id) {
            config.data_colors[id] = args.colors[id];
        });
    }
    // use cache if exists
    if ('cacheIds' in args && $$.hasCaches(args.cacheIds)) {
        $$.load($$.getCaches(args.cacheIds), args.done);
        return;
    }
    // unload if needed
    if ('unload' in args) {
        // TODO: do not unload if target will load (included in url/rows/columns)
        $$.unload($$.mapToTargetIds(typeof args.unload === 'boolean' && args.unload ? null : args.unload), function () {
            $$.loadFromArgs(args);
        });
    } else {
        $$.loadFromArgs(args);
    }
};

c3_chart_fn.unload = function (args) {
    var $$ = this.internal;
    args = args || {};
    if (args instanceof Array) {
        args = { ids: args };
    } else if (typeof args === 'string') {
        args = { ids: [args] };
    }
    $$.unload($$.mapToTargetIds(args.ids), function () {
        $$.redraw({ withUpdateOrgXDomain: true, withUpdateXDomain: true, withLegend: true });
        if (args.done) {
            args.done();
        }
    });
};

c3_chart_fn.regions = function (regions) {
    var $$ = this.internal,
        config = $$.config;
    if (!regions) {
        return config.regions;
    }
    config.regions = regions;
    $$.redrawWithoutRescale();
    return config.regions;
};
c3_chart_fn.regions.add = function (regions) {
    var $$ = this.internal,
        config = $$.config;
    if (!regions) {
        return config.regions;
    }
    config.regions = config.regions.concat(regions);
    $$.redrawWithoutRescale();
    return config.regions;
};
c3_chart_fn.regions.remove = function (options) {
    var $$ = this.internal,
        config = $$.config,
        duration,
        classes,
        regions;

    options = options || {};
    duration = $$.getOption(options, "duration", config.transition_duration);
    classes = $$.getOption(options, "classes", [CLASS.region]);

    regions = $$.main.select('.' + CLASS.regions).selectAll(classes.map(function (c) {
        return '.' + c;
    }));
    (duration ? regions.transition().duration(duration) : regions).style('opacity', 0).remove();

    config.regions = config.regions.filter(function (region) {
        var found = false;
        if (!region['class']) {
            return true;
        }
        region['class'].split(' ').forEach(function (c) {
            if (classes.indexOf(c) >= 0) {
                found = true;
            }
        });
        return !found;
    });

    return config.regions;
};

c3_chart_fn.selected = function (targetId) {
    var $$ = this.internal,
        d3 = $$.d3;
    return d3.merge($$.main.selectAll('.' + CLASS.shapes + $$.getTargetSelectorSuffix(targetId)).selectAll('.' + CLASS.shape).filter(function () {
        return d3.select(this).classed(CLASS.SELECTED);
    }).map(function (d) {
        return d.map(function (d) {
            var data = d.__data__;return data.data ? data.data : data;
        });
    }));
};
c3_chart_fn.select = function (ids, indices, resetOther) {
    var $$ = this.internal,
        d3 = $$.d3,
        config = $$.config;
    if (!config.data_selection_enabled) {
        return;
    }
    $$.main.selectAll('.' + CLASS.shapes).selectAll('.' + CLASS.shape).each(function (d, i) {
        var shape = d3.select(this),
            id = d.data ? d.data.id : d.id,
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
    var $$ = this.internal,
        d3 = $$.d3,
        config = $$.config;
    if (!config.data_selection_enabled) {
        return;
    }
    $$.main.selectAll('.' + CLASS.shapes).selectAll('.' + CLASS.shape).each(function (d, i) {
        var shape = d3.select(this),
            id = d.data ? d.data.id : d.id,
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

c3_chart_fn.show = function (targetIds, options) {
    var $$ = this.internal,
        targets;

    targetIds = $$.mapToTargetIds(targetIds);
    options = options || {};

    $$.removeHiddenTargetIds(targetIds);
    targets = $$.svg.selectAll($$.selectorTargets(targetIds));

    targets.transition().style('opacity', 1, 'important').call($$.endall, function () {
        targets.style('opacity', null).style('opacity', 1);
    });

    if (options.withLegend) {
        $$.showLegend(targetIds);
    }

    $$.redraw({ withUpdateOrgXDomain: true, withUpdateXDomain: true, withLegend: true });
};

c3_chart_fn.hide = function (targetIds, options) {
    var $$ = this.internal,
        targets;

    targetIds = $$.mapToTargetIds(targetIds);
    options = options || {};

    $$.addHiddenTargetIds(targetIds);
    targets = $$.svg.selectAll($$.selectorTargets(targetIds));

    targets.transition().style('opacity', 0, 'important').call($$.endall, function () {
        targets.style('opacity', null).style('opacity', 0);
    });

    if (options.withLegend) {
        $$.hideLegend(targetIds);
    }

    $$.redraw({ withUpdateOrgXDomain: true, withUpdateXDomain: true, withLegend: true });
};

c3_chart_fn.toggle = function (targetIds, options) {
    var that = this,
        $$ = this.internal;
    $$.mapToTargetIds(targetIds).forEach(function (targetId) {
        $$.isTargetToShow(targetId) ? that.hide(targetId, options) : that.show(targetId, options);
    });
};

c3_chart_fn.tooltip = function () {};
c3_chart_fn.tooltip.show = function (args) {
    var $$ = this.internal,
        index,
        mouse;

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
    } else if (typeof args.x !== 'undefined') {
        index = $$.getIndexByX(args.x);
    } else if (typeof args.index !== 'undefined') {
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

c3_chart_fn.transform = function (type, targetIds) {
    var $$ = this.internal,
        options = ['pie', 'donut'].indexOf(type) >= 0 ? { withTransform: true } : null;
    $$.transformTo(targetIds, type, options);
};

c3_chart_internal_fn.transformTo = function (targetIds, type, optionsForRedraw) {
    var $$ = this,
        withTransitionForAxis = !$$.hasArcType(),
        options = optionsForRedraw || { withTransitionForAxis: withTransitionForAxis };
    options.withTransitionForTransform = false;
    $$.transiting = false;
    $$.setTargetType(targetIds, type);
    $$.updateTargets($$.data.targets); // this is needed when transforming to arc
    $$.updateAndRedraw(options);
};

c3_chart_fn.x = function (x) {
    var $$ = this.internal;
    if (arguments.length) {
        $$.updateTargetX($$.data.targets, x);
        $$.redraw({ withUpdateOrgXDomain: true, withUpdateXDomain: true });
    }
    return $$.data.xs;
};
c3_chart_fn.xs = function (xs) {
    var $$ = this.internal;
    if (arguments.length) {
        $$.updateTargetXs($$.data.targets, xs);
        $$.redraw({ withUpdateOrgXDomain: true, withUpdateXDomain: true });
    }
    return $$.data.xs;
};

c3_chart_fn.zoom = function (domain) {
    var $$ = this.internal;
    if (domain) {
        if ($$.isTimeSeries()) {
            domain = domain.map(function (x) {
                return $$.parseDate(x);
            });
        }
        $$.brush.extent(domain);
        $$.redraw({ withUpdateXDomain: true, withY: $$.config.zoom_rescale });
        $$.config.zoom_onzoom.call(this, $$.x.orgDomain());
    }
    return $$.brush.extent();
};
c3_chart_fn.zoom.enable = function (enabled) {
    var $$ = this.internal;
    $$.config.zoom_enabled = enabled;
    $$.updateAndRedraw();
};
c3_chart_fn.unzoom = function () {
    var $$ = this.internal;
    $$.brush.clear().update();
    $$.redraw({ withUpdateXDomain: true });
};

c3_chart_fn.zoom.max = function (max) {
    var $$ = this.internal,
        config = $$.config,
        d3 = $$.d3;
    if (max === 0 || max) {
        config.zoom_x_max = d3.max([$$.orgXDomain[1], max]);
    } else {
        return config.zoom_x_max;
    }
};

c3_chart_fn.zoom.min = function (min) {
    var $$ = this.internal,
        config = $$.config,
        d3 = $$.d3;
    if (min === 0 || min) {
        config.zoom_x_min = d3.min([$$.orgXDomain[0], min]);
    } else {
        return config.zoom_x_min;
    }
};

c3_chart_fn.zoom.range = function (range) {
    if (arguments.length) {
        if (isDefined(range.max)) {
            this.domain.max(range.max);
        }
        if (isDefined(range.min)) {
            this.domain.min(range.min);
        }
    } else {
        return {
            max: this.domain.max(),
            min: this.domain.min()
        };
    }
};

c3_chart_internal_fn.initPie = function () {
    var $$ = this,
        d3 = $$.d3;
    $$.pie = d3.layout.pie().value(function (d) {
        return d.values.reduce(function (a, b) {
            return a + b.value;
        }, 0);
    });
    $$.pie.sort($$.getOrderFunction() || null);
};

c3_chart_internal_fn.updateRadius = function () {
    var $$ = this,
        config = $$.config,
        w = config.gauge_width || config.donut_width;
    $$.radiusExpanded = Math.min($$.arcWidth, $$.arcHeight) / 2;
    $$.radius = $$.radiusExpanded * 0.95;
    $$.innerRadiusRatio = w ? ($$.radius - w) / $$.radius : 0.6;
    $$.innerRadius = $$.hasType('donut') || $$.hasType('gauge') ? $$.radius * $$.innerRadiusRatio : 0;
};

c3_chart_internal_fn.updateArc = function () {
    var $$ = this;
    $$.svgArc = $$.getSvgArc();
    $$.svgArcExpanded = $$.getSvgArcExpanded();
    $$.svgArcExpandedSub = $$.getSvgArcExpanded(0.98);
};

c3_chart_internal_fn.updateAngle = function (d) {
    var $$ = this,
        config = $$.config,
        found = false,
        index = 0,
        gMin,
        gMax,
        gTic,
        gValue;

    if (!config) {
        return null;
    }

    $$.pie($$.filterTargetsToShow($$.data.targets)).forEach(function (t) {
        if (!found && t.data.id === d.data.id) {
            found = true;
            d = t;
            d.index = index;
        }
        index++;
    });
    if (isNaN(d.startAngle)) {
        d.startAngle = 0;
    }
    if (isNaN(d.endAngle)) {
        d.endAngle = d.startAngle;
    }
    if ($$.isGaugeType(d.data)) {
        gMin = config.gauge_min;
        gMax = config.gauge_max;
        gTic = Math.PI * (config.gauge_fullCircle ? 2 : 1) / (gMax - gMin);
        gValue = d.value < gMin ? 0 : d.value < gMax ? d.value - gMin : gMax - gMin;
        d.startAngle = config.gauge_startingAngle;
        d.endAngle = d.startAngle + gTic * gValue;
    }
    return found ? d : null;
};

c3_chart_internal_fn.getSvgArc = function () {
    var $$ = this,
        arc = $$.d3.svg.arc().outerRadius($$.radius).innerRadius($$.innerRadius),
        newArc = function newArc(d, withoutUpdate) {
        var updated;
        if (withoutUpdate) {
            return arc(d);
        } // for interpolate
        updated = $$.updateAngle(d);
        return updated ? arc(updated) : "M 0 0";
    };
    // TODO: extends all function
    newArc.centroid = arc.centroid;
    return newArc;
};

c3_chart_internal_fn.getSvgArcExpanded = function (rate) {
    var $$ = this,
        arc = $$.d3.svg.arc().outerRadius($$.radiusExpanded * (rate ? rate : 1)).innerRadius($$.innerRadius);
    return function (d) {
        var updated = $$.updateAngle(d);
        return updated ? arc(updated) : "M 0 0";
    };
};

c3_chart_internal_fn.getArc = function (d, withoutUpdate, force) {
    return force || this.isArcType(d.data) ? this.svgArc(d, withoutUpdate) : "M 0 0";
};

c3_chart_internal_fn.transformForArcLabel = function (d) {
    var $$ = this,
        config = $$.config,
        updated = $$.updateAngle(d),
        c,
        x,
        y,
        h,
        ratio,
        translate = "";
    if (updated && !$$.hasType('gauge')) {
        c = this.svgArc.centroid(updated);
        x = isNaN(c[0]) ? 0 : c[0];
        y = isNaN(c[1]) ? 0 : c[1];
        h = Math.sqrt(x * x + y * y);
        if ($$.hasType('donut') && config.donut_label_ratio) {
            ratio = isFunction(config.donut_label_ratio) ? config.donut_label_ratio(d, $$.radius, h) : config.donut_label_ratio;
        } else if ($$.hasType('pie') && config.pie_label_ratio) {
            ratio = isFunction(config.pie_label_ratio) ? config.pie_label_ratio(d, $$.radius, h) : config.pie_label_ratio;
        } else {
            ratio = $$.radius && h ? (36 / $$.radius > 0.375 ? 1.175 - 36 / $$.radius : 0.8) * $$.radius / h : 0;
        }
        translate = "translate(" + x * ratio + ',' + y * ratio + ")";
    }
    return translate;
};

c3_chart_internal_fn.getArcRatio = function (d) {
    var $$ = this,
        config = $$.config,
        whole = Math.PI * ($$.hasType('gauge') && !config.gauge_fullCircle ? 1 : 2);
    return d ? (d.endAngle - d.startAngle) / whole : null;
};

c3_chart_internal_fn.convertToArcData = function (d) {
    return this.addName({
        id: d.data.id,
        value: d.value,
        ratio: this.getArcRatio(d),
        index: d.index
    });
};

c3_chart_internal_fn.textForArcLabel = function (d) {
    var $$ = this,
        updated,
        value,
        ratio,
        id,
        format;
    if (!$$.shouldShowArcLabel()) {
        return "";
    }
    updated = $$.updateAngle(d);
    value = updated ? updated.value : null;
    ratio = $$.getArcRatio(updated);
    id = d.data.id;
    if (!$$.hasType('gauge') && !$$.meetsArcLabelThreshold(ratio)) {
        return "";
    }
    format = $$.getArcLabelFormat();
    return format ? format(value, ratio, id) : $$.defaultArcValueFormat(value, ratio);
};

c3_chart_internal_fn.textForGaugeMinMax = function (value, isMax) {
    var $$ = this,
        format = $$.getGaugeLabelExtents();

    return format ? format(value, isMax) : value;
};

c3_chart_internal_fn.expandArc = function (targetIds) {
    var $$ = this,
        interval;

    // MEMO: avoid to cancel transition
    if ($$.transiting) {
        interval = window.setInterval(function () {
            if (!$$.transiting) {
                window.clearInterval(interval);
                if ($$.legend.selectAll('.c3-legend-item-focused').size() > 0) {
                    $$.expandArc(targetIds);
                }
            }
        }, 10);
        return;
    }

    targetIds = $$.mapToTargetIds(targetIds);

    $$.svg.selectAll($$.selectorTargets(targetIds, '.' + CLASS.chartArc)).each(function (d) {
        if (!$$.shouldExpand(d.data.id)) {
            return;
        }
        $$.d3.select(this).selectAll('path').transition().duration($$.expandDuration(d.data.id)).attr("d", $$.svgArcExpanded).transition().duration($$.expandDuration(d.data.id) * 2).attr("d", $$.svgArcExpandedSub).each(function (d) {
            if ($$.isDonutType(d.data)) {
                // callback here
            }
        });
    });
};

c3_chart_internal_fn.unexpandArc = function (targetIds) {
    var $$ = this;

    if ($$.transiting) {
        return;
    }

    targetIds = $$.mapToTargetIds(targetIds);

    $$.svg.selectAll($$.selectorTargets(targetIds, '.' + CLASS.chartArc)).selectAll('path').transition().duration(function (d) {
        return $$.expandDuration(d.data.id);
    }).attr("d", $$.svgArc);
    $$.svg.selectAll('.' + CLASS.arc);
};

c3_chart_internal_fn.expandDuration = function (id) {
    var $$ = this,
        config = $$.config;

    if ($$.isDonutType(id)) {
        return config.donut_expand_duration;
    } else if ($$.isGaugeType(id)) {
        return config.gauge_expand_duration;
    } else if ($$.isPieType(id)) {
        return config.pie_expand_duration;
    } else {
        return 50;
    }
};

c3_chart_internal_fn.shouldExpand = function (id) {
    var $$ = this,
        config = $$.config;
    return $$.isDonutType(id) && config.donut_expand || $$.isGaugeType(id) && config.gauge_expand || $$.isPieType(id) && config.pie_expand;
};

c3_chart_internal_fn.shouldShowArcLabel = function () {
    var $$ = this,
        config = $$.config,
        shouldShow = true;
    if ($$.hasType('donut')) {
        shouldShow = config.donut_label_show;
    } else if ($$.hasType('pie')) {
        shouldShow = config.pie_label_show;
    }
    // when gauge, always true
    return shouldShow;
};

c3_chart_internal_fn.meetsArcLabelThreshold = function (ratio) {
    var $$ = this,
        config = $$.config,
        threshold = $$.hasType('donut') ? config.donut_label_threshold : config.pie_label_threshold;
    return ratio >= threshold;
};

c3_chart_internal_fn.getArcLabelFormat = function () {
    var $$ = this,
        config = $$.config,
        format = config.pie_label_format;
    if ($$.hasType('gauge')) {
        format = config.gauge_label_format;
    } else if ($$.hasType('donut')) {
        format = config.donut_label_format;
    }
    return format;
};

c3_chart_internal_fn.getGaugeLabelExtents = function () {
    var $$ = this,
        config = $$.config;
    return config.gauge_label_extents;
};

c3_chart_internal_fn.getArcTitle = function () {
    var $$ = this;
    return $$.hasType('donut') ? $$.config.donut_title : "";
};

c3_chart_internal_fn.updateTargetsForArc = function (targets) {
    var $$ = this,
        main = $$.main,
        mainPieUpdate,
        mainPieEnter,
        classChartArc = $$.classChartArc.bind($$),
        classArcs = $$.classArcs.bind($$),
        classFocus = $$.classFocus.bind($$);
    mainPieUpdate = main.select('.' + CLASS.chartArcs).selectAll('.' + CLASS.chartArc).data($$.pie(targets)).attr("class", function (d) {
        return classChartArc(d) + classFocus(d.data);
    });
    mainPieEnter = mainPieUpdate.enter().append("g").attr("class", classChartArc);
    mainPieEnter.append('g').attr('class', classArcs);
    mainPieEnter.append("text").attr("dy", $$.hasType('gauge') ? "-.1em" : ".35em").style("opacity", 0).style("text-anchor", "middle").style("pointer-events", "none");
    // MEMO: can not keep same color..., but not bad to update color in redraw
    //mainPieUpdate.exit().remove();
};

c3_chart_internal_fn.initArc = function () {
    var $$ = this;
    $$.arcs = $$.main.select('.' + CLASS.chart).append("g").attr("class", CLASS.chartArcs).attr("transform", $$.getTranslate('arc'));
    $$.arcs.append('text').attr('class', CLASS.chartArcsTitle).style("text-anchor", "middle").text($$.getArcTitle());
};

c3_chart_internal_fn.redrawArc = function (duration, durationForExit, withTransform) {
    var $$ = this,
        d3 = $$.d3,
        config = $$.config,
        main = $$.main,
        mainArc;
    mainArc = main.selectAll('.' + CLASS.arcs).selectAll('.' + CLASS.arc).data($$.arcData.bind($$));
    mainArc.enter().append('path').attr("class", $$.classArc.bind($$)).style("fill", function (d) {
        return $$.color(d.data);
    }).style("cursor", function (d) {
        return config.interaction_enabled && config.data_selection_isselectable(d) ? "pointer" : null;
    }).each(function (d) {
        if ($$.isGaugeType(d.data)) {
            d.startAngle = d.endAngle = config.gauge_startingAngle;
        }
        this._current = d;
    });
    mainArc.attr("transform", function (d) {
        return !$$.isGaugeType(d.data) && withTransform ? "scale(0)" : "";
    }).on('mouseover', config.interaction_enabled ? function (d) {
        var updated, arcData;
        if ($$.transiting) {
            // skip while transiting
            return;
        }
        updated = $$.updateAngle(d);
        if (updated) {
            arcData = $$.convertToArcData(updated);
            // transitions
            $$.expandArc(updated.data.id);
            $$.api.focus(updated.data.id);
            $$.toggleFocusLegend(updated.data.id, true);
            $$.config.data_onmouseover(arcData, this);
        }
    } : null).on('mousemove', config.interaction_enabled ? function (d) {
        var updated = $$.updateAngle(d),
            arcData,
            selectedData;
        if (updated) {
            arcData = $$.convertToArcData(updated), selectedData = [arcData];
            $$.showTooltip(selectedData, this);
        }
    } : null).on('mouseout', config.interaction_enabled ? function (d) {
        var updated, arcData;
        if ($$.transiting) {
            // skip while transiting
            return;
        }
        updated = $$.updateAngle(d);
        if (updated) {
            arcData = $$.convertToArcData(updated);
            // transitions
            $$.unexpandArc(updated.data.id);
            $$.api.revert();
            $$.revertLegend();
            $$.hideTooltip();
            $$.config.data_onmouseout(arcData, this);
        }
    } : null).on('click', config.interaction_enabled ? function (d, i) {
        var updated = $$.updateAngle(d),
            arcData;
        if (updated) {
            arcData = $$.convertToArcData(updated);
            if ($$.toggleShape) {
                $$.toggleShape(this, arcData, i);
            }
            $$.config.data_onclick.call($$.api, arcData, this);
        }
    } : null).each(function () {
        $$.transiting = true;
    }).transition().duration(duration).attrTween("d", function (d) {
        var updated = $$.updateAngle(d),
            interpolate;
        if (!updated) {
            return function () {
                return "M 0 0";
            };
        }
        //                if (this._current === d) {
        //                    this._current = {
        //                        startAngle: Math.PI*2,
        //                        endAngle: Math.PI*2,
        //                    };
        //                }
        if (isNaN(this._current.startAngle)) {
            this._current.startAngle = 0;
        }
        if (isNaN(this._current.endAngle)) {
            this._current.endAngle = this._current.startAngle;
        }
        interpolate = d3.interpolate(this._current, updated);
        this._current = interpolate(0);
        return function (t) {
            var interpolated = interpolate(t);
            interpolated.data = d.data; // data.id will be updated by interporator
            return $$.getArc(interpolated, true);
        };
    }).attr("transform", withTransform ? "scale(1)" : "").style("fill", function (d) {
        return $$.levelColor ? $$.levelColor(d.data.values[0].value) : $$.color(d.data.id);
    } // Where gauge reading color would receive customization.
    ).call($$.endall, function () {
        $$.transiting = false;
    });
    mainArc.exit().transition().duration(durationForExit).style('opacity', 0).remove();
    main.selectAll('.' + CLASS.chartArc).select('text').style("opacity", 0).attr('class', function (d) {
        return $$.isGaugeType(d.data) ? CLASS.gaugeValue : '';
    }).text($$.textForArcLabel.bind($$)).attr("transform", $$.transformForArcLabel.bind($$)).style('font-size', function (d) {
        return $$.isGaugeType(d.data) ? Math.round($$.radius / 5) + 'px' : '';
    }).transition().duration(duration).style("opacity", function (d) {
        return $$.isTargetToShow(d.data.id) && $$.isArcType(d.data) ? 1 : 0;
    });
    main.select('.' + CLASS.chartArcsTitle).style("opacity", $$.hasType('donut') || $$.hasType('gauge') ? 1 : 0);

    if ($$.hasType('gauge')) {
        $$.arcs.select('.' + CLASS.chartArcsBackground).attr("d", function () {
            var d = {
                data: [{ value: config.gauge_max }],
                startAngle: config.gauge_startingAngle,
                endAngle: -1 * config.gauge_startingAngle
            };
            return $$.getArc(d, true, true);
        });
        $$.arcs.select('.' + CLASS.chartArcsGaugeUnit).attr("dy", ".75em").text(config.gauge_label_show ? config.gauge_units : '');
        $$.arcs.select('.' + CLASS.chartArcsGaugeMin).attr("dx", -1 * ($$.innerRadius + ($$.radius - $$.innerRadius) / (config.gauge_fullCircle ? 1 : 2)) + "px").attr("dy", "1.2em").text(config.gauge_label_show ? $$.textForGaugeMinMax(config.gauge_min, false) : '');
        $$.arcs.select('.' + CLASS.chartArcsGaugeMax).attr("dx", $$.innerRadius + ($$.radius - $$.innerRadius) / (config.gauge_fullCircle ? 1 : 2) + "px").attr("dy", "1.2em").text(config.gauge_label_show ? $$.textForGaugeMinMax(config.gauge_max, true) : '');
    }
};
c3_chart_internal_fn.initGauge = function () {
    var arcs = this.arcs;
    if (this.hasType('gauge')) {
        arcs.append('path').attr("class", CLASS.chartArcsBackground);
        arcs.append("text").attr("class", CLASS.chartArcsGaugeUnit).style("text-anchor", "middle").style("pointer-events", "none");
        arcs.append("text").attr("class", CLASS.chartArcsGaugeMin).style("text-anchor", "middle").style("pointer-events", "none");
        arcs.append("text").attr("class", CLASS.chartArcsGaugeMax).style("text-anchor", "middle").style("pointer-events", "none");
    }
};
c3_chart_internal_fn.getGaugeLabelHeight = function () {
    return this.config.gauge_label_show ? 20 : 0;
};

c3_chart_internal_fn.hasCaches = function (ids) {
    for (var i = 0; i < ids.length; i++) {
        if (!(ids[i] in this.cache)) {
            return false;
        }
    }
    return true;
};
c3_chart_internal_fn.addCache = function (id, target) {
    this.cache[id] = this.cloneTarget(target);
};
c3_chart_internal_fn.getCaches = function (ids) {
    var targets = [],
        i;
    for (i = 0; i < ids.length; i++) {
        if (ids[i] in this.cache) {
            targets.push(this.cloneTarget(this.cache[ids[i]]));
        }
    }
    return targets;
};

c3_chart_internal_fn.categoryName = function (i) {
    var config = this.config;
    return i < config.axis_x_categories.length ? config.axis_x_categories[i] : i;
};

c3_chart_internal_fn.generateClass = function (prefix, targetId) {
    return " " + prefix + " " + prefix + this.getTargetSelectorSuffix(targetId);
};
c3_chart_internal_fn.classText = function (d) {
    return this.generateClass(CLASS.text, d.index);
};
c3_chart_internal_fn.classTexts = function (d) {
    return this.generateClass(CLASS.texts, d.id);
};
c3_chart_internal_fn.classShape = function (d) {
    return this.generateClass(CLASS.shape, d.index);
};
c3_chart_internal_fn.classShapes = function (d) {
    return this.generateClass(CLASS.shapes, d.id);
};
c3_chart_internal_fn.classLine = function (d) {
    return this.classShape(d) + this.generateClass(CLASS.line, d.id);
};
c3_chart_internal_fn.classLines = function (d) {
    return this.classShapes(d) + this.generateClass(CLASS.lines, d.id);
};
c3_chart_internal_fn.classCircle = function (d) {
    return this.classShape(d) + this.generateClass(CLASS.circle, d.index);
};
c3_chart_internal_fn.classCircles = function (d) {
    return this.classShapes(d) + this.generateClass(CLASS.circles, d.id);
};
c3_chart_internal_fn.classBar = function (d) {
    return this.classShape(d) + this.generateClass(CLASS.bar, d.index);
};
c3_chart_internal_fn.classBars = function (d) {
    return this.classShapes(d) + this.generateClass(CLASS.bars, d.id);
};
c3_chart_internal_fn.classArc = function (d) {
    return this.classShape(d.data) + this.generateClass(CLASS.arc, d.data.id);
};
c3_chart_internal_fn.classArcs = function (d) {
    return this.classShapes(d.data) + this.generateClass(CLASS.arcs, d.data.id);
};
c3_chart_internal_fn.classArea = function (d) {
    return this.classShape(d) + this.generateClass(CLASS.area, d.id);
};
c3_chart_internal_fn.classAreas = function (d) {
    return this.classShapes(d) + this.generateClass(CLASS.areas, d.id);
};
c3_chart_internal_fn.classRegion = function (d, i) {
    return this.generateClass(CLASS.region, i) + ' ' + ('class' in d ? d['class'] : '');
};
c3_chart_internal_fn.classEvent = function (d) {
    return this.generateClass(CLASS.eventRect, d.index);
};
c3_chart_internal_fn.classTarget = function (id) {
    var $$ = this;
    var additionalClassSuffix = $$.config.data_classes[id],
        additionalClass = '';
    if (additionalClassSuffix) {
        additionalClass = ' ' + CLASS.target + '-' + additionalClassSuffix;
    }
    return $$.generateClass(CLASS.target, id) + additionalClass;
};
c3_chart_internal_fn.classFocus = function (d) {
    return this.classFocused(d) + this.classDefocused(d);
};
c3_chart_internal_fn.classFocused = function (d) {
    return ' ' + (this.focusedTargetIds.indexOf(d.id) >= 0 ? CLASS.focused : '');
};
c3_chart_internal_fn.classDefocused = function (d) {
    return ' ' + (this.defocusedTargetIds.indexOf(d.id) >= 0 ? CLASS.defocused : '');
};
c3_chart_internal_fn.classChartText = function (d) {
    return CLASS.chartText + this.classTarget(d.id);
};
c3_chart_internal_fn.classChartLine = function (d) {
    return CLASS.chartLine + this.classTarget(d.id);
};
c3_chart_internal_fn.classChartBar = function (d) {
    return CLASS.chartBar + this.classTarget(d.id);
};
c3_chart_internal_fn.classChartArc = function (d) {
    return CLASS.chartArc + this.classTarget(d.data.id);
};
c3_chart_internal_fn.getTargetSelectorSuffix = function (targetId) {
    return targetId || targetId === 0 ? ('-' + targetId).replace(/[\s?!@#$%^&*()_=+,.<>'":;\[\]\/|~`{}\\]/g, '-') : '';
};
c3_chart_internal_fn.selectorTarget = function (id, prefix) {
    return (prefix || '') + '.' + CLASS.target + this.getTargetSelectorSuffix(id);
};
c3_chart_internal_fn.selectorTargets = function (ids, prefix) {
    var $$ = this;
    ids = ids || [];
    return ids.length ? ids.map(function (id) {
        return $$.selectorTarget(id, prefix);
    }) : null;
};
c3_chart_internal_fn.selectorLegend = function (id) {
    return '.' + CLASS.legendItem + this.getTargetSelectorSuffix(id);
};
c3_chart_internal_fn.selectorLegends = function (ids) {
    var $$ = this;
    return ids && ids.length ? ids.map(function (id) {
        return $$.selectorLegend(id);
    }) : null;
};

c3_chart_internal_fn.getClipPath = function (id) {
    var isIE9 = window.navigator.appVersion.toLowerCase().indexOf("msie 9.") >= 0;
    return "url(" + (isIE9 ? "" : document.URL.split('#')[0]) + "#" + id + ")";
};
c3_chart_internal_fn.appendClip = function (parent, id) {
    return parent.append("clipPath").attr("id", id).append("rect");
};
c3_chart_internal_fn.getAxisClipX = function (forHorizontal) {
    // axis line width + padding for left
    var left = Math.max(30, this.margin.left);
    return forHorizontal ? -(1 + left) : -(left - 1);
};
c3_chart_internal_fn.getAxisClipY = function (forHorizontal) {
    return forHorizontal ? -20 : -this.margin.top;
};
c3_chart_internal_fn.getXAxisClipX = function () {
    var $$ = this;
    return $$.getAxisClipX(!$$.config.axis_rotated);
};
c3_chart_internal_fn.getXAxisClipY = function () {
    var $$ = this;
    return $$.getAxisClipY(!$$.config.axis_rotated);
};
c3_chart_internal_fn.getYAxisClipX = function () {
    var $$ = this;
    return $$.config.axis_y_inner ? -1 : $$.getAxisClipX($$.config.axis_rotated);
};
c3_chart_internal_fn.getYAxisClipY = function () {
    var $$ = this;
    return $$.getAxisClipY($$.config.axis_rotated);
};
c3_chart_internal_fn.getAxisClipWidth = function (forHorizontal) {
    var $$ = this,
        left = Math.max(30, $$.margin.left),
        right = Math.max(30, $$.margin.right);
    // width + axis line width + padding for left/right
    return forHorizontal ? $$.width + 2 + left + right : $$.margin.left + 20;
};
c3_chart_internal_fn.getAxisClipHeight = function (forHorizontal) {
    // less than 20 is not enough to show the axis label 'outer' without legend
    return (forHorizontal ? this.margin.bottom : this.margin.top + this.height) + 20;
};
c3_chart_internal_fn.getXAxisClipWidth = function () {
    var $$ = this;
    return $$.getAxisClipWidth(!$$.config.axis_rotated);
};
c3_chart_internal_fn.getXAxisClipHeight = function () {
    var $$ = this;
    return $$.getAxisClipHeight(!$$.config.axis_rotated);
};
c3_chart_internal_fn.getYAxisClipWidth = function () {
    var $$ = this;
    return $$.getAxisClipWidth($$.config.axis_rotated) + ($$.config.axis_y_inner ? 20 : 0);
};
c3_chart_internal_fn.getYAxisClipHeight = function () {
    var $$ = this;
    return $$.getAxisClipHeight($$.config.axis_rotated);
};

c3_chart_internal_fn.generateColor = function () {
    var $$ = this,
        config = $$.config,
        d3 = $$.d3,
        colors = config.data_colors,
        pattern = notEmpty(config.color_pattern) ? config.color_pattern : d3.scale.category10().range(),
        callback = config.data_color,
        ids = [];

    return function (d) {
        var id = d.id || d.data && d.data.id || d,
            color;

        // if callback function is provided
        if (colors[id] instanceof Function) {
            color = colors[id](d);
        }
        // if specified, choose that color
        else if (colors[id]) {
                color = colors[id];
            }
            // if not specified, choose from pattern
            else {
                    if (ids.indexOf(id) < 0) {
                        ids.push(id);
                    }
                    color = pattern[ids.indexOf(id) % pattern.length];
                    colors[id] = color;
                }
        return callback instanceof Function ? callback(color, d) : color;
    };
};
c3_chart_internal_fn.generateLevelColor = function () {
    var $$ = this,
        config = $$.config,
        colors = config.color_pattern,
        threshold = config.color_threshold,
        asValue = threshold.unit === 'value',
        values = threshold.values && threshold.values.length ? threshold.values : [],
        max = threshold.max || 100;
    return notEmpty(config.color_threshold) ? function (value) {
        var i,
            v,
            color = colors[colors.length - 1];
        for (i = 0; i < values.length; i++) {
            v = asValue ? value : value * 100 / max;
            if (v < values[i]) {
                color = colors[i];
                break;
            }
        }
        return color;
    } : null;
};

c3_chart_internal_fn.getDefaultConfig = function () {
    var config = {
        bindto: '#chart',
        svg_classname: undefined,
        size_width: undefined,
        size_height: undefined,
        padding_left: undefined,
        padding_right: undefined,
        padding_top: undefined,
        padding_bottom: undefined,
        resize_auto: true,
        zoom_enabled: false,
        zoom_extent: undefined,
        zoom_privileged: false,
        zoom_rescale: false,
        zoom_onzoom: function zoom_onzoom() {},
        zoom_onzoomstart: function zoom_onzoomstart() {},
        zoom_onzoomend: function zoom_onzoomend() {},
        zoom_x_min: undefined,
        zoom_x_max: undefined,
        interaction_brighten: true,
        interaction_enabled: true,
        onmouseover: function onmouseover() {},
        onmouseout: function onmouseout() {},
        onresize: function onresize() {},
        onresized: function onresized() {},
        oninit: function oninit() {},
        onrendered: function onrendered() {},
        transition_duration: 350,
        data_x: undefined,
        data_xs: {},
        data_xFormat: '%Y-%m-%d',
        data_xLocaltime: true,
        data_xSort: true,
        data_idConverter: function data_idConverter(id) {
            return id;
        },
        data_names: {},
        data_classes: {},
        data_groups: [],
        data_axes: {},
        data_type: undefined,
        data_types: {},
        data_labels: {},
        data_order: 'desc',
        data_regions: {},
        data_color: undefined,
        data_colors: {},
        data_hide: false,
        data_filter: undefined,
        data_selection_enabled: false,
        data_selection_grouped: false,
        data_selection_isselectable: function data_selection_isselectable() {
            return true;
        },
        data_selection_multiple: true,
        data_selection_draggable: false,
        data_onclick: function data_onclick() {},
        data_onmouseover: function data_onmouseover() {},
        data_onmouseout: function data_onmouseout() {},
        data_onselected: function data_onselected() {},
        data_onunselected: function data_onunselected() {},
        data_url: undefined,
        data_headers: undefined,
        data_json: undefined,
        data_rows: undefined,
        data_columns: undefined,
        data_mimeType: undefined,
        data_keys: undefined,
        // configuration for no plot-able data supplied.
        data_empty_label_text: "",
        // subchart
        subchart_show: false,
        subchart_size_height: 60,
        subchart_axis_x_show: true,
        subchart_onbrush: function subchart_onbrush() {},
        // color
        color_pattern: [],
        color_threshold: {},
        // legend
        legend_show: true,
        legend_hide: false,
        legend_position: 'bottom',
        legend_inset_anchor: 'top-left',
        legend_inset_x: 10,
        legend_inset_y: 0,
        legend_inset_step: undefined,
        legend_item_onclick: undefined,
        legend_item_onmouseover: undefined,
        legend_item_onmouseout: undefined,
        legend_equally: false,
        legend_padding: 0,
        legend_item_tile_width: 10,
        legend_item_tile_height: 10,
        // axis
        axis_rotated: false,
        axis_x_show: true,
        axis_x_type: 'indexed',
        axis_x_localtime: true,
        axis_x_categories: [],
        axis_x_tick_centered: false,
        axis_x_tick_format: undefined,
        axis_x_tick_culling: {},
        axis_x_tick_culling_max: 10,
        axis_x_tick_count: undefined,
        axis_x_tick_fit: true,
        axis_x_tick_values: null,
        axis_x_tick_rotate: 0,
        axis_x_tick_outer: true,
        axis_x_tick_multiline: true,
        axis_x_tick_width: null,
        axis_x_max: undefined,
        axis_x_min: undefined,
        axis_x_padding: {},
        axis_x_height: undefined,
        axis_x_extent: undefined,
        axis_x_label: {},
        axis_y_show: true,
        axis_y_type: undefined,
        axis_y_max: undefined,
        axis_y_min: undefined,
        axis_y_inverted: false,
        axis_y_center: undefined,
        axis_y_inner: undefined,
        axis_y_label: {},
        axis_y_tick_format: undefined,
        axis_y_tick_outer: true,
        axis_y_tick_values: null,
        axis_y_tick_rotate: 0,
        axis_y_tick_count: undefined,
        axis_y_tick_time_value: undefined,
        axis_y_tick_time_interval: undefined,
        axis_y_padding: {},
        axis_y_default: undefined,
        axis_y2_show: false,
        axis_y2_max: undefined,
        axis_y2_min: undefined,
        axis_y2_inverted: false,
        axis_y2_center: undefined,
        axis_y2_inner: undefined,
        axis_y2_label: {},
        axis_y2_tick_format: undefined,
        axis_y2_tick_outer: true,
        axis_y2_tick_values: null,
        axis_y2_tick_count: undefined,
        axis_y2_padding: {},
        axis_y2_default: undefined,
        // grid
        grid_x_show: false,
        grid_x_type: 'tick',
        grid_x_lines: [],
        grid_y_show: false,
        // not used
        // grid_y_type: 'tick',
        grid_y_lines: [],
        grid_y_ticks: 10,
        grid_focus_show: true,
        grid_lines_front: true,
        // point - point of each data
        point_show: true,
        point_r: 2.5,
        point_sensitivity: 10,
        point_focus_expand_enabled: true,
        point_focus_expand_r: undefined,
        point_select_r: undefined,
        // line
        line_connectNull: false,
        line_step_type: 'step',
        // bar
        bar_width: undefined,
        bar_width_ratio: 0.6,
        bar_width_max: undefined,
        bar_zerobased: true,
        bar_space: 0,
        // area
        area_zerobased: true,
        area_above: false,
        // pie
        pie_label_show: true,
        pie_label_format: undefined,
        pie_label_threshold: 0.05,
        pie_label_ratio: undefined,
        pie_expand: {},
        pie_expand_duration: 50,
        // gauge
        gauge_fullCircle: false,
        gauge_label_show: true,
        gauge_label_format: undefined,
        gauge_min: 0,
        gauge_max: 100,
        gauge_startingAngle: -1 * Math.PI / 2,
        gauge_label_extents: undefined,
        gauge_units: undefined,
        gauge_width: undefined,
        gauge_expand: {},
        gauge_expand_duration: 50,
        // donut
        donut_label_show: true,
        donut_label_format: undefined,
        donut_label_threshold: 0.05,
        donut_label_ratio: undefined,
        donut_width: undefined,
        donut_title: "",
        donut_expand: {},
        donut_expand_duration: 50,
        // spline
        spline_interpolation_type: 'cardinal',
        // region - region to change style
        regions: [],
        // tooltip - show when mouseover on each data
        tooltip_show: true,
        tooltip_grouped: true,
        tooltip_order: undefined,
        tooltip_format_title: undefined,
        tooltip_format_name: undefined,
        tooltip_format_value: undefined,
        tooltip_position: undefined,
        tooltip_contents: function tooltip_contents(d, defaultTitleFormat, defaultValueFormat, color) {
            return this.getTooltipContent ? this.getTooltipContent(d, defaultTitleFormat, defaultValueFormat, color) : '';
        },
        tooltip_init_show: false,
        tooltip_init_x: 0,
        tooltip_init_position: { top: '0px', left: '50px' },
        tooltip_onshow: function tooltip_onshow() {},
        tooltip_onhide: function tooltip_onhide() {},
        // title
        title_text: undefined,
        title_padding: {
            top: 0,
            right: 0,
            bottom: 0,
            left: 0
        },
        title_position: 'top-center'
    };

    Object.keys(this.additionalConfig).forEach(function (key) {
        config[key] = this.additionalConfig[key];
    }, this);

    return config;
};
c3_chart_internal_fn.additionalConfig = {};

c3_chart_internal_fn.loadConfig = function (config) {
    var this_config = this.config,
        target,
        keys,
        read;
    function find() {
        var key = keys.shift();
        //        console.log("key =>", key, ", target =>", target);
        if (key && target && (typeof target === 'undefined' ? 'undefined' : _typeof(target)) === 'object' && key in target) {
            target = target[key];
            return find();
        } else if (!key) {
            return target;
        } else {
            return undefined;
        }
    }
    Object.keys(this_config).forEach(function (key) {
        target = config;
        keys = key.split('_');
        read = find();
        //        console.log("CONFIG : ", key, read);
        if (isDefined(read)) {
            this_config[key] = read;
        }
    });
};

c3_chart_internal_fn.convertUrlToData = function (url, mimeType, headers, keys, done) {
    var $$ = this,
        type = mimeType ? mimeType : 'csv';
    var req = $$.d3.xhr(url);
    if (headers) {
        Object.keys(headers).forEach(function (header) {
            req.header(header, headers[header]);
        });
    }
    req.get(function (error, data) {
        var d;
        var dataResponse = data.response || data.responseText; // Fixes IE9 XHR issue; see #1345
        if (!data) {
            throw new Error(error.responseURL + ' ' + error.status + ' (' + error.statusText + ')');
        }
        if (type === 'json') {
            d = $$.convertJsonToData(JSON.parse(dataResponse), keys);
        } else if (type === 'tsv') {
            d = $$.convertTsvToData(dataResponse);
        } else {
            d = $$.convertCsvToData(dataResponse);
        }
        done.call($$, d);
    });
};
c3_chart_internal_fn.convertXsvToData = function (xsv, parser) {
    var rows = parser.parseRows(xsv),
        d;
    if (rows.length === 1) {
        d = [{}];
        rows[0].forEach(function (id) {
            d[0][id] = null;
        });
    } else {
        d = parser.parse(xsv);
    }
    return d;
};
c3_chart_internal_fn.convertCsvToData = function (csv) {
    return this.convertXsvToData(csv, this.d3.csv);
};
c3_chart_internal_fn.convertTsvToData = function (tsv) {
    return this.convertXsvToData(tsv, this.d3.tsv);
};
c3_chart_internal_fn.convertJsonToData = function (json, keys) {
    var $$ = this,
        new_rows = [],
        targetKeys,
        data;
    if (keys) {
        // when keys specified, json would be an array that includes objects
        if (keys.x) {
            targetKeys = keys.value.concat(keys.x);
            $$.config.data_x = keys.x;
        } else {
            targetKeys = keys.value;
        }
        new_rows.push(targetKeys);
        json.forEach(function (o) {
            var new_row = [];
            targetKeys.forEach(function (key) {
                // convert undefined to null because undefined data will be removed in convertDataToTargets()
                var v = $$.findValueInJson(o, key);
                if (isUndefined(v)) {
                    v = null;
                }
                new_row.push(v);
            });
            new_rows.push(new_row);
        });
        data = $$.convertRowsToData(new_rows);
    } else {
        Object.keys(json).forEach(function (key) {
            new_rows.push([key].concat(json[key]));
        });
        data = $$.convertColumnsToData(new_rows);
    }
    return data;
};
c3_chart_internal_fn.findValueInJson = function (object, path) {
    path = path.replace(/\[(\w+)\]/g, '.$1'); // convert indexes to properties (replace [] with .)
    path = path.replace(/^\./, ''); // strip a leading dot
    var pathArray = path.split('.');
    for (var i = 0; i < pathArray.length; ++i) {
        var k = pathArray[i];
        if (k in object) {
            object = object[k];
        } else {
            return;
        }
    }
    return object;
};

/**
 * Converts the rows to normalized data.
 * @param {any[][]} rows The row data
 * @return {Object[]}
 */
c3_chart_internal_fn.convertRowsToData = function (rows) {
    var newRows = [];
    var keys = rows[0];

    for (var i = 1; i < rows.length; i++) {
        var newRow = {};
        for (var j = 0; j < rows[i].length; j++) {
            if (isUndefined(rows[i][j])) {
                throw new Error("Source data is missing a component at (" + i + "," + j + ")!");
            }
            newRow[keys[j]] = rows[i][j];
        }
        newRows.push(newRow);
    }
    return newRows;
};

/**
 * Converts the columns to normalized data.
 * @param {any[][]} columns The column data
 * @return {Object[]}
 */
c3_chart_internal_fn.convertColumnsToData = function (columns) {
    var newRows = [];

    for (var i = 0; i < columns.length; i++) {
        var key = columns[i][0];
        for (var j = 1; j < columns[i].length; j++) {
            if (isUndefined(newRows[j - 1])) {
                newRows[j - 1] = {};
            }
            if (isUndefined(columns[i][j])) {
                throw new Error("Source data is missing a component at (" + i + "," + j + ")!");
            }
            newRows[j - 1][key] = columns[i][j];
        }
    }

    return newRows;
};

c3_chart_internal_fn.convertDataToTargets = function (data, appendXs) {
    var $$ = this,
        config = $$.config,
        ids = $$.d3.keys(data[0]).filter($$.isNotX, $$),
        xs = $$.d3.keys(data[0]).filter($$.isX, $$),
        targets;

    // save x for update data by load when custom x and c3.x API
    ids.forEach(function (id) {
        var xKey = $$.getXKey(id);

        if ($$.isCustomX() || $$.isTimeSeries()) {
            // if included in input data
            if (xs.indexOf(xKey) >= 0) {
                $$.data.xs[id] = (appendXs && $$.data.xs[id] ? $$.data.xs[id] : []).concat(data.map(function (d) {
                    return d[xKey];
                }).filter(isValue).map(function (rawX, i) {
                    return $$.generateTargetX(rawX, id, i);
                }));
            }
            // if not included in input data, find from preloaded data of other id's x
            else if (config.data_x) {
                    $$.data.xs[id] = $$.getOtherTargetXs();
                }
                // if not included in input data, find from preloaded data
                else if (notEmpty(config.data_xs)) {
                        $$.data.xs[id] = $$.getXValuesOfXKey(xKey, $$.data.targets);
                    }
            // MEMO: if no x included, use same x of current will be used
        } else {
            $$.data.xs[id] = data.map(function (d, i) {
                return i;
            });
        }
    });

    // check x is defined
    ids.forEach(function (id) {
        if (!$$.data.xs[id]) {
            throw new Error('x is not defined for id = "' + id + '".');
        }
    });

    // convert to target
    targets = ids.map(function (id, index) {
        var convertedId = config.data_idConverter(id);
        return {
            id: convertedId,
            id_org: id,
            values: data.map(function (d, i) {
                var xKey = $$.getXKey(id),
                    rawX = d[xKey],
                    value = d[id] !== null && !isNaN(d[id]) ? +d[id] : null,
                    x;
                // use x as categories if custom x and categorized
                if ($$.isCustomX() && $$.isCategorized() && !isUndefined(rawX)) {
                    if (index === 0 && i === 0) {
                        config.axis_x_categories = [];
                    }
                    x = config.axis_x_categories.indexOf(rawX);
                    if (x === -1) {
                        x = config.axis_x_categories.length;
                        config.axis_x_categories.push(rawX);
                    }
                } else {
                    x = $$.generateTargetX(rawX, id, i);
                }
                // mark as x = undefined if value is undefined and filter to remove after mapped
                if (isUndefined(d[id]) || $$.data.xs[id].length <= i) {
                    x = undefined;
                }
                return { x: x, value: value, id: convertedId };
            }).filter(function (v) {
                return isDefined(v.x);
            })
        };
    });

    // finish targets
    targets.forEach(function (t) {
        var i;
        // sort values by its x
        if (config.data_xSort) {
            t.values = t.values.sort(function (v1, v2) {
                var x1 = v1.x || v1.x === 0 ? v1.x : Infinity,
                    x2 = v2.x || v2.x === 0 ? v2.x : Infinity;
                return x1 - x2;
            });
        }
        // indexing each value
        i = 0;
        t.values.forEach(function (v) {
            v.index = i++;
        });
        // this needs to be sorted because its index and value.index is identical
        $$.data.xs[t.id].sort(function (v1, v2) {
            return v1 - v2;
        });
    });

    // cache information about values
    $$.hasNegativeValue = $$.hasNegativeValueInTargets(targets);
    $$.hasPositiveValue = $$.hasPositiveValueInTargets(targets);

    // set target types
    if (config.data_type) {
        $$.setTargetType($$.mapToIds(targets).filter(function (id) {
            return !(id in config.data_types);
        }), config.data_type);
    }

    // cache as original id keyed
    targets.forEach(function (d) {
        $$.addCache(d.id_org, d);
    });

    return targets;
};

c3_chart_internal_fn.isX = function (key) {
    var $$ = this,
        config = $$.config;
    return config.data_x && key === config.data_x || notEmpty(config.data_xs) && hasValue(config.data_xs, key);
};
c3_chart_internal_fn.isNotX = function (key) {
    return !this.isX(key);
};
c3_chart_internal_fn.getXKey = function (id) {
    var $$ = this,
        config = $$.config;
    return config.data_x ? config.data_x : notEmpty(config.data_xs) ? config.data_xs[id] : null;
};
c3_chart_internal_fn.getXValuesOfXKey = function (key, targets) {
    var $$ = this,
        xValues,
        ids = targets && notEmpty(targets) ? $$.mapToIds(targets) : [];
    ids.forEach(function (id) {
        if ($$.getXKey(id) === key) {
            xValues = $$.data.xs[id];
        }
    });
    return xValues;
};
c3_chart_internal_fn.getIndexByX = function (x) {
    var $$ = this,
        data = $$.filterByX($$.data.targets, x);
    return data.length ? data[0].index : null;
};
c3_chart_internal_fn.getXValue = function (id, i) {
    var $$ = this;
    return id in $$.data.xs && $$.data.xs[id] && isValue($$.data.xs[id][i]) ? $$.data.xs[id][i] : i;
};
c3_chart_internal_fn.getOtherTargetXs = function () {
    var $$ = this,
        idsForX = Object.keys($$.data.xs);
    return idsForX.length ? $$.data.xs[idsForX[0]] : null;
};
c3_chart_internal_fn.getOtherTargetX = function (index) {
    var xs = this.getOtherTargetXs();
    return xs && index < xs.length ? xs[index] : null;
};
c3_chart_internal_fn.addXs = function (xs) {
    var $$ = this;
    Object.keys(xs).forEach(function (id) {
        $$.config.data_xs[id] = xs[id];
    });
};
c3_chart_internal_fn.hasMultipleX = function (xs) {
    return this.d3.set(Object.keys(xs).map(function (id) {
        return xs[id];
    })).size() > 1;
};
c3_chart_internal_fn.isMultipleX = function () {
    return notEmpty(this.config.data_xs) || !this.config.data_xSort || this.hasType('scatter');
};
c3_chart_internal_fn.addName = function (data) {
    var $$ = this,
        name;
    if (data) {
        name = $$.config.data_names[data.id];
        data.name = name !== undefined ? name : data.id;
    }
    return data;
};
c3_chart_internal_fn.getValueOnIndex = function (values, index) {
    var valueOnIndex = values.filter(function (v) {
        return v.index === index;
    });
    return valueOnIndex.length ? valueOnIndex[0] : null;
};
c3_chart_internal_fn.updateTargetX = function (targets, x) {
    var $$ = this;
    targets.forEach(function (t) {
        t.values.forEach(function (v, i) {
            v.x = $$.generateTargetX(x[i], t.id, i);
        });
        $$.data.xs[t.id] = x;
    });
};
c3_chart_internal_fn.updateTargetXs = function (targets, xs) {
    var $$ = this;
    targets.forEach(function (t) {
        if (xs[t.id]) {
            $$.updateTargetX([t], xs[t.id]);
        }
    });
};
c3_chart_internal_fn.generateTargetX = function (rawX, id, index) {
    var $$ = this,
        x;
    if ($$.isTimeSeries()) {
        x = rawX ? $$.parseDate(rawX) : $$.parseDate($$.getXValue(id, index));
    } else if ($$.isCustomX() && !$$.isCategorized()) {
        x = isValue(rawX) ? +rawX : $$.getXValue(id, index);
    } else {
        x = index;
    }
    return x;
};
c3_chart_internal_fn.cloneTarget = function (target) {
    return {
        id: target.id,
        id_org: target.id_org,
        values: target.values.map(function (d) {
            return { x: d.x, value: d.value, id: d.id };
        })
    };
};
c3_chart_internal_fn.updateXs = function () {
    var $$ = this;
    if ($$.data.targets.length) {
        $$.xs = [];
        $$.data.targets[0].values.forEach(function (v) {
            $$.xs[v.index] = v.x;
        });
    }
};
c3_chart_internal_fn.getPrevX = function (i) {
    var x = this.xs[i - 1];
    return typeof x !== 'undefined' ? x : null;
};
c3_chart_internal_fn.getNextX = function (i) {
    var x = this.xs[i + 1];
    return typeof x !== 'undefined' ? x : null;
};
c3_chart_internal_fn.getMaxDataCount = function () {
    var $$ = this;
    return $$.d3.max($$.data.targets, function (t) {
        return t.values.length;
    });
};
c3_chart_internal_fn.getMaxDataCountTarget = function (targets) {
    var length = targets.length,
        max = 0,
        maxTarget;
    if (length > 1) {
        targets.forEach(function (t) {
            if (t.values.length > max) {
                maxTarget = t;
                max = t.values.length;
            }
        });
    } else {
        maxTarget = length ? targets[0] : null;
    }
    return maxTarget;
};
c3_chart_internal_fn.getEdgeX = function (targets) {
    var $$ = this;
    return !targets.length ? [0, 0] : [$$.d3.min(targets, function (t) {
        return t.values[0].x;
    }), $$.d3.max(targets, function (t) {
        return t.values[t.values.length - 1].x;
    })];
};
c3_chart_internal_fn.mapToIds = function (targets) {
    return targets.map(function (d) {
        return d.id;
    });
};
c3_chart_internal_fn.mapToTargetIds = function (ids) {
    var $$ = this;
    return ids ? [].concat(ids) : $$.mapToIds($$.data.targets);
};
c3_chart_internal_fn.hasTarget = function (targets, id) {
    var ids = this.mapToIds(targets),
        i;
    for (i = 0; i < ids.length; i++) {
        if (ids[i] === id) {
            return true;
        }
    }
    return false;
};
c3_chart_internal_fn.isTargetToShow = function (targetId) {
    return this.hiddenTargetIds.indexOf(targetId) < 0;
};
c3_chart_internal_fn.isLegendToShow = function (targetId) {
    return this.hiddenLegendIds.indexOf(targetId) < 0;
};
c3_chart_internal_fn.filterTargetsToShow = function (targets) {
    var $$ = this;
    return targets.filter(function (t) {
        return $$.isTargetToShow(t.id);
    });
};
c3_chart_internal_fn.mapTargetsToUniqueXs = function (targets) {
    var $$ = this;
    var xs = $$.d3.set($$.d3.merge(targets.map(function (t) {
        return t.values.map(function (v) {
            return +v.x;
        });
    }))).values();
    xs = $$.isTimeSeries() ? xs.map(function (x) {
        return new Date(+x);
    }) : xs.map(function (x) {
        return +x;
    });
    return xs.sort(function (a, b) {
        return a < b ? -1 : a > b ? 1 : a >= b ? 0 : NaN;
    });
};
c3_chart_internal_fn.addHiddenTargetIds = function (targetIds) {
    targetIds = targetIds instanceof Array ? targetIds : new Array(targetIds);
    for (var i = 0; i < targetIds.length; i++) {
        if (this.hiddenTargetIds.indexOf(targetIds[i]) < 0) {
            this.hiddenTargetIds = this.hiddenTargetIds.concat(targetIds[i]);
        }
    }
};
c3_chart_internal_fn.removeHiddenTargetIds = function (targetIds) {
    this.hiddenTargetIds = this.hiddenTargetIds.filter(function (id) {
        return targetIds.indexOf(id) < 0;
    });
};
c3_chart_internal_fn.addHiddenLegendIds = function (targetIds) {
    targetIds = targetIds instanceof Array ? targetIds : new Array(targetIds);
    for (var i = 0; i < targetIds.length; i++) {
        if (this.hiddenLegendIds.indexOf(targetIds[i]) < 0) {
            this.hiddenLegendIds = this.hiddenLegendIds.concat(targetIds[i]);
        }
    }
};
c3_chart_internal_fn.removeHiddenLegendIds = function (targetIds) {
    this.hiddenLegendIds = this.hiddenLegendIds.filter(function (id) {
        return targetIds.indexOf(id) < 0;
    });
};
c3_chart_internal_fn.getValuesAsIdKeyed = function (targets) {
    var ys = {};
    targets.forEach(function (t) {
        ys[t.id] = [];
        t.values.forEach(function (v) {
            ys[t.id].push(v.value);
        });
    });
    return ys;
};
c3_chart_internal_fn.checkValueInTargets = function (targets, checker) {
    var ids = Object.keys(targets),
        i,
        j,
        values;
    for (i = 0; i < ids.length; i++) {
        values = targets[ids[i]].values;
        for (j = 0; j < values.length; j++) {
            if (checker(values[j].value)) {
                return true;
            }
        }
    }
    return false;
};
c3_chart_internal_fn.hasNegativeValueInTargets = function (targets) {
    return this.checkValueInTargets(targets, function (v) {
        return v < 0;
    });
};
c3_chart_internal_fn.hasPositiveValueInTargets = function (targets) {
    return this.checkValueInTargets(targets, function (v) {
        return v > 0;
    });
};
c3_chart_internal_fn.isOrderDesc = function () {
    var config = this.config;
    return typeof config.data_order === 'string' && config.data_order.toLowerCase() === 'desc';
};
c3_chart_internal_fn.isOrderAsc = function () {
    var config = this.config;
    return typeof config.data_order === 'string' && config.data_order.toLowerCase() === 'asc';
};
c3_chart_internal_fn.getOrderFunction = function () {
    var $$ = this,
        config = $$.config,
        orderAsc = $$.isOrderAsc(),
        orderDesc = $$.isOrderDesc();
    if (orderAsc || orderDesc) {
        return function (t1, t2) {
            var reducer = function reducer(p, c) {
                return p + Math.abs(c.value);
            };
            var t1Sum = t1.values.reduce(reducer, 0),
                t2Sum = t2.values.reduce(reducer, 0);
            return orderDesc ? t2Sum - t1Sum : t1Sum - t2Sum;
        };
    } else if (isFunction(config.data_order)) {
        return config.data_order;
    } else if (isArray(config.data_order)) {
        var order = config.data_order;
        return function (t1, t2) {
            return order.indexOf(t1.id) - order.indexOf(t2.id);
        };
    }
};
c3_chart_internal_fn.orderTargets = function (targets) {
    var fct = this.getOrderFunction();
    if (fct) {
        targets.sort(fct);
        if (this.isOrderAsc() || this.isOrderDesc()) {
            targets.reverse();
        }
    }
    return targets;
};
c3_chart_internal_fn.filterByX = function (targets, x) {
    return this.d3.merge(targets.map(function (t) {
        return t.values;
    })).filter(function (v) {
        return v.x - x === 0;
    });
};
c3_chart_internal_fn.filterRemoveNull = function (data) {
    return data.filter(function (d) {
        return isValue(d.value);
    });
};
c3_chart_internal_fn.filterByXDomain = function (targets, xDomain) {
    return targets.map(function (t) {
        return {
            id: t.id,
            id_org: t.id_org,
            values: t.values.filter(function (v) {
                return xDomain[0] <= v.x && v.x <= xDomain[1];
            })
        };
    });
};
c3_chart_internal_fn.hasDataLabel = function () {
    var config = this.config;
    if (typeof config.data_labels === 'boolean' && config.data_labels) {
        return true;
    } else if (_typeof(config.data_labels) === 'object' && notEmpty(config.data_labels)) {
        return true;
    }
    return false;
};
c3_chart_internal_fn.getDataLabelLength = function (min, max, key) {
    var $$ = this,
        lengths = [0, 0],
        paddingCoef = 1.3;
    $$.selectChart.select('svg').selectAll('.dummy').data([min, max]).enter().append('text').text(function (d) {
        return $$.dataLabelFormat(d.id)(d);
    }).each(function (d, i) {
        lengths[i] = this.getBoundingClientRect()[key] * paddingCoef;
    }).remove();
    return lengths;
};
c3_chart_internal_fn.isNoneArc = function (d) {
    return this.hasTarget(this.data.targets, d.id);
}, c3_chart_internal_fn.isArc = function (d) {
    return 'data' in d && this.hasTarget(this.data.targets, d.data.id);
};
c3_chart_internal_fn.findSameXOfValues = function (values, index) {
    var i,
        targetX = values[index].x,
        sames = [];
    for (i = index - 1; i >= 0; i--) {
        if (targetX !== values[i].x) {
            break;
        }
        sames.push(values[i]);
    }
    for (i = index; i < values.length; i++) {
        if (targetX !== values[i].x) {
            break;
        }
        sames.push(values[i]);
    }
    return sames;
};

c3_chart_internal_fn.findClosestFromTargets = function (targets, pos) {
    var $$ = this,
        candidates;

    // map to array of closest points of each target
    candidates = targets.map(function (target) {
        return $$.findClosest(target.values, pos);
    });

    // decide closest point and return
    return $$.findClosest(candidates, pos);
};
c3_chart_internal_fn.findClosest = function (values, pos) {
    var $$ = this,
        minDist = $$.config.point_sensitivity,
        closest;

    // find mouseovering bar
    values.filter(function (v) {
        return v && $$.isBarType(v.id);
    }).forEach(function (v) {
        var shape = $$.main.select('.' + CLASS.bars + $$.getTargetSelectorSuffix(v.id) + ' .' + CLASS.bar + '-' + v.index).node();
        if (!closest && $$.isWithinBar(shape)) {
            closest = v;
        }
    });

    // find closest point from non-bar
    values.filter(function (v) {
        return v && !$$.isBarType(v.id);
    }).forEach(function (v) {
        var d = $$.dist(v, pos);
        if (d < minDist) {
            minDist = d;
            closest = v;
        }
    });

    return closest;
};
c3_chart_internal_fn.dist = function (data, pos) {
    var $$ = this,
        config = $$.config,
        xIndex = config.axis_rotated ? 1 : 0,
        yIndex = config.axis_rotated ? 0 : 1,
        y = $$.circleY(data, data.index),
        x = $$.x(data.x);
    return Math.sqrt(Math.pow(x - pos[xIndex], 2) + Math.pow(y - pos[yIndex], 2));
};
c3_chart_internal_fn.convertValuesToStep = function (values) {
    var converted = [].concat(values),
        i;

    if (!this.isCategorized()) {
        return values;
    }

    for (i = values.length + 1; 0 < i; i--) {
        converted[i] = converted[i - 1];
    }

    converted[0] = {
        x: converted[0].x - 1,
        value: converted[0].value,
        id: converted[0].id
    };
    converted[values.length + 1] = {
        x: converted[values.length].x + 1,
        value: converted[values.length].value,
        id: converted[values.length].id
    };

    return converted;
};
c3_chart_internal_fn.updateDataAttributes = function (name, attrs) {
    var $$ = this,
        config = $$.config,
        current = config['data_' + name];
    if (typeof attrs === 'undefined') {
        return current;
    }
    Object.keys(attrs).forEach(function (id) {
        current[id] = attrs[id];
    });
    $$.redraw({ withLegend: true });
    return current;
};

c3_chart_internal_fn.load = function (targets, args) {
    var $$ = this;
    if (targets) {
        // filter loading targets if needed
        if (args.filter) {
            targets = targets.filter(args.filter);
        }
        // set type if args.types || args.type specified
        if (args.type || args.types) {
            targets.forEach(function (t) {
                var type = args.types && args.types[t.id] ? args.types[t.id] : args.type;
                $$.setTargetType(t.id, type);
            });
        }
        // Update/Add data
        $$.data.targets.forEach(function (d) {
            for (var i = 0; i < targets.length; i++) {
                if (d.id === targets[i].id) {
                    d.values = targets[i].values;
                    targets.splice(i, 1);
                    break;
                }
            }
        });
        $$.data.targets = $$.data.targets.concat(targets); // add remained
    }

    // Set targets
    $$.updateTargets($$.data.targets);

    // Redraw with new targets
    $$.redraw({ withUpdateOrgXDomain: true, withUpdateXDomain: true, withLegend: true });

    if (args.done) {
        args.done();
    }
};
c3_chart_internal_fn.loadFromArgs = function (args) {
    var $$ = this;
    if (args.data) {
        $$.load($$.convertDataToTargets(args.data), args);
    } else if (args.url) {
        $$.convertUrlToData(args.url, args.mimeType, args.headers, args.keys, function (data) {
            $$.load($$.convertDataToTargets(data), args);
        });
    } else if (args.json) {
        $$.load($$.convertDataToTargets($$.convertJsonToData(args.json, args.keys)), args);
    } else if (args.rows) {
        $$.load($$.convertDataToTargets($$.convertRowsToData(args.rows)), args);
    } else if (args.columns) {
        $$.load($$.convertDataToTargets($$.convertColumnsToData(args.columns)), args);
    } else {
        $$.load(null, args);
    }
};
c3_chart_internal_fn.unload = function (targetIds, done) {
    var $$ = this;
    if (!done) {
        done = function done() {};
    }
    // filter existing target
    targetIds = targetIds.filter(function (id) {
        return $$.hasTarget($$.data.targets, id);
    });
    // If no target, call done and return
    if (!targetIds || targetIds.length === 0) {
        done();
        return;
    }
    $$.svg.selectAll(targetIds.map(function (id) {
        return $$.selectorTarget(id);
    })).transition().style('opacity', 0).remove().call($$.endall, done);
    targetIds.forEach(function (id) {
        // Reset fadein for future load
        $$.withoutFadeIn[id] = false;
        // Remove target's elements
        if ($$.legend) {
            $$.legend.selectAll('.' + CLASS.legendItem + $$.getTargetSelectorSuffix(id)).remove();
        }
        // Remove target
        $$.data.targets = $$.data.targets.filter(function (t) {
            return t.id !== id;
        });
    });
};

c3_chart_internal_fn.getYDomainMin = function (targets) {
    var $$ = this,
        config = $$.config,
        ids = $$.mapToIds(targets),
        ys = $$.getValuesAsIdKeyed(targets),
        j,
        k,
        baseId,
        idsInGroup,
        id,
        hasNegativeValue;
    if (config.data_groups.length > 0) {
        hasNegativeValue = $$.hasNegativeValueInTargets(targets);
        for (j = 0; j < config.data_groups.length; j++) {
            // Determine baseId
            idsInGroup = config.data_groups[j].filter(function (id) {
                return ids.indexOf(id) >= 0;
            });
            if (idsInGroup.length === 0) {
                continue;
            }
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
                if (!ys[id]) {
                    continue;
                }
                ys[id].forEach(function (v, i) {
                    if ($$.axis.getId(id) === $$.axis.getId(baseId) && ys[baseId] && !(hasNegativeValue && +v > 0)) {
                        ys[baseId][i] += +v;
                    }
                });
            }
        }
    }
    return $$.d3.min(Object.keys(ys).map(function (key) {
        return $$.d3.min(ys[key]);
    }));
};
c3_chart_internal_fn.getYDomainMax = function (targets) {
    var $$ = this,
        config = $$.config,
        ids = $$.mapToIds(targets),
        ys = $$.getValuesAsIdKeyed(targets),
        j,
        k,
        baseId,
        idsInGroup,
        id,
        hasPositiveValue;
    if (config.data_groups.length > 0) {
        hasPositiveValue = $$.hasPositiveValueInTargets(targets);
        for (j = 0; j < config.data_groups.length; j++) {
            // Determine baseId
            idsInGroup = config.data_groups[j].filter(function (id) {
                return ids.indexOf(id) >= 0;
            });
            if (idsInGroup.length === 0) {
                continue;
            }
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
                if (!ys[id]) {
                    continue;
                }
                ys[id].forEach(function (v, i) {
                    if ($$.axis.getId(id) === $$.axis.getId(baseId) && ys[baseId] && !(hasPositiveValue && +v < 0)) {
                        ys[baseId][i] += +v;
                    }
                });
            }
        }
    }
    return $$.d3.max(Object.keys(ys).map(function (key) {
        return $$.d3.max(ys[key]);
    }));
};
c3_chart_internal_fn.getYDomain = function (targets, axisId, xDomain) {
    var $$ = this,
        config = $$.config,
        targetsByAxisId = targets.filter(function (t) {
        return $$.axis.getId(t.id) === axisId;
    }),
        yTargets = xDomain ? $$.filterByXDomain(targetsByAxisId, xDomain) : targetsByAxisId,
        yMin = axisId === 'y2' ? config.axis_y2_min : config.axis_y_min,
        yMax = axisId === 'y2' ? config.axis_y2_max : config.axis_y_max,
        yDomainMin = $$.getYDomainMin(yTargets),
        yDomainMax = $$.getYDomainMax(yTargets),
        domain,
        domainLength,
        padding,
        padding_top,
        padding_bottom,
        center = axisId === 'y2' ? config.axis_y2_center : config.axis_y_center,
        yDomainAbs,
        lengths,
        diff,
        ratio,
        isAllPositive,
        isAllNegative,
        isZeroBased = $$.hasType('bar', yTargets) && config.bar_zerobased || $$.hasType('area', yTargets) && config.area_zerobased,
        isInverted = axisId === 'y2' ? config.axis_y2_inverted : config.axis_y_inverted,
        showHorizontalDataLabel = $$.hasDataLabel() && config.axis_rotated,
        showVerticalDataLabel = $$.hasDataLabel() && !config.axis_rotated;

    // MEMO: avoid inverting domain unexpectedly
    yDomainMin = isValue(yMin) ? yMin : isValue(yMax) ? yDomainMin < yMax ? yDomainMin : yMax - 10 : yDomainMin;
    yDomainMax = isValue(yMax) ? yMax : isValue(yMin) ? yMin < yDomainMax ? yDomainMax : yMin + 10 : yDomainMax;

    if (yTargets.length === 0) {
        // use current domain if target of axisId is none
        return axisId === 'y2' ? $$.y2.domain() : $$.y.domain();
    }
    if (isNaN(yDomainMin)) {
        // set minimum to zero when not number
        yDomainMin = 0;
    }
    if (isNaN(yDomainMax)) {
        // set maximum to have same value as yDomainMin
        yDomainMax = yDomainMin;
    }
    if (yDomainMin === yDomainMax) {
        yDomainMin < 0 ? yDomainMax = 0 : yDomainMin = 0;
    }
    isAllPositive = yDomainMin >= 0 && yDomainMax >= 0;
    isAllNegative = yDomainMin <= 0 && yDomainMax <= 0;

    // Cancel zerobased if axis_*_min / axis_*_max specified
    if (isValue(yMin) && isAllPositive || isValue(yMax) && isAllNegative) {
        isZeroBased = false;
    }

    // Bar/Area chart should be 0-based if all positive|negative
    if (isZeroBased) {
        if (isAllPositive) {
            yDomainMin = 0;
        }
        if (isAllNegative) {
            yDomainMax = 0;
        }
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
        if (isAllPositive) {
            padding_bottom = yDomainMin;
        }
        if (isAllNegative) {
            padding_top = -yDomainMax;
        }
    }
    domain = [yDomainMin - padding_bottom, yDomainMax + padding_top];
    return isInverted ? domain.reverse() : domain;
};
c3_chart_internal_fn.getXDomainMin = function (targets) {
    var $$ = this,
        config = $$.config;
    return isDefined(config.axis_x_min) ? $$.isTimeSeries() ? this.parseDate(config.axis_x_min) : config.axis_x_min : $$.d3.min(targets, function (t) {
        return $$.d3.min(t.values, function (v) {
            return v.x;
        });
    });
};
c3_chart_internal_fn.getXDomainMax = function (targets) {
    var $$ = this,
        config = $$.config;
    return isDefined(config.axis_x_max) ? $$.isTimeSeries() ? this.parseDate(config.axis_x_max) : config.axis_x_max : $$.d3.max(targets, function (t) {
        return $$.d3.max(t.values, function (v) {
            return v.x;
        });
    });
};
c3_chart_internal_fn.getXDomainPadding = function (domain) {
    var $$ = this,
        config = $$.config,
        diff = domain[1] - domain[0],
        maxDataCount,
        padding,
        paddingLeft,
        paddingRight;
    if ($$.isCategorized()) {
        padding = 0;
    } else if ($$.hasType('bar')) {
        maxDataCount = $$.getMaxDataCount();
        padding = maxDataCount > 1 ? diff / (maxDataCount - 1) / 2 : 0.5;
    } else {
        padding = diff * 0.01;
    }
    if (_typeof(config.axis_x_padding) === 'object' && notEmpty(config.axis_x_padding)) {
        paddingLeft = isValue(config.axis_x_padding.left) ? config.axis_x_padding.left : padding;
        paddingRight = isValue(config.axis_x_padding.right) ? config.axis_x_padding.right : padding;
    } else if (typeof config.axis_x_padding === 'number') {
        paddingLeft = paddingRight = config.axis_x_padding;
    } else {
        paddingLeft = paddingRight = padding;
    }
    return { left: paddingLeft, right: paddingRight };
};
c3_chart_internal_fn.getXDomain = function (targets) {
    var $$ = this,
        xDomain = [$$.getXDomainMin(targets), $$.getXDomainMax(targets)],
        firstX = xDomain[0],
        lastX = xDomain[1],
        padding = $$.getXDomainPadding(xDomain),
        min = 0,
        max = 0;
    // show center of x domain if min and max are the same
    if (firstX - lastX === 0 && !$$.isCategorized()) {
        if ($$.isTimeSeries()) {
            firstX = new Date(firstX.getTime() * 0.5);
            lastX = new Date(lastX.getTime() * 1.5);
        } else {
            firstX = firstX === 0 ? 1 : firstX * 0.5;
            lastX = lastX === 0 ? -1 : lastX * 1.5;
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
    var $$ = this,
        config = $$.config;

    if (withUpdateOrgXDomain) {
        $$.x.domain(domain ? domain : $$.d3.extent($$.getXDomain(targets)));
        $$.orgXDomain = $$.x.domain();
        if (config.zoom_enabled) {
            $$.zoom.scale($$.x).updateScaleExtent();
        }
        $$.subX.domain($$.x.domain());
        if ($$.brush) {
            $$.brush.scale($$.subX);
        }
    }
    if (withUpdateXDomain) {
        $$.x.domain(domain ? domain : !$$.brush || $$.brush.empty() ? $$.orgXDomain : $$.brush.extent());
        if (config.zoom_enabled) {
            $$.zoom.scale($$.x).updateScaleExtent();
        }
    }

    // Trim domain when too big by zoom mousemove event
    if (withTrim) {
        $$.x.domain($$.trimXDomain($$.x.orgDomain()));
    }

    return $$.x.domain();
};
c3_chart_internal_fn.trimXDomain = function (domain) {
    var zoomDomain = this.getZoomDomain(),
        min = zoomDomain[0],
        max = zoomDomain[1];
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

c3_chart_internal_fn.drag = function (mouse) {
    var $$ = this,
        config = $$.config,
        main = $$.main,
        d3 = $$.d3;
    var sx, sy, mx, my, minX, maxX, minY, maxY;

    if ($$.hasArcType()) {
        return;
    }
    if (!config.data_selection_enabled) {
        return;
    } // do nothing if not selectable
    if (config.zoom_enabled && !$$.zoom.altDomain) {
        return;
    } // skip if zoomable because of conflict drag dehavior
    if (!config.data_selection_multiple) {
        return;
    } // skip when single selection because drag is used for multiple selection

    sx = $$.dragStart[0];
    sy = $$.dragStart[1];
    mx = mouse[0];
    my = mouse[1];
    minX = Math.min(sx, mx);
    maxX = Math.max(sx, mx);
    minY = config.data_selection_grouped ? $$.margin.top : Math.min(sy, my);
    maxY = config.data_selection_grouped ? $$.height : Math.max(sy, my);

    main.select('.' + CLASS.dragarea).attr('x', minX).attr('y', minY).attr('width', maxX - minX).attr('height', maxY - minY);
    // TODO: binary search when multiple xs
    main.selectAll('.' + CLASS.shapes).selectAll('.' + CLASS.shape).filter(function (d) {
        return config.data_selection_isselectable(d);
    }).each(function (d, i) {
        var shape = d3.select(this),
            isSelected = shape.classed(CLASS.SELECTED),
            isIncluded = shape.classed(CLASS.INCLUDED),
            _x,
            _y,
            _w,
            _h,
            toggle,
            isWithin = false,
            box;
        if (shape.classed(CLASS.circle)) {
            _x = shape.attr("cx") * 1;
            _y = shape.attr("cy") * 1;
            toggle = $$.togglePoint;
            isWithin = minX < _x && _x < maxX && minY < _y && _y < maxY;
        } else if (shape.classed(CLASS.bar)) {
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
    var $$ = this,
        config = $$.config;
    if ($$.hasArcType()) {
        return;
    }
    if (!config.data_selection_enabled) {
        return;
    } // do nothing if not selectable
    $$.dragStart = mouse;
    $$.main.select('.' + CLASS.chart).append('rect').attr('class', CLASS.dragarea).style('opacity', 0.1);
    $$.dragging = true;
};

c3_chart_internal_fn.dragend = function () {
    var $$ = this,
        config = $$.config;
    if ($$.hasArcType()) {
        return;
    }
    if (!config.data_selection_enabled) {
        return;
    } // do nothing if not selectable
    $$.main.select('.' + CLASS.dragarea).transition().duration(100).style('opacity', 0).remove();
    $$.main.selectAll('.' + CLASS.shape).classed(CLASS.INCLUDED, false);
    $$.dragging = false;
};

c3_chart_internal_fn.getYFormat = function (forArc) {
    var $$ = this,
        formatForY = forArc && !$$.hasType('gauge') ? $$.defaultArcValueFormat : $$.yFormat,
        formatForY2 = forArc && !$$.hasType('gauge') ? $$.defaultArcValueFormat : $$.y2Format;
    return function (v, ratio, id) {
        var format = $$.axis.getId(id) === 'y2' ? formatForY2 : formatForY;
        return format.call($$, v, ratio);
    };
};
c3_chart_internal_fn.yFormat = function (v) {
    var $$ = this,
        config = $$.config,
        format = config.axis_y_tick_format ? config.axis_y_tick_format : $$.defaultValueFormat;
    return format(v);
};
c3_chart_internal_fn.y2Format = function (v) {
    var $$ = this,
        config = $$.config,
        format = config.axis_y2_tick_format ? config.axis_y2_tick_format : $$.defaultValueFormat;
    return format(v);
};
c3_chart_internal_fn.defaultValueFormat = function (v) {
    return isValue(v) ? +v : "";
};
c3_chart_internal_fn.defaultArcValueFormat = function (v, ratio) {
    return (ratio * 100).toFixed(1) + '%';
};
c3_chart_internal_fn.dataLabelFormat = function (targetId) {
    var $$ = this,
        data_labels = $$.config.data_labels,
        format,
        defaultFormat = function defaultFormat(v) {
        return isValue(v) ? +v : "";
    };
    // find format according to axis id
    if (typeof data_labels.format === 'function') {
        format = data_labels.format;
    } else if (_typeof(data_labels.format) === 'object') {
        if (data_labels.format[targetId]) {
            format = data_labels.format[targetId] === true ? defaultFormat : data_labels.format[targetId];
        } else {
            format = function format() {
                return '';
            };
        }
    } else {
        format = defaultFormat;
    }
    return format;
};

c3_chart_internal_fn.initGrid = function () {
    var $$ = this,
        config = $$.config,
        d3 = $$.d3;
    $$.grid = $$.main.append('g').attr("clip-path", $$.clipPathForGrid).attr('class', CLASS.grid);
    if (config.grid_x_show) {
        $$.grid.append("g").attr("class", CLASS.xgrids);
    }
    if (config.grid_y_show) {
        $$.grid.append('g').attr('class', CLASS.ygrids);
    }
    if (config.grid_focus_show) {
        $$.grid.append('g').attr("class", CLASS.xgridFocus).append('line').attr('class', CLASS.xgridFocus);
    }
    $$.xgrid = d3.selectAll([]);
    if (!config.grid_lines_front) {
        $$.initGridLines();
    }
};
c3_chart_internal_fn.initGridLines = function () {
    var $$ = this,
        d3 = $$.d3;
    $$.gridLines = $$.main.append('g').attr("clip-path", $$.clipPathForGrid).attr('class', CLASS.grid + ' ' + CLASS.gridLines);
    $$.gridLines.append('g').attr("class", CLASS.xgridLines);
    $$.gridLines.append('g').attr('class', CLASS.ygridLines);
    $$.xgridLines = d3.selectAll([]);
};
c3_chart_internal_fn.updateXGrid = function (withoutUpdate) {
    var $$ = this,
        config = $$.config,
        d3 = $$.d3,
        xgridData = $$.generateGridData(config.grid_x_type, $$.x),
        tickOffset = $$.isCategorized() ? $$.xAxis.tickOffset() : 0;

    $$.xgridAttr = config.axis_rotated ? {
        'x1': 0,
        'x2': $$.width,
        'y1': function y1(d) {
            return $$.x(d) - tickOffset;
        },
        'y2': function y2(d) {
            return $$.x(d) - tickOffset;
        }
    } : {
        'x1': function x1(d) {
            return $$.x(d) + tickOffset;
        },
        'x2': function x2(d) {
            return $$.x(d) + tickOffset;
        },
        'y1': 0,
        'y2': $$.height
    };

    $$.xgrid = $$.main.select('.' + CLASS.xgrids).selectAll('.' + CLASS.xgrid).data(xgridData);
    $$.xgrid.enter().append('line').attr("class", CLASS.xgrid);
    if (!withoutUpdate) {
        $$.xgrid.attr($$.xgridAttr).style("opacity", function () {
            return +d3.select(this).attr(config.axis_rotated ? 'y1' : 'x1') === (config.axis_rotated ? $$.height : 0) ? 0 : 1;
        });
    }
    $$.xgrid.exit().remove();
};

c3_chart_internal_fn.updateYGrid = function () {
    var $$ = this,
        config = $$.config,
        gridValues = $$.yAxis.tickValues() || $$.y.ticks(config.grid_y_ticks);
    $$.ygrid = $$.main.select('.' + CLASS.ygrids).selectAll('.' + CLASS.ygrid).data(gridValues);
    $$.ygrid.enter().append('line').attr('class', CLASS.ygrid);
    $$.ygrid.attr("x1", config.axis_rotated ? $$.y : 0).attr("x2", config.axis_rotated ? $$.y : $$.width).attr("y1", config.axis_rotated ? 0 : $$.y).attr("y2", config.axis_rotated ? $$.height : $$.y);
    $$.ygrid.exit().remove();
    $$.smoothLines($$.ygrid, 'grid');
};

c3_chart_internal_fn.gridTextAnchor = function (d) {
    return d.position ? d.position : "end";
};
c3_chart_internal_fn.gridTextDx = function (d) {
    return d.position === 'start' ? 4 : d.position === 'middle' ? 0 : -4;
};
c3_chart_internal_fn.xGridTextX = function (d) {
    return d.position === 'start' ? -this.height : d.position === 'middle' ? -this.height / 2 : 0;
};
c3_chart_internal_fn.yGridTextX = function (d) {
    return d.position === 'start' ? 0 : d.position === 'middle' ? this.width / 2 : this.width;
};
c3_chart_internal_fn.updateGrid = function (duration) {
    var $$ = this,
        main = $$.main,
        config = $$.config,
        xgridLine,
        ygridLine,
        yv;

    // hide if arc type
    $$.grid.style('visibility', $$.hasArcType() ? 'hidden' : 'visible');

    main.select('line.' + CLASS.xgridFocus).style("visibility", "hidden");
    if (config.grid_x_show) {
        $$.updateXGrid();
    }
    $$.xgridLines = main.select('.' + CLASS.xgridLines).selectAll('.' + CLASS.xgridLine).data(config.grid_x_lines);
    // enter
    xgridLine = $$.xgridLines.enter().append('g').attr("class", function (d) {
        return CLASS.xgridLine + (d['class'] ? ' ' + d['class'] : '');
    });
    xgridLine.append('line').style("opacity", 0);
    xgridLine.append('text').attr("text-anchor", $$.gridTextAnchor).attr("transform", config.axis_rotated ? "" : "rotate(-90)").attr('dx', $$.gridTextDx).attr('dy', -5).style("opacity", 0);
    // udpate
    // done in d3.transition() of the end of this function
    // exit
    $$.xgridLines.exit().transition().duration(duration).style("opacity", 0).remove();

    // Y-Grid
    if (config.grid_y_show) {
        $$.updateYGrid();
    }
    $$.ygridLines = main.select('.' + CLASS.ygridLines).selectAll('.' + CLASS.ygridLine).data(config.grid_y_lines);
    // enter
    ygridLine = $$.ygridLines.enter().append('g').attr("class", function (d) {
        return CLASS.ygridLine + (d['class'] ? ' ' + d['class'] : '');
    });
    ygridLine.append('line').style("opacity", 0);
    ygridLine.append('text').attr("text-anchor", $$.gridTextAnchor).attr("transform", config.axis_rotated ? "rotate(-90)" : "").attr('dx', $$.gridTextDx).attr('dy', -5).style("opacity", 0);
    // update
    yv = $$.yv.bind($$);
    $$.ygridLines.select('line').transition().duration(duration).attr("x1", config.axis_rotated ? yv : 0).attr("x2", config.axis_rotated ? yv : $$.width).attr("y1", config.axis_rotated ? 0 : yv).attr("y2", config.axis_rotated ? $$.height : yv).style("opacity", 1);
    $$.ygridLines.select('text').transition().duration(duration).attr("x", config.axis_rotated ? $$.xGridTextX.bind($$) : $$.yGridTextX.bind($$)).attr("y", yv).text(function (d) {
        return d.text;
    }).style("opacity", 1);
    // exit
    $$.ygridLines.exit().transition().duration(duration).style("opacity", 0).remove();
};
c3_chart_internal_fn.redrawGrid = function (withTransition) {
    var $$ = this,
        config = $$.config,
        xv = $$.xv.bind($$),
        lines = $$.xgridLines.select('line'),
        texts = $$.xgridLines.select('text');
    return [(withTransition ? lines.transition() : lines).attr("x1", config.axis_rotated ? 0 : xv).attr("x2", config.axis_rotated ? $$.width : xv).attr("y1", config.axis_rotated ? xv : 0).attr("y2", config.axis_rotated ? xv : $$.height).style("opacity", 1), (withTransition ? texts.transition() : texts).attr("x", config.axis_rotated ? $$.yGridTextX.bind($$) : $$.xGridTextX.bind($$)).attr("y", xv).text(function (d) {
        return d.text;
    }).style("opacity", 1)];
};
c3_chart_internal_fn.showXGridFocus = function (selectedData) {
    var $$ = this,
        config = $$.config,
        dataToShow = selectedData.filter(function (d) {
        return d && isValue(d.value);
    }),
        focusEl = $$.main.selectAll('line.' + CLASS.xgridFocus),
        xx = $$.xx.bind($$);
    if (!config.tooltip_show) {
        return;
    }
    // Hide when scatter plot exists
    if ($$.hasType('scatter') || $$.hasArcType()) {
        return;
    }
    focusEl.style("visibility", "visible").data([dataToShow[0]]).attr(config.axis_rotated ? 'y1' : 'x1', xx).attr(config.axis_rotated ? 'y2' : 'x2', xx);
    $$.smoothLines(focusEl, 'grid');
};
c3_chart_internal_fn.hideXGridFocus = function () {
    this.main.select('line.' + CLASS.xgridFocus).style("visibility", "hidden");
};
c3_chart_internal_fn.updateXgridFocus = function () {
    var $$ = this,
        config = $$.config;
    $$.main.select('line.' + CLASS.xgridFocus).attr("x1", config.axis_rotated ? 0 : -10).attr("x2", config.axis_rotated ? $$.width : -10).attr("y1", config.axis_rotated ? -10 : 0).attr("y2", config.axis_rotated ? -10 : $$.height);
};
c3_chart_internal_fn.generateGridData = function (type, scale) {
    var $$ = this,
        gridData = [],
        xDomain,
        firstYear,
        lastYear,
        i,
        tickNum = $$.main.select("." + CLASS.axisX).selectAll('.tick').size();
    if (type === 'year') {
        xDomain = $$.getXDomain();
        firstYear = xDomain[0].getFullYear();
        lastYear = xDomain[1].getFullYear();
        for (i = firstYear; i <= lastYear; i++) {
            gridData.push(new Date(i + '-01-01 00:00:00'));
        }
    } else {
        gridData = scale.ticks(10);
        if (gridData.length > tickNum) {
            // use only int
            gridData = gridData.filter(function (d) {
                return ("" + d).indexOf('.') < 0;
            });
        }
    }
    return gridData;
};
c3_chart_internal_fn.getGridFilterToRemove = function (params) {
    return params ? function (line) {
        var found = false;
        [].concat(params).forEach(function (param) {
            if ('value' in param && line.value === param.value || 'class' in param && line['class'] === param['class']) {
                found = true;
            }
        });
        return found;
    } : function () {
        return true;
    };
};
c3_chart_internal_fn.removeGridLines = function (params, forX) {
    var $$ = this,
        config = $$.config,
        toRemove = $$.getGridFilterToRemove(params),
        toShow = function toShow(line) {
        return !toRemove(line);
    },
        classLines = forX ? CLASS.xgridLines : CLASS.ygridLines,
        classLine = forX ? CLASS.xgridLine : CLASS.ygridLine;
    $$.main.select('.' + classLines).selectAll('.' + classLine).filter(toRemove).transition().duration(config.transition_duration).style('opacity', 0).remove();
    if (forX) {
        config.grid_x_lines = config.grid_x_lines.filter(toShow);
    } else {
        config.grid_y_lines = config.grid_y_lines.filter(toShow);
    }
};

c3_chart_internal_fn.initEventRect = function () {
    var $$ = this;
    $$.main.select('.' + CLASS.chart).append("g").attr("class", CLASS.eventRects).style('fill-opacity', 0);
};
c3_chart_internal_fn.redrawEventRect = function () {
    var $$ = this,
        config = $$.config,
        eventRectUpdate,
        maxDataCountTarget,
        isMultipleX = $$.isMultipleX();

    // rects for mouseover
    var eventRects = $$.main.select('.' + CLASS.eventRects).style('cursor', config.zoom_enabled ? config.axis_rotated ? 'ns-resize' : 'ew-resize' : null).classed(CLASS.eventRectsMultiple, isMultipleX).classed(CLASS.eventRectsSingle, !isMultipleX);

    // clear old rects
    eventRects.selectAll('.' + CLASS.eventRect).remove();

    // open as public variable
    $$.eventRect = eventRects.selectAll('.' + CLASS.eventRect);

    if (isMultipleX) {
        eventRectUpdate = $$.eventRect.data([0]);
        // enter : only one rect will be added
        $$.generateEventRectsForMultipleXs(eventRectUpdate.enter());
        // update
        $$.updateEventRect(eventRectUpdate);
        // exit : not needed because always only one rect exists
    } else {
        // Set data and update $$.eventRect
        maxDataCountTarget = $$.getMaxDataCountTarget($$.data.targets);
        eventRects.datum(maxDataCountTarget ? maxDataCountTarget.values : []);
        $$.eventRect = eventRects.selectAll('.' + CLASS.eventRect);
        eventRectUpdate = $$.eventRect.data(function (d) {
            return d;
        });
        // enter
        $$.generateEventRectsForSingleX(eventRectUpdate.enter());
        // update
        $$.updateEventRect(eventRectUpdate);
        // exit
        eventRectUpdate.exit().remove();
    }
};
c3_chart_internal_fn.updateEventRect = function (eventRectUpdate) {
    var $$ = this,
        config = $$.config,
        x,
        y,
        w,
        h,
        rectW,
        rectX;

    // set update selection if null
    eventRectUpdate = eventRectUpdate || $$.eventRect.data(function (d) {
        return d;
    });

    if ($$.isMultipleX()) {
        // TODO: rotated not supported yet
        x = 0;
        y = 0;
        w = $$.width;
        h = $$.height;
    } else {
        if (($$.isCustomX() || $$.isTimeSeries()) && !$$.isCategorized()) {

            // update index for x that is used by prevX and nextX
            $$.updateXs();

            rectW = function rectW(d) {
                var prevX = $$.getPrevX(d.index),
                    nextX = $$.getNextX(d.index);

                // if there this is a single data point make the eventRect full width (or height)
                if (prevX === null && nextX === null) {
                    return config.axis_rotated ? $$.height : $$.width;
                }

                if (prevX === null) {
                    prevX = $$.x.domain()[0];
                }
                if (nextX === null) {
                    nextX = $$.x.domain()[1];
                }

                return Math.max(0, ($$.x(nextX) - $$.x(prevX)) / 2);
            };
            rectX = function rectX(d) {
                var prevX = $$.getPrevX(d.index),
                    nextX = $$.getNextX(d.index),
                    thisX = $$.data.xs[d.id][d.index];

                // if there this is a single data point position the eventRect at 0
                if (prevX === null && nextX === null) {
                    return 0;
                }

                if (prevX === null) {
                    prevX = $$.x.domain()[0];
                }

                return ($$.x(thisX) + $$.x(prevX)) / 2;
            };
        } else {
            rectW = $$.getEventRectWidth();
            rectX = function rectX(d) {
                return $$.x(d.x) - rectW / 2;
            };
        }
        x = config.axis_rotated ? 0 : rectX;
        y = config.axis_rotated ? rectX : 0;
        w = config.axis_rotated ? $$.width : rectW;
        h = config.axis_rotated ? rectW : $$.height;
    }

    eventRectUpdate.attr('class', $$.classEvent.bind($$)).attr("x", x).attr("y", y).attr("width", w).attr("height", h);
};
c3_chart_internal_fn.generateEventRectsForSingleX = function (eventRectEnter) {
    var $$ = this,
        d3 = $$.d3,
        config = $$.config;
    eventRectEnter.append("rect").attr("class", $$.classEvent.bind($$)).style("cursor", config.data_selection_enabled && config.data_selection_grouped ? "pointer" : null).on('mouseover', function (d) {
        var index = d.index;

        if ($$.dragging || $$.flowing) {
            return;
        } // do nothing while dragging/flowing
        if ($$.hasArcType()) {
            return;
        }

        // Expand shapes for selection
        if (config.point_focus_expand_enabled) {
            $$.expandCircles(index, null, true);
        }
        $$.expandBars(index, null, true);

        // Call event handler
        $$.main.selectAll('.' + CLASS.shape + '-' + index).each(function (d) {
            config.data_onmouseover.call($$.api, d);
        });
    }).on('mouseout', function (d) {
        var index = d.index;
        if (!$$.config) {
            return;
        } // chart is destroyed
        if ($$.hasArcType()) {
            return;
        }
        $$.hideXGridFocus();
        $$.hideTooltip();
        // Undo expanded shapes
        $$.unexpandCircles();
        $$.unexpandBars();
        // Call event handler
        $$.main.selectAll('.' + CLASS.shape + '-' + index).each(function (d) {
            config.data_onmouseout.call($$.api, d);
        });
    }).on('mousemove', function (d) {
        var selectedData,
            index = d.index,
            eventRect = $$.svg.select('.' + CLASS.eventRect + '-' + index);

        if ($$.dragging || $$.flowing) {
            return;
        } // do nothing while dragging/flowing
        if ($$.hasArcType()) {
            return;
        }

        if ($$.isStepType(d) && $$.config.line_step_type === 'step-after' && d3.mouse(this)[0] < $$.x($$.getXValue(d.id, index))) {
            index -= 1;
        }

        // Show tooltip
        selectedData = $$.filterTargetsToShow($$.data.targets).map(function (t) {
            return $$.addName($$.getValueOnIndex(t.values, index));
        });

        if (config.tooltip_grouped) {
            $$.showTooltip(selectedData, this);
            $$.showXGridFocus(selectedData);
        }

        if (config.tooltip_grouped && (!config.data_selection_enabled || config.data_selection_grouped)) {
            return;
        }

        $$.main.selectAll('.' + CLASS.shape + '-' + index).each(function () {
            d3.select(this).classed(CLASS.EXPANDED, true);
            if (config.data_selection_enabled) {
                eventRect.style('cursor', config.data_selection_grouped ? 'pointer' : null);
            }
            if (!config.tooltip_grouped) {
                $$.hideXGridFocus();
                $$.hideTooltip();
                if (!config.data_selection_grouped) {
                    $$.unexpandCircles(index);
                    $$.unexpandBars(index);
                }
            }
        }).filter(function (d) {
            return $$.isWithinShape(this, d);
        }).each(function (d) {
            if (config.data_selection_enabled && (config.data_selection_grouped || config.data_selection_isselectable(d))) {
                eventRect.style('cursor', 'pointer');
            }
            if (!config.tooltip_grouped) {
                $$.showTooltip([d], this);
                $$.showXGridFocus([d]);
                if (config.point_focus_expand_enabled) {
                    $$.expandCircles(index, d.id, true);
                }
                $$.expandBars(index, d.id, true);
            }
        });
    }).on('click', function (d) {
        var index = d.index;
        if ($$.hasArcType() || !$$.toggleShape) {
            return;
        }
        if ($$.cancelClick) {
            $$.cancelClick = false;
            return;
        }
        if ($$.isStepType(d) && config.line_step_type === 'step-after' && d3.mouse(this)[0] < $$.x($$.getXValue(d.id, index))) {
            index -= 1;
        }
        $$.main.selectAll('.' + CLASS.shape + '-' + index).each(function (d) {
            if (config.data_selection_grouped || $$.isWithinShape(this, d)) {
                $$.toggleShape(this, d, index);
                $$.config.data_onclick.call($$.api, d, this);
            }
        });
    }).call(config.data_selection_draggable && $$.drag ? d3.behavior.drag().origin(Object).on('drag', function () {
        $$.drag(d3.mouse(this));
    }).on('dragstart', function () {
        $$.dragstart(d3.mouse(this));
    }).on('dragend', function () {
        $$.dragend();
    }) : function () {});
};

c3_chart_internal_fn.generateEventRectsForMultipleXs = function (eventRectEnter) {
    var $$ = this,
        d3 = $$.d3,
        config = $$.config;

    function mouseout() {
        $$.svg.select('.' + CLASS.eventRect).style('cursor', null);
        $$.hideXGridFocus();
        $$.hideTooltip();
        $$.unexpandCircles();
        $$.unexpandBars();
    }

    eventRectEnter.append('rect').attr('x', 0).attr('y', 0).attr('width', $$.width).attr('height', $$.height).attr('class', CLASS.eventRect).on('mouseout', function () {
        if (!$$.config) {
            return;
        } // chart is destroyed
        if ($$.hasArcType()) {
            return;
        }
        mouseout();
    }).on('mousemove', function () {
        var targetsToShow = $$.filterTargetsToShow($$.data.targets);
        var mouse, closest, sameXData, selectedData;

        if ($$.dragging) {
            return;
        } // do nothing when dragging
        if ($$.hasArcType(targetsToShow)) {
            return;
        }

        mouse = d3.mouse(this);
        closest = $$.findClosestFromTargets(targetsToShow, mouse);

        if ($$.mouseover && (!closest || closest.id !== $$.mouseover.id)) {
            config.data_onmouseout.call($$.api, $$.mouseover);
            $$.mouseover = undefined;
        }

        if (!closest) {
            mouseout();
            return;
        }

        if ($$.isScatterType(closest) || !config.tooltip_grouped) {
            sameXData = [closest];
        } else {
            sameXData = $$.filterByX(targetsToShow, closest.x);
        }

        // show tooltip when cursor is close to some point
        selectedData = sameXData.map(function (d) {
            return $$.addName(d);
        });
        $$.showTooltip(selectedData, this);

        // expand points
        if (config.point_focus_expand_enabled) {
            $$.expandCircles(closest.index, closest.id, true);
        }
        $$.expandBars(closest.index, closest.id, true);

        // Show xgrid focus line
        $$.showXGridFocus(selectedData);

        // Show cursor as pointer if point is close to mouse position
        if ($$.isBarType(closest.id) || $$.dist(closest, mouse) < config.point_sensitivity) {
            $$.svg.select('.' + CLASS.eventRect).style('cursor', 'pointer');
            if (!$$.mouseover) {
                config.data_onmouseover.call($$.api, closest);
                $$.mouseover = closest;
            }
        }
    }).on('click', function () {
        var targetsToShow = $$.filterTargetsToShow($$.data.targets);
        var mouse, closest;
        if ($$.hasArcType(targetsToShow)) {
            return;
        }

        mouse = d3.mouse(this);
        closest = $$.findClosestFromTargets(targetsToShow, mouse);
        if (!closest) {
            return;
        }
        // select if selection enabled
        if ($$.isBarType(closest.id) || $$.dist(closest, mouse) < config.point_sensitivity) {
            $$.main.selectAll('.' + CLASS.shapes + $$.getTargetSelectorSuffix(closest.id)).selectAll('.' + CLASS.shape + '-' + closest.index).each(function () {
                if (config.data_selection_grouped || $$.isWithinShape(this, closest)) {
                    $$.toggleShape(this, closest, closest.index);
                    $$.config.data_onclick.call($$.api, closest, this);
                }
            });
        }
    }).call(config.data_selection_draggable && $$.drag ? d3.behavior.drag().origin(Object).on('drag', function () {
        $$.drag(d3.mouse(this));
    }).on('dragstart', function () {
        $$.dragstart(d3.mouse(this));
    }).on('dragend', function () {
        $$.dragend();
    }) : function () {});
};
c3_chart_internal_fn.dispatchEvent = function (type, index, mouse) {
    var $$ = this,
        selector = '.' + CLASS.eventRect + (!$$.isMultipleX() ? '-' + index : ''),
        eventRect = $$.main.select(selector).node(),
        box = eventRect.getBoundingClientRect(),
        x = box.left + (mouse ? mouse[0] : 0),
        y = box.top + (mouse ? mouse[1] : 0),
        event = document.createEvent("MouseEvents");

    event.initMouseEvent(type, true, true, window, 0, x, y, x, y, false, false, false, false, 0, null);
    eventRect.dispatchEvent(event);
};

c3_chart_internal_fn.initLegend = function () {
    var $$ = this;
    $$.legendItemTextBox = {};
    $$.legendHasRendered = false;
    $$.legend = $$.svg.append("g").attr("transform", $$.getTranslate('legend'));
    if (!$$.config.legend_show) {
        $$.legend.style('visibility', 'hidden');
        $$.hiddenLegendIds = $$.mapToIds($$.data.targets);
        return;
    }
    // MEMO: call here to update legend box and tranlate for all
    // MEMO: translate will be upated by this, so transform not needed in updateLegend()
    $$.updateLegendWithDefaults();
};
c3_chart_internal_fn.updateLegendWithDefaults = function () {
    var $$ = this;
    $$.updateLegend($$.mapToIds($$.data.targets), { withTransform: false, withTransitionForTransform: false, withTransition: false });
};
c3_chart_internal_fn.updateSizeForLegend = function (legendHeight, legendWidth) {
    var $$ = this,
        config = $$.config,
        insetLegendPosition = {
        top: $$.isLegendTop ? $$.getCurrentPaddingTop() + config.legend_inset_y + 5.5 : $$.currentHeight - legendHeight - $$.getCurrentPaddingBottom() - config.legend_inset_y,
        left: $$.isLegendLeft ? $$.getCurrentPaddingLeft() + config.legend_inset_x + 0.5 : $$.currentWidth - legendWidth - $$.getCurrentPaddingRight() - config.legend_inset_x + 0.5
    };

    $$.margin3 = {
        top: $$.isLegendRight ? 0 : $$.isLegendInset ? insetLegendPosition.top : $$.currentHeight - legendHeight,
        right: NaN,
        bottom: 0,
        left: $$.isLegendRight ? $$.currentWidth - legendWidth : $$.isLegendInset ? insetLegendPosition.left : 0
    };
};
c3_chart_internal_fn.transformLegend = function (withTransition) {
    var $$ = this;
    (withTransition ? $$.legend.transition() : $$.legend).attr("transform", $$.getTranslate('legend'));
};
c3_chart_internal_fn.updateLegendStep = function (step) {
    this.legendStep = step;
};
c3_chart_internal_fn.updateLegendItemWidth = function (w) {
    this.legendItemWidth = w;
};
c3_chart_internal_fn.updateLegendItemHeight = function (h) {
    this.legendItemHeight = h;
};
c3_chart_internal_fn.getLegendWidth = function () {
    var $$ = this;
    return $$.config.legend_show ? $$.isLegendRight || $$.isLegendInset ? $$.legendItemWidth * ($$.legendStep + 1) : $$.currentWidth : 0;
};
c3_chart_internal_fn.getLegendHeight = function () {
    var $$ = this,
        h = 0;
    if ($$.config.legend_show) {
        if ($$.isLegendRight) {
            h = $$.currentHeight;
        } else {
            h = Math.max(20, $$.legendItemHeight) * ($$.legendStep + 1);
        }
    }
    return h;
};
c3_chart_internal_fn.opacityForLegend = function (legendItem) {
    return legendItem.classed(CLASS.legendItemHidden) ? null : 1;
};
c3_chart_internal_fn.opacityForUnfocusedLegend = function (legendItem) {
    return legendItem.classed(CLASS.legendItemHidden) ? null : 0.3;
};
c3_chart_internal_fn.toggleFocusLegend = function (targetIds, focus) {
    var $$ = this;
    targetIds = $$.mapToTargetIds(targetIds);
    $$.legend.selectAll('.' + CLASS.legendItem).filter(function (id) {
        return targetIds.indexOf(id) >= 0;
    }).classed(CLASS.legendItemFocused, focus).transition().duration(100).style('opacity', function () {
        var opacity = focus ? $$.opacityForLegend : $$.opacityForUnfocusedLegend;
        return opacity.call($$, $$.d3.select(this));
    });
};
c3_chart_internal_fn.revertLegend = function () {
    var $$ = this,
        d3 = $$.d3;
    $$.legend.selectAll('.' + CLASS.legendItem).classed(CLASS.legendItemFocused, false).transition().duration(100).style('opacity', function () {
        return $$.opacityForLegend(d3.select(this));
    });
};
c3_chart_internal_fn.showLegend = function (targetIds) {
    var $$ = this,
        config = $$.config;
    if (!config.legend_show) {
        config.legend_show = true;
        $$.legend.style('visibility', 'visible');
        if (!$$.legendHasRendered) {
            $$.updateLegendWithDefaults();
        }
    }
    $$.removeHiddenLegendIds(targetIds);
    $$.legend.selectAll($$.selectorLegends(targetIds)).style('visibility', 'visible').transition().style('opacity', function () {
        return $$.opacityForLegend($$.d3.select(this));
    });
};
c3_chart_internal_fn.hideLegend = function (targetIds) {
    var $$ = this,
        config = $$.config;
    if (config.legend_show && isEmpty(targetIds)) {
        config.legend_show = false;
        $$.legend.style('visibility', 'hidden');
    }
    $$.addHiddenLegendIds(targetIds);
    $$.legend.selectAll($$.selectorLegends(targetIds)).style('opacity', 0).style('visibility', 'hidden');
};
c3_chart_internal_fn.clearLegendItemTextBoxCache = function () {
    this.legendItemTextBox = {};
};
c3_chart_internal_fn.updateLegend = function (targetIds, options, transitions) {
    var $$ = this,
        config = $$.config;
    var xForLegend, xForLegendText, xForLegendRect, yForLegend, yForLegendText, yForLegendRect, x1ForLegendTile, x2ForLegendTile, yForLegendTile;
    var paddingTop = 4,
        paddingRight = 10,
        maxWidth = 0,
        maxHeight = 0,
        posMin = 10,
        tileWidth = config.legend_item_tile_width + 5;
    var l,
        totalLength = 0,
        offsets = {},
        widths = {},
        heights = {},
        margins = [0],
        steps = {},
        step = 0;
    var withTransition, withTransitionForTransform;
    var texts, rects, tiles, background;

    // Skip elements when their name is set to null
    targetIds = targetIds.filter(function (id) {
        return !isDefined(config.data_names[id]) || config.data_names[id] !== null;
    });

    options = options || {};
    withTransition = getOption(options, "withTransition", true);
    withTransitionForTransform = getOption(options, "withTransitionForTransform", true);

    function getTextBox(textElement, id) {
        if (!$$.legendItemTextBox[id]) {
            $$.legendItemTextBox[id] = $$.getTextRect(textElement.textContent, CLASS.legendItem, textElement);
        }
        return $$.legendItemTextBox[id];
    }

    function updatePositions(textElement, id, index) {
        var reset = index === 0,
            isLast = index === targetIds.length - 1,
            box = getTextBox(textElement, id),
            itemWidth = box.width + tileWidth + (isLast && !($$.isLegendRight || $$.isLegendInset) ? 0 : paddingRight) + config.legend_padding,
            itemHeight = box.height + paddingTop,
            itemLength = $$.isLegendRight || $$.isLegendInset ? itemHeight : itemWidth,
            areaLength = $$.isLegendRight || $$.isLegendInset ? $$.getLegendHeight() : $$.getLegendWidth(),
            margin,
            maxLength;

        // MEMO: care about condifion of step, totalLength
        function updateValues(id, withoutStep) {
            if (!withoutStep) {
                margin = (areaLength - totalLength - itemLength) / 2;
                if (margin < posMin) {
                    margin = (areaLength - itemLength) / 2;
                    totalLength = 0;
                    step++;
                }
            }
            steps[id] = step;
            margins[step] = $$.isLegendInset ? 10 : margin;
            offsets[id] = totalLength;
            totalLength += itemLength;
        }

        if (reset) {
            totalLength = 0;
            step = 0;
            maxWidth = 0;
            maxHeight = 0;
        }

        if (config.legend_show && !$$.isLegendToShow(id)) {
            widths[id] = heights[id] = steps[id] = offsets[id] = 0;
            return;
        }

        widths[id] = itemWidth;
        heights[id] = itemHeight;

        if (!maxWidth || itemWidth >= maxWidth) {
            maxWidth = itemWidth;
        }
        if (!maxHeight || itemHeight >= maxHeight) {
            maxHeight = itemHeight;
        }
        maxLength = $$.isLegendRight || $$.isLegendInset ? maxHeight : maxWidth;

        if (config.legend_equally) {
            Object.keys(widths).forEach(function (id) {
                widths[id] = maxWidth;
            });
            Object.keys(heights).forEach(function (id) {
                heights[id] = maxHeight;
            });
            margin = (areaLength - maxLength * targetIds.length) / 2;
            if (margin < posMin) {
                totalLength = 0;
                step = 0;
                targetIds.forEach(function (id) {
                    updateValues(id);
                });
            } else {
                updateValues(id, true);
            }
        } else {
            updateValues(id);
        }
    }

    if ($$.isLegendInset) {
        step = config.legend_inset_step ? config.legend_inset_step : targetIds.length;
        $$.updateLegendStep(step);
    }

    if ($$.isLegendRight) {
        xForLegend = function xForLegend(id) {
            return maxWidth * steps[id];
        };
        yForLegend = function yForLegend(id) {
            return margins[steps[id]] + offsets[id];
        };
    } else if ($$.isLegendInset) {
        xForLegend = function xForLegend(id) {
            return maxWidth * steps[id] + 10;
        };
        yForLegend = function yForLegend(id) {
            return margins[steps[id]] + offsets[id];
        };
    } else {
        xForLegend = function xForLegend(id) {
            return margins[steps[id]] + offsets[id];
        };
        yForLegend = function yForLegend(id) {
            return maxHeight * steps[id];
        };
    }
    xForLegendText = function xForLegendText(id, i) {
        return xForLegend(id, i) + 4 + config.legend_item_tile_width;
    };
    yForLegendText = function yForLegendText(id, i) {
        return yForLegend(id, i) + 9;
    };
    xForLegendRect = function xForLegendRect(id, i) {
        return xForLegend(id, i);
    };
    yForLegendRect = function yForLegendRect(id, i) {
        return yForLegend(id, i) - 5;
    };
    x1ForLegendTile = function x1ForLegendTile(id, i) {
        return xForLegend(id, i) - 2;
    };
    x2ForLegendTile = function x2ForLegendTile(id, i) {
        return xForLegend(id, i) - 2 + config.legend_item_tile_width;
    };
    yForLegendTile = function yForLegendTile(id, i) {
        return yForLegend(id, i) + 4;
    };

    // Define g for legend area
    l = $$.legend.selectAll('.' + CLASS.legendItem).data(targetIds).enter().append('g').attr('class', function (id) {
        return $$.generateClass(CLASS.legendItem, id);
    }).style('visibility', function (id) {
        return $$.isLegendToShow(id) ? 'visible' : 'hidden';
    }).style('cursor', 'pointer').on('click', function (id) {
        if (config.legend_item_onclick) {
            config.legend_item_onclick.call($$, id);
        } else {
            if ($$.d3.event.altKey) {
                $$.api.hide();
                $$.api.show(id);
            } else {
                $$.api.toggle(id);
                $$.isTargetToShow(id) ? $$.api.focus(id) : $$.api.revert();
            }
        }
    }).on('mouseover', function (id) {
        if (config.legend_item_onmouseover) {
            config.legend_item_onmouseover.call($$, id);
        } else {
            $$.d3.select(this).classed(CLASS.legendItemFocused, true);
            if (!$$.transiting && $$.isTargetToShow(id)) {
                $$.api.focus(id);
            }
        }
    }).on('mouseout', function (id) {
        if (config.legend_item_onmouseout) {
            config.legend_item_onmouseout.call($$, id);
        } else {
            $$.d3.select(this).classed(CLASS.legendItemFocused, false);
            $$.api.revert();
        }
    });
    l.append('text').text(function (id) {
        return isDefined(config.data_names[id]) ? config.data_names[id] : id;
    }).each(function (id, i) {
        updatePositions(this, id, i);
    }).style("pointer-events", "none").attr('x', $$.isLegendRight || $$.isLegendInset ? xForLegendText : -200).attr('y', $$.isLegendRight || $$.isLegendInset ? -200 : yForLegendText);
    l.append('rect').attr("class", CLASS.legendItemEvent).style('fill-opacity', 0).attr('x', $$.isLegendRight || $$.isLegendInset ? xForLegendRect : -200).attr('y', $$.isLegendRight || $$.isLegendInset ? -200 : yForLegendRect);
    l.append('line').attr('class', CLASS.legendItemTile).style('stroke', $$.color).style("pointer-events", "none").attr('x1', $$.isLegendRight || $$.isLegendInset ? x1ForLegendTile : -200).attr('y1', $$.isLegendRight || $$.isLegendInset ? -200 : yForLegendTile).attr('x2', $$.isLegendRight || $$.isLegendInset ? x2ForLegendTile : -200).attr('y2', $$.isLegendRight || $$.isLegendInset ? -200 : yForLegendTile).attr('stroke-width', config.legend_item_tile_height);

    // Set background for inset legend
    background = $$.legend.select('.' + CLASS.legendBackground + ' rect');
    if ($$.isLegendInset && maxWidth > 0 && background.size() === 0) {
        background = $$.legend.insert('g', '.' + CLASS.legendItem).attr("class", CLASS.legendBackground).append('rect');
    }

    texts = $$.legend.selectAll('text').data(targetIds).text(function (id) {
        return isDefined(config.data_names[id]) ? config.data_names[id] : id;
    } // MEMO: needed for update
    ).each(function (id, i) {
        updatePositions(this, id, i);
    });
    (withTransition ? texts.transition() : texts).attr('x', xForLegendText).attr('y', yForLegendText);

    rects = $$.legend.selectAll('rect.' + CLASS.legendItemEvent).data(targetIds);
    (withTransition ? rects.transition() : rects).attr('width', function (id) {
        return widths[id];
    }).attr('height', function (id) {
        return heights[id];
    }).attr('x', xForLegendRect).attr('y', yForLegendRect);

    tiles = $$.legend.selectAll('line.' + CLASS.legendItemTile).data(targetIds);
    (withTransition ? tiles.transition() : tiles).style('stroke', $$.color).attr('x1', x1ForLegendTile).attr('y1', yForLegendTile).attr('x2', x2ForLegendTile).attr('y2', yForLegendTile);

    if (background) {
        (withTransition ? background.transition() : background).attr('height', $$.getLegendHeight() - 12).attr('width', maxWidth * (step + 1) + 10);
    }

    // toggle legend state
    $$.legend.selectAll('.' + CLASS.legendItem).classed(CLASS.legendItemHidden, function (id) {
        return !$$.isTargetToShow(id);
    });

    // Update all to reflect change of legend
    $$.updateLegendItemWidth(maxWidth);
    $$.updateLegendItemHeight(maxHeight);
    $$.updateLegendStep(step);
    // Update size and scale
    $$.updateSizes();
    $$.updateScales();
    $$.updateSvgSize();
    // Update g positions
    $$.transformAll(withTransitionForTransform, transitions);
    $$.legendHasRendered = true;
};

c3_chart_internal_fn.initRegion = function () {
    var $$ = this;
    $$.region = $$.main.append('g').attr("clip-path", $$.clipPath).attr("class", CLASS.regions);
};
c3_chart_internal_fn.updateRegion = function (duration) {
    var $$ = this,
        config = $$.config;

    // hide if arc type
    $$.region.style('visibility', $$.hasArcType() ? 'hidden' : 'visible');

    $$.mainRegion = $$.main.select('.' + CLASS.regions).selectAll('.' + CLASS.region).data(config.regions);
    $$.mainRegion.enter().append('g').append('rect').style("fill-opacity", 0);
    $$.mainRegion.attr('class', $$.classRegion.bind($$));
    $$.mainRegion.exit().transition().duration(duration).style("opacity", 0).remove();
};
c3_chart_internal_fn.redrawRegion = function (withTransition) {
    var $$ = this,
        regions = $$.mainRegion.selectAll('rect').each(function () {
        // data is binded to g and it's not transferred to rect (child node) automatically,
        // then data of each rect has to be updated manually.
        // TODO: there should be more efficient way to solve this?
        var parentData = $$.d3.select(this.parentNode).datum();
        $$.d3.select(this).datum(parentData);
    }),
        x = $$.regionX.bind($$),
        y = $$.regionY.bind($$),
        w = $$.regionWidth.bind($$),
        h = $$.regionHeight.bind($$);
    return [(withTransition ? regions.transition() : regions).attr("x", x).attr("y", y).attr("width", w).attr("height", h).style("fill-opacity", function (d) {
        return isValue(d.opacity) ? d.opacity : 0.1;
    })];
};
c3_chart_internal_fn.regionX = function (d) {
    var $$ = this,
        config = $$.config,
        xPos,
        yScale = d.axis === 'y' ? $$.y : $$.y2;
    if (d.axis === 'y' || d.axis === 'y2') {
        xPos = config.axis_rotated ? 'start' in d ? yScale(d.start) : 0 : 0;
    } else {
        xPos = config.axis_rotated ? 0 : 'start' in d ? $$.x($$.isTimeSeries() ? $$.parseDate(d.start) : d.start) : 0;
    }
    return xPos;
};
c3_chart_internal_fn.regionY = function (d) {
    var $$ = this,
        config = $$.config,
        yPos,
        yScale = d.axis === 'y' ? $$.y : $$.y2;
    if (d.axis === 'y' || d.axis === 'y2') {
        yPos = config.axis_rotated ? 0 : 'end' in d ? yScale(d.end) : 0;
    } else {
        yPos = config.axis_rotated ? 'start' in d ? $$.x($$.isTimeSeries() ? $$.parseDate(d.start) : d.start) : 0 : 0;
    }
    return yPos;
};
c3_chart_internal_fn.regionWidth = function (d) {
    var $$ = this,
        config = $$.config,
        start = $$.regionX(d),
        end,
        yScale = d.axis === 'y' ? $$.y : $$.y2;
    if (d.axis === 'y' || d.axis === 'y2') {
        end = config.axis_rotated ? 'end' in d ? yScale(d.end) : $$.width : $$.width;
    } else {
        end = config.axis_rotated ? $$.width : 'end' in d ? $$.x($$.isTimeSeries() ? $$.parseDate(d.end) : d.end) : $$.width;
    }
    return end < start ? 0 : end - start;
};
c3_chart_internal_fn.regionHeight = function (d) {
    var $$ = this,
        config = $$.config,
        start = this.regionY(d),
        end,
        yScale = d.axis === 'y' ? $$.y : $$.y2;
    if (d.axis === 'y' || d.axis === 'y2') {
        end = config.axis_rotated ? $$.height : 'start' in d ? yScale(d.start) : $$.height;
    } else {
        end = config.axis_rotated ? 'end' in d ? $$.x($$.isTimeSeries() ? $$.parseDate(d.end) : d.end) : $$.height : $$.height;
    }
    return end < start ? 0 : end - start;
};
c3_chart_internal_fn.isRegionOnX = function (d) {
    return !d.axis || d.axis === 'x';
};

c3_chart_internal_fn.getScale = function (min, max, forTimeseries) {
    return (forTimeseries ? this.d3.time.scale() : this.d3.scale.linear()).range([min, max]);
};
c3_chart_internal_fn.getX = function (min, max, domain, offset) {
    var $$ = this,
        scale = $$.getScale(min, max, $$.isTimeSeries()),
        _scale = domain ? scale.domain(domain) : scale,
        key;
    // Define customized scale if categorized axis
    if ($$.isCategorized()) {
        offset = offset || function () {
            return 0;
        };
        scale = function scale(d, raw) {
            var v = _scale(d) + offset(d);
            return raw ? v : Math.ceil(v);
        };
    } else {
        scale = function scale(d, raw) {
            var v = _scale(d);
            return raw ? v : Math.ceil(v);
        };
    }
    // define functions
    for (key in _scale) {
        scale[key] = _scale[key];
    }
    scale.orgDomain = function () {
        return _scale.domain();
    };
    // define custom domain() for categorized axis
    if ($$.isCategorized()) {
        scale.domain = function (domain) {
            if (!arguments.length) {
                domain = this.orgDomain();
                return [domain[0], domain[1] + 1];
            }
            _scale.domain(domain);
            return scale;
        };
    }
    return scale;
};
c3_chart_internal_fn.getY = function (min, max, domain) {
    var scale = this.getScale(min, max, this.isTimeSeriesY());
    if (domain) {
        scale.domain(domain);
    }
    return scale;
};
c3_chart_internal_fn.getYScale = function (id) {
    return this.axis.getId(id) === 'y2' ? this.y2 : this.y;
};
c3_chart_internal_fn.getSubYScale = function (id) {
    return this.axis.getId(id) === 'y2' ? this.subY2 : this.subY;
};
c3_chart_internal_fn.updateScales = function () {
    var $$ = this,
        config = $$.config,
        forInit = !$$.x;
    // update edges
    $$.xMin = config.axis_rotated ? 1 : 0;
    $$.xMax = config.axis_rotated ? $$.height : $$.width;
    $$.yMin = config.axis_rotated ? 0 : $$.height;
    $$.yMax = config.axis_rotated ? $$.width : 1;
    $$.subXMin = $$.xMin;
    $$.subXMax = $$.xMax;
    $$.subYMin = config.axis_rotated ? 0 : $$.height2;
    $$.subYMax = config.axis_rotated ? $$.width2 : 1;
    // update scales
    $$.x = $$.getX($$.xMin, $$.xMax, forInit ? undefined : $$.x.orgDomain(), function () {
        return $$.xAxis.tickOffset();
    });
    $$.y = $$.getY($$.yMin, $$.yMax, forInit ? config.axis_y_default : $$.y.domain());
    $$.y2 = $$.getY($$.yMin, $$.yMax, forInit ? config.axis_y2_default : $$.y2.domain());
    $$.subX = $$.getX($$.xMin, $$.xMax, $$.orgXDomain, function (d) {
        return d % 1 ? 0 : $$.subXAxis.tickOffset();
    });
    $$.subY = $$.getY($$.subYMin, $$.subYMax, forInit ? config.axis_y_default : $$.subY.domain());
    $$.subY2 = $$.getY($$.subYMin, $$.subYMax, forInit ? config.axis_y2_default : $$.subY2.domain());
    // update axes
    $$.xAxisTickFormat = $$.axis.getXAxisTickFormat();
    $$.xAxisTickValues = $$.axis.getXAxisTickValues();
    $$.yAxisTickValues = $$.axis.getYAxisTickValues();
    $$.y2AxisTickValues = $$.axis.getY2AxisTickValues();

    $$.xAxis = $$.axis.getXAxis($$.x, $$.xOrient, $$.xAxisTickFormat, $$.xAxisTickValues, config.axis_x_tick_outer);
    $$.subXAxis = $$.axis.getXAxis($$.subX, $$.subXOrient, $$.xAxisTickFormat, $$.xAxisTickValues, config.axis_x_tick_outer);
    $$.yAxis = $$.axis.getYAxis($$.y, $$.yOrient, config.axis_y_tick_format, $$.yAxisTickValues, config.axis_y_tick_outer);
    $$.y2Axis = $$.axis.getYAxis($$.y2, $$.y2Orient, config.axis_y2_tick_format, $$.y2AxisTickValues, config.axis_y2_tick_outer);

    // Set initialized scales to brush and zoom
    if (!forInit) {
        if ($$.brush) {
            $$.brush.scale($$.subX);
        }
        if (config.zoom_enabled) {
            $$.zoom.scale($$.x);
        }
    }
    // update for arc
    if ($$.updateArc) {
        $$.updateArc();
    }
};

c3_chart_internal_fn.selectPoint = function (target, d, i) {
    var $$ = this,
        config = $$.config,
        cx = (config.axis_rotated ? $$.circleY : $$.circleX).bind($$),
        cy = (config.axis_rotated ? $$.circleX : $$.circleY).bind($$),
        r = $$.pointSelectR.bind($$);
    config.data_onselected.call($$.api, d, target.node());
    // add selected-circle on low layer g
    $$.main.select('.' + CLASS.selectedCircles + $$.getTargetSelectorSuffix(d.id)).selectAll('.' + CLASS.selectedCircle + '-' + i).data([d]).enter().append('circle').attr("class", function () {
        return $$.generateClass(CLASS.selectedCircle, i);
    }).attr("cx", cx).attr("cy", cy).attr("stroke", function () {
        return $$.color(d);
    }).attr("r", function (d) {
        return $$.pointSelectR(d) * 1.4;
    }).transition().duration(100).attr("r", r);
};
c3_chart_internal_fn.unselectPoint = function (target, d, i) {
    var $$ = this;
    $$.config.data_onunselected.call($$.api, d, target.node());
    // remove selected-circle from low layer g
    $$.main.select('.' + CLASS.selectedCircles + $$.getTargetSelectorSuffix(d.id)).selectAll('.' + CLASS.selectedCircle + '-' + i).transition().duration(100).attr('r', 0).remove();
};
c3_chart_internal_fn.togglePoint = function (selected, target, d, i) {
    selected ? this.selectPoint(target, d, i) : this.unselectPoint(target, d, i);
};
c3_chart_internal_fn.selectPath = function (target, d) {
    var $$ = this;
    $$.config.data_onselected.call($$, d, target.node());
    if ($$.config.interaction_brighten) {
        target.transition().duration(100).style("fill", function () {
            return $$.d3.rgb($$.color(d)).brighter(0.75);
        });
    }
};
c3_chart_internal_fn.unselectPath = function (target, d) {
    var $$ = this;
    $$.config.data_onunselected.call($$, d, target.node());
    if ($$.config.interaction_brighten) {
        target.transition().duration(100).style("fill", function () {
            return $$.color(d);
        });
    }
};
c3_chart_internal_fn.togglePath = function (selected, target, d, i) {
    selected ? this.selectPath(target, d, i) : this.unselectPath(target, d, i);
};
c3_chart_internal_fn.getToggle = function (that, d) {
    var $$ = this,
        toggle;
    if (that.nodeName === 'circle') {
        if ($$.isStepType(d)) {
            // circle is hidden in step chart, so treat as within the click area
            toggle = function toggle() {}; // TODO: how to select step chart?
        } else {
            toggle = $$.togglePoint;
        }
    } else if (that.nodeName === 'path') {
        toggle = $$.togglePath;
    }
    return toggle;
};
c3_chart_internal_fn.toggleShape = function (that, d, i) {
    var $$ = this,
        d3 = $$.d3,
        config = $$.config,
        shape = d3.select(that),
        isSelected = shape.classed(CLASS.SELECTED),
        toggle = $$.getToggle(that, d).bind($$);

    if (config.data_selection_enabled && config.data_selection_isselectable(d)) {
        if (!config.data_selection_multiple) {
            $$.main.selectAll('.' + CLASS.shapes + (config.data_selection_grouped ? $$.getTargetSelectorSuffix(d.id) : "")).selectAll('.' + CLASS.shape).each(function (d, i) {
                var shape = d3.select(this);
                if (shape.classed(CLASS.SELECTED)) {
                    toggle(false, shape.classed(CLASS.SELECTED, false), d, i);
                }
            });
        }
        shape.classed(CLASS.SELECTED, !isSelected);
        toggle(!isSelected, shape, d, i);
    }
};

c3_chart_internal_fn.initBar = function () {
    var $$ = this;
    $$.main.select('.' + CLASS.chart).append("g").attr("class", CLASS.chartBars);
};
c3_chart_internal_fn.updateTargetsForBar = function (targets) {
    var $$ = this,
        config = $$.config,
        mainBarUpdate,
        mainBarEnter,
        classChartBar = $$.classChartBar.bind($$),
        classBars = $$.classBars.bind($$),
        classFocus = $$.classFocus.bind($$);
    mainBarUpdate = $$.main.select('.' + CLASS.chartBars).selectAll('.' + CLASS.chartBar).data(targets).attr('class', function (d) {
        return classChartBar(d) + classFocus(d);
    });
    mainBarEnter = mainBarUpdate.enter().append('g').attr('class', classChartBar).style("pointer-events", "none");
    // Bars for each data
    mainBarEnter.append('g').attr("class", classBars).style("cursor", function (d) {
        return config.data_selection_isselectable(d) ? "pointer" : null;
    });
};
c3_chart_internal_fn.updateBar = function (durationForExit) {
    var $$ = this,
        barData = $$.barData.bind($$),
        classBar = $$.classBar.bind($$),
        initialOpacity = $$.initialOpacity.bind($$),
        color = function color(d) {
        return $$.color(d.id);
    };
    $$.mainBar = $$.main.selectAll('.' + CLASS.bars).selectAll('.' + CLASS.bar).data(barData);
    $$.mainBar.enter().append('path').attr("class", classBar).style("stroke", color).style("fill", color);
    $$.mainBar.style("opacity", initialOpacity);
    $$.mainBar.exit().transition().duration(durationForExit).remove();
};
c3_chart_internal_fn.redrawBar = function (drawBar, withTransition) {
    return [(withTransition ? this.mainBar.transition(Math.random().toString()) : this.mainBar).attr('d', drawBar).style("stroke", this.color).style("fill", this.color).style("opacity", 1)];
};
c3_chart_internal_fn.getBarW = function (axis, barTargetsNum) {
    var $$ = this,
        config = $$.config,
        w = typeof config.bar_width === 'number' ? config.bar_width : barTargetsNum ? axis.tickInterval() * config.bar_width_ratio / barTargetsNum : 0;
    return config.bar_width_max && w > config.bar_width_max ? config.bar_width_max : w;
};
c3_chart_internal_fn.getBars = function (i, id) {
    var $$ = this;
    return (id ? $$.main.selectAll('.' + CLASS.bars + $$.getTargetSelectorSuffix(id)) : $$.main).selectAll('.' + CLASS.bar + (isValue(i) ? '-' + i : ''));
};
c3_chart_internal_fn.expandBars = function (i, id, reset) {
    var $$ = this;
    if (reset) {
        $$.unexpandBars();
    }
    $$.getBars(i, id).classed(CLASS.EXPANDED, true);
};
c3_chart_internal_fn.unexpandBars = function (i) {
    var $$ = this;
    $$.getBars(i).classed(CLASS.EXPANDED, false);
};
c3_chart_internal_fn.generateDrawBar = function (barIndices, isSub) {
    var $$ = this,
        config = $$.config,
        getPoints = $$.generateGetBarPoints(barIndices, isSub);
    return function (d, i) {
        // 4 points that make a bar
        var points = getPoints(d, i);

        // switch points if axis is rotated, not applicable for sub chart
        var indexX = config.axis_rotated ? 1 : 0;
        var indexY = config.axis_rotated ? 0 : 1;

        var path = 'M ' + points[0][indexX] + ',' + points[0][indexY] + ' ' + 'L' + points[1][indexX] + ',' + points[1][indexY] + ' ' + 'L' + points[2][indexX] + ',' + points[2][indexY] + ' ' + 'L' + points[3][indexX] + ',' + points[3][indexY] + ' ' + 'z';

        return path;
    };
};
c3_chart_internal_fn.generateGetBarPoints = function (barIndices, isSub) {
    var $$ = this,
        axis = isSub ? $$.subXAxis : $$.xAxis,
        barTargetsNum = barIndices.__max__ + 1,
        barW = $$.getBarW(axis, barTargetsNum),
        barX = $$.getShapeX(barW, barTargetsNum, barIndices, !!isSub),
        barY = $$.getShapeY(!!isSub),
        barOffset = $$.getShapeOffset($$.isBarType, barIndices, !!isSub),
        barSpaceOffset = barW * ($$.config.bar_space / 2),
        yScale = isSub ? $$.getSubYScale : $$.getYScale;
    return function (d, i) {
        var y0 = yScale.call($$, d.id)(0),
            offset = barOffset(d, i) || y0,
            // offset is for stacked bar chart
        posX = barX(d),
            posY = barY(d);
        // fix posY not to overflow opposite quadrant
        if ($$.config.axis_rotated) {
            if (0 < d.value && posY < y0 || d.value < 0 && y0 < posY) {
                posY = y0;
            }
        }
        // 4 points that make a bar
        return [[posX + barSpaceOffset, offset], [posX + barSpaceOffset, posY - (y0 - offset)], [posX + barW - barSpaceOffset, posY - (y0 - offset)], [posX + barW - barSpaceOffset, offset]];
    };
};
c3_chart_internal_fn.isWithinBar = function (that) {
    var mouse = this.d3.mouse(that),
        box = that.getBoundingClientRect(),
        seg0 = that.pathSegList.getItem(0),
        seg1 = that.pathSegList.getItem(1),
        x = Math.min(seg0.x, seg1.x),
        y = Math.min(seg0.y, seg1.y),
        w = box.width,
        h = box.height,
        offset = 2,
        sx = x - offset,
        ex = x + w + offset,
        sy = y + h + offset,
        ey = y - offset;
    return sx < mouse[0] && mouse[0] < ex && ey < mouse[1] && mouse[1] < sy;
};

c3_chart_internal_fn.getShapeIndices = function (typeFilter) {
    var $$ = this,
        config = $$.config,
        indices = {},
        i = 0,
        j,
        k;
    $$.filterTargetsToShow($$.data.targets.filter(typeFilter, $$)).forEach(function (d) {
        for (j = 0; j < config.data_groups.length; j++) {
            if (config.data_groups[j].indexOf(d.id) < 0) {
                continue;
            }
            for (k = 0; k < config.data_groups[j].length; k++) {
                if (config.data_groups[j][k] in indices) {
                    indices[d.id] = indices[config.data_groups[j][k]];
                    break;
                }
            }
        }
        if (isUndefined(indices[d.id])) {
            indices[d.id] = i++;
        }
    });
    indices.__max__ = i - 1;
    return indices;
};
c3_chart_internal_fn.getShapeX = function (offset, targetsNum, indices, isSub) {
    var $$ = this,
        scale = isSub ? $$.subX : $$.x;
    return function (d) {
        var index = d.id in indices ? indices[d.id] : 0;
        return d.x || d.x === 0 ? scale(d.x) - offset * (targetsNum / 2 - index) : 0;
    };
};
c3_chart_internal_fn.getShapeY = function (isSub) {
    var $$ = this;
    return function (d) {
        var scale = isSub ? $$.getSubYScale(d.id) : $$.getYScale(d.id);
        return scale(d.value);
    };
};
c3_chart_internal_fn.getShapeOffset = function (typeFilter, indices, isSub) {
    var $$ = this,
        targets = $$.orderTargets($$.filterTargetsToShow($$.data.targets.filter(typeFilter, $$))),
        targetIds = targets.map(function (t) {
        return t.id;
    });
    return function (d, i) {
        var scale = isSub ? $$.getSubYScale(d.id) : $$.getYScale(d.id),
            y0 = scale(0),
            offset = y0;
        targets.forEach(function (t) {
            var values = $$.isStepType(d) ? $$.convertValuesToStep(t.values) : t.values;
            if (t.id === d.id || indices[t.id] !== indices[d.id]) {
                return;
            }
            if (targetIds.indexOf(t.id) < targetIds.indexOf(d.id)) {
                // check if the x values line up
                if (typeof values[i] === 'undefined' || +values[i].x !== +d.x) {
                    // "+" for timeseries
                    // if not, try to find the value that does line up
                    i = -1;
                    values.forEach(function (v, j) {
                        if (v.x === d.x) {
                            i = j;
                        }
                    });
                }
                if (i in values && values[i].value * d.value >= 0) {
                    offset += scale(values[i].value) - y0;
                }
            }
        });
        return offset;
    };
};
c3_chart_internal_fn.isWithinShape = function (that, d) {
    var $$ = this,
        shape = $$.d3.select(that),
        isWithin;
    if (!$$.isTargetToShow(d.id)) {
        isWithin = false;
    } else if (that.nodeName === 'circle') {
        isWithin = $$.isStepType(d) ? $$.isWithinStep(that, $$.getYScale(d.id)(d.value)) : $$.isWithinCircle(that, $$.pointSelectR(d) * 1.5);
    } else if (that.nodeName === 'path') {
        isWithin = shape.classed(CLASS.bar) ? $$.isWithinBar(that) : true;
    }
    return isWithin;
};

c3_chart_internal_fn.getInterpolate = function (d) {
    var $$ = this,
        interpolation = $$.isInterpolationType($$.config.spline_interpolation_type) ? $$.config.spline_interpolation_type : 'cardinal';
    return $$.isSplineType(d) ? interpolation : $$.isStepType(d) ? $$.config.line_step_type : "linear";
};

c3_chart_internal_fn.initLine = function () {
    var $$ = this;
    $$.main.select('.' + CLASS.chart).append("g").attr("class", CLASS.chartLines);
};
c3_chart_internal_fn.updateTargetsForLine = function (targets) {
    var $$ = this,
        config = $$.config,
        mainLineUpdate,
        mainLineEnter,
        classChartLine = $$.classChartLine.bind($$),
        classLines = $$.classLines.bind($$),
        classAreas = $$.classAreas.bind($$),
        classCircles = $$.classCircles.bind($$),
        classFocus = $$.classFocus.bind($$);
    mainLineUpdate = $$.main.select('.' + CLASS.chartLines).selectAll('.' + CLASS.chartLine).data(targets).attr('class', function (d) {
        return classChartLine(d) + classFocus(d);
    });
    mainLineEnter = mainLineUpdate.enter().append('g').attr('class', classChartLine).style('opacity', 0).style("pointer-events", "none");
    // Lines for each data
    mainLineEnter.append('g').attr("class", classLines);
    // Areas
    mainLineEnter.append('g').attr('class', classAreas);
    // Circles for each data point on lines
    mainLineEnter.append('g').attr("class", function (d) {
        return $$.generateClass(CLASS.selectedCircles, d.id);
    });
    mainLineEnter.append('g').attr("class", classCircles).style("cursor", function (d) {
        return config.data_selection_isselectable(d) ? "pointer" : null;
    });
    // Update date for selected circles
    targets.forEach(function (t) {
        $$.main.selectAll('.' + CLASS.selectedCircles + $$.getTargetSelectorSuffix(t.id)).selectAll('.' + CLASS.selectedCircle).each(function (d) {
            d.value = t.values[d.index].value;
        });
    });
    // MEMO: can not keep same color...
    //mainLineUpdate.exit().remove();
};
c3_chart_internal_fn.updateLine = function (durationForExit) {
    var $$ = this;
    $$.mainLine = $$.main.selectAll('.' + CLASS.lines).selectAll('.' + CLASS.line).data($$.lineData.bind($$));
    $$.mainLine.enter().append('path').attr('class', $$.classLine.bind($$)).style("stroke", $$.color);
    $$.mainLine.style("opacity", $$.initialOpacity.bind($$)).style('shape-rendering', function (d) {
        return $$.isStepType(d) ? 'crispEdges' : '';
    }).attr('transform', null);
    $$.mainLine.exit().transition().duration(durationForExit).style('opacity', 0).remove();
};
c3_chart_internal_fn.redrawLine = function (drawLine, withTransition) {
    return [(withTransition ? this.mainLine.transition(Math.random().toString()) : this.mainLine).attr("d", drawLine).style("stroke", this.color).style("opacity", 1)];
};
c3_chart_internal_fn.generateDrawLine = function (lineIndices, isSub) {
    var $$ = this,
        config = $$.config,
        line = $$.d3.svg.line(),
        getPoints = $$.generateGetLinePoints(lineIndices, isSub),
        yScaleGetter = isSub ? $$.getSubYScale : $$.getYScale,
        xValue = function xValue(d) {
        return (isSub ? $$.subxx : $$.xx).call($$, d);
    },
        yValue = function yValue(d, i) {
        return config.data_groups.length > 0 ? getPoints(d, i)[0][1] : yScaleGetter.call($$, d.id)(d.value);
    };

    line = config.axis_rotated ? line.x(yValue).y(xValue) : line.x(xValue).y(yValue);
    if (!config.line_connectNull) {
        line = line.defined(function (d) {
            return d.value != null;
        });
    }
    return function (d) {
        var values = config.line_connectNull ? $$.filterRemoveNull(d.values) : d.values,
            x = isSub ? $$.x : $$.subX,
            y = yScaleGetter.call($$, d.id),
            x0 = 0,
            y0 = 0,
            path;
        if ($$.isLineType(d)) {
            if (config.data_regions[d.id]) {
                path = $$.lineWithRegions(values, x, y, config.data_regions[d.id]);
            } else {
                if ($$.isStepType(d)) {
                    values = $$.convertValuesToStep(values);
                }
                path = line.interpolate($$.getInterpolate(d))(values);
            }
        } else {
            if (values[0]) {
                x0 = x(values[0].x);
                y0 = y(values[0].value);
            }
            path = config.axis_rotated ? "M " + y0 + " " + x0 : "M " + x0 + " " + y0;
        }
        return path ? path : "M 0 0";
    };
};
c3_chart_internal_fn.generateGetLinePoints = function (lineIndices, isSub) {
    // partial duplication of generateGetBarPoints
    var $$ = this,
        config = $$.config,
        lineTargetsNum = lineIndices.__max__ + 1,
        x = $$.getShapeX(0, lineTargetsNum, lineIndices, !!isSub),
        y = $$.getShapeY(!!isSub),
        lineOffset = $$.getShapeOffset($$.isLineType, lineIndices, !!isSub),
        yScale = isSub ? $$.getSubYScale : $$.getYScale;
    return function (d, i) {
        var y0 = yScale.call($$, d.id)(0),
            offset = lineOffset(d, i) || y0,
            // offset is for stacked area chart
        posX = x(d),
            posY = y(d);
        // fix posY not to overflow opposite quadrant
        if (config.axis_rotated) {
            if (0 < d.value && posY < y0 || d.value < 0 && y0 < posY) {
                posY = y0;
            }
        }
        // 1 point that marks the line position
        return [[posX, posY - (y0 - offset)], [posX, posY - (y0 - offset)], // needed for compatibility
        [posX, posY - (y0 - offset)], // needed for compatibility
        [posX, posY - (y0 - offset)] // needed for compatibility
        ];
    };
};

c3_chart_internal_fn.lineWithRegions = function (d, x, y, _regions) {
    var $$ = this,
        config = $$.config,
        prev = -1,
        i,
        j,
        s = "M",
        sWithRegion,
        xp,
        yp,
        dx,
        dy,
        dd,
        diff,
        diffx2,
        xOffset = $$.isCategorized() ? 0.5 : 0,
        xValue,
        yValue,
        regions = [];

    function isWithinRegions(x, regions) {
        var i;
        for (i = 0; i < regions.length; i++) {
            if (regions[i].start < x && x <= regions[i].end) {
                return true;
            }
        }
        return false;
    }

    // Check start/end of regions
    if (isDefined(_regions)) {
        for (i = 0; i < _regions.length; i++) {
            regions[i] = {};
            if (isUndefined(_regions[i].start)) {
                regions[i].start = d[0].x;
            } else {
                regions[i].start = $$.isTimeSeries() ? $$.parseDate(_regions[i].start) : _regions[i].start;
            }
            if (isUndefined(_regions[i].end)) {
                regions[i].end = d[d.length - 1].x;
            } else {
                regions[i].end = $$.isTimeSeries() ? $$.parseDate(_regions[i].end) : _regions[i].end;
            }
        }
    }

    // Set scales
    xValue = config.axis_rotated ? function (d) {
        return y(d.value);
    } : function (d) {
        return x(d.x);
    };
    yValue = config.axis_rotated ? function (d) {
        return x(d.x);
    } : function (d) {
        return y(d.value);
    };

    // Define svg generator function for region
    function generateM(points) {
        return 'M' + points[0][0] + ' ' + points[0][1] + ' ' + points[1][0] + ' ' + points[1][1];
    }
    if ($$.isTimeSeries()) {
        sWithRegion = function sWithRegion(d0, d1, j, diff) {
            var x0 = d0.x.getTime(),
                x_diff = d1.x - d0.x,
                xv0 = new Date(x0 + x_diff * j),
                xv1 = new Date(x0 + x_diff * (j + diff)),
                points;
            if (config.axis_rotated) {
                points = [[y(yp(j)), x(xv0)], [y(yp(j + diff)), x(xv1)]];
            } else {
                points = [[x(xv0), y(yp(j))], [x(xv1), y(yp(j + diff))]];
            }
            return generateM(points);
        };
    } else {
        sWithRegion = function sWithRegion(d0, d1, j, diff) {
            var points;
            if (config.axis_rotated) {
                points = [[y(yp(j), true), x(xp(j))], [y(yp(j + diff), true), x(xp(j + diff))]];
            } else {
                points = [[x(xp(j), true), y(yp(j))], [x(xp(j + diff), true), y(yp(j + diff))]];
            }
            return generateM(points);
        };
    }

    // Generate
    for (i = 0; i < d.length; i++) {

        // Draw as normal
        if (isUndefined(regions) || !isWithinRegions(d[i].x, regions)) {
            s += " " + xValue(d[i]) + " " + yValue(d[i]);
        }
        // Draw with region // TODO: Fix for horizotal charts
        else {
                xp = $$.getScale(d[i - 1].x + xOffset, d[i].x + xOffset, $$.isTimeSeries());
                yp = $$.getScale(d[i - 1].value, d[i].value);

                dx = x(d[i].x) - x(d[i - 1].x);
                dy = y(d[i].value) - y(d[i - 1].value);
                dd = Math.sqrt(Math.pow(dx, 2) + Math.pow(dy, 2));
                diff = 2 / dd;
                diffx2 = diff * 2;

                for (j = diff; j <= 1; j += diffx2) {
                    s += sWithRegion(d[i - 1], d[i], j, diff);
                }
            }
        prev = d[i].x;
    }

    return s;
};

c3_chart_internal_fn.updateArea = function (durationForExit) {
    var $$ = this,
        d3 = $$.d3;
    $$.mainArea = $$.main.selectAll('.' + CLASS.areas).selectAll('.' + CLASS.area).data($$.lineData.bind($$));
    $$.mainArea.enter().append('path').attr("class", $$.classArea.bind($$)).style("fill", $$.color).style("opacity", function () {
        $$.orgAreaOpacity = +d3.select(this).style('opacity');return 0;
    });
    $$.mainArea.style("opacity", $$.orgAreaOpacity);
    $$.mainArea.exit().transition().duration(durationForExit).style('opacity', 0).remove();
};
c3_chart_internal_fn.redrawArea = function (drawArea, withTransition) {
    return [(withTransition ? this.mainArea.transition(Math.random().toString()) : this.mainArea).attr("d", drawArea).style("fill", this.color).style("opacity", this.orgAreaOpacity)];
};
c3_chart_internal_fn.generateDrawArea = function (areaIndices, isSub) {
    var $$ = this,
        config = $$.config,
        area = $$.d3.svg.area(),
        getPoints = $$.generateGetAreaPoints(areaIndices, isSub),
        yScaleGetter = isSub ? $$.getSubYScale : $$.getYScale,
        xValue = function xValue(d) {
        return (isSub ? $$.subxx : $$.xx).call($$, d);
    },
        value0 = function value0(d, i) {
        return config.data_groups.length > 0 ? getPoints(d, i)[0][1] : yScaleGetter.call($$, d.id)($$.getAreaBaseValue(d.id));
    },
        value1 = function value1(d, i) {
        return config.data_groups.length > 0 ? getPoints(d, i)[1][1] : yScaleGetter.call($$, d.id)(d.value);
    };

    area = config.axis_rotated ? area.x0(value0).x1(value1).y(xValue) : area.x(xValue).y0(config.area_above ? 0 : value0).y1(value1);
    if (!config.line_connectNull) {
        area = area.defined(function (d) {
            return d.value !== null;
        });
    }

    return function (d) {
        var values = config.line_connectNull ? $$.filterRemoveNull(d.values) : d.values,
            x0 = 0,
            y0 = 0,
            path;
        if ($$.isAreaType(d)) {
            if ($$.isStepType(d)) {
                values = $$.convertValuesToStep(values);
            }
            path = area.interpolate($$.getInterpolate(d))(values);
        } else {
            if (values[0]) {
                x0 = $$.x(values[0].x);
                y0 = $$.getYScale(d.id)(values[0].value);
            }
            path = config.axis_rotated ? "M " + y0 + " " + x0 : "M " + x0 + " " + y0;
        }
        return path ? path : "M 0 0";
    };
};
c3_chart_internal_fn.getAreaBaseValue = function () {
    return 0;
};
c3_chart_internal_fn.generateGetAreaPoints = function (areaIndices, isSub) {
    // partial duplication of generateGetBarPoints
    var $$ = this,
        config = $$.config,
        areaTargetsNum = areaIndices.__max__ + 1,
        x = $$.getShapeX(0, areaTargetsNum, areaIndices, !!isSub),
        y = $$.getShapeY(!!isSub),
        areaOffset = $$.getShapeOffset($$.isAreaType, areaIndices, !!isSub),
        yScale = isSub ? $$.getSubYScale : $$.getYScale;
    return function (d, i) {
        var y0 = yScale.call($$, d.id)(0),
            offset = areaOffset(d, i) || y0,
            // offset is for stacked area chart
        posX = x(d),
            posY = y(d);
        // fix posY not to overflow opposite quadrant
        if (config.axis_rotated) {
            if (0 < d.value && posY < y0 || d.value < 0 && y0 < posY) {
                posY = y0;
            }
        }
        // 1 point that marks the area position
        return [[posX, offset], [posX, posY - (y0 - offset)], [posX, posY - (y0 - offset)], // needed for compatibility
        [posX, offset] // needed for compatibility
        ];
    };
};

c3_chart_internal_fn.updateCircle = function () {
    var $$ = this;
    $$.mainCircle = $$.main.selectAll('.' + CLASS.circles).selectAll('.' + CLASS.circle).data($$.lineOrScatterData.bind($$));
    $$.mainCircle.enter().append("circle").attr("class", $$.classCircle.bind($$)).attr("r", $$.pointR.bind($$)).style("fill", $$.color);
    $$.mainCircle.style("opacity", $$.initialOpacityForCircle.bind($$));
    $$.mainCircle.exit().remove();
};
c3_chart_internal_fn.redrawCircle = function (cx, cy, withTransition) {
    var selectedCircles = this.main.selectAll('.' + CLASS.selectedCircle);
    return [(withTransition ? this.mainCircle.transition(Math.random().toString()) : this.mainCircle).style('opacity', this.opacityForCircle.bind(this)).style("fill", this.color).attr("cx", cx).attr("cy", cy), (withTransition ? selectedCircles.transition(Math.random().toString()) : selectedCircles).attr("cx", cx).attr("cy", cy)];
};
c3_chart_internal_fn.circleX = function (d) {
    return d.x || d.x === 0 ? this.x(d.x) : null;
};
c3_chart_internal_fn.updateCircleY = function () {
    var $$ = this,
        lineIndices,
        getPoints;
    if ($$.config.data_groups.length > 0) {
        lineIndices = $$.getShapeIndices($$.isLineType), getPoints = $$.generateGetLinePoints(lineIndices);
        $$.circleY = function (d, i) {
            return getPoints(d, i)[0][1];
        };
    } else {
        $$.circleY = function (d) {
            return $$.getYScale(d.id)(d.value);
        };
    }
};
c3_chart_internal_fn.getCircles = function (i, id) {
    var $$ = this;
    return (id ? $$.main.selectAll('.' + CLASS.circles + $$.getTargetSelectorSuffix(id)) : $$.main).selectAll('.' + CLASS.circle + (isValue(i) ? '-' + i : ''));
};
c3_chart_internal_fn.expandCircles = function (i, id, reset) {
    var $$ = this,
        r = $$.pointExpandedR.bind($$);
    if (reset) {
        $$.unexpandCircles();
    }
    $$.getCircles(i, id).classed(CLASS.EXPANDED, true).attr('r', r);
};
c3_chart_internal_fn.unexpandCircles = function (i) {
    var $$ = this,
        r = $$.pointR.bind($$);
    $$.getCircles(i).filter(function () {
        return $$.d3.select(this).classed(CLASS.EXPANDED);
    }).classed(CLASS.EXPANDED, false).attr('r', r);
};
c3_chart_internal_fn.pointR = function (d) {
    var $$ = this,
        config = $$.config;
    return $$.isStepType(d) ? 0 : isFunction(config.point_r) ? config.point_r(d) : config.point_r;
};
c3_chart_internal_fn.pointExpandedR = function (d) {
    var $$ = this,
        config = $$.config;
    if (config.point_focus_expand_enabled) {
        return isFunction(config.point_focus_expand_r) ? config.point_focus_expand_r(d) : config.point_focus_expand_r ? config.point_focus_expand_r : $$.pointR(d) * 1.75;
    } else {
        return $$.pointR(d);
    }
};
c3_chart_internal_fn.pointSelectR = function (d) {
    var $$ = this,
        config = $$.config;
    return isFunction(config.point_select_r) ? config.point_select_r(d) : config.point_select_r ? config.point_select_r : $$.pointR(d) * 4;
};
c3_chart_internal_fn.isWithinCircle = function (that, r) {
    var d3 = this.d3,
        mouse = d3.mouse(that),
        d3_this = d3.select(that),
        cx = +d3_this.attr("cx"),
        cy = +d3_this.attr("cy");
    return Math.sqrt(Math.pow(cx - mouse[0], 2) + Math.pow(cy - mouse[1], 2)) < r;
};
c3_chart_internal_fn.isWithinStep = function (that, y) {
    return Math.abs(y - this.d3.mouse(that)[1]) < 30;
};

c3_chart_internal_fn.getCurrentWidth = function () {
    var $$ = this,
        config = $$.config;
    return config.size_width ? config.size_width : $$.getParentWidth();
};
c3_chart_internal_fn.getCurrentHeight = function () {
    var $$ = this,
        config = $$.config,
        h = config.size_height ? config.size_height : $$.getParentHeight();
    return h > 0 ? h : 320 / ($$.hasType('gauge') && !config.gauge_fullCircle ? 2 : 1);
};
c3_chart_internal_fn.getCurrentPaddingTop = function () {
    var $$ = this,
        config = $$.config,
        padding = isValue(config.padding_top) ? config.padding_top : 0;
    if ($$.title && $$.title.node()) {
        padding += $$.getTitlePadding();
    }
    return padding;
};
c3_chart_internal_fn.getCurrentPaddingBottom = function () {
    var config = this.config;
    return isValue(config.padding_bottom) ? config.padding_bottom : 0;
};
c3_chart_internal_fn.getCurrentPaddingLeft = function (withoutRecompute) {
    var $$ = this,
        config = $$.config;
    if (isValue(config.padding_left)) {
        return config.padding_left;
    } else if (config.axis_rotated) {
        return !config.axis_x_show ? 1 : Math.max(ceil10($$.getAxisWidthByAxisId('x', withoutRecompute)), 40);
    } else if (!config.axis_y_show || config.axis_y_inner) {
        // && !config.axis_rotated
        return $$.axis.getYAxisLabelPosition().isOuter ? 30 : 1;
    } else {
        return ceil10($$.getAxisWidthByAxisId('y', withoutRecompute));
    }
};
c3_chart_internal_fn.getCurrentPaddingRight = function () {
    var $$ = this,
        config = $$.config,
        defaultPadding = 10,
        legendWidthOnRight = $$.isLegendRight ? $$.getLegendWidth() + 20 : 0;
    if (isValue(config.padding_right)) {
        return config.padding_right + 1; // 1 is needed not to hide tick line
    } else if (config.axis_rotated) {
        return defaultPadding + legendWidthOnRight;
    } else if (!config.axis_y2_show || config.axis_y2_inner) {
        // && !config.axis_rotated
        return 2 + legendWidthOnRight + ($$.axis.getY2AxisLabelPosition().isOuter ? 20 : 0);
    } else {
        return ceil10($$.getAxisWidthByAxisId('y2')) + legendWidthOnRight;
    }
};

c3_chart_internal_fn.getParentRectValue = function (key) {
    var parent = this.selectChart.node(),
        v;
    while (parent && parent.tagName !== 'BODY') {
        try {
            v = parent.getBoundingClientRect()[key];
        } catch (e) {
            if (key === 'width') {
                // In IE in certain cases getBoundingClientRect
                // will cause an "unspecified error"
                v = parent.offsetWidth;
            }
        }
        if (v) {
            break;
        }
        parent = parent.parentNode;
    }
    return v;
};
c3_chart_internal_fn.getParentWidth = function () {
    return this.getParentRectValue('width');
};
c3_chart_internal_fn.getParentHeight = function () {
    var h = this.selectChart.style('height');
    return h.indexOf('px') > 0 ? +h.replace('px', '') : 0;
};

c3_chart_internal_fn.getSvgLeft = function (withoutRecompute) {
    var $$ = this,
        config = $$.config,
        hasLeftAxisRect = config.axis_rotated || !config.axis_rotated && !config.axis_y_inner,
        leftAxisClass = config.axis_rotated ? CLASS.axisX : CLASS.axisY,
        leftAxis = $$.main.select('.' + leftAxisClass).node(),
        svgRect = leftAxis && hasLeftAxisRect ? leftAxis.getBoundingClientRect() : { right: 0 },
        chartRect = $$.selectChart.node().getBoundingClientRect(),
        hasArc = $$.hasArcType(),
        svgLeft = svgRect.right - chartRect.left - (hasArc ? 0 : $$.getCurrentPaddingLeft(withoutRecompute));
    return svgLeft > 0 ? svgLeft : 0;
};

c3_chart_internal_fn.getAxisWidthByAxisId = function (id, withoutRecompute) {
    var $$ = this,
        position = $$.axis.getLabelPositionById(id);
    return $$.axis.getMaxTickWidth(id, withoutRecompute) + (position.isInner ? 20 : 40);
};
c3_chart_internal_fn.getHorizontalAxisHeight = function (axisId) {
    var $$ = this,
        config = $$.config,
        h = 30;
    if (axisId === 'x' && !config.axis_x_show) {
        return 8;
    }
    if (axisId === 'x' && config.axis_x_height) {
        return config.axis_x_height;
    }
    if (axisId === 'y' && !config.axis_y_show) {
        return config.legend_show && !$$.isLegendRight && !$$.isLegendInset ? 10 : 1;
    }
    if (axisId === 'y2' && !config.axis_y2_show) {
        return $$.rotated_padding_top;
    }
    // Calculate x axis height when tick rotated
    if (axisId === 'x' && !config.axis_rotated && config.axis_x_tick_rotate) {
        h = 30 + $$.axis.getMaxTickWidth(axisId) * Math.cos(Math.PI * (90 - config.axis_x_tick_rotate) / 180);
    }
    // Calculate y axis height when tick rotated
    if (axisId === 'y' && config.axis_rotated && config.axis_y_tick_rotate) {
        h = 30 + $$.axis.getMaxTickWidth(axisId) * Math.cos(Math.PI * (90 - config.axis_y_tick_rotate) / 180);
    }
    return h + ($$.axis.getLabelPositionById(axisId).isInner ? 0 : 10) + (axisId === 'y2' ? -10 : 0);
};

c3_chart_internal_fn.getEventRectWidth = function () {
    return Math.max(0, this.xAxis.tickInterval());
};

c3_chart_internal_fn.initBrush = function () {
    var $$ = this,
        d3 = $$.d3;
    $$.brush = d3.svg.brush().on("brush", function () {
        $$.redrawForBrush();
    });
    $$.brush.update = function () {
        if ($$.context) {
            $$.context.select('.' + CLASS.brush).call(this);
        }
        return this;
    };
    $$.brush.scale = function (scale) {
        return $$.config.axis_rotated ? this.y(scale) : this.x(scale);
    };
};
c3_chart_internal_fn.initSubchart = function () {
    var $$ = this,
        config = $$.config,
        context = $$.context = $$.svg.append("g").attr("transform", $$.getTranslate('context')),
        visibility = config.subchart_show ? 'visible' : 'hidden';

    context.style('visibility', visibility);

    // Define g for chart area
    context.append('g').attr("clip-path", $$.clipPathForSubchart).attr('class', CLASS.chart);

    // Define g for bar chart area
    context.select('.' + CLASS.chart).append("g").attr("class", CLASS.chartBars);

    // Define g for line chart area
    context.select('.' + CLASS.chart).append("g").attr("class", CLASS.chartLines);

    // Add extent rect for Brush
    context.append("g").attr("clip-path", $$.clipPath).attr("class", CLASS.brush).call($$.brush);

    // ATTENTION: This must be called AFTER chart added
    // Add Axis
    $$.axes.subx = context.append("g").attr("class", CLASS.axisX).attr("transform", $$.getTranslate('subx')).attr("clip-path", config.axis_rotated ? "" : $$.clipPathForXAxis).style("visibility", config.subchart_axis_x_show ? visibility : 'hidden');
};
c3_chart_internal_fn.updateTargetsForSubchart = function (targets) {
    var $$ = this,
        context = $$.context,
        config = $$.config,
        contextLineEnter,
        contextLineUpdate,
        contextBarEnter,
        contextBarUpdate,
        classChartBar = $$.classChartBar.bind($$),
        classBars = $$.classBars.bind($$),
        classChartLine = $$.classChartLine.bind($$),
        classLines = $$.classLines.bind($$),
        classAreas = $$.classAreas.bind($$);

    if (config.subchart_show) {
        //-- Bar --//
        contextBarUpdate = context.select('.' + CLASS.chartBars).selectAll('.' + CLASS.chartBar).data(targets).attr('class', classChartBar);
        contextBarEnter = contextBarUpdate.enter().append('g').style('opacity', 0).attr('class', classChartBar);
        // Bars for each data
        contextBarEnter.append('g').attr("class", classBars);

        //-- Line --//
        contextLineUpdate = context.select('.' + CLASS.chartLines).selectAll('.' + CLASS.chartLine).data(targets).attr('class', classChartLine);
        contextLineEnter = contextLineUpdate.enter().append('g').style('opacity', 0).attr('class', classChartLine);
        // Lines for each data
        contextLineEnter.append("g").attr("class", classLines);
        // Area
        contextLineEnter.append("g").attr("class", classAreas);

        //-- Brush --//
        context.selectAll('.' + CLASS.brush + ' rect').attr(config.axis_rotated ? "width" : "height", config.axis_rotated ? $$.width2 : $$.height2);
    }
};
c3_chart_internal_fn.updateBarForSubchart = function (durationForExit) {
    var $$ = this;
    $$.contextBar = $$.context.selectAll('.' + CLASS.bars).selectAll('.' + CLASS.bar).data($$.barData.bind($$));
    $$.contextBar.enter().append('path').attr("class", $$.classBar.bind($$)).style("stroke", 'none').style("fill", $$.color);
    $$.contextBar.style("opacity", $$.initialOpacity.bind($$));
    $$.contextBar.exit().transition().duration(durationForExit).style('opacity', 0).remove();
};
c3_chart_internal_fn.redrawBarForSubchart = function (drawBarOnSub, withTransition, duration) {
    (withTransition ? this.contextBar.transition(Math.random().toString()).duration(duration) : this.contextBar).attr('d', drawBarOnSub).style('opacity', 1);
};
c3_chart_internal_fn.updateLineForSubchart = function (durationForExit) {
    var $$ = this;
    $$.contextLine = $$.context.selectAll('.' + CLASS.lines).selectAll('.' + CLASS.line).data($$.lineData.bind($$));
    $$.contextLine.enter().append('path').attr('class', $$.classLine.bind($$)).style('stroke', $$.color);
    $$.contextLine.style("opacity", $$.initialOpacity.bind($$));
    $$.contextLine.exit().transition().duration(durationForExit).style('opacity', 0).remove();
};
c3_chart_internal_fn.redrawLineForSubchart = function (drawLineOnSub, withTransition, duration) {
    (withTransition ? this.contextLine.transition(Math.random().toString()).duration(duration) : this.contextLine).attr("d", drawLineOnSub).style('opacity', 1);
};
c3_chart_internal_fn.updateAreaForSubchart = function (durationForExit) {
    var $$ = this,
        d3 = $$.d3;
    $$.contextArea = $$.context.selectAll('.' + CLASS.areas).selectAll('.' + CLASS.area).data($$.lineData.bind($$));
    $$.contextArea.enter().append('path').attr("class", $$.classArea.bind($$)).style("fill", $$.color).style("opacity", function () {
        $$.orgAreaOpacity = +d3.select(this).style('opacity');return 0;
    });
    $$.contextArea.style("opacity", 0);
    $$.contextArea.exit().transition().duration(durationForExit).style('opacity', 0).remove();
};
c3_chart_internal_fn.redrawAreaForSubchart = function (drawAreaOnSub, withTransition, duration) {
    (withTransition ? this.contextArea.transition(Math.random().toString()).duration(duration) : this.contextArea).attr("d", drawAreaOnSub).style("fill", this.color).style("opacity", this.orgAreaOpacity);
};
c3_chart_internal_fn.redrawSubchart = function (withSubchart, transitions, duration, durationForExit, areaIndices, barIndices, lineIndices) {
    var $$ = this,
        d3 = $$.d3,
        config = $$.config,
        drawAreaOnSub,
        drawBarOnSub,
        drawLineOnSub;

    $$.context.style('visibility', config.subchart_show ? 'visible' : 'hidden');

    // subchart
    if (config.subchart_show) {
        // reflect main chart to extent on subchart if zoomed
        if (d3.event && d3.event.type === 'zoom') {
            $$.brush.extent($$.x.orgDomain()).update();
        }
        // update subchart elements if needed
        if (withSubchart) {

            // extent rect
            if (!$$.brush.empty()) {
                $$.brush.extent($$.x.orgDomain()).update();
            }
            // setup drawer - MEMO: this must be called after axis updated
            drawAreaOnSub = $$.generateDrawArea(areaIndices, true);
            drawBarOnSub = $$.generateDrawBar(barIndices, true);
            drawLineOnSub = $$.generateDrawLine(lineIndices, true);

            $$.updateBarForSubchart(duration);
            $$.updateLineForSubchart(duration);
            $$.updateAreaForSubchart(duration);

            $$.redrawBarForSubchart(drawBarOnSub, duration, duration);
            $$.redrawLineForSubchart(drawLineOnSub, duration, duration);
            $$.redrawAreaForSubchart(drawAreaOnSub, duration, duration);
        }
    }
};
c3_chart_internal_fn.redrawForBrush = function () {
    var $$ = this,
        x = $$.x;
    $$.redraw({
        withTransition: false,
        withY: $$.config.zoom_rescale,
        withSubchart: false,
        withUpdateXDomain: true,
        withDimension: false
    });
    $$.config.subchart_onbrush.call($$.api, x.orgDomain());
};
c3_chart_internal_fn.transformContext = function (withTransition, transitions) {
    var $$ = this,
        subXAxis;
    if (transitions && transitions.axisSubX) {
        subXAxis = transitions.axisSubX;
    } else {
        subXAxis = $$.context.select('.' + CLASS.axisX);
        if (withTransition) {
            subXAxis = subXAxis.transition();
        }
    }
    $$.context.attr("transform", $$.getTranslate('context'));
    subXAxis.attr("transform", $$.getTranslate('subx'));
};
c3_chart_internal_fn.getDefaultExtent = function () {
    var $$ = this,
        config = $$.config,
        extent = isFunction(config.axis_x_extent) ? config.axis_x_extent($$.getXDomain($$.data.targets)) : config.axis_x_extent;
    if ($$.isTimeSeries()) {
        extent = [$$.parseDate(extent[0]), $$.parseDate(extent[1])];
    }
    return extent;
};

c3_chart_internal_fn.initText = function () {
    var $$ = this;
    $$.main.select('.' + CLASS.chart).append("g").attr("class", CLASS.chartTexts);
    $$.mainText = $$.d3.selectAll([]);
};
c3_chart_internal_fn.updateTargetsForText = function (targets) {
    var $$ = this,
        mainTextUpdate,
        mainTextEnter,
        classChartText = $$.classChartText.bind($$),
        classTexts = $$.classTexts.bind($$),
        classFocus = $$.classFocus.bind($$);
    mainTextUpdate = $$.main.select('.' + CLASS.chartTexts).selectAll('.' + CLASS.chartText).data(targets).attr('class', function (d) {
        return classChartText(d) + classFocus(d);
    });
    mainTextEnter = mainTextUpdate.enter().append('g').attr('class', classChartText).style('opacity', 0).style("pointer-events", "none");
    mainTextEnter.append('g').attr('class', classTexts);
};
c3_chart_internal_fn.updateText = function (durationForExit) {
    var $$ = this,
        config = $$.config,
        barOrLineData = $$.barOrLineData.bind($$),
        classText = $$.classText.bind($$);
    $$.mainText = $$.main.selectAll('.' + CLASS.texts).selectAll('.' + CLASS.text).data(barOrLineData);
    $$.mainText.enter().append('text').attr("class", classText).attr('text-anchor', function (d) {
        return config.axis_rotated ? d.value < 0 ? 'end' : 'start' : 'middle';
    }).style("stroke", 'none').style("fill", function (d) {
        return $$.color(d);
    }).style("fill-opacity", 0);
    $$.mainText.text(function (d, i, j) {
        return $$.dataLabelFormat(d.id)(d.value, d.id, i, j);
    });
    $$.mainText.exit().transition().duration(durationForExit).style('fill-opacity', 0).remove();
};
c3_chart_internal_fn.redrawText = function (xForText, yForText, forFlow, withTransition) {
    return [(withTransition ? this.mainText.transition() : this.mainText).attr('x', xForText).attr('y', yForText).style("fill", this.color).style("fill-opacity", forFlow ? 0 : this.opacityForText.bind(this))];
};
c3_chart_internal_fn.getTextRect = function (text, cls, element) {
    var dummy = this.d3.select('body').append('div').classed('c3', true),
        svg = dummy.append("svg").style('visibility', 'hidden').style('position', 'fixed').style('top', 0).style('left', 0),
        font = this.d3.select(element).style('font'),
        rect;
    svg.selectAll('.dummy').data([text]).enter().append('text').classed(cls ? cls : "", true).style('font', font).text(text).each(function () {
        rect = this.getBoundingClientRect();
    });
    dummy.remove();
    return rect;
};
c3_chart_internal_fn.generateXYForText = function (areaIndices, barIndices, lineIndices, forX) {
    var $$ = this,
        getAreaPoints = $$.generateGetAreaPoints(areaIndices, false),
        getBarPoints = $$.generateGetBarPoints(barIndices, false),
        getLinePoints = $$.generateGetLinePoints(lineIndices, false),
        getter = forX ? $$.getXForText : $$.getYForText;
    return function (d, i) {
        var getPoints = $$.isAreaType(d) ? getAreaPoints : $$.isBarType(d) ? getBarPoints : getLinePoints;
        return getter.call($$, getPoints(d, i), d, this);
    };
};
c3_chart_internal_fn.getXForText = function (points, d, textElement) {
    var $$ = this,
        box = textElement.getBoundingClientRect(),
        xPos,
        padding;
    if ($$.config.axis_rotated) {
        padding = $$.isBarType(d) ? 4 : 6;
        xPos = points[2][1] + padding * (d.value < 0 ? -1 : 1);
    } else {
        xPos = $$.hasType('bar') ? (points[2][0] + points[0][0]) / 2 : points[0][0];
    }
    // show labels regardless of the domain if value is null
    if (d.value === null) {
        if (xPos > $$.width) {
            xPos = $$.width - box.width;
        } else if (xPos < 0) {
            xPos = 4;
        }
    }
    return xPos;
};
c3_chart_internal_fn.getYForText = function (points, d, textElement) {
    var $$ = this,
        box = textElement.getBoundingClientRect(),
        yPos;
    if ($$.config.axis_rotated) {
        yPos = (points[0][0] + points[2][0] + box.height * 0.6) / 2;
    } else {
        yPos = points[2][1];
        if (d.value < 0 || d.value === 0 && !$$.hasPositiveValue) {
            yPos += box.height;
            if ($$.isBarType(d) && $$.isSafari()) {
                yPos -= 3;
            } else if (!$$.isBarType(d) && $$.isChrome()) {
                yPos += 3;
            }
        } else {
            yPos += $$.isBarType(d) ? -3 : -6;
        }
    }
    // show labels regardless of the domain if value is null
    if (d.value === null && !$$.config.axis_rotated) {
        if (yPos < box.height) {
            yPos = box.height;
        } else if (yPos > this.height) {
            yPos = this.height - 4;
        }
    }
    return yPos;
};

c3_chart_internal_fn.initTitle = function () {
    var $$ = this;
    $$.title = $$.svg.append("text").text($$.config.title_text).attr("class", $$.CLASS.title);
};
c3_chart_internal_fn.redrawTitle = function () {
    var $$ = this;
    $$.title.attr("x", $$.xForTitle.bind($$)).attr("y", $$.yForTitle.bind($$));
};
c3_chart_internal_fn.xForTitle = function () {
    var $$ = this,
        config = $$.config,
        position = config.title_position || 'left',
        x;
    if (position.indexOf('right') >= 0) {
        x = $$.currentWidth - $$.getTextRect($$.title.node().textContent, $$.CLASS.title, $$.title.node()).width - config.title_padding.right;
    } else if (position.indexOf('center') >= 0) {
        x = ($$.currentWidth - $$.getTextRect($$.title.node().textContent, $$.CLASS.title, $$.title.node()).width) / 2;
    } else {
        // left
        x = config.title_padding.left;
    }
    return x;
};
c3_chart_internal_fn.yForTitle = function () {
    var $$ = this;
    return $$.config.title_padding.top + $$.getTextRect($$.title.node().textContent, $$.CLASS.title, $$.title.node()).height;
};
c3_chart_internal_fn.getTitlePadding = function () {
    var $$ = this;
    return $$.yForTitle() + $$.config.title_padding.bottom;
};

c3_chart_internal_fn.initTooltip = function () {
    var $$ = this,
        config = $$.config,
        i;
    $$.tooltip = $$.selectChart.style("position", "relative").append("div").attr('class', CLASS.tooltipContainer).style("position", "absolute").style("pointer-events", "none").style("display", "none");
    // Show tooltip if needed
    if (config.tooltip_init_show) {
        if ($$.isTimeSeries() && isString(config.tooltip_init_x)) {
            config.tooltip_init_x = $$.parseDate(config.tooltip_init_x);
            for (i = 0; i < $$.data.targets[0].values.length; i++) {
                if ($$.data.targets[0].values[i].x - config.tooltip_init_x === 0) {
                    break;
                }
            }
            config.tooltip_init_x = i;
        }
        $$.tooltip.html(config.tooltip_contents.call($$, $$.data.targets.map(function (d) {
            return $$.addName(d.values[config.tooltip_init_x]);
        }), $$.axis.getXAxisTickFormat(), $$.getYFormat($$.hasArcType()), $$.color));
        $$.tooltip.style("top", config.tooltip_init_position.top).style("left", config.tooltip_init_position.left).style("display", "block");
    }
};
c3_chart_internal_fn.getTooltipSortFunction = function () {
    var $$ = this,
        config = $$.config;

    if (config.data_groups.length === 0 || config.tooltip_order !== undefined) {
        // if data are not grouped or if an order is specified
        // for the tooltip values we sort them by their values

        var order = config.tooltip_order;
        if (order === undefined) {
            order = config.data_order;
        }

        var valueOf = function valueOf(obj) {
            return obj ? obj.value : null;
        };

        // if data are not grouped, we sort them by their value
        if (isString(order) && order.toLowerCase() === 'asc') {
            return function (a, b) {
                return valueOf(a) - valueOf(b);
            };
        } else if (isString(order) && order.toLowerCase() === 'desc') {
            return function (a, b) {
                return valueOf(b) - valueOf(a);
            };
        } else if (isFunction(order)) {

            // if the function is from data_order we need
            // to wrap the returned function in order to format
            // the sorted value to the expected format

            var sortFunction = order;

            if (config.tooltip_order === undefined) {
                sortFunction = function sortFunction(a, b) {
                    return order(a ? {
                        id: a.id,
                        values: [a]
                    } : null, b ? {
                        id: b.id,
                        values: [b]
                    } : null);
                };
            }

            return sortFunction;
        } else if (isArray(order)) {
            return function (a, b) {
                return order.indexOf(a.id) - order.indexOf(b.id);
            };
        }
    } else {
        // if data are grouped, we follow the order of grouped targets
        var ids = $$.orderTargets($$.data.targets).map(function (i) {
            return i.id;
        });

        // if it was either asc or desc we need to invert the order
        // returned by orderTargets
        if ($$.isOrderAsc() || $$.isOrderDesc()) {
            ids = ids.reverse();
        }

        return function (a, b) {
            return ids.indexOf(a.id) - ids.indexOf(b.id);
        };
    }
};
c3_chart_internal_fn.getTooltipContent = function (d, defaultTitleFormat, defaultValueFormat, color) {
    var $$ = this,
        config = $$.config,
        titleFormat = config.tooltip_format_title || defaultTitleFormat,
        nameFormat = config.tooltip_format_name || function (name) {
        return name;
    },
        valueFormat = config.tooltip_format_value || defaultValueFormat,
        text,
        i,
        title,
        value,
        name,
        bgcolor;

    var tooltipSortFunction = this.getTooltipSortFunction();
    if (tooltipSortFunction) {
        d.sort(tooltipSortFunction);
    }

    for (i = 0; i < d.length; i++) {
        if (!(d[i] && (d[i].value || d[i].value === 0))) {
            continue;
        }

        if (!text) {
            title = sanitise(titleFormat ? titleFormat(d[i].x) : d[i].x);
            text = "<table class='" + $$.CLASS.tooltip + "'>" + (title || title === 0 ? "<tr><th colspan='2'>" + title + "</th></tr>" : "");
        }

        value = sanitise(valueFormat(d[i].value, d[i].ratio, d[i].id, d[i].index, d));
        if (value !== undefined) {
            // Skip elements when their name is set to null
            if (d[i].name === null) {
                continue;
            }
            name = sanitise(nameFormat(d[i].name, d[i].ratio, d[i].id, d[i].index));
            bgcolor = $$.levelColor ? $$.levelColor(d[i].value) : color(d[i].id);

            text += "<tr class='" + $$.CLASS.tooltipName + "-" + $$.getTargetSelectorSuffix(d[i].id) + "'>";
            text += "<td class='name'><span style='background-color:" + bgcolor + "'></span>" + name + "</td>";
            text += "<td class='value'>" + value + "</td>";
            text += "</tr>";
        }
    }
    return text + "</table>";
};
c3_chart_internal_fn.tooltipPosition = function (dataToShow, tWidth, tHeight, element) {
    var $$ = this,
        config = $$.config,
        d3 = $$.d3;
    var svgLeft, tooltipLeft, tooltipRight, tooltipTop, chartRight;
    var forArc = $$.hasArcType(),
        mouse = d3.mouse(element);
    // Determin tooltip position
    if (forArc) {
        tooltipLeft = ($$.width - ($$.isLegendRight ? $$.getLegendWidth() : 0)) / 2 + mouse[0];
        tooltipTop = $$.height / 2 + mouse[1] + 20;
    } else {
        svgLeft = $$.getSvgLeft(true);
        if (config.axis_rotated) {
            tooltipLeft = svgLeft + mouse[0] + 100;
            tooltipRight = tooltipLeft + tWidth;
            chartRight = $$.currentWidth - $$.getCurrentPaddingRight();
            tooltipTop = $$.x(dataToShow[0].x) + 20;
        } else {
            tooltipLeft = svgLeft + $$.getCurrentPaddingLeft(true) + $$.x(dataToShow[0].x) + 20;
            tooltipRight = tooltipLeft + tWidth;
            chartRight = svgLeft + $$.currentWidth - $$.getCurrentPaddingRight();
            tooltipTop = mouse[1] + 15;
        }

        if (tooltipRight > chartRight) {
            // 20 is needed for Firefox to keep tooltip width
            tooltipLeft -= tooltipRight - chartRight + 20;
        }
        if (tooltipTop + tHeight > $$.currentHeight) {
            tooltipTop -= tHeight + 30;
        }
    }
    if (tooltipTop < 0) {
        tooltipTop = 0;
    }
    return { top: tooltipTop, left: tooltipLeft };
};
c3_chart_internal_fn.showTooltip = function (selectedData, element) {
    var $$ = this,
        config = $$.config;
    var tWidth, tHeight, position;
    var forArc = $$.hasArcType(),
        dataToShow = selectedData.filter(function (d) {
        return d && isValue(d.value);
    }),
        positionFunction = config.tooltip_position || c3_chart_internal_fn.tooltipPosition;
    if (dataToShow.length === 0 || !config.tooltip_show) {
        return;
    }
    $$.tooltip.html(config.tooltip_contents.call($$, selectedData, $$.axis.getXAxisTickFormat(), $$.getYFormat(forArc), $$.color)).style("display", "block");

    // Get tooltip dimensions
    tWidth = $$.tooltip.property('offsetWidth');
    tHeight = $$.tooltip.property('offsetHeight');

    position = positionFunction.call(this, dataToShow, tWidth, tHeight, element);
    // Set tooltip
    $$.tooltip.style("top", position.top + "px").style("left", position.left + 'px');
};
c3_chart_internal_fn.hideTooltip = function () {
    this.tooltip.style("display", "none");
};

c3_chart_internal_fn.setTargetType = function (targetIds, type) {
    var $$ = this,
        config = $$.config;
    $$.mapToTargetIds(targetIds).forEach(function (id) {
        $$.withoutFadeIn[id] = type === config.data_types[id];
        config.data_types[id] = type;
    });
    if (!targetIds) {
        config.data_type = type;
    }
};
c3_chart_internal_fn.hasType = function (type, targets) {
    var $$ = this,
        types = $$.config.data_types,
        has = false;
    targets = targets || $$.data.targets;
    if (targets && targets.length) {
        targets.forEach(function (target) {
            var t = types[target.id];
            if (t && t.indexOf(type) >= 0 || !t && type === 'line') {
                has = true;
            }
        });
    } else if (Object.keys(types).length) {
        Object.keys(types).forEach(function (id) {
            if (types[id] === type) {
                has = true;
            }
        });
    } else {
        has = $$.config.data_type === type;
    }
    return has;
};
c3_chart_internal_fn.hasArcType = function (targets) {
    return this.hasType('pie', targets) || this.hasType('donut', targets) || this.hasType('gauge', targets);
};
c3_chart_internal_fn.isLineType = function (d) {
    var config = this.config,
        id = isString(d) ? d : d.id;
    return !config.data_types[id] || ['line', 'spline', 'area', 'area-spline', 'step', 'area-step'].indexOf(config.data_types[id]) >= 0;
};
c3_chart_internal_fn.isStepType = function (d) {
    var id = isString(d) ? d : d.id;
    return ['step', 'area-step'].indexOf(this.config.data_types[id]) >= 0;
};
c3_chart_internal_fn.isSplineType = function (d) {
    var id = isString(d) ? d : d.id;
    return ['spline', 'area-spline'].indexOf(this.config.data_types[id]) >= 0;
};
c3_chart_internal_fn.isAreaType = function (d) {
    var id = isString(d) ? d : d.id;
    return ['area', 'area-spline', 'area-step'].indexOf(this.config.data_types[id]) >= 0;
};
c3_chart_internal_fn.isBarType = function (d) {
    var id = isString(d) ? d : d.id;
    return this.config.data_types[id] === 'bar';
};
c3_chart_internal_fn.isScatterType = function (d) {
    var id = isString(d) ? d : d.id;
    return this.config.data_types[id] === 'scatter';
};
c3_chart_internal_fn.isPieType = function (d) {
    var id = isString(d) ? d : d.id;
    return this.config.data_types[id] === 'pie';
};
c3_chart_internal_fn.isGaugeType = function (d) {
    var id = isString(d) ? d : d.id;
    return this.config.data_types[id] === 'gauge';
};
c3_chart_internal_fn.isDonutType = function (d) {
    var id = isString(d) ? d : d.id;
    return this.config.data_types[id] === 'donut';
};
c3_chart_internal_fn.isArcType = function (d) {
    return this.isPieType(d) || this.isDonutType(d) || this.isGaugeType(d);
};
c3_chart_internal_fn.lineData = function (d) {
    return this.isLineType(d) ? [d] : [];
};
c3_chart_internal_fn.arcData = function (d) {
    return this.isArcType(d.data) ? [d] : [];
};
/* not used
 function scatterData(d) {
 return isScatterType(d) ? d.values : [];
 }
 */
c3_chart_internal_fn.barData = function (d) {
    return this.isBarType(d) ? d.values : [];
};
c3_chart_internal_fn.lineOrScatterData = function (d) {
    return this.isLineType(d) || this.isScatterType(d) ? d.values : [];
};
c3_chart_internal_fn.barOrLineData = function (d) {
    return this.isBarType(d) || this.isLineType(d) ? d.values : [];
};
c3_chart_internal_fn.isInterpolationType = function (type) {
    return ['linear', 'linear-closed', 'basis', 'basis-open', 'basis-closed', 'bundle', 'cardinal', 'cardinal-open', 'cardinal-closed', 'monotone'].indexOf(type) >= 0;
};

c3_chart_internal_fn.isSafari = function () {
    var ua = window.navigator.userAgent;
    return ua.indexOf('Safari') >= 0 && ua.indexOf('Chrome') < 0;
};
c3_chart_internal_fn.isChrome = function () {
    var ua = window.navigator.userAgent;
    return ua.indexOf('Chrome') >= 0;
};

c3_chart_internal_fn.initZoom = function () {
    var $$ = this,
        d3 = $$.d3,
        config = $$.config,
        startEvent;

    $$.zoom = d3.behavior.zoom().on("zoomstart", function () {
        startEvent = d3.event.sourceEvent;
        $$.zoom.altDomain = d3.event.sourceEvent.altKey ? $$.x.orgDomain() : null;
        config.zoom_onzoomstart.call($$.api, d3.event.sourceEvent);
    }).on("zoom", function () {
        $$.redrawForZoom.call($$);
    }).on('zoomend', function () {
        var event = d3.event.sourceEvent;
        // if click, do nothing. otherwise, click interaction will be canceled.
        if (event && startEvent.clientX === event.clientX && startEvent.clientY === event.clientY) {
            return;
        }
        $$.redrawEventRect();
        $$.updateZoom();
        config.zoom_onzoomend.call($$.api, $$.x.orgDomain());
    });
    $$.zoom.scale = function (scale) {
        return config.axis_rotated ? this.y(scale) : this.x(scale);
    };
    $$.zoom.orgScaleExtent = function () {
        var extent = config.zoom_extent ? config.zoom_extent : [1, 10];
        return [extent[0], Math.max($$.getMaxDataCount() / extent[1], extent[1])];
    };
    $$.zoom.updateScaleExtent = function () {
        var ratio = diffDomain($$.x.orgDomain()) / diffDomain($$.getZoomDomain()),
            extent = this.orgScaleExtent();
        this.scaleExtent([extent[0] * ratio, extent[1] * ratio]);
        return this;
    };
};
c3_chart_internal_fn.getZoomDomain = function () {
    var $$ = this,
        config = $$.config,
        d3 = $$.d3,
        min = d3.min([$$.orgXDomain[0], config.zoom_x_min]),
        max = d3.max([$$.orgXDomain[1], config.zoom_x_max]);
    return [min, max];
};
c3_chart_internal_fn.updateZoom = function () {
    var $$ = this,
        z = $$.config.zoom_enabled ? $$.zoom : function () {};
    $$.main.select('.' + CLASS.zoomRect).call(z).on("dblclick.zoom", null);
    $$.main.selectAll('.' + CLASS.eventRect).call(z).on("dblclick.zoom", null);
};
c3_chart_internal_fn.redrawForZoom = function () {
    var $$ = this,
        d3 = $$.d3,
        config = $$.config,
        zoom = $$.zoom,
        x = $$.x;
    if (!config.zoom_enabled) {
        return;
    }
    if ($$.filterTargetsToShow($$.data.targets).length === 0) {
        return;
    }
    if (d3.event.sourceEvent.type === 'mousemove' && zoom.altDomain) {
        x.domain(zoom.altDomain);
        zoom.scale(x).updateScaleExtent();
        return;
    }
    if ($$.isCategorized() && x.orgDomain()[0] === $$.orgXDomain[0]) {
        x.domain([$$.orgXDomain[0] - 1e-10, x.orgDomain()[1]]);
    }
    $$.redraw({
        withTransition: false,
        withY: config.zoom_rescale,
        withSubchart: false,
        withEventRect: false,
        withDimension: false
    });
    if (d3.event.sourceEvent.type === 'mousemove') {
        $$.cancelClick = true;
    }
    config.zoom_onzoom.call($$.api, x.orgDomain());
};

return c3$1;

})));
