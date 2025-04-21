"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.JumpLinksList = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const react_styles_1 = require("@patternfly/react-styles");
const jump_links_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/JumpLinks/jump-links"));
const JumpLinksList = (_a) => {
    var { children, className } = _a, props = tslib_1.__rest(_a, ["children", "className"]);
    return (React.createElement("ul", Object.assign({ className: react_styles_1.css(jump_links_1.default.jumpLinksList, className) }, props), children));
};
exports.JumpLinksList = JumpLinksList;
exports.JumpLinksList.displayName = 'JumpLinksList';
//# sourceMappingURL=JumpLinksList.js.map