"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.TabContentBody = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const react_styles_1 = require("@patternfly/react-styles");
const tab_content_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/TabContent/tab-content"));
const TabContentBody = (_a) => {
    var { children, className, hasPadding } = _a, props = tslib_1.__rest(_a, ["children", "className", "hasPadding"]);
    return (React.createElement("div", Object.assign({ className: react_styles_1.css(tab_content_1.default.tabContentBody, hasPadding && tab_content_1.default.modifiers.padding, className) }, props), children));
};
exports.TabContentBody = TabContentBody;
exports.TabContentBody.displayName = 'TabContentBody';
//# sourceMappingURL=TabContentBody.js.map