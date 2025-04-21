import * as React from 'react';
export interface ToolbarContentProps extends React.HTMLProps<HTMLDivElement> {
    /** Classes applied to root element of the data toolbar content row */
    className?: string;
    /** Visibility at various breakpoints. */
    visibility?: {
        default?: 'hidden' | 'visible';
        md?: 'hidden' | 'visible';
        lg?: 'hidden' | 'visible';
        xl?: 'hidden' | 'visible';
        '2xl'?: 'hidden' | 'visible';
    };
    /** @deprecated prop misspelled */
    visiblity?: {
        default?: 'hidden' | 'visible';
        md?: 'hidden' | 'visible';
        lg?: 'hidden' | 'visible';
        xl?: 'hidden' | 'visible';
        '2xl'?: 'hidden' | 'visible';
    };
    /** Alignment at various breakpoints. */
    alignment?: {
        default?: 'alignRight' | 'alignLeft';
        md?: 'alignRight' | 'alignLeft';
        lg?: 'alignRight' | 'alignLeft';
        xl?: 'alignRight' | 'alignLeft';
        '2xl'?: 'alignRight' | 'alignLeft';
    };
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
    /** Id of the parent Toolbar component */
    toolbarId?: string;
}
export declare class ToolbarContent extends React.Component<ToolbarContentProps> {
    static displayName: string;
    private expandableContentRef;
    private chipContainerRef;
    private static currentId;
    static defaultProps: ToolbarContentProps;
    render(): JSX.Element;
}
//# sourceMappingURL=ToolbarContent.d.ts.map