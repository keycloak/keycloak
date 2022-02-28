import * as React from 'react';
import globalBreakpointMd from '@patternfly/react-tokens/dist/js/global_breakpoint_md';
import globalBreakpointLg from '@patternfly/react-tokens/dist/js/global_breakpoint_lg';
import globalBreakpointXl from '@patternfly/react-tokens/dist/js/global_breakpoint_xl';
import globalBreakpoint2xl from '@patternfly/react-tokens/dist/js/global_breakpoint_2xl';
export const DataToolbarContext = React.createContext({
  isExpanded: false,
  toggleIsExpanded: () => {},
  chipGroupContentRef: null,
  updateNumberFilters: () => {},
  numberOfFilters: 0
});
export const DataToolbarContentContext = React.createContext({
  expandableContentRef: null,
  expandableContentId: '',
  chipContainerRef: null
});
export const globalBreakpoints = breakpoint => {
  const breakpoints = {
    md: parseInt(globalBreakpointMd.value),
    lg: parseInt(globalBreakpointLg.value),
    xl: parseInt(globalBreakpointXl.value),
    '2xl': parseInt(globalBreakpoint2xl.value)
  };
  return breakpoints[breakpoint];
};
//# sourceMappingURL=DataToolbarUtils.js.map