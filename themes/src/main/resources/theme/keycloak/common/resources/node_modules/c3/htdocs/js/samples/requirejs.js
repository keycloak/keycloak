require.config({
    baseUrl: '/js',
    paths: {
        d3: "http://d3js.org/d3.v3.min"
    }
});

require(["d3", "c3"], function(d3, c3) {

    window.chart = c3.generate({
        data: {
            columns: [
                ['sample', 30, 200, 100, 400, 150, 250]
            ]
        }
    });

});
