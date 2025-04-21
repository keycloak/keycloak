import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/OverflowMenu/overflow-menu';
import { css } from '@patternfly/react-styles';
import { OverflowMenuContext } from './OverflowMenuContext';
import mdBreakpoint from '@patternfly/react-tokens/dist/esm/global_breakpoint_md';
import lgBreakpoint from '@patternfly/react-tokens/dist/esm/global_breakpoint_lg';
import xlBreakpoint from '@patternfly/react-tokens/dist/esm/global_breakpoint_xl';
import xl2Breakpoint from '@patternfly/react-tokens/dist/esm/global_breakpoint_2xl';
import { debounce, canUseDOM } from '../../helpers/util';
const breakpoints = {
    md: mdBreakpoint,
    lg: lgBreakpoint,
    xl: xlBreakpoint,
    '2xl': xl2Breakpoint
};
export class OverflowMenu extends React.Component {
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
        if (canUseDOM) {
            window.addEventListener('resize', debounce(this.handleResize, 250));
        }
    }
    componentWillUnmount() {
        if (canUseDOM) {
            window.removeEventListener('resize', debounce(this.handleResize, 250));
        }
    }
    render() {
        // eslint-disable-next-line @typescript-eslint/no-unused-vars
        const _a = this.props, { className, breakpoint, children } = _a, props = __rest(_a, ["className", "breakpoint", "children"]);
        return (React.createElement("div", Object.assign({}, props, { className: css(styles.overflowMenu, className) }),
            React.createElement(OverflowMenuContext.Provider, { value: { isBelowBreakpoint: this.state.isBelowBreakpoint } }, children)));
    }
}
OverflowMenu.displayName = 'OverflowMenu';
OverflowMenu.contextType = OverflowMenuContext;
//# sourceMappingURL=OverflowMenu.js.map