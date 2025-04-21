import * as React from 'react';
import { RefObject } from 'react';
import globalBreakpointMd from '@patternfly/react-tokens/dist/esm/global_breakpoint_md';
import globalBreakpointLg from '@patternfly/react-tokens/dist/esm/global_breakpoint_lg';
import globalBreakpointXl from '@patternfly/react-tokens/dist/esm/global_breakpoint_xl';
import globalBreakpoint2xl from '@patternfly/react-tokens/dist/esm/global_breakpoint_2xl';

export interface ToolbarContextProps {
  isExpanded: boolean;
  toggleIsExpanded: () => void;
  chipGroupContentRef: RefObject<HTMLDivElement>;
  updateNumberFilters: (categoryName: string, numberOfFilters: number) => void;
  numberOfFilters: number;
  clearAllFilters?: () => void;
  clearFiltersButtonText?: string;
  showClearFiltersButton?: boolean;
  toolbarId?: string;
  customChipGroupContent?: React.ReactNode;
}

export const ToolbarContext = React.createContext<ToolbarContextProps>({
  isExpanded: false,
  toggleIsExpanded: () => {},
  chipGroupContentRef: null,
  updateNumberFilters: () => {},
  numberOfFilters: 0,
  clearAllFilters: () => {}
});

interface ToolbarContentContextProps {
  expandableContentRef: RefObject<HTMLDivElement>;
  expandableContentId: string;
  chipContainerRef: RefObject<any>;
}

export const ToolbarContentContext = React.createContext<ToolbarContentContextProps>({
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
