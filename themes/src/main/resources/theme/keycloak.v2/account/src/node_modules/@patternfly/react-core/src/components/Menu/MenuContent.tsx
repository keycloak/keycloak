import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Menu/menu';
import { css } from '@patternfly/react-styles';
import { MenuContext } from './MenuContext';

export interface MenuContentProps extends React.HTMLProps<HTMLElement> {
  /** Items within group */
  children?: React.ReactNode;
  /** Forwarded ref */
  innerRef?: React.Ref<any>;
  /** Height of the menu content */
  menuHeight?: string;
  /** Maximum height of menu content */
  maxMenuHeight?: string;
  /** Callback to return the height of the menu content */
  getHeight?: (height: string) => void;
}

export const MenuContent = React.forwardRef((props: MenuContentProps, ref: React.Ref<HTMLDivElement>) => {
  const { getHeight, children, menuHeight, maxMenuHeight, ...rest } = props;
  const menuContentRef = React.createRef<HTMLDivElement>();
  const refCallback = (el: HTMLElement, menuId: string, onGetMenuHeight: (menuId: string, height: number) => void) => {
    if (el) {
      let clientHeight = el.clientHeight;

      // if this menu is a submenu, we need to account for the root menu list's padding and root menu content's border.
      let rootMenuList = null;
      let parentEl = el.closest(`.${styles.menuList}`);
      while (parentEl !== null && parentEl.nodeType === 1) {
        if (parentEl.classList.contains(styles.menuList)) {
          rootMenuList = parentEl;
        }
        parentEl = parentEl.parentElement;
      }

      if (rootMenuList) {
        const rootMenuListStyles = getComputedStyle(rootMenuList);
        const rootMenuListPaddingOffset =
          parseFloat(rootMenuListStyles.getPropertyValue('padding-top').replace(/px/g, '')) +
          parseFloat(rootMenuListStyles.getPropertyValue('padding-bottom').replace(/px/g, '')) +
          parseFloat(
            getComputedStyle(rootMenuList.parentElement)
              .getPropertyValue('border-bottom-width')
              .replace(/px/g, '')
          );
        clientHeight = clientHeight + rootMenuListPaddingOffset;
      }

      onGetMenuHeight && onGetMenuHeight(menuId, clientHeight);
      getHeight && getHeight(clientHeight.toString());
    }
    return ref || menuContentRef;
  };
  return (
    <MenuContext.Consumer>
      {({ menuId, onGetMenuHeight }) => (
        <div
          {...rest}
          className={css(styles.menuContent, props.className)}
          ref={el => refCallback(el, menuId, onGetMenuHeight)}
          style={
            {
              ...(menuHeight && { '--pf-c-menu__content--Height': menuHeight }),
              ...(maxMenuHeight && { '--pf-c-menu__content--MaxHeight': maxMenuHeight })
            } as React.CSSProperties
          }
        >
          {children}
        </div>
      )}
    </MenuContext.Consumer>
  );
});
MenuContent.displayName = 'MenuContent';
