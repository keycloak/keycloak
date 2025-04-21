"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.OverflowMenu = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const overflow_menu_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/OverflowMenu/overflow-menu"));
const react_styles_1 = require("@patternfly/react-styles");
const OverflowMenuContext_1 = require("./OverflowMenuContext");
const global_breakpoint_md_1 = tslib_1.__importDefault(require('@patternfly/react-tokens/dist/js/global_breakpoint_md'));
const global_breakpoint_lg_1 = tslib_1.__importDefault(require('@patternfly/react-tokens/dist/js/global_breakpoint_lg'));
const global_breakpoint_xl_1 = tslib_1.__importDefault(require('@patternfly/react-tokens/dist/js/global_breakpoint_xl'));
const global_breakpoint_2xl_1 = tslib_1.__importDefault(require('@patternfly/react-tokens/dist/js/global_breakpoint_2xl'));
const util_1 = require("../../helpers/util");
const breakpoints = {
    md: global_breakpoint_md_1.default,
    lg: global_breakpoint_lg_1.default,
    xl: global_breakpoint_xl_1.default,
    '2xl': global_breakpoint_2xl_1.default
};
class OverflowMenu extends React.Component {
    constructor(props) {
        super(props);
        this.handleResize = () => {
            const breakpointPx = breakpoints[this.props.breakpoint];
            if (!breakpointPx) {
                // eslint-disable-next-line no-console
                console.error('OverflowMenu will not be visible without a valid breakpoint.');
                return;
            }
            const breakpointWidth = Number(breakpointPx.value.replace('px', ''));
            const isBelowBreakpoint = window.innerWidth < breakpointWidth;
            this.setState({ isBelowBreakpoint });
        };
        this.state = {
            isBelowBreakpoint: false
        };
    }
    componentDidMount() {
        this.handleResize();
        if (util_1.canUseDOM) {
            window.addEventListener('resize', util_1.debounce(this.handleResize, 250));
        }
    }
    componentWillUnmount() {
        if (util_1.canUseDOM) {
            window.removeEventListener('resize', util_1.debounce(this.handleResize, 250));
        }
    }
    render() {
        // eslint-disable-next-line @typescript-eslint/no-unused-vars
        const _a = this.props, { className, breakpoint, children } = _a, props = tslib_1.__rest(_a, ["className", "breakpoint", "children"]);
        return (React.createElement("div", Object.assign({}, props, { className: react_styles_1.css(overflow_menu_1.default.overflowMenu, className) }),
            React.createElement(OverflowMenuContext_1.OverflowMenuContext.Provider, { value: { isBelowBreakpoint: this.state.isBelowBreakpoint } }, children)));
    }
}
exports.OverflowMenu = OverflowMenu;
OverflowMenu.displayName = 'OverflowMenu';
OverflowMenu.contextType = OverflowMenuContext_1.OverflowMenuContext;
//# sourceMappingURL=OverflowMenu.js.map