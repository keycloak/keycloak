import * as React from 'react';
import { DropdownDirection } from '../Dropdown';
import { ToggleTemplate, ToggleTemplateProps } from './ToggleTemplate';
import styles from '@patternfly/react-styles/css/components/Pagination/pagination';
import { css } from '@patternfly/react-styles';
import { Navigation } from './Navigation';
import { PaginationOptionsMenu } from './PaginationOptionsMenu';
import { InjectedOuiaProps, withOuiaContext } from '../withOuia';
import { c_pagination__nav_page_select_c_form_control_width_chars as widthChars } from '@patternfly/react-tokens';
import { PickOptional } from '../../helpers';

export enum PaginationVariant {
  top = 'top',
  bottom = 'bottom',
  left = 'left',
  right = 'right'
}

const defaultPerPageOptions = [
  {
    title: '10',
    value: 10
  },
  {
    title: '20',
    value: 20
  },
  {
    title: '50',
    value: 50
  },
  {
    title: '100',
    value: 100
  }
];

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

export type OnSetPage = (
  _evt: React.MouseEvent | React.KeyboardEvent | MouseEvent,
  newPage: number,
  perPage?: number,
  startIdx?: number,
  endIdx?: number
) => void;

export type OnPerPageSelect = (
  _evt: React.MouseEvent | React.KeyboardEvent | MouseEvent,
  newPerPage: number,
  newPage: number,
  startIdx?: number,
  endIdx?: number
) => void;

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

const handleInputWidth = (lastPage: number, node: HTMLDivElement) => {
  if (!node) {
    return;
  }
  const len = String(lastPage).length;
  if (len >= 3) {
    node.style.setProperty(widthChars.name, `${len}`);
  } else {
    node.style.setProperty(widthChars.name, '2');
  }
};

let paginationId = 0;
class Pagination extends React.Component<PaginationProps & InjectedOuiaProps> {
  paginationRef = React.createRef<HTMLDivElement>();
  static defaultProps: PickOptional<PaginationProps & InjectedOuiaProps> = {
    children: null,
    className: '',
    variant: PaginationVariant.top,
    isDisabled: false,
    isCompact: false,
    perPage: defaultPerPageOptions[0].value,
    titles: {
      items: '',
      page: '',
      itemsPerPage: 'Items per page',
      perPageSuffix: 'per page',
      toFirstPage: 'Go to first page',
      toPreviousPage: 'Go to previous page',
      toLastPage: 'Go to last page',
      toNextPage: 'Go to next page',
      optionsToggle: 'Items per page',
      currPage: 'Current page',
      paginationTitle: 'Pagination'
    },
    firstPage: 1,
    page: 0,
    offset: 0,
    defaultToFullPage: false,
    itemsStart: null,
    itemsEnd: null,
    perPageOptions: defaultPerPageOptions,
    dropDirection: DropdownDirection.down,
    widgetId: 'pagination-options-menu',
    toggleTemplate: ToggleTemplate,
    onSetPage: () => undefined,
    onPerPageSelect: () => undefined,
    onFirstClick: () => undefined,
    onPreviousClick: () => undefined,
    onNextClick: () => undefined,
    onPageInput: () => undefined,
    onLastClick: () => undefined,
    ouiaContext: null,
    ouiaId: null
  };

  getLastPage() {
    const { itemCount, perPage } = this.props;
    return Math.ceil(itemCount / perPage) || 0;
  }

  componentDidMount() {
    const node = this.paginationRef.current;
    handleInputWidth(this.getLastPage(), node);
  }

  componentDidUpdate(prevProps: PaginationProps & InjectedOuiaProps) {
    const node = this.paginationRef.current;
    if (prevProps.perPage !== this.props.perPage || prevProps.itemCount !== this.props.itemCount) {
      handleInputWidth(this.getLastPage(), node);
    }
  }
  render() {
    const {
      children,
      className,
      variant,
      isDisabled,
      isCompact,
      perPage,
      titles,
      firstPage,
      page: propPage,
      offset,
      defaultToFullPage,
      itemCount,
      itemsStart,
      itemsEnd,
      perPageOptions,
      dropDirection,
      widgetId,
      toggleTemplate,
      onSetPage,
      onPerPageSelect,
      onFirstClick,
      onPreviousClick,
      onNextClick,
      onPageInput,
      onLastClick,
      ouiaContext,
      ouiaId,
      ...props
    } = this.props;

    let page = propPage;
    if (!page && offset) {
      page = Math.ceil(offset / perPage);
    }

    const lastPage = this.getLastPage();
    if (page < firstPage && itemCount > 0) {
      page = firstPage;
    } else if (page > lastPage) {
      page = lastPage;
    }

    const firstIndex = itemCount <= 0 ? 0 : (page - 1) * perPage + 1;
    let lastIndex;
    if (itemCount <= 0) {
      lastIndex = 0;
    } else {
      lastIndex = page === lastPage ? itemCount : page * perPage;
    }

    return (
      <div
        ref={this.paginationRef}
        className={css(
          styles.pagination,
          variant === PaginationVariant.bottom && styles.modifiers.footer,
          isCompact && styles.modifiers.compact,
          className
        )}
        id={`${widgetId}-${paginationId++}`}
        {...(ouiaContext.isOuia && {
          'data-ouia-component-type': 'Pagination',
          'data-ouia-component-id': ouiaId || ouiaContext.ouiaId
        })}
        {...props}
      >
        {variant === PaginationVariant.top && (
          <div className={css(styles.paginationTotalItems)}>
            <ToggleTemplate
              firstIndex={firstIndex}
              lastIndex={lastIndex}
              itemCount={itemCount}
              itemsTitle={titles.items}
            />
          </div>
        )}
        <PaginationOptionsMenu
          itemsPerPageTitle={titles.itemsPerPage}
          perPageSuffix={titles.perPageSuffix}
          itemsTitle={isCompact ? '' : titles.items}
          optionsToggle={titles.optionsToggle}
          perPageOptions={perPageOptions}
          firstIndex={itemsStart !== null ? itemsStart : firstIndex}
          lastIndex={itemsEnd !== null ? itemsEnd : lastIndex}
          defaultToFullPage={defaultToFullPage}
          itemCount={itemCount}
          page={page}
          perPage={perPage}
          lastPage={lastPage}
          onPerPageSelect={onPerPageSelect}
          dropDirection={dropDirection}
          widgetId={widgetId}
          toggleTemplate={toggleTemplate}
          isDisabled={isDisabled}
        />
        <Navigation
          pagesTitle={titles.page}
          toLastPage={titles.toLastPage}
          toPreviousPage={titles.toPreviousPage}
          toNextPage={titles.toNextPage}
          toFirstPage={titles.toFirstPage}
          currPage={titles.currPage}
          paginationTitle={titles.paginationTitle}
          page={itemCount <= 0 ? 0 : page}
          perPage={perPage}
          firstPage={itemsStart !== null ? itemsStart : 1}
          lastPage={lastPage}
          onSetPage={onSetPage}
          onFirstClick={onFirstClick}
          onPreviousClick={onPreviousClick}
          onNextClick={onNextClick}
          onLastClick={onLastClick}
          onPageInput={onPageInput}
          isDisabled={isDisabled}
          isCompact={isCompact}
        />
        {children}
      </div>
    );
  }
}

const PaginationWithOuiaContext = withOuiaContext(Pagination);
export { PaginationWithOuiaContext as Pagination };
