"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.ToolbarExpandableContent = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const toolbar_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Toolbar/toolbar"));
const react_styles_1 = require("@patternfly/react-styles");
const ToolbarGroup_1 = require("./ToolbarGroup");
const ToolbarItem_1 = require("./ToolbarItem");
const Button_1 = require("../Button");
const ToolbarUtils_1 = require("./ToolbarUtils");
class ToolbarExpandableContent extends React.Component {
    render() {
        const _a = this.props, { className, expandableContentRef, chipContainerRef, 
        // eslint-disable-next-line @typescript-eslint/no-unused-vars
        isExpanded, clearAllFilters, clearFiltersButtonText, showClearFiltersButton } = _a, props = tslib_1.__rest(_a, ["className", "expandableContentRef", "chipContainerRef", "isExpanded", "clearAllFilters", "clearFiltersButtonText", "showClearFiltersButton"]);
        const { numberOfFilters, customChipGroupContent } = this.context;
        const clearChipGroups = () => {
            clearAllFilters();
        };
        return (React.createElement("div", Object.assign({ className: react_styles_1.css(toolbar_1.default.toolbarExpandableContent, className), ref: expandableContentRef }, props),
            React.createElement(ToolbarGroup_1.ToolbarGroup, null),
            numberOfFilters > 0 && (React.createElement(ToolbarGroup_1.ToolbarGroup, { className: toolbar_1.default.modifiers.chipContainer },
                React.createElement(ToolbarGroup_1.ToolbarGroup, { ref: chipContainerRef }),
                showClearFiltersButton && !customChipGroupContent && (React.createElement(ToolbarItem_1.ToolbarItem, null,
                    React.createElement(Button_1.Button, { variant: "link", onClick: clearChipGroups, isInline: true }, clearFiltersButtonText))),
                customChipGroupContent && customChipGroupContent))));
    }
}
exports.ToolbarExpandableContent = ToolbarExpandableContent;
ToolbarExpandableContent.displayName = 'ToolbarExpandableContent';
ToolbarExpandableContent.contextType = ToolbarUtils_1.ToolbarContext;
ToolbarExpandableContent.defaultProps = {
    isExpanded: false,
    clearFiltersButtonText: 'Clear all filters'
};
//# sourceMappingURL=ToolbarExpandableContent.js.map