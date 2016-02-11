(function (window, bs, undefined) {

    var styleElem = bs.addDomNode({
        placement: "head",
        attrs: {
            "type": "text/css",
            id: "__bs_overlay-grid-styles__"
        },
        tagName: "style"
    });

    bs.socket.on("ui:remote-debug:css-overlay-grid", function (data) {
        styleElem.innerHTML = data.innerHTML;
    });

    bs.socket.emit("ui:remote-debug:css-overlay-grid:ready");

}(window, window.___browserSync___));