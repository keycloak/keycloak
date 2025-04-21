"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.PageHeaderTools = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const page_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Page/page"));
const react_styles_1 = require("@patternfly/react-styles");
const PageHeaderTools = (_a) => {
    var { children, className } = _a, props = tslib_1.__rest(_a, ["children", "className"]);
    return (React.createElement("div", Object.assign({ className: react_styles_1.css(page_1.default.pageHeaderTools, className) }, props), children));
};
exports.PageHeaderTools = PageHeaderTools;
exports.PageHeaderTools.displayName = 'PageHeaderTools';
//# sourceMappingURL=PageHeaderTools.js.map