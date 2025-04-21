import * as React from 'react';
import { ToggleTemplateProps } from './ToggleTemplate';
import { PerPageOptions, OnPerPageSelect } from './Pagination';
export interface PaginationOptionsMenuProps extends React.HTMLProps<HTMLDivElement> {
    /** Custom class name added to the pagination options menu */
    className?: string;
    /** Id added to the title of the Pagination options menu */
    widgetId?: string;
    /** Flag indicating if pagination options menu is disabled */
    isDisabled?: boolean;
    /** Menu will open up or open down from the options menu toggle */
    dropDirection?: 'up' | 'down';
    /** Array of titles and values which will be the options on the options menu dropdown */
    perPageOptions?: PerPageOptions[];
    /** The title of the pagination options menu */
    itemsPerPageTitle?: string;
    /** Current page number */
    page?: number;
    /** The suffix to be displayed after each option on the options menu dropdown */
    perPageSuffix?: string;
    /** The type or title of the items being paginated */
    itemsTitle?: string;
    /** Accessible label for the options toggle */
    optionsToggle?: string;
    /** The total number of items being paginated */
    itemCount?: number;
    /** The first index of the items being paginated */
    firstIndex?: number;
    /** The last index of the items being paginated */
    lastIndex?: number;
    /** Flag to show last full page of results if perPage selected > remaining rows */
    defaultToFullPage?: boolean;
    /** The number of items to be displayed per page */
    perPage?: number;
    /** The number of the last page */
    lastPage?: number;
    /** This will be shown in pagination toggle span. You can use firstIndex, lastIndex, itemCount, itemsTitle props. */
    toggleTemplate: ((props: ToggleTemplateProps) => React.ReactElement) | string;
    /** Function called when user selects number of items per page. */
    onPerPageSelect?: OnPerPageSelect;
    /** Label for the English word "of" */
    ofWord?: string;
    /** Component to be used for wrapping the toggle contents. Use 'button' when you want
     * all of the toggle text to be clickable.
     */
    perPageComponent?: 'div' | 'button';
}
interface PaginationOptionsMenuState {
    isOpen: boolean;
}
export declare class PaginationOptionsMenu extends React.Component<PaginationOptionsMenuProps, PaginationOptionsMenuState> {
    static displayName: string;
    private parentRef;
    static defaultProps: PaginationOptionsMenuProps;
    constructor(props: PaginationOptionsMenuProps);
    onToggle: (isOpen: boolean) => void;
    onSelect: () => void;
    handleNewPerPage: (_evt: React.MouseEvent | React.KeyboardEvent | MouseEvent, newPerPage: number) => void;
    renderItems: () => JSX.Element[];
    render(): JSX.Element;
}
export {};
//# sourceMappingURL=PaginationOptionsMenu.d.ts.map