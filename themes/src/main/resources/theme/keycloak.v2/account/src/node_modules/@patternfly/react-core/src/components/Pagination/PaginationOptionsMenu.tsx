import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/OptionsMenu/options-menu';
import { css } from '@patternfly/react-styles';
import { DropdownItem, DropdownDirection, DropdownWithContext, DropdownContext } from '../Dropdown';
import CheckIcon from '@patternfly/react-icons/dist/esm/icons/check-icon';
import { OptionsToggle } from './OptionsToggle';
import { ToggleTemplateProps, ToggleTemplate } from './ToggleTemplate';
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

export class PaginationOptionsMenu extends React.Component<PaginationOptionsMenuProps, PaginationOptionsMenuState> {
  static displayName = 'PaginationOptionsMenu';
  private parentRef = React.createRef<HTMLDivElement>();
  static defaultProps: PaginationOptionsMenuProps = {
    className: '',
    widgetId: '',
    isDisabled: false,
    dropDirection: DropdownDirection.down,
    perPageOptions: [] as PerPageOptions[],
    itemsPerPageTitle: 'Items per page',
    perPageSuffix: 'per page',
    optionsToggle: '',
    ofWord: 'of',
    perPage: 0,
    firstIndex: 0,
    lastIndex: 0,
    defaultToFullPage: false,
    itemsTitle: 'items',
    toggleTemplate: ToggleTemplate,
    onPerPageSelect: () => null as any,
    perPageComponent: 'div'
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
        {` ${perPageSuffix}`}
        {perPage === value && (
          <div className={css(styles.optionsMenuMenuItemIcon)}>
            <CheckIcon />
          </div>
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
      itemsTitle,
      ofWord,
      perPageComponent
    } = this.props;
    const { isOpen } = this.state;

    return (
      <DropdownContext.Provider
        value={{
          id: widgetId,
          onSelect: this.onSelect,
          toggleIndicatorClass:
            perPageComponent === 'div' ? styles.optionsMenuToggleButtonIcon : styles.optionsMenuToggleIcon,
          toggleTextClass: styles.optionsMenuToggleText,
          menuClass: styles.optionsMenuMenu,
          itemClass: styles.optionsMenuMenuItem,
          toggleClass: ' ',
          baseClass: styles.optionsMenu,
          disabledClass: styles.modifiers.disabled,
          menuComponent: 'ul',
          baseComponent: 'div',
          ouiaComponentType: PaginationOptionsMenu.displayName
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
              ofWord={ofWord}
              toggleTemplate={toggleTemplate}
              parentRef={this.parentRef.current}
              isDisabled={isDisabled}
              perPageComponent={perPageComponent}
            />
          }
          dropdownItems={this.renderItems()}
          isPlain
        />
      </DropdownContext.Provider>
    );
  }
}
