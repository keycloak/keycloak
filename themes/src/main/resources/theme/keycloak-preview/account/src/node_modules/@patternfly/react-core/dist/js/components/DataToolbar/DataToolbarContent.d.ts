import * as React from 'react';
import { DataToolbarBreakpointMod } from './DataToolbarUtils';
export interface DataToolbarContentProps extends React.HTMLProps<HTMLDivElement> {
    /** Classes applied to root element of the data toolbar content row */
    className?: string;
    /** An array of objects representing the various modifiers to apply to the content row at various breakpoints */
    breakpointMods?: DataToolbarBreakpointMod[];
    /** Content to be rendered as children of the content row */
    children?: React.ReactNode;
    /** Flag indicating if a data toolbar toggle group's expandable content is expanded */
    isExpanded?: boolean;
    /** Optional callback for clearing all filters in the toolbar */
    clearAllFilters?: () => void;
    /** Flag indicating that the clear all filters button should be visible */
    showClearFiltersButton?: boolean;
    /** Text to display in the clear all filters button */
    clearFiltersButtonText?: string;
    /** Id of the parent DataToolbar component */
    toolbarId?: string;
}
export declare class DataToolbarContent extends React.Component<DataToolbarContentProps> {
    private expandableContentRef;
    private chipContainerRef;
    private static currentId;
    static defaultProps: DataToolbarContentProps;
    render(): JSX.Element;
}
