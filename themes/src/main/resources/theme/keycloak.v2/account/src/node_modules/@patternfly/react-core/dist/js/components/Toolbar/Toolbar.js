"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.Toolbar = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const toolbar_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Toolbar/toolbar"));
const GenerateId_1 = require("../../helpers/GenerateId/GenerateId");
const react_styles_1 = require("@patternfly/react-styles");
const ToolbarUtils_1 = require("./ToolbarUtils");
const ToolbarChipGroupContent_1 = require("./ToolbarChipGroupContent");
const util_1 = require("../../helpers/util");
const helpers_1 = require("../../helpers");
const Page_1 = require("../Page/Page");
class Toolbar extends React.Component {
    constructor() {
        super(...arguments);
        this.chipGroupContentRef = React.createRef();
        this.staticFilterInfo = {};
        this.state = {
            isManagedToggleExpanded: false,
            filterInfo: {},
            windowWidth: util_1.canUseDOM ? window.innerWidth : 1200,
            ouiaStateId: helpers_1.getDefaultOUIAId(Toolbar.displayName)
        };
        this.isToggleManaged = () => !(this.props.isExpanded || !!this.props.toggleIsExpanded);
        this.toggleIsExpanded = () => {
            this.setState(prevState => ({
                isManagedToggleExpanded: !prevState.isManagedToggleExpanded
            }));
        };
        this.closeExpandableContent = (e) => {
            if (e.target.innerWidth !== this.state.windowWidth) {
                this.setState(() => ({
                    isManagedToggleExpanded: false,
                    windowWidth: e.target.innerWidth
                }));
            }
        };
        this.updateNumberFilters = (categoryName, numberOfFilters) => {
            const filterInfoToUpdate = Object.assign({}, this.staticFilterInfo);
            if (!filterInfoToUpdate.hasOwnProperty(categoryName) || filterInfoToUpdate[categoryName] !== numberOfFilters) {
                filterInfoToUpdate[categoryName] = numberOfFilters;
                this.staticFilterInfo = filterInfoToUpdate;
                this.setState({ filterInfo: filterInfoToUpdate });
            }
        };
        this.getNumberOfFilters = () => Object.values(this.state.filterInfo).reduce((acc, cur) => acc + cur, 0);
        this.renderToolbar = (randomId) => {
            const _a = this.props, { clearAllFilters, clearFiltersButtonText, collapseListedFiltersBreakpoint, isExpanded: isExpandedProp, toggleIsExpanded, className, children, isFullHeight, isStatic, inset, usePageInsets, isSticky, ouiaId, numberOfFiltersText, customChipGroupContent } = _a, props = tslib_1.__rest(_a, ["clearAllFilters", "clearFiltersButtonText", "collapseListedFiltersBreakpoint", "isExpanded", "toggleIsExpanded", "className", "children", "isFullHeight", "isStatic", "inset", "usePageInsets", "isSticky", "ouiaId", "numberOfFiltersText", "customChipGroupContent"]);
            const { isManagedToggleExpanded } = this.state;
            const isToggleManaged = this.isToggleManaged();
            const isExpanded = isToggleManaged ? isManagedToggleExpanded : isExpandedProp;
            const numberOfFilters = this.getNumberOfFilters();
            const showClearFiltersButton = numberOfFilters > 0;
            return (React.createElement(Page_1.PageContext.Consumer, null, ({ width, getBreakpoint }) => (React.createElement("div", Object.assign({ className: react_styles_1.css(toolbar_1.default.toolbar, isFullHeight && toolbar_1.default.modifiers.fullHeight, isStatic && toolbar_1.default.modifiers.static, usePageInsets && toolbar_1.default.modifiers.pageInsets, isSticky && toolbar_1.default.modifiers.sticky, util_1.formatBreakpointMods(inset, toolbar_1.default, '', getBreakpoint(width)), className), id: randomId }, helpers_1.getOUIAProps(Toolbar.displayName, ouiaId !== undefined ? ouiaId : this.state.ouiaStateId), props),
                React.createElement(ToolbarUtils_1.ToolbarContext.Provider, { value: {
                        isExpanded,
                        toggleIsExpanded: isToggleManaged ? this.toggleIsExpanded : toggleIsExpanded,
                        chipGroupContentRef: this.chipGroupContentRef,
                        updateNumberFilters: this.updateNumberFilters,
                        numberOfFilters,
                        clearAllFilters,
                        clearFiltersButtonText,
                        showClearFiltersButton,
                        toolbarId: randomId,
                        customChipGroupContent
                    } },
                    children,
                    React.createElement(ToolbarChipGroupContent_1.ToolbarChipGroupContent, { isExpanded: isExpanded, chipGroupContentRef: this.chipGroupContentRef, clearAllFilters: clearAllFilters, showClearFiltersButton: showClearFiltersButton, clearFiltersButtonText: clearFiltersButtonText, numberOfFilters: numberOfFilters, numberOfFiltersText: numberOfFiltersText, collapseListedFiltersBreakpoint: collapseListedFiltersBreakpoint, customChipGroupContent: customChipGroupContent }))))));
        };
    }
    componentDidMount() {
        if (this.isToggleManaged() && util_1.canUseDOM) {
            window.addEventListener('resize', this.closeExpandableContent);
        }
    }
    componentWillUnmount() {
        if (this.isToggleManaged() && util_1.canUseDOM) {
            window.removeEventListener('resize', this.closeExpandableContent);
        }
    }
    render() {
        return this.props.id ? (this.renderToolbar(this.props.id)) : (React.createElement(GenerateId_1.GenerateId, null, randomId => this.renderToolbar(randomId)));
    }
}
exports.Toolbar = Toolbar;
Toolbar.displayName = 'Toolbar';
//# sourceMappingURL=Toolbar.js.map