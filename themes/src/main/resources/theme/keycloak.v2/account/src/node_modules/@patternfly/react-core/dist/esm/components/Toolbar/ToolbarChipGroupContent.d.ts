import * as React from 'react';
import { RefObject } from 'react';
import { PickOptional } from '../../helpers/typeUtils';
export interface ToolbarChipGroupContentProps extends React.HTMLProps<HTMLDivElement> {
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
    /** Total number of filters currently being applied across all ToolbarFilter components */
    numberOfFilters: number;
    /** Text to display in the total number of applied filters ToolbarFilter */
    numberOfFiltersText?: (numberOfFilters: number) => string;
    /** The breakpoint at which the listed filters in chip groups are collapsed down to a summary */
    collapseListedFiltersBreakpoint?: 'all' | 'md' | 'lg' | 'xl' | '2xl';
    /** Custom additional content appended to the generated chips. To maintain spacing and styling, each node should be a ToolbarItem or ToolbarGroup. This property will remove the built in "Clear all filters" button. */
    customChipGroupContent?: React.ReactNode;
}
export declare class ToolbarChipGroupContent extends React.Component<ToolbarChipGroupContentProps> {
    static displayName: string;
    static defaultProps: PickOptional<ToolbarChipGroupContentProps>;
    render(): JSX.Element;
}
//# sourceMappingURL=ToolbarChipGroupContent.d.ts.map