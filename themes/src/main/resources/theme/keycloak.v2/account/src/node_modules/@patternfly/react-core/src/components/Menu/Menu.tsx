import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Menu/menu';
import { css } from '@patternfly/react-styles';
import { getOUIAProps, OUIAProps, getDefaultOUIAId } from '../../helpers';
import { MenuContext } from './MenuContext';
import { canUseDOM } from '../../helpers/util';
import { KeyboardHandler } from '../../helpers';
export interface MenuProps extends Omit<React.HTMLAttributes<HTMLDivElement>, 'ref' | 'onSelect'>, OUIAProps {
  /** Anything that can be rendered inside of the Menu */
  children?: React.ReactNode;
  /** Additional classes added to the Menu */
  className?: string;
  /** ID of the menu */
  id?: string;
  /** Callback for updating when item selection changes. You can also specify onClick on the MenuItem. */
  onSelect?: (event?: React.MouseEvent, itemId?: string | number) => void;
  /** Single itemId for single select menus, or array of itemIds for multi select. You can also specify isSelected on the MenuItem. */
  selected?: any | any[];
  /** Callback called when an MenuItems's action button is clicked. You can also specify it within a MenuItemAction. */
  onActionClick?: (event?: any, itemId?: any, actionId?: any) => void;
  /** Search input of menu */
  hasSearchInput?: boolean;
  /** A callback for when the input value changes. */
  onSearchInputChange?: (
    event: React.FormEvent<HTMLInputElement> | React.SyntheticEvent<HTMLButtonElement>,
    value: string
  ) => void;
  /** Accessibility label */
  'aria-label'?: string;
  /** @beta Indicates if menu contains a flyout menu */
  containsFlyout?: boolean;
  /** @beta Indicating that the menu should have nav flyout styling */
  isNavFlyout?: boolean;
  /** @beta Indicates if menu contains a drilldown menu */
  containsDrilldown?: boolean;
  /** @beta Indicates if a menu is drilled into */
  isMenuDrilledIn?: boolean;
  /** @beta Indicates the path of drilled in menu itemIds */
  drilldownItemPath?: string[];
  /** @beta Array of menus that are drilled in */
  drilledInMenus?: string[];
  /** @beta Callback for drilling into a submenu */
  onDrillIn?: (fromItemId: string, toItemId: string, itemId: string) => void;
  /** @beta Callback for drilling out of a submenu */
  onDrillOut?: (toItemId: string, itemId: string) => void;
  /** @beta Callback for collecting menu heights */
  onGetMenuHeight?: (menuId: string, height: number) => void;
  /** @beta ID of parent menu for drilldown menus */
  parentMenu?: string;
  /** @beta ID of the currently active menu for the drilldown variant */
  activeMenu?: string;
  /** @beta itemId of the currently active item. You can also specify isActive on the MenuItem. */
  activeItemId?: string | number;
  /** @hide Forwarded ref */
  innerRef?: React.Ref<HTMLDivElement>;
  /** Internal flag indicating if the Menu is the root of a menu tree */
  isRootMenu?: boolean;
  /** Indicates if the menu should be without the outer box-shadow */
  isPlain?: boolean;
  /** Indicates if the menu should be srollable */
  isScrollable?: boolean;
}

export interface MenuState {
  searchInputValue: string | null;
  ouiaStateId: string;
  transitionMoveTarget: HTMLElement;
  flyoutRef: React.Ref<HTMLLIElement> | null;
  disableHover: boolean;
}

class MenuBase extends React.Component<MenuProps, MenuState> {
  static displayName = 'Menu';
  static contextType = MenuContext;
  context!: React.ContextType<typeof MenuContext>;
  private menuRef = React.createRef<HTMLDivElement>();
  private activeMenu = null as Element;
  static defaultProps: MenuProps = {
    ouiaSafe: true,
    isRootMenu: true,
    isPlain: false,
    isScrollable: false
  };

  constructor(props: MenuProps) {
    super(props);
    if (props.innerRef) {
      this.menuRef = props.innerRef as React.RefObject<HTMLDivElement>;
    }
  }

  state: MenuState = {
    ouiaStateId: getDefaultOUIAId(Menu.displayName),
    searchInputValue: '',
    transitionMoveTarget: null,
    flyoutRef: null,
    disableHover: false
  };

  allowTabFirstItem() {
    // Allow tabbing to first menu item
    const current = this.menuRef.current;
    if (current) {
      const first = current.querySelector('ul button, ul a') as HTMLButtonElement | HTMLAnchorElement;
      if (first) {
        first.tabIndex = 0;
      }
    }
  }

  componentDidMount() {
    if (this.context) {
      this.setState({ disableHover: this.context.disableHover });
    }
    if (canUseDOM) {
      window.addEventListener('transitionend', this.props.isRootMenu ? this.handleDrilldownTransition : null);
    }

    this.allowTabFirstItem();
  }

  componentWillUnmount() {
    if (canUseDOM) {
      window.removeEventListener('transitionend', this.handleDrilldownTransition);
    }
  }

  componentDidUpdate(prevProps: MenuProps) {
    if (prevProps.children !== this.props.children) {
      this.allowTabFirstItem();
    }
  }

  handleDrilldownTransition = (event: TransitionEvent) => {
    const current = this.menuRef.current;

    if (
      !current ||
      (current !== (event.target as HTMLElement).closest('.pf-c-menu') &&
        !Array.from(current.getElementsByClassName('pf-c-menu')).includes(
          (event.target as HTMLElement).closest('.pf-c-menu')
        ))
    ) {
      return;
    }

    if (this.state.transitionMoveTarget) {
      this.state.transitionMoveTarget.focus();
      this.setState({ transitionMoveTarget: null });
    } else {
      const nextMenu = current.querySelector('#' + this.props.activeMenu) || current || null;
      const nextTarget = Array.from(nextMenu.getElementsByTagName('UL')[0].children).filter(
        el => !(el.classList.contains('pf-m-disabled') || el.classList.contains('pf-c-divider'))
      )[0].firstChild;
      (nextTarget as HTMLElement).focus();
      (nextTarget as HTMLElement).tabIndex = 0;
    }
  };

  handleExtraKeys = (event: KeyboardEvent) => {
    const isDrilldown = this.props.containsDrilldown;
    const activeElement = document.activeElement;

    if (
      (event.target as HTMLElement).closest('.pf-c-menu') !== this.activeMenu &&
      !(event.target as HTMLElement).classList.contains('pf-c-breadcrumb__link')
    ) {
      this.activeMenu = (event.target as HTMLElement).closest('.pf-c-menu');
      this.setState({ disableHover: true });
    }

    if ((event.target as HTMLElement).tagName === 'INPUT') {
      return;
    }

    const parentMenu = this.activeMenu;
    const key = event.key;
    const isFromBreadcrumb =
      activeElement.classList.contains('pf-c-breadcrumb__link') ||
      activeElement.classList.contains('pf-c-dropdown__toggle');

    if (key === ' ' || key === 'Enter') {
      event.preventDefault();
      if (isDrilldown && !isFromBreadcrumb) {
        const isDrillingOut = activeElement.closest('li').classList.contains('pf-m-current-path');
        if (isDrillingOut && parentMenu.parentElement.tagName === 'LI') {
          (activeElement as HTMLElement).tabIndex = -1;
          (parentMenu.parentElement.firstChild as HTMLElement).tabIndex = 0;
          this.setState({ transitionMoveTarget: parentMenu.parentElement.firstChild as HTMLElement });
        } else {
          if (activeElement.nextElementSibling && activeElement.nextElementSibling.classList.contains('pf-c-menu')) {
            const childItems = Array.from(
              activeElement.nextElementSibling.getElementsByTagName('UL')[0].children
            ).filter(el => !(el.classList.contains('pf-m-disabled') || el.classList.contains('pf-c-divider')));

            (activeElement as HTMLElement).tabIndex = -1;
            (childItems[0].firstChild as HTMLElement).tabIndex = 0;
            this.setState({ transitionMoveTarget: childItems[0].firstChild as HTMLElement });
          }
        }
      }
      (document.activeElement as HTMLElement).click();
    }
  };

  createNavigableElements = () => {
    const isDrilldown = this.props.containsDrilldown;

    return isDrilldown
      ? Array.from(this.activeMenu.getElementsByTagName('UL')[0].children).filter(
          el => !(el.classList.contains('pf-m-disabled') || el.classList.contains('pf-c-divider'))
        )
      : Array.from(this.menuRef.current.getElementsByTagName('LI')).filter(
          el => !(el.classList.contains('pf-m-disabled') || el.classList.contains('pf-c-divider'))
        );
  };

  render() {
    const {
      'aria-label': ariaLabel,
      id,
      children,
      className,
      onSelect,
      selected = null,
      onActionClick,
      ouiaId,
      ouiaSafe,
      containsFlyout,
      isNavFlyout,
      containsDrilldown,
      isMenuDrilledIn,
      isPlain,
      isScrollable,
      drilldownItemPath,
      drilledInMenus,
      onDrillIn,
      onDrillOut,
      onGetMenuHeight,
      parentMenu = null,
      activeItemId = null,
      /* eslint-disable @typescript-eslint/no-unused-vars */
      innerRef,
      isRootMenu,
      activeMenu,
      /* eslint-enable @typescript-eslint/no-unused-vars */
      ...props
    } = this.props;
    const _isMenuDrilledIn = isMenuDrilledIn || (drilledInMenus && drilledInMenus.includes(id)) || false;
    return (
      <MenuContext.Provider
        value={{
          menuId: id,
          parentMenu: parentMenu || id,
          onSelect,
          onActionClick,
          activeItemId,
          selected,
          drilledInMenus,
          drilldownItemPath,
          onDrillIn,
          onDrillOut,
          onGetMenuHeight,
          flyoutRef: this.state.flyoutRef,
          setFlyoutRef: flyoutRef => this.setState({ flyoutRef }),
          disableHover: this.state.disableHover
        }}
      >
        {isRootMenu && (
          <KeyboardHandler
            containerRef={(this.menuRef as React.RefObject<HTMLDivElement>) || null}
            additionalKeyHandler={this.handleExtraKeys}
            createNavigableElements={this.createNavigableElements}
            isActiveElement={(element: Element) =>
              document.activeElement.parentElement === element ||
              (document.activeElement.closest('ol') && document.activeElement.closest('ol').firstChild === element)
            }
            getFocusableElement={(navigableElement: Element) => navigableElement.firstChild as Element}
            noHorizontalArrowHandling={
              document.activeElement &&
              (document.activeElement.classList.contains('pf-c-breadcrumb__link') ||
                document.activeElement.classList.contains('pf-c-dropdown__toggle'))
            }
            noEnterHandling
            noSpaceHandling
          />
        )}
        <div
          id={id}
          className={css(
            styles.menu,
            isPlain && styles.modifiers.plain,
            isScrollable && styles.modifiers.scrollable,
            containsFlyout && styles.modifiers.flyout,
            isNavFlyout && styles.modifiers.nav,
            containsDrilldown && styles.modifiers.drilldown,
            _isMenuDrilledIn && styles.modifiers.drilledIn,
            className
          )}
          aria-label={ariaLabel}
          ref={this.menuRef}
          {...getOUIAProps(Menu.displayName, ouiaId !== undefined ? ouiaId : this.state.ouiaStateId, ouiaSafe)}
          {...props}
        >
          {children}
        </div>
      </MenuContext.Provider>
    );
  }
}

export const Menu = React.forwardRef((props: MenuProps, ref: React.Ref<HTMLDivElement>) => (
  <MenuBase {...props} innerRef={ref} />
));
Menu.displayName = 'Menu';
