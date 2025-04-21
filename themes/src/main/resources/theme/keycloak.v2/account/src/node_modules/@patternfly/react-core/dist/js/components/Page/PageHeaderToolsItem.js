"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.PageHeaderToolsItem = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const page_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Page/page"));
const react_styles_1 = require("@patternfly/react-styles");
const util_1 = require("../../helpers/util");
const Page_1 = require("../Page/Page");
const PageHeaderToolsItem = (_a) => {
    var { children, id, className, visibility, isSelected } = _a, props = tslib_1.__rest(_a, ["children", "id", "className", "visibility", "isSelected"]);
    const { width, getBreakpoint } = React.useContext(Page_1.PageContext);
    return (React.createElement("div", Object.assign({ className: react_styles_1.css(page_1.default.pageHeaderToolsItem, isSelected && page_1.default.modifiers.selected, util_1.formatBreakpointMods(visibility, page_1.default, '', getBreakpoint(width)), className), id: id }, props), children));
};
exports.PageHeaderToolsItem = PageHeaderToolsItem;
exports.PageHeaderToolsItem.displayName = 'PageHeaderToolsItem';
//# sourceMappingURL=PageHeaderToolsItem.js.map