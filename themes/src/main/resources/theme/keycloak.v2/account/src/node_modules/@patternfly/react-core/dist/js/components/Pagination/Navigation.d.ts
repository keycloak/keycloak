import * as React from 'react';
import { OnSetPage } from './Pagination';
import { PickOptional } from '../../helpers';
export interface NavigationProps extends React.HTMLProps<HTMLElement> {
    /** Additional classes for the container */
    className?: string;
    /** Flag indicating if the pagination is disabled */
    isDisabled?: boolean;
    /** Flag indicating if the pagination is compact */
    isCompact?: boolean;
    /** Total number of items. */
    itemCount?: number;
    /** The number of the last page */
    lastPage?: number;
    /** The number of first page where pagination starts */
    firstPage?: number;
    /** The title of a page displayed beside the page number */
    pagesTitle?: string;
    /** The title of a page displayed beside the page number (the plural form) */
    pagesTitlePlural?: string;
    /** Accessible label for the button which moves to the last page */
    toLastPage?: string;
    /** Accessible label for the button which moves to the previous page */
    toPreviousPage?: string;
    /** Accessible label for the button which moves to the next page */
    toNextPage?: string;
    /** Accessible label for the button which moves to the first page */
    toFirstPage?: string;
    /** Accessible label for the input displaying the current page */
    currPage?: string;
    /** Accessible label for the pagination component */
    paginationTitle?: string;
    /** Accessible label for the English word "of" */
    ofWord?: string;
    /** The number of the current page */
    page: React.ReactText;
    /** Number of items per page. */
    perPage?: number;
    /** Function called when page is changed */
    onSetPage: OnSetPage;
    /** Function called when user clicks to navigate to next page */
    onNextClick?: (event: React.SyntheticEvent<HTMLButtonElement>, page: number) => void;
    /** Function called when user clicks to navigate to previous page */
    onPreviousClick?: (event: React.SyntheticEvent<HTMLButtonElement>, page: number) => void;
    /** Function called when user clicks to navigate to first page */
    onFirstClick?: (event: React.SyntheticEvent<HTMLButtonElement>, page: number) => void;
    /** Function called when user clicks to navigate to last page */
    onLastClick?: (event: React.SyntheticEvent<HTMLButtonElement>, page: number) => void;
    /** Function called when user inputs page number */
    onPageInput?: (event: React.SyntheticEvent<HTMLButtonElement>, page: number) => void;
}
export interface NavigationState {
    userInputPage?: React.ReactText;
}
export declare class Navigation extends React.Component<NavigationProps, NavigationState> {
    static displayName: string;
    constructor(props: NavigationProps);
    static defaultProps: PickOptional<NavigationProps>;
    private static parseInteger;
    private onChange;
    private onKeyDown;
    handleNewPage: (_evt: React.MouseEvent | React.KeyboardEvent | MouseEvent, newPage: number) => void;
    componentDidUpdate(lastState: NavigationProps): void;
    render(): JSX.Element;
}
//# sourceMappingURL=Navigation.d.ts.map