(function() {
    var extra = {};

    c3.chart.internal.fn.additionalConfig = {
        data_pairs: [],
    };

    c3.chart.internal.fn.beforeInit = function (config) {

        var that = this;

        // update internals only when chart type is "bubble"
        if (config.data.type !== 'bubble') {
            return;
        }

        // Set extra to ba able to be used in other part
        this.extra = extra;

        extra.getKey = function (x, y) {
            return x + '::' + y;
        };

        this.config.data_type = 'scatter';

        this.config.axis_x_padding = 0;
        this.config.axis_y_padding = 0;
        this.config.axis_x_tick_centered = true;
        this.config.axis_x_tick_format = function (d) {
            return extra.names[d];
        };
        this.config.axis_y_tick_format = function (d) {
            return extra.names[d];
        };

        if (!config.color || !config.color.pattern) {
            this.config.color_pattern = ['#1f77b4'];
        }

        this.config.point_r = function (d) {
            var names = extra.names, values = extra.values, base_length = extra.base_length,
                x = names[d.x], y = d.id,
                key = extra.getKey(x, y), value = !values[key] ? 0 : values[key],
                max, max_r, max_area, a, area, r;

            if (!base_length) {
                base_length = extra.base_length = d3.min([
                    that.svg.select('.c3-axis.c3-axis-y path').node().getTotalLength(),
                    that.svg.select('.c3-axis.c3-axis-x path').node().getTotalLength(),
                ]);
            }

            max = d3.max(Object.keys(values).map(function (key) { return values[key]; }));
            max_r = (base_length / (names.length * 2));
            max_area = max_r * max_r * Math.PI;

            a = max_area / max;

            area = value * a;
            r = Math.sqrt(area / Math.PI);

            return r;
        };
        this.config.point_sensitivity = 25;
        this.config.point_focus_expand_enabled = false;

        this.config.legend_show = false;

        if (!config.tooltip || !config.tooltip.contents) {
            this.config.tooltip_contents = function (d, defaultTitleFormat, defaultValueFormat, color) {
                var x = extra.names[d[0].x], y = d[0].name, v = extra.values[extra.getKey(x, y)], text;

                text = "<table class='" + this.CLASS.tooltip + "'>";
                text += "<tr><th colspan='2'>" + x + "&nbsp;/&nbsp;" + y + "</th></tr>";
                text += "<tr><td class='value'>" + (!v ? 0 : v) + "</td></tr>";
                text += "</table>";

                return text;
            };
        }

        // construct bubble chart data and setup config based on the values

        var xs = this.config.data_pairs.map(function (pair) { return pair.x; }),
            ys = this.config.data_pairs.map(function (pair) { return pair.y; });

        extra.names = d3.set(xs.concat(ys)).values().sort();

        this.config.axis_y_tick_values = extra.names.map(function (name, i) { return i; });

        var data_xs = {};
        extra.names.forEach(function (name) {
            data_xs[name] = name + '_x';
        });
        var data_columns_xs = Object.keys(data_xs).map(function (key) {
            return [data_xs[key]].concat(extra.names.map(function (name, i) { return i; }));
        });
        var data_columns_values = extra.names.map(function (name, i) {
            return [name].concat(extra.names.map(function (name) { return i; }));
        });
        this.config.data_xs = data_xs;
        this.config.data_columns = data_columns_xs.concat(data_columns_values);

        var values = {};
        this.config.data_pairs.forEach(function (pair) {
            if (!pair.x || !pair.y) {
                throw "x and y are required in data.";
            }
            values[extra.getKey(pair.x, pair.y)] = pair.value;
        });
        extra.values = values;

        this.config.axis_x_min = this.config.axis_y_min = -0.5;
        this.config.axis_x_max = this.config.axis_y_max = extra.names.length - 0.5;
    };
})(window);
