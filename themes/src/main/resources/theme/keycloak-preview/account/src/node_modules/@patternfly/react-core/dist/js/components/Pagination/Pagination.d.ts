import * as React from 'react';
import { ToggleTemplateProps } from './ToggleTemplate';
import { InjectedOuiaProps } from '../withOuia';
export declare enum PaginationVariant {
    top = "top",
    bottom = "bottom",
    left = "left",
    right = "right"
}
export interface PerPageOptions {
    title?: string;
    value?: number;
}
export interface PaginationTitles {
    page?: string;
    items?: string;
    itemsPerPage?: string;
    perPageSuffix?: string;
    toFirstPage?: string;
    toPreviousPage?: string;
    toLastPage?: string;
    toNextPage?: string;
    optionsToggle?: string;
    currPage?: string;
    paginationTitle?: string;
}
export declare type OnSetPage = (_evt: React.MouseEvent | React.KeyboardEvent | MouseEvent, newPage: number, perPage?: number, startIdx?: number, endIdx?: number) => void;
export declare type OnPerPageSelect = (_evt: React.MouseEvent | React.KeyboardEvent | MouseEvent, newPerPage: number, newPage: number, startIdx?: number, endIdx?: number) => void;
export interface PaginationProps extends React.HTMLProps<HTMLDivElement> {
    /** What should be rendered inside */
    children?: React.ReactNode;
    /** Additional classes for the container. */
    className?: string;
    /** Total number of items. */
    itemCount: number;
    /** Position where pagination is rendered. */
    variant?: 'top' | 'bottom' | 'left' | 'right';
    /** Flag indicating if pagination is disabled */
    isDisabled?: boolean;
    /** Flag indicating if pagination is compact */
    isCompact?: boolean;
    /** Number of items per page. */
    perPage?: number;
    /** Select from options to number of items per page. */
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
    /** This will be shown in pagination toggle span. You can use firstIndex, lastIndex, itemCount, itemsTitle props. */
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
}
declare const PaginationWithOuiaContext: React.FunctionComponent<PaginationProps & InjectedOuiaProps>;
export { PaginationWithOuiaContext as Pagination };
