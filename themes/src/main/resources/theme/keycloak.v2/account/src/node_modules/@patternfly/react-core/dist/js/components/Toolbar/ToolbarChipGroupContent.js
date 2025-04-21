"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.ToolbarChipGroupContent = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const toolbar_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Toolbar/toolbar"));
const react_styles_1 = require("@patternfly/react-styles");
const util_1 = require("../../helpers/util");
const ToolbarItem_1 = require("./ToolbarItem");
const Button_1 = require("../Button");
const ToolbarGroup_1 = require("./ToolbarGroup");
const ToolbarUtils_1 = require("./ToolbarUtils");
class ToolbarChipGroupContent extends React.Component {
    render() {
        const _a = this.props, { className, isExpanded, chipGroupContentRef, clearAllFilters, showClearFiltersButton, clearFiltersButtonText, collapseListedFiltersBreakpoint, numberOfFilters, numberOfFiltersText, customChipGroupContent } = _a, props = tslib_1.__rest(_a, ["className", "isExpanded", "chipGroupContentRef", "clearAllFilters", "showClearFiltersButton", "clearFiltersButtonText", "collapseListedFiltersBreakpoint", "numberOfFilters", "numberOfFiltersText", "customChipGroupContent"]);
        const clearChipGroups = () => {
            clearAllFilters();
        };
        let collapseListedFilters = false;
        if (collapseListedFiltersBreakpoint === 'all') {
            collapseListedFilters = true;
        }
        else if (util_1.canUseDOM) {
            collapseListedFilters =
                (util_1.canUseDOM ? window.innerWidth : 1200) < ToolbarUtils_1.globalBreakpoints[collapseListedFiltersBreakpoint];
        }
        return (React.createElement("div", Object.assign({ className: react_styles_1.css(toolbar_1.default.toolbarContent, (numberOfFilters === 0 || isExpanded) && toolbar_1.default.modifiers.hidden, className) }, ((numberOfFilters === 0 || isExpanded) && { hidden: true }), { ref: chipGroupContentRef }, props),
            React.createElement(ToolbarGroup_1.ToolbarGroup, Object.assign({ className: react_styles_1.css(collapseListedFilters && toolbar_1.default.modifiers.hidden) }, (collapseListedFilters && { hidden: true }), (collapseListedFilters && { 'aria-hidden': true }))),
            collapseListedFilters && numberOfFilters > 0 && !isExpanded && (React.createElement(ToolbarGroup_1.ToolbarGroup, null,
                React.createElement(ToolbarItem_1.ToolbarItem, null, numberOfFiltersText(numberOfFilters)))),
            showClearFiltersButton && !isExpanded && !customChipGroupContent && (React.createElement(ToolbarItem_1.ToolbarItem, null,
                React.createElement(Button_1.Button, { variant: "link", onClick: clearChipGroups, isInline: true }, clearFiltersButtonText))),
            customChipGroupContent && customChipGroupContent));
    }
}
exports.ToolbarChipGroupContent = ToolbarChipGroupContent;
ToolbarChipGroupContent.displayName = 'ToolbarChipGroupContent';
ToolbarChipGroupContent.defaultProps = {
    clearFiltersButtonText: 'Clear all filters',
    collapseListedFiltersBreakpoint: 'lg',
    numberOfFiltersText: (numberOfFilters) => `${numberOfFilters} filters applied`
};
//# sourceMappingURL=ToolbarChipGroupContent.js.map