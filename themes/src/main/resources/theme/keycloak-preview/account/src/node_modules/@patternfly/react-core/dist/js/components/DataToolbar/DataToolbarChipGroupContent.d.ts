import * as React from 'react';
import { RefObject } from 'react';
import { PickOptional } from '../../helpers/typeUtils';
export interface DataToolbarChipGroupContentProps extends React.HTMLProps<HTMLDivElement> {
    /** Classes applied to root element of the data toolbar content row */
    className?: string;
    /** Flag indicating if a data toolbar toggle group's expandable content is expanded */
    isExpanded?: boolean;
    /** Chip group content reference for passing to data toolbar children */
    chipGroupContentRef?: RefObject<any>;
    /** optional callback for clearing all filters in the toolbar */
    clearAllFilters?: () => void;
    /** Flag indicating that the clear all filters button should be visible */
    showClearFiltersButton: boolean;
    /** Text to display in the clear all filters button */
    clearFiltersButtonText?: string;
    /** Total number of filters currently being applied across all DataToolbarFilter components */
    numberOfFilters: number;
    /** The breakpoint at which the listed filters in chip groups are collapsed down to a summary */
    collapseListedFiltersBreakpoint?: 'md' | 'lg' | 'xl' | '2xl';
}
export declare class DataToolbarChipGroupContent extends React.Component<DataToolbarChipGroupContentProps> {
    static defaultProps: PickOptional<DataToolbarChipGroupContentProps>;
    render(): JSX.Element;
}
