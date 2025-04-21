import * as React from 'react';
import { ToggleTemplateProps } from './ToggleTemplate';
import { OUIAProps } from '../../helpers';
import { PickOptional } from '../../helpers';
export declare enum PaginationVariant {
    top = "top",
    bottom = "bottom"
}
export interface PerPageOptions {
    /** option title */
    title?: string;
    /** option value */
    value?: number;
}
export interface PaginationTitles {
    /** The title of a page displayed beside the page number */
    page?: string;
    /** The title of a page displayed beside the page number (plural form) */
    pages?: string;
    /** The type or title of the items being paginated */
    items?: string;
    /** The title of the pagination options menu */
    itemsPerPage?: string;
    /** The suffix to be displayed after each option on the options menu dropdown */
    perPageSuffix?: string;
    /** Accessible label for the button which moves to the first page */
    toFirstPage?: string;
    /** Accessible label for the button which moves to the previous page */
    toPreviousPage?: string;
    /** Accessible label for the button which moves to the last page */
    toLastPage?: string;
    /** Accessible label for the button which moves to the next page */
    toNextPage?: string;
    /** Accessible label for the options toggle */
    optionsToggle?: string;
    /** Accessible label for the input displaying the current page */
    currPage?: string;
    /** Accessible label for the pagination component */
    paginationTitle?: string;
    /** Accessible label for the English word "of" */
    ofWord?: string;
}
export declare type OnSetPage = (_evt: React.MouseEvent | React.KeyboardEvent | MouseEvent, newPage: number, perPage?: number, startIdx?: number, endIdx?: number) => void;
export declare type OnPerPageSelect = (_evt: React.MouseEvent | React.KeyboardEvent | MouseEvent, newPerPage: number, newPage: number, startIdx?: number, endIdx?: number) => void;
export interface PaginationProps extends React.HTMLProps<HTMLDivElement>, OUIAProps {
    /** What should be rendered inside */
    children?: React.ReactNode;
    /** Additional classes for the container. */
    className?: string;
    /** Total number of items. */
    itemCount?: number;
    /** Position where pagination is rendered. */
    variant?: 'top' | 'bottom' | PaginationVariant;
    /** Flag indicating if pagination is disabled */
    isDisabled?: boolean;
    /** Flag indicating if pagination is compact */
    isCompact?: boolean;
    /** Flag indicating if pagination should not be sticky on mobile */
    isStatic?: boolean;
    /** Flag indicating if pagination should stick to its position (based on variant) */
    isSticky?: boolean;
    /** Number of items per page. */
    perPage?: number;
    /** Array of the number of items per page  options. */
    perPageOptions?: PerPageOptions[];
    /** Indicate whether to show last full page of results when user selects perPage value greater than remaining rows */
    defaultToFullPage?: boolean;
    /** Page we start at. */
    firstPage?: number;
    /** Current page number. */
    page?: number;
    /** Start index of rows to display, used in place of providing page */
    offset?: number;
    /** First index of items on current page. */
    itemsStart?: number;
    /** Last index of items on current page. */
    itemsEnd?: number;
    /** ID to ideintify widget on page. */
    widgetId?: string;
    /** Direction of dropdown context menu. */
    dropDirection?: 'up' | 'down';
    /** Object with titles to display in pagination. */
    titles?: PaginationTitles;
    /** This will be shown in pagination toggle span. You can use firstIndex, lastIndex, itemCount, itemsTitle, ofWord props. */
    toggleTemplate?: ((props: ToggleTemplateProps) => React.ReactElement) | string;
    /** Function called when user sets page. */
    onSetPage?: OnSetPage;
    /** Function called when user clicks on navigate to first page. */
    onFirstClick?: (event: React.SyntheticEvent<HTMLButtonElement>, page: number) => void;
    /** Function called when user clicks on navigate to previous page. */
    onPreviousClick?: (event: React.SyntheticEvent<HTMLButtonElement>, page: number) => void;
    /** Function called when user clicks on navigate to next page. */
    onNextClick?: (event: React.SyntheticEvent<HTMLButtonElement>, page: number) => void;
    /** Function called when user clicks on navigate to last page. */
    onLastClick?: (event: React.SyntheticEvent<HTMLButtonElement>, page: number) => void;
    /** Function called when user inputs page number. */
    onPageInput?: (event: React.SyntheticEvent<HTMLButtonElement>, page: number) => void;
    /** Function called when user selects number of items per page. */
    onPerPageSelect?: OnPerPageSelect;
    /** Component to be used for wrapping the toggle contents. Use 'button' when you want
     * all of the toggle text to be clickable.
     */
    perPageComponent?: 'div' | 'button';
}
export declare class Pagination extends React.Component<PaginationProps, {
    ouiaStateId: string;
}> {
    static displayName: string;
    paginationRef: React.RefObject<HTMLDivElement>;
    static defaultProps: PickOptional<PaginationProps>;
    state: {
        ouiaStateId: string;
    };
    getLastPage(): number;
    componentDidMount(): void;
    componentDidUpdate(prevProps: PaginationProps & OUIAProps): void;
    render(): JSX.Element;
}
//# sourceMappingURL=Pagination.d.ts.map