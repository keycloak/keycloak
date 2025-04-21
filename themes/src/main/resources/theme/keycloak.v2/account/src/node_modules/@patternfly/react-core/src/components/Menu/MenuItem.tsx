import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Menu/menu';
import { css } from '@patternfly/react-styles';
import topOffset from '@patternfly/react-tokens/dist/esm/c_menu_m_flyout__menu_top_offset';
import rightOffset from '@patternfly/react-tokens/dist/esm/c_menu_m_flyout__menu_m_left_right_offset';
import leftOffset from '@patternfly/react-tokens/dist/esm/c_menu_m_flyout__menu_left_offset';
import ExternalLinkAltIcon from '@patternfly/react-icons/dist/esm/icons/external-link-alt-icon';
import AngleRightIcon from '@patternfly/react-icons/dist/esm/icons/angle-right-icon';
import AngleLeftIcon from '@patternfly/react-icons/dist/esm/icons/angle-left-icon';
import CheckIcon from '@patternfly/react-icons/dist/esm/icons/check-icon';
import { Checkbox } from '../Checkbox';
import { MenuContext, MenuItemContext } from './MenuContext';
import { MenuItemAction } from './MenuItemAction';
import { canUseDOM } from '../../helpers/util';
import { useIsomorphicLayoutEffect } from '../../helpers/useIsomorphicLayout';
import { GenerateId } from '../../helpers/GenerateId/GenerateId';

export interface MenuItemProps extends Omit<React.HTMLProps<HTMLLIElement>, 'onClick'> {
  /** Content rendered inside the menu list item. */
  children?: React.ReactNode;
  /** Additional classes added to the menu list item */
  className?: string;
  /** Identifies the component in the Menu onSelect or onActionClick callback */
  itemId?: any;
  /** Target navigation link */
  to?: string;
  /** @beta Flag indicating the item has a checkbox */
  hasCheck?: boolean;
  /** Flag indicating whether the item is active */
  isActive?: boolean;
  /** Flag indicating if the item is favorited */
  isFavorited?: boolean;
  /** Flag indicating if the item causes a load */
  isLoadButton?: boolean;
  /** Flag indicating a loading state */
  isLoading?: boolean;
  /** Callback for item click */
  onClick?: (event?: any) => void;
  /** Component used to render the menu item */
  component?: React.ElementType<any> | React.ComponentType<any>;
  /** Render item as disabled option */
  isDisabled?: boolean;
  /** Render item with icon */
  icon?: React.ReactNode;
  /** Render item with one or more actions */
  actions?: React.ReactNode;
  /** Description of the menu item */
  description?: React.ReactNode;
  /** Render external link icon */
  isExternalLink?: boolean;
  /** Flag indicating if the option is selected */
  isSelected?: boolean;
  /** @beta Flyout menu */
  flyoutMenu?: React.ReactElement;
  /** @beta Callback function when mouse leaves trigger */
  onShowFlyout?: (event?: any) => void;
  /** @beta Drilldown menu of the item. Should be a Menu or DrilldownMenu type. */
  drilldownMenu?: React.ReactNode | (() => React.ReactNode);
  /** @beta Sub menu direction */
  direction?: 'down' | 'up';
  /** @beta True if item is on current selection path */
  isOnPath?: boolean;
  /** Accessibility label */
  'aria-label'?: string;
  /** @hide Forwarded ref */
  innerRef?: React.Ref<HTMLAnchorElement | HTMLButtonElement>;
}

const FlyoutContext = React.createContext({
  direction: 'right' as 'left' | 'right'
});

