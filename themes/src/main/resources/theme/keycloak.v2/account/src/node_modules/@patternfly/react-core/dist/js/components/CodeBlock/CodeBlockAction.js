"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.CodeBlockAction = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const react_styles_1 = require("@patternfly/react-styles");
const CodeBlockAction = (_a) => {
    var { children = null, className } = _a, props = tslib_1.__rest(_a, ["children", "className"]);
    return (React.createElement("div", Object.assign({ className: react_styles_1.css('pf-c-code-block__actions-item', className) }, props), children));
};
exports.CodeBlockAction = CodeBlockAction;
exports.CodeBlockAction.displayName = 'CodeBlockAction';
//# sourceMappingURL=CodeBlockAction.js.map