"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.createIcon = exports.getSize = exports.IconSize = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
var IconSize;
(function (IconSize) {
    IconSize["sm"] = "sm";
    IconSize["md"] = "md";
    IconSize["lg"] = "lg";
    IconSize["xl"] = "xl";
})(IconSize = exports.IconSize || (exports.IconSize = {}));
const getSize = (size) => {
    switch (size) {
        case IconSize.sm:
            return '1em';
        case IconSize.md:
            return '1.5em';
        case IconSize.lg:
            return '2em';
        case IconSize.xl:
            return '3em';
        default:
            return '1em';
    }
};
exports.getSize = getSize;
let currentId = 0;
/**
 * Factory to create Icon class components for consumers
 */
function createIcon({ name, xOffset = 0, yOffset = 0, width, height, svgPath }) {
    var _a;
    return _a = class SVGIcon extends React.Component {
            constructor() {
                super(...arguments);
                this.id = `icon-title-${currentId++}`;
            }
            render() {
                const _a = this.props, { size, color, title, noVerticalAlign } = _a, props = tslib_1.__rest(_a, ["size", "color", "title", "noVerticalAlign"]);
                const hasTitle = Boolean(title);
                const heightWidth = exports.getSize(size);
                const baseAlign = -0.125 * Number.parseFloat(heightWidth);
                const style = noVerticalAlign ? null : { verticalAlign: `${baseAlign}em` };
                const viewBox = [xOffset, yOffset, width, height].join(' ');
                return (React.createElement("svg", Object.assign({ style: style, fill: color, height: heightWidth, width: heightWidth, viewBox: viewBox, "aria-labelledby": hasTitle ? this.id : null, "aria-hidden": hasTitle ? null : true, role: "img" }, props),
                    hasTitle && React.createElement("title", { id: this.id }, title),
                    React.createElement("path", { d: svgPath })));
            }
        },
        _a.displayName = name,
        _a.defaultProps = {
            color: 'currentColor',
            size: IconSize.sm,
            noVerticalAlign: false
        },
        _a;
}
exports.createIcon = createIcon;
//# sourceMappingURL=createIcon.js.map