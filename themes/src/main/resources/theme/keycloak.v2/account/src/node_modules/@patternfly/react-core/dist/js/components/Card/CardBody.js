"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.CardBody = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const card_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Card/card"));
const react_styles_1 = require("@patternfly/react-styles");
const CardBody = (_a) => {
    var { children = null, className = '', component = 'div', isFilled = true } = _a, props = tslib_1.__rest(_a, ["children", "className", "component", "isFilled"]);
    const Component = component;
    return (React.createElement(Component, Object.assign({ className: react_styles_1.css(card_1.default.cardBody, !isFilled && card_1.default.modifiers.noFill, className) }, props), children));
};
exports.CardBody = CardBody;
exports.CardBody.displayName = 'CardBody';
//# sourceMappingURL=CardBody.js.map