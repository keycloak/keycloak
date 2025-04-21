import * as React from 'react';
import { RefObject } from 'react';
import { ToolbarContext } from './ToolbarUtils';
import { PickOptional } from '../../helpers/typeUtils';
export interface ToolbarExpandableContentProps extends React.HTMLProps<HTMLDivElement> {
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
export declare class ToolbarExpandableContent extends React.Component<ToolbarExpandableContentProps> {
    static displayName: string;
    static contextType: React.Context<import("./ToolbarUtils").ToolbarContextProps>;
    context: React.ContextType<typeof ToolbarContext>;
    static defaultProps: PickOptional<ToolbarExpandableContentProps>;
    render(): JSX.Element;
}
//# sourceMappingURL=ToolbarExpandableContent.d.ts.map