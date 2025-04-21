"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.TextContent = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const content_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Content/content"));
const react_styles_1 = require("@patternfly/react-styles");
const TextContent = (_a) => {
    var { children = null, className = '', isVisited = false } = _a, props = tslib_1.__rest(_a, ["children", "className", "isVisited"]);
    return (React.createElement("div", Object.assign({}, props, { className: react_styles_1.css(content_1.default.content, isVisited && content_1.default.modifiers.visited, className) }), children));
};
exports.TextContent = TextContent;
exports.TextContent.displayName = 'TextContent';
//# sourceMappingURL=TextContent.js.map