"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.CardHeadMain = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const react_styles_1 = require("@patternfly/react-styles");
const CardHeadMain = (_a) => {
    var { children = null, className = '' } = _a, props = tslib_1.__rest(_a, ["children", "className"]);
    return (React.createElement("div", Object.assign({ className: react_styles_1.css('pf-c-card__head-main', className) }, props), children));
};
exports.CardHeadMain = CardHeadMain;
exports.CardHeadMain.displayName = 'CardHeadMain';
//# sourceMappingURL=CardHeadMain.js.map