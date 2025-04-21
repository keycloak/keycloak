"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.ToolbarToggleGroup = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const ReactDOM = tslib_1.__importStar(require("react-dom"));
const toolbar_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Toolbar/toolbar"));
const react_styles_1 = require("@patternfly/react-styles");
const ToolbarUtils_1 = require("./ToolbarUtils");
const Button_1 = require("../Button");
const global_breakpoint_lg_1 = tslib_1.__importDefault(require('@patternfly/react-tokens/dist/js/global_breakpoint_lg'));
const util_1 = require("../../helpers/util");
const Page_1 = require("../Page/Page");
class ToolbarToggleGroup extends React.Component {
    constructor() {
        super(...arguments);
        this.isContentPopup = () => {
            const viewportSize = util_1.canUseDOM ? window.innerWidth : 1200;
            const lgBreakpointValue = parseInt(global_breakpoint_lg_1.default.value);
            return viewportSize < lgBreakpointValue;
        };
    }
    render() {
        const _a = this.props, { toggleIcon, variant, visibility, visiblity, breakpoint, alignment, spacer, spaceItems, className, children } = _a, props = tslib_1.__rest(_a, ["toggleIcon", "variant", "visibility", "visiblity", "breakpoint", "alignment", "spacer", "spaceItems", "className", "children"]);
        if (!breakpoint && !toggleIcon) {
            // eslint-disable-next-line no-console
            console.error('ToolbarToggleGroup will not be visible without a breakpoint or toggleIcon.');
        }
        if (visiblity !== undefined) {
            // eslint-disable-next-line no-console
            console.warn('The ToolbarToggleGroup visiblity prop has been deprecated. ' +
                'Please use the correctly spelled visibility prop instead.');
        }
        return (React.createElement(Page_1.PageContext.Consumer, null, ({ width, getBreakpoint }) => (React.createElement(ToolbarUtils_1.ToolbarContext.Consumer, null, ({ isExpanded, toggleIsExpanded }) => (React.createElement(ToolbarUtils_1.ToolbarContentContext.Consumer, null, ({ expandableContentRef, expandableContentId }) => {
            if (expandableContentRef.current && expandableContentRef.current.classList) {
                if (isExpanded) {
                    expandableContentRef.current.classList.add(toolbar_1.default.modifiers.expanded);
                }
                else {
                    expandableContentRef.current.classList.remove(toolbar_1.default.modifiers.expanded);
                }
            }
            const breakpointMod = {};
            breakpointMod[breakpoint] = 'show';
            return (React.createElement("div", Object.assign({ className: react_styles_1.css(toolbar_1.default.toolbarGroup, toolbar_1.default.modifiers.toggleGroup, variant &&
                    toolbar_1.default.modifiers[util_1.toCamel(variant)], util_1.formatBreakpointMods(breakpointMod, toolbar_1.default, '', getBreakpoint(width)), util_1.formatBreakpointMods(visibility || visiblity, toolbar_1.default, '', getBreakpoint(width)), util_1.formatBreakpointMods(alignment, toolbar_1.default, '', getBreakpoint(width)), util_1.formatBreakpointMods(spacer, toolbar_1.default, '', getBreakpoint(width)), util_1.formatBreakpointMods(spaceItems, toolbar_1.default, '', getBreakpoint(width)), className) }, props),
                React.createElement("div", { className: react_styles_1.css(toolbar_1.default.toolbarToggle) },
                    React.createElement(Button_1.Button, Object.assign({ variant: "plain", onClick: toggleIsExpanded, "aria-label": "Show Filters" }, (isExpanded && { 'aria-expanded': true }), { "aria-haspopup": isExpanded && this.isContentPopup(), "aria-controls": expandableContentId }), toggleIcon)),
                isExpanded
                    ? ReactDOM.createPortal(children, expandableContentRef.current.firstElementChild)
                    : children));
        }))))));
    }
}
exports.ToolbarToggleGroup = ToolbarToggleGroup;
ToolbarToggleGroup.displayName = 'ToolbarToggleGroup';
//# sourceMappingURL=ToolbarToggleGroup.js.map