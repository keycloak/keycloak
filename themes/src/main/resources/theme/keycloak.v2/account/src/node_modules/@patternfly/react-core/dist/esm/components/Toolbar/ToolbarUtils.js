import * as React from 'react';
import globalBreakpointMd from '@patternfly/react-tokens/dist/esm/global_breakpoint_md';
import globalBreakpointLg from '@patternfly/react-tokens/dist/esm/global_breakpoint_lg';
import globalBreakpointXl from '@patternfly/react-tokens/dist/esm/global_breakpoint_xl';
import globalBreakpoint2xl from '@patternfly/react-tokens/dist/esm/global_breakpoint_2xl';
export const ToolbarContext = React.createContext({
    isExpanded: false,
    toggleIsExpanded: () => { },
    chipGroupContentRef: null,
    updateNumberFilters: () => { },
    numberOfFilters: 0,
    clearAllFilters: () => { }
});
export const ToolbarContentContext = React.createContext({
    expandableContentRef: null,
    expandableContentId: '',
    chipContainerRef: null
});
export const globalBreakpoints = {
    md: parseInt(globalBreakpointMd.value),
    lg: parseInt(globalBreakpointLg.value),
    xl: parseInt(globalBreakpointXl.value),
    '2xl': parseInt(globalBreakpoint2xl.value)
};
//# sourceMappingURL=ToolbarUtils.js.map