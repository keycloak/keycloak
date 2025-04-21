import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Pagination/pagination';
import { css } from '@patternfly/react-styles';
import AngleLeftIcon from '@patternfly/react-icons/dist/esm/icons/angle-left-icon';
import AngleDoubleLeftIcon from '@patternfly/react-icons/dist/esm/icons/angle-double-left-icon';
import AngleRightIcon from '@patternfly/react-icons/dist/esm/icons/angle-right-icon';
import AngleDoubleRightIcon from '@patternfly/react-icons/dist/esm/icons/angle-double-right-icon';
import { Button, ButtonVariant } from '../Button';
import { OnSetPage } from './Pagination';
import { pluralize, PickOptional } from '../../helpers';
import { KEY_CODES } from '../../helpers/constants';

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

export class Navigation extends React.Component<NavigationProps, NavigationState> {
  static displayName = 'Navigation';
  constructor(props: NavigationProps) {
    super(props);
    this.state = { userInputPage: this.props.page };
  }

  static defaultProps: PickOptional<NavigationProps> = {
    className: '',
    isDisabled: false,
    isCompact: false,
    lastPage: 0,
    firstPage: 0,
    pagesTitle: '',
    pagesTitlePlural: '',
    toLastPage: 'Go to last page',
    toNextPage: 'Go to next page',
    toFirstPage: 'Go to first page',
    toPreviousPage: 'Go to previous page',
    currPage: 'Current page',
    paginationTitle: 'Pagination',
    ofWord: 'of',
    onNextClick: () => undefined as any,
    onPreviousClick: () => undefined as any,
    onFirstClick: () => undefined as any,
    onLastClick: () => undefined as any,
    onPageInput: () => undefined as any
  };

  private static parseInteger(input: React.ReactText, lastPage: number): number {
    // eslint-disable-next-line radix
    let inputPage = Number.parseInt(input as string, 10);
    if (!Number.isNaN(inputPage)) {
      inputPage = inputPage > lastPage ? lastPage : inputPage;
      inputPage = inputPage < 1 ? 1 : inputPage;
    }
    return inputPage;
  }

  private onChange(event: React.ChangeEvent<HTMLInputElement>, lastPage: number): void {
    const inputPage = Navigation.parseInteger(event.target.value, lastPage);
    this.setState({ userInputPage: Number.isNaN(inputPage as number) ? event.target.value : inputPage });
  }

  private onKeyDown(
    event: React.KeyboardEvent<HTMLInputElement>,
    page: number | string,
    lastPage: number,
    onPageInput: (event: React.SyntheticEvent<HTMLButtonElement>, page: number) => void
  ): void {
    if (event.keyCode === KEY_CODES.ENTER) {
      const inputPage = Navigation.parseInteger(this.state.userInputPage, lastPage) as number;
      onPageInput(event, Number.isNaN(inputPage) ? (page as number) : inputPage);
      this.handleNewPage(event, Number.isNaN(inputPage) ? (page as number) : inputPage);
    }
  }

  handleNewPage = (_evt: React.MouseEvent | React.KeyboardEvent | MouseEvent, newPage: number) => {
    const { perPage, onSetPage } = this.props;
    const startIdx = (newPage - 1) * perPage;
    const endIdx = newPage * perPage;
    return onSetPage(_evt, newPage, perPage, startIdx, endIdx);
  };

  componentDidUpdate(lastState: NavigationProps) {
    if (
      this.props.page !== lastState.page &&
      this.props.page <= this.props.lastPage &&
      this.state.userInputPage !== this.props.page
    ) {
      this.setState({ userInputPage: this.props.page });
    }
  }

  render() {
    const {
      page,
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      perPage,
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      onSetPage,
      isDisabled,
      itemCount,
      lastPage,
      firstPage,
      pagesTitle,
      pagesTitlePlural,
      toLastPage,
      toNextPage,
      toFirstPage,
      toPreviousPage,
      currPage,
      paginationTitle,
      ofWord,
      onNextClick,
      onPreviousClick,
      onFirstClick,
      onLastClick,
      onPageInput,
      className,
      isCompact,
      ...props
    } = this.props;
    const { userInputPage } = this.state;
    return (
      <nav className={css(styles.paginationNav, className)} aria-label={paginationTitle} {...props}>
        {!isCompact && (
          <div className={css(styles.paginationNavControl, styles.modifiers.first)}>
            <Button
              variant={ButtonVariant.plain}
              isDisabled={isDisabled || page === firstPage || page === 0}
              aria-label={toFirstPage}
              data-action="first"
              onClick={event => {
                onFirstClick(event, 1);
                this.handleNewPage(event, 1);
                this.setState({ userInputPage: 1 });
              }}
            >
              <AngleDoubleLeftIcon />
            </Button>
          </div>
        )}
        <div className={styles.paginationNavControl}>
          <Button
            variant={ButtonVariant.plain}
            isDisabled={isDisabled || page === firstPage || page === 0}
            data-action="previous"
            onClick={event => {
              const newPage = (page as number) - 1 >= 1 ? (page as number) - 1 : 1;
              onPreviousClick(event, newPage);
              this.handleNewPage(event, newPage);
              this.setState({ userInputPage: newPage });
            }}
            aria-label={toPreviousPage}
          >
            <AngleLeftIcon />
          </Button>
        </div>
        {!isCompact && (
          <div className={styles.paginationNavPageSelect}>
            <input
              className={css(styles.formControl)}
              aria-label={currPage}
              type="number"
              disabled={
                isDisabled || (itemCount && page === firstPage && page === lastPage && itemCount >= 0) || page === 0
              }
              min={lastPage <= 0 && firstPage <= 0 ? 0 : 1}
              max={lastPage}
              value={userInputPage}
              onKeyDown={event => this.onKeyDown(event, page, lastPage, onPageInput)}
              onChange={event => this.onChange(event, lastPage)}
            />
            {(itemCount || itemCount === 0) && (
              <span aria-hidden="true">
                {ofWord} {pagesTitle ? pluralize(lastPage, pagesTitle, pagesTitlePlural) : lastPage}
              </span>
            )}
          </div>
        )}
        <div className={styles.paginationNavControl}>
          <Button
            variant={ButtonVariant.plain}
            isDisabled={isDisabled || page === lastPage}
            aria-label={toNextPage}
            data-action="next"
            onClick={event => {
              const newPage = (page as number) + 1 <= lastPage ? (page as number) + 1 : lastPage;
              onNextClick(event, newPage);
              this.handleNewPage(event, newPage);
              this.setState({ userInputPage: newPage });
            }}
          >
            <AngleRightIcon />
          </Button>
        </div>
        {!isCompact && (
          <div className={css(styles.paginationNavControl, styles.modifiers.last)}>
            <Button
              variant={ButtonVariant.plain}
              isDisabled={isDisabled || page === lastPage}
              aria-label={toLastPage}
              data-action="last"
              onClick={event => {
                onLastClick(event, lastPage);
                this.handleNewPage(event, lastPage);
                this.setState({ userInputPage: lastPage });
              }}
            >
              <AngleDoubleRightIcon />
            </Button>
          </div>
        )}
      </nav>
    );
  }
}
