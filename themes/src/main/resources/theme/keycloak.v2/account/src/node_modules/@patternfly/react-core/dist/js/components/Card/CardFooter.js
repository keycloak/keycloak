"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.CardFooter = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const card_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Card/card"));
const react_styles_1 = require("@patternfly/react-styles");
const CardFooter = (_a) => {
    var { children = null, className = '', component = 'div' } = _a, props = tslib_1.__rest(_a, ["children", "className", "component"]);
    const Component = component;
    return (React.createElement(Component, Object.assign({ className: react_styles_1.css(card_1.default.cardFooter, className) }, props), children));
};
exports.CardFooter = CardFooter;
exports.CardFooter.displayName = 'CardFooter';
//# sourceMappingURL=CardFooter.js.map