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

export interface OverflowMenuProps extends React.HTMLProps<HTMLDivElement> {
  /** Any elements that can be rendered in the menu */
  children?: any;
  /** Additional classes added to the OverflowMenu. */
  className?: string;
  /** Indicates breakpoint at which to switch between horizontal menu and vertical dropdown */
  breakpoint: 'md' | 'lg' | 'xl' | '2xl';
}

export interface OverflowMenuState extends React.HTMLProps<HTMLDivElement> {
  isBelowBreakpoint: boolean;
}

export class OverflowMenu extends React.Component<OverflowMenuProps, OverflowMenuState> {
  static displayName = 'OverflowMenu';
  constructor(props: OverflowMenuProps) {
    super(props);
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

  handleResize = () => {
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

  render() {
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    const { className, breakpoint, children, ...props } = this.props;

    return (
      <div {...props} className={css(styles.overflowMenu, className)}>
        <OverflowMenuContext.Provider value={{ isBelowBreakpoint: this.state.isBelowBreakpoint }}>
          {children}
        </OverflowMenuContext.Provider>
      </div>
    );
  }
}

OverflowMenu.contextType = OverflowMenuContext;
