"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.Backdrop = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const react_styles_1 = require("@patternfly/react-styles");
const backdrop_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Backdrop/backdrop"));
const Backdrop = (_a) => {
    var { children = null, className = '' } = _a, props = tslib_1.__rest(_a, ["children", "className"]);
    return (React.createElement("div", Object.assign({}, props, { className: react_styles_1.css(backdrop_1.default.backdrop, className) }), children));
};
exports.Backdrop = Backdrop;
exports.Backdrop.displayName = 'Backdrop';
//# sourceMappingURL=Backdrop.js.map