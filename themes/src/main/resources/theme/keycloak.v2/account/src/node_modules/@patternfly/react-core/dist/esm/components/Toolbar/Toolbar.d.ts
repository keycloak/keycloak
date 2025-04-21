import * as React from 'react';
import { OUIAProps } from '../../helpers';
export interface ToolbarProps extends React.HTMLProps<HTMLDivElement>, OUIAProps {
    /** Optional callback for clearing all filters in the toolbar */
    clearAllFilters?: () => void;
    /** Text to display in the clear all filters button */
    clearFiltersButtonText?: string;
    /** Custom content appended to the filter generated chip group. To maintain spacing and styling, each node should be wrapped in a ToolbarItem or ToolbarGroup. This property will remove the default "Clear all filters" button. */
    customChipGroupContent?: React.ReactNode;
    /** The breakpoint at which the listed filters in chip groups are collapsed down to a summary */
    collapseListedFiltersBreakpoint?: 'all' | 'md' | 'lg' | 'xl' | '2xl';
    /** Flag indicating if a data toolbar toggle group's expandable content is expanded */
    isExpanded?: boolean;
    /** A callback for setting the isExpanded flag */
    toggleIsExpanded?: () => void;
    /** Classes applied to root element of the data toolbar */
    className?: string;
    /** Content to be rendered as rows in the data toolbar */
    children?: React.ReactNode;
    /** Id of the data toolbar */
    id?: string;
    /** Flag indicating the toolbar height should expand to the full height of the container */
    isFullHeight?: boolean;
    /** Flag indicating the toolbar is static */
    isStatic?: boolean;
    /** Flag indicating the toolbar should use the Page insets */
    usePageInsets?: boolean;
    /** Flag indicating the toolbar should stick to the top of its container */
    isSticky?: boolean;
    /** Insets at various breakpoints. */
    inset?: {
        default?: 'insetNone' | 'insetSm' | 'insetMd' | 'insetLg' | 'insetXl' | 'inset2xl';
        sm?: 'insetNone' | 'insetSm' | 'insetMd' | 'insetLg' | 'insetXl' | 'inset2xl';
        md?: 'insetNone' | 'insetSm' | 'insetMd' | 'insetLg' | 'insetXl' | 'inset2xl';
        lg?: 'insetNone' | 'insetSm' | 'insetMd' | 'insetLg' | 'insetXl' | 'inset2xl';
        xl?: 'insetNone' | 'insetSm' | 'insetMd' | 'insetLg' | 'insetXl' | 'inset2xl';
        '2xl'?: 'insetNone' | 'insetSm' | 'insetMd' | 'insetLg' | 'insetXl' | 'inset2xl';
    };
    /** Text to display in the total number of applied filters ToolbarFilter */
    numberOfFiltersText?: (numberOfFilters: number) => string;
}
export interface ToolbarState {
    /** Flag used if the user has opted NOT to manage the 'isExpanded' state of the toggle group.
     *  Indicates whether or not the toggle group is expanded. */
    isManagedToggleExpanded: boolean;
    /** Object managing information about how many chips are in each chip group */
    filterInfo: FilterInfo;
    /** Used to keep track of window width so we can collapse expanded content when window is resizing */
    windowWidth: number;
    ouiaStateId: string;
}
interface FilterInfo {
    [key: string]: number;
}
export declare class Toolbar extends React.Component<ToolbarProps, ToolbarState> {
    static displayName: string;
    chipGroupContentRef: React.RefObject<HTMLDivElement>;
    staticFilterInfo: {};
    state: {
        isManagedToggleExpanded: boolean;
        filterInfo: {};
        windowWidth: number;
        ouiaStateId: string;
    };
    isToggleManaged: () => boolean;
    toggleIsExpanded: () => void;
    closeExpandableContent: (e: any) => void;
    componentDidMount(): void;
    componentWillUnmount(): void;
    updateNumberFilters: (categoryName: string, numberOfFilters: number) => void;
    getNumberOfFilters: () => number;
    renderToolbar: (randomId: string) => JSX.Element;
    render(): JSX.Element;
}
export {};
//# sourceMappingURL=Toolbar.d.ts.map