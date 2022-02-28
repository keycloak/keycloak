import * as React from 'react';
import { RefObject } from 'react';

import globalBreakpointMd from '@patternfly/react-tokens/dist/js/global_breakpoint_md';
import globalBreakpointLg from '@patternfly/react-tokens/dist/js/global_breakpoint_lg';
import globalBreakpointXl from '@patternfly/react-tokens/dist/js/global_breakpoint_xl';
import globalBreakpoint2xl from '@patternfly/react-tokens/dist/js/global_breakpoint_2xl';

interface DataToolbarContextProps {
  isExpanded: boolean;
  toggleIsExpanded: () => void;
  chipGroupContentRef: RefObject<HTMLDivElement>;
  updateNumberFilters: (categoryName: string, numberOfFilters: number) => void;
  numberOfFilters: number;
}

export const DataToolbarContext = React.createContext<Partial<DataToolbarContextProps>>({
  isExpanded: false,
  toggleIsExpanded: () => {},
  chipGroupContentRef: null,
  updateNumberFilters: () => {},
  numberOfFilters: 0
});

interface DataToolbarContentContextProps {
  expandableContentRef: RefObject<HTMLDivElement>;
  expandableContentId: string;
  chipContainerRef: RefObject<any>;
}

export const DataToolbarContentContext = React.createContext<Partial<DataToolbarContentContextProps>>({
  expandableContentRef: null,
  expandableContentId: '',
  chipContainerRef: null
});

export interface DataToolbarBreakpointMod {
  /** The attribute to modify  */
  modifier:
    | 'hidden'
    | 'visible'
    | 'align-right'
    | 'align-left'
    | 'spacer-none'
    | 'spacer-sm'
    | 'spacer-md'
    | 'spacer-lg'
    | 'space-items-none'
    | 'space-items-sm'
    | 'space-items-md'
    | 'space-items-lg';
  /** The breakpoint at which to apply the modifier */
  breakpoint: 'md' | 'lg' | 'xl' | '2xl';
}

export const globalBreakpoints = (breakpoint: string) => {
  const breakpoints: { [key: string]: number } = {
    md: parseInt(globalBreakpointMd.value),
    lg: parseInt(globalBreakpointLg.value),
    xl: parseInt(globalBreakpointXl.value),
    '2xl': parseInt(globalBreakpoint2xl.value)
  };
  return breakpoints[breakpoint];
};
