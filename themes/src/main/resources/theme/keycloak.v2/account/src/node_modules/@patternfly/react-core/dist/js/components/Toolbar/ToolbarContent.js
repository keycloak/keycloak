"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.ToolbarContent = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const toolbar_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Toolbar/toolbar"));
const react_styles_1 = require("@patternfly/react-styles");
const ToolbarUtils_1 = require("./ToolbarUtils");
const util_1 = require("../../helpers/util");
const ToolbarExpandableContent_1 = require("./ToolbarExpandableContent");
const Page_1 = require("../Page/Page");
class ToolbarContent extends React.Component {
    constructor() {
        super(...arguments);
        this.expandableContentRef = React.createRef();
        this.chipContainerRef = React.createRef();
    }
    render() {
        const _a = this.props, { className, children, isExpanded, toolbarId, visibility, visiblity, alignment, clearAllFilters, showClearFiltersButton, clearFiltersButtonText } = _a, props = tslib_1.__rest(_a, ["className", "children", "isExpanded", "toolbarId", "visibility", "visiblity", "alignment", "clearAllFilters", "showClearFiltersButton", "clearFiltersButtonText"]);
        if (visiblity !== undefined) {
            // eslint-disable-next-line no-console
            console.warn('The ToolbarContent visiblity prop has been deprecated. ' +
                'Please use the correctly spelled visibility prop instead.');
        }
        return (React.createElement(Page_1.PageContext.Consumer, null, ({ width, getBreakpoint }) => (React.createElement("div", Object.assign({ className: react_styles_1.css(toolbar_1.default.toolbarContent, util_1.formatBreakpointMods(visibility || visiblity, toolbar_1.default, '', getBreakpoint(width)), util_1.formatBreakpointMods(alignment, toolbar_1.default, '', getBreakpoint(width)), className) }, props),
            React.createElement(ToolbarUtils_1.ToolbarContext.Consumer, null, ({ clearAllFilters: clearAllFiltersContext, clearFiltersButtonText: clearFiltersButtonContext, showClearFiltersButton: showClearFiltersButtonContext, toolbarId: toolbarIdContext }) => {
                const expandableContentId = `${toolbarId ||
                    toolbarIdContext}-expandable-content-${ToolbarContent.currentId++}`;
                return (React.createElement(ToolbarUtils_1.ToolbarContentContext.Provider, { value: {
                        expandableContentRef: this.expandableContentRef,
                        expandableContentId,
                        chipContainerRef: this.chipContainerRef
                    } },
                    React.createElement("div", { className: react_styles_1.css(toolbar_1.default.toolbarContentSection) }, children),
                    React.createElement(ToolbarExpandableContent_1.ToolbarExpandableContent, { id: expandableContentId, isExpanded: isExpanded, expandableContentRef: this.expandableContentRef, chipContainerRef: this.chipContainerRef, clearAllFilters: clearAllFilters || clearAllFiltersContext, showClearFiltersButton: showClearFiltersButton || showClearFiltersButtonContext, clearFiltersButtonText: clearFiltersButtonText || clearFiltersButtonContext })));
            })))));
    }
}
exports.ToolbarContent = ToolbarContent;
ToolbarContent.displayName = 'ToolbarContent';
ToolbarContent.currentId = 0;
ToolbarContent.defaultProps = {
    isExpanded: false,
    showClearFiltersButton: false
};
//# sourceMappingURL=ToolbarContent.js.map