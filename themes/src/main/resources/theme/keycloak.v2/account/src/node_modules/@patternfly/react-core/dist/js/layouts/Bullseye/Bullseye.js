"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.Bullseye = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const react_styles_1 = require("@patternfly/react-styles");
const bullseye_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/layouts/Bullseye/bullseye"));
const Bullseye = (_a) => {
    var { children = null, className = '', component = 'div' } = _a, props = tslib_1.__rest(_a, ["children", "className", "component"]);
    const Component = component;
    return (React.createElement(Component, Object.assign({ className: react_styles_1.css(bullseye_1.default.bullseye, className) }, props), children));
};
exports.Bullseye = Bullseye;
exports.Bullseye.displayName = 'Bullseye';
//# sourceMappingURL=Bullseye.js.map