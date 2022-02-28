import * as React from 'react';
import { RefObject } from 'react';
import { PickOptional } from '../../helpers/typeUtils';
export interface DataToolbarExpandableContentProps extends React.HTMLProps<HTMLDivElement> {
    /** Classes added to the root element of the data toolbar expandable content */
    className?: string;
    /** Flag indicating the expandable content is expanded */
    isExpanded?: boolean;
    /** Expandable content reference for passing to data toolbar children */
    expandableContentRef?: RefObject<HTMLDivElement>;
    /** Chip container reference for passing to data toolbar children */
    chipContainerRef?: RefObject<any>;
    /** optional callback for clearing all filters in the toolbar */
    clearAllFilters?: () => void;
    /** Text to display in the clear all filters button */
    clearFiltersButtonText?: string;
    /** Flag indicating that the clear all filters button should be visible */
    showClearFiltersButton: boolean;
}
export declare class DataToolbarExpandableContent extends React.Component<DataToolbarExpandableContentProps> {
    static contextType: any;
    static defaultProps: PickOptional<DataToolbarExpandableContentProps>;
    render(): JSX.Element;
}
