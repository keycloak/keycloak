"use strict";

(function (window, document, bs, undefined) {

    var socket = bs.socket;

    var uiOptions = {
        bs: {}
    };

    socket.on("ui:connection", function (options) {

        uiOptions = options;

        bs.socket.emit("ui:history:connected", {
            href: window.location.href
        });
    });

    socket.on("ui:element:remove", function (data) {
        if (data.id) {
            var elem = document.getElementById(data.id);
            if (elem) {
                removeElement(elem);
            }
        }
    });

    socket.on("highlight", function () {
        var id = "__browser-sync-highlight__";
        var elem = document.getElementById(id);
        if (elem) {
            return removeElement(elem);
        }
        (function (e) {
            e.style.position = "fixed";
            e.style.zIndex = "1000";
            e.style.width = "100%";
            e.style.height = "100%";
            e.style.borderWidth = "5px";
            e.style.borderColor = "red";
            e.style.borderStyle = "solid";
            e.style.top = "0";
            e.style.left = "0";
            e.setAttribute("id", id);
            document.getElementsByTagName("body")[0].appendChild(e);
        })(document.createElement("div"));
    });

    socket.on("ui:element:add", function (data) {

        var elem = document.getElementById(data.id);

        if (!elem) {
            if (data.type === "css") {
                return addCss(data);
            }
            if (data.type === "js") {
                return addJs(data);
            }
            if (data.type === "dom") {
                return addDomNode(data);
            }
        }
    });

    bs.addDomNode = addDomNode;
    bs.addJs      = addJs;
    bs.addCss     = addJs;

    function addJs(data) {
        (function (e) {
            e.setAttribute("src", getAbsoluteUrl(data.src));
            e.setAttribute("id", data.id);
            document.getElementsByTagName("body")[0].appendChild(e);
        })(document.createElement("script"));
    }

    function addCss(data) {
        (function (e) {
            e.setAttribute("rel",  "stylesheet");
            e.setAttribute("type", "text/css");
            e.setAttribute("id",   data.id);
            e.setAttribute("media", "all");
            e.setAttribute("href", getAbsoluteUrl(data.src));
            document.getElementsByTagName("head")[0].appendChild(e);
        })(document.createElement("link"));
    }

    function addDomNode(data) {
        var elem = document.createElement(data.tagName);
        for (var attr in data.attrs) {
            elem.setAttribute(attr,  data.attrs[attr]);
        }
        if (data.placement) {
            document.getElementsByTagName(data.placement)[0].appendChild(elem);
        } else {
            document.getElementsByTagName("body")[0].appendChild(elem);
        }
        return elem;
    }

    function removeElement(element) {
        if (element && element.parentNode) {
            element.parentNode.removeChild(element);
        }
    }

    function getAbsoluteUrl(path) {
        if (path.match(/^h/)) {
            return path;
        }
        return [window.location.protocol, "//", getHost(), path].join("");
    }

    function getHost () {
        return uiOptions.bs.mode === "snippet" ? window.location.hostname + ":" + uiOptions.bs.port : window.location.host;
    }

})(window, document, ___browserSync___);