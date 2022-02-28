import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/OptionsMenu/options-menu';
import paginationStyles from '@patternfly/react-styles/css/components/Pagination/pagination';
import { css } from '@patternfly/react-styles';
import { DropdownItem, DropdownDirection, DropdownWithContext, DropdownContext } from '../Dropdown';
import CheckIcon from '@patternfly/react-icons/dist/js/icons/check-icon';
import { OptionsToggle } from './OptionsToggle';
import { ToggleTemplateProps } from './ToggleTemplate';
import { PerPageOptions, OnPerPageSelect } from './Pagination';

export interface PaginationOptionsMenuProps extends React.HTMLProps<HTMLDivElement> {
  /** Custom class name added to the Pagination Options Menu */
  className?: string;
  /** Id added to the title of the Pagination Options Menu */
  widgetId?: string;
  /** Flag indicating if Pagination Options Menu is disabled */
  isDisabled?: boolean;
  /** Menu will open up or open down from the Options menu toggle */
  dropDirection?: 'up' | 'down';
  /** Array of titles and values which will be the options on the Options Menu dropdown */
  perPageOptions?: PerPageOptions[];
  /** The Title of the Pagination Options Menu */
  itemsPerPageTitle?: string;
  /** Current page number */
  page?: number;
  /** The suffix to be displayed after each option on the Options Menu dropdown */
  perPageSuffix?: string;
  /** The type or title of the items being paginated */
  itemsTitle?: string;
  /** The text to be displayed on the Options Toggle */
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
  toggleTemplate?: ((props: ToggleTemplateProps) => React.ReactElement) | string;
  /** Function called when user selects number of items per page. */
  onPerPageSelect?: OnPerPageSelect;
}

interface PaginationOptionsMenuState {
  isOpen: boolean;
}

export class PaginationOptionsMenu extends React.Component<PaginationOptionsMenuProps, PaginationOptionsMenuState> {
  private parentRef = React.createRef<HTMLDivElement>();
  static defaultProps: PaginationOptionsMenuProps = {
    className: '',
    widgetId: '',
    isDisabled: false,
    dropDirection: DropdownDirection.down,
    perPageOptions: [] as PerPageOptions[],
    itemsPerPageTitle: 'Items per page',
    perPageSuffix: 'per page',
    optionsToggle: 'Select',
    perPage: 0,
    firstIndex: 0,
    lastIndex: 0,
    defaultToFullPage: false,
    itemCount: 0,
    itemsTitle: 'items',
    toggleTemplate: ({ firstIndex, lastIndex, itemCount, itemsTitle }: ToggleTemplateProps) => (
      <React.Fragment>
        <b>
          {firstIndex} - {lastIndex}
        </b>{' '}
        of<b>{itemCount}</b> {itemsTitle}
      </React.Fragment>
    ),
    onPerPageSelect: () => null as any
  };

  constructor(props: PaginationOptionsMenuProps) {
    super(props);
    this.state = {
      isOpen: false
    };
  }

  onToggle = (isOpen: boolean) => {
    this.setState({ isOpen });
  };

  onSelect = () => {
    this.setState((prevState: PaginationOptionsMenuState) => ({ isOpen: !prevState.isOpen }));
  };

  handleNewPerPage = (_evt: React.MouseEvent | React.KeyboardEvent | MouseEvent, newPerPage: number) => {
    const { page, onPerPageSelect, itemCount, defaultToFullPage } = this.props;
    let newPage = page;

    while (Math.ceil(itemCount / newPerPage) < newPage) {
      newPage--;
    }

    if (defaultToFullPage) {
      if (itemCount / newPerPage !== newPage) {
        while (newPage > 1 && itemCount - newPerPage * newPage < 0) {
          newPage--;
        }
      }
    }
    const startIdx = (newPage - 1) * newPerPage;
    const endIdx = newPage * newPerPage;
    return onPerPageSelect(_evt, newPerPage, newPage, startIdx, endIdx);
  };

  renderItems = () => {
    const { perPageOptions, perPage, perPageSuffix } = this.props;

    return perPageOptions.map(({ value, title }) => (
      <DropdownItem
        key={value}
        component="button"
        data-action={`per-page-${value}`}
        className={css(perPage === value && 'pf-m-selected')}
        onClick={event => this.handleNewPerPage(event, value)}
      >
        {title}
        <span className={css(paginationStyles.paginationMenuText)}>{` ${perPageSuffix}`}</span>
        {perPage === value && (
          <i className={css(styles.optionsMenuMenuItemIcon)}>
            <CheckIcon />
          </i>
        )}
      </DropdownItem>
    ));
  };

  render() {
    const {
      widgetId,
      isDisabled,
      itemsPerPageTitle,
      dropDirection,
      optionsToggle,
      perPageOptions,
      toggleTemplate,
      firstIndex,
      lastIndex,
      itemCount,
      itemsTitle
    } = this.props;
    const { isOpen } = this.state;

    return (
      <DropdownContext.Provider
        value={{
          id: widgetId,
          onSelect: this.onSelect,
          toggleIconClass: styles.optionsMenuToggleIcon,
          toggleTextClass: styles.optionsMenuToggleText,
          menuClass: styles.optionsMenuMenu,
          itemClass: styles.optionsMenuMenuItem,
          toggleClass: ' ',
          baseClass: styles.optionsMenu,
          disabledClass: styles.modifiers.disabled,
          menuComponent: 'ul',
          baseComponent: 'div'
        }}
      >
        <DropdownWithContext
          direction={dropDirection}
          isOpen={isOpen}
          toggle={
            <OptionsToggle
              optionsToggle={optionsToggle}
              itemsPerPageTitle={itemsPerPageTitle}
              showToggle={perPageOptions && perPageOptions.length > 0}
              onToggle={this.onToggle}
              isOpen={isOpen}
              widgetId={widgetId}
              firstIndex={firstIndex}
              lastIndex={lastIndex}
              itemCount={itemCount}
              itemsTitle={itemsTitle}
              toggleTemplate={toggleTemplate}
              parentRef={this.parentRef.current}
              isDisabled={isDisabled}
            />
          }
          dropdownItems={this.renderItems()}
          isPlain
        />
      </DropdownContext.Provider>
    );
  }
}
