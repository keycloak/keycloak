"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.MastheadMain = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const masthead_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Masthead/masthead"));
const react_styles_1 = require("@patternfly/react-styles");
const MastheadMain = (_a) => {
    var { children, className } = _a, props = tslib_1.__rest(_a, ["children", "className"]);
    return (React.createElement("div", Object.assign({ className: react_styles_1.css(masthead_1.default.mastheadMain, className) }, props), children));
};
exports.MastheadMain = MastheadMain;
exports.MastheadMain.displayName = 'MastheadMain';
//# sourceMappingURL=MastheadMain.js.map