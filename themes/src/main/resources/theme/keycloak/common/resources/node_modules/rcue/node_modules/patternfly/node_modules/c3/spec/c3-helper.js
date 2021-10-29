import c3 from '../src';

window.c3 = c3;

window.initDom = function () {
    'use strict';

    var div = document.createElement('div');
    div.id = 'chart';
    div.style.width = '640px';
    div.style.height = '480px';
    document.body.appendChild(div);
    document.body.style.margin = '0px';
};

window.setMouseEvent = function(chart, name, x, y, element) {
    'use strict';

    var paddingLeft = chart.internal.main.node().transform.baseVal.getItem(0).matrix.e,
        event = document.createEvent("MouseEvents");
    event.initMouseEvent(name, true, true, window,
                       0, 0, 0, x + paddingLeft, y + 5,
                       false, false, false, false, 0, null);
    chart.internal.d3.event = event;
    if (element) { element.dispatchEvent(event); }
};

window.initChart = function (chart, args, done) {
    'use strict';

    if (typeof chart === 'undefined') {
        window.initDom();
    }
    if (args) {
        chart = window.c3.generate(args);
        window.d3 = chart.internal.d3;
        window.d3.select('.jasmine_html-reporter')
            .style('position', 'absolute')
            .style('width', '640px')
            .style('right', 0);
    }

    window.setTimeout(function () {
        done();
    }, 10);

    return chart;
};