const MenuItemBase: React.FunctionComponent<MenuItemProps> = ({
  children,
  className,
  itemId = null,
  to,
  hasCheck = false,
  isActive = null,
  isFavorited = null,
  isLoadButton = false,
  isLoading = false,
  flyoutMenu,
  direction,
  description = null as string,
  onClick = () => {},
  component = 'button',
  isDisabled = false,
  isExternalLink = false,
  isSelected = null,
  icon,
  actions,
  onShowFlyout,
  drilldownMenu,
  isOnPath,
  innerRef,
  ...props
}: MenuItemProps) => {
  const {
    menuId,
    parentMenu,
    onSelect,
    onActionClick,
    activeItemId,
    selected,
    drilldownItemPath,
    onDrillIn,
    onDrillOut,
    flyoutRef,
    setFlyoutRef,
    disableHover
  } = React.useContext(MenuContext);
  let Component = (to ? 'a' : component) as any;
  if (hasCheck && !to) {
    Component = 'label' as any;
  }
  const [flyoutTarget, setFlyoutTarget] = React.useState(null);
  const flyoutContext = React.useContext(FlyoutContext);
  const [flyoutXDirection, setFlyoutXDirection] = React.useState(flyoutContext.direction);
  const ref = React.useRef<HTMLLIElement>();
  const flyoutVisible = ref === flyoutRef;

  const hasFlyout = flyoutMenu !== undefined;
  const showFlyout = (show: boolean) => {
    if (!flyoutVisible && show) {
      setFlyoutRef(ref);
    } else if (flyoutVisible && !show) {
      setFlyoutRef(null);
    }
    onShowFlyout && show && onShowFlyout();
  };

  useIsomorphicLayoutEffect(() => {
    if (hasFlyout && ref.current && canUseDOM) {
      const flyoutMenu = ref.current.lastElementChild as HTMLElement;
      if (flyoutMenu && flyoutMenu.classList.contains(styles.menu)) {
        const origin = ref.current.getClientRects()[0];
        const rect = flyoutMenu.getClientRects()[0];
        if (origin && rect) {
          const spaceLeftLeft = origin.x - rect.width;
          const spaceLeftRight = window.innerWidth - origin.x - origin.width - rect.width;
          let xDir = flyoutXDirection as 'left' | 'right' | 'none';
          if (spaceLeftRight < 0 && xDir !== 'left') {
            setFlyoutXDirection('left');
            xDir = 'left';
          } else if (spaceLeftLeft < 0 && xDir !== 'right') {
            setFlyoutXDirection('right');
            xDir = 'right';
          }
          let xOffset = 0;
          if (spaceLeftLeft < 0 && spaceLeftRight < 0) {
            xOffset = xDir === 'right' ? -spaceLeftRight : -spaceLeftLeft;
          }
          if (xDir === 'left') {
            flyoutMenu.classList.add(styles.modifiers.left);
            flyoutMenu.style.setProperty(rightOffset.name, `-${xOffset}px`);
          } else {
            flyoutMenu.style.setProperty(leftOffset.name, `-${xOffset}px`);
          }

          const spaceLeftBot = window.innerHeight - origin.y - rect.height;
          const spaceLeftTop = window.innerHeight - rect.height;
          if (spaceLeftTop < 0 && spaceLeftBot < 0) {
            // working idea: page can usually scroll down, but not up
            // TODO: proper scroll buttons
          } else if (spaceLeftBot < 0) {
            flyoutMenu.style.setProperty(topOffset.name, `${spaceLeftBot}px`);
          }
        }
      }
    }
  }, [flyoutVisible, flyoutMenu]);

  React.useEffect(() => {
    setFlyoutXDirection(flyoutContext.direction);
  }, [flyoutContext]);

  React.useEffect(() => {
    if (flyoutTarget) {
      if (flyoutVisible) {
        const flyoutMenu = (flyoutTarget as HTMLElement).nextElementSibling;
        const flyoutItems = Array.from(flyoutMenu.getElementsByTagName('UL')[0].children).filter(
          el => !(el.classList.contains('pf-m-disabled') || el.classList.contains('pf-c-divider'))
        );
        (flyoutItems[0].firstChild as HTMLElement).focus();
      } else {
        flyoutTarget.focus();
      }
    }
  }, [flyoutVisible, flyoutTarget]);

  const handleFlyout = (event: React.KeyboardEvent) => {
    const key = event.key;
    const target = event.target;

    if (key === ' ' || key === 'Enter' || key === 'ArrowRight') {
      event.stopPropagation();
      if (!flyoutVisible) {
        showFlyout(true);
        setFlyoutTarget(target as HTMLElement);
      }
    }

    if (key === 'Escape' || key === 'ArrowLeft') {
      if (flyoutVisible) {
        event.stopPropagation();
        showFlyout(false);
      }
    }
  };

  const onItemSelect = (event: any, onSelect: any) => {
    // Trigger callback for Menu onSelect
    onSelect && onSelect(event, itemId);
    // Trigger callback for item onClick
    onClick && onClick(event);
  };
  const _isOnPath = (isOnPath && isOnPath) || (drilldownItemPath && drilldownItemPath.includes(itemId)) || false;
  let _drill: () => void;
  if (direction) {
    if (direction === 'down') {
      _drill = () =>
        onDrillIn &&
        onDrillIn(
          menuId,
          typeof drilldownMenu === 'function'
            ? (drilldownMenu() as any).props.id
            : (drilldownMenu as React.ReactElement).props.id,
          itemId
        );
    } else {
      _drill = () => onDrillOut && onDrillOut(parentMenu, itemId);
    }
  }
  let additionalProps = {} as any;
  if (Component === 'a') {
    additionalProps = {
      href: to,
      'aria-disabled': isDisabled ? true : null,
      // prevent invalid 'disabled' attribute on <a> tags
      disabled: null
    };
  } else if (Component === 'button') {
    additionalProps = {
      type: 'button'
    };
  }
  if (isOnPath) {
    additionalProps['aria-expanded'] = true;
  } else if (hasFlyout) {
    additionalProps['aria-haspopup'] = true;
    additionalProps['aria-expanded'] = flyoutVisible;
  }
  const getAriaCurrent = () => {
    if (isActive !== null) {
      if (isActive) {
        return 'page';
      } else {
        return null;
      }
    } else if (itemId !== null && activeItemId !== null) {
      return itemId === activeItemId;
    }
    return null;
  };
  const getIsSelected = () => {
    if (isSelected !== null) {
      return isSelected;
    } else if (selected !== null && itemId !== null) {
      return (Array.isArray(selected) && selected.includes(itemId)) || itemId === selected;
    }
    return false;
  };
  const onMouseOver = () => {
    if (disableHover) {
      return;
    }
    if (hasFlyout) {
      showFlyout(true);
    } else {
      setFlyoutRef(null);
    }
  };

  return (
    <li
      className={css(
        styles.menuListItem,
        isDisabled && styles.modifiers.disabled,
        _isOnPath && styles.modifiers.currentPath,
        isLoadButton && styles.modifiers.load,
        isLoading && styles.modifiers.loading,
        className
      )}
      onMouseOver={onMouseOver}
      {...(flyoutMenu && { onKeyDown: handleFlyout })}
      ref={ref}
      role={!hasCheck ? 'none' : 'menuitem'}
      {...props}
    >
      <GenerateId>
        {randomId => (
          <Component
            tabIndex={-1}
            className={css(styles.menuItem, getIsSelected() && !hasCheck && styles.modifiers.selected, className)}
            aria-current={getAriaCurrent()}
            {...(!hasCheck && { disabled: isDisabled })}
            {...(!hasCheck && { role: 'menuitem' })}
            ref={innerRef}
            {...(!hasCheck && {
              onClick: (event: any) => {
                onItemSelect(event, onSelect);
                _drill && _drill();
              }
            })}
            {...(hasCheck && { htmlFor: randomId })}
            {...additionalProps}
          >
            <span className={css(styles.menuItemMain)}>
              {direction === 'up' && (
                <span className={css(styles.menuItemToggleIcon)}>
                  <AngleLeftIcon aria-hidden />
                </span>
              )}
              {icon && <span className={css(styles.menuItemIcon)}>{icon}</span>}
              {hasCheck && (
                <span className={css('pf-c-menu__item-check')}>
                  <Checkbox
                    id={randomId}
                    component="span"
                    isChecked={isSelected || false}
                    onChange={event => onItemSelect(event, onSelect)}
                    isDisabled={isDisabled}
                  />
                </span>
              )}
              <span className={css(styles.menuItemText)}>{children}</span>
              {isExternalLink && (
                <span className={css(styles.menuItemExternalIcon)}>
                  <ExternalLinkAltIcon aria-hidden />
                </span>
              )}
              {(flyoutMenu || direction === 'down') && (
                <span className={css(styles.menuItemToggleIcon)}>
                  <AngleRightIcon aria-hidden />
                </span>
              )}
              {getIsSelected() && (
                <span className={css(styles.menuItemSelectIcon)}>
                  <CheckIcon aria-hidden />
                </span>
              )}
            </span>
            {description && direction !== 'up' && (
              <span className={css(styles.menuItemDescription)}>
                <span>{description}</span>
              </span>
            )}
          </Component>
        )}
      </GenerateId>
      {flyoutVisible && (
        <MenuContext.Provider value={{ disableHover }}>
          <FlyoutContext.Provider value={{ direction: flyoutXDirection }}>{flyoutMenu}</FlyoutContext.Provider>
        </MenuContext.Provider>
      )}
      {typeof drilldownMenu === 'function' ? drilldownMenu() : drilldownMenu}
      <MenuItemContext.Provider value={{ itemId, isDisabled }}>
        {actions}
        {isFavorited !== null && (
          <MenuItemAction
            icon="favorites"
            isFavorited={isFavorited}
            aria-label={isFavorited ? 'starred' : 'not starred'}
            onClick={event => onActionClick(event, itemId)}
            tabIndex={-1}
            actionId="fav"
          />
        )}
      </MenuItemContext.Provider>
    </li>
  );
};

export const MenuItem = React.forwardRef((props: MenuItemProps, ref: React.Ref<any>) => (
  <MenuItemBase {...props} innerRef={ref} />
));

MenuItem.displayName = 'MenuItem';
