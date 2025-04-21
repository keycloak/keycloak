import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Menu/menu';
import { css } from '@patternfly/react-styles';
import StarIcon from '@patternfly/react-icons/dist/esm/icons/star-icon';
import { MenuContext, MenuItemContext } from './MenuContext';

export interface MenuItemActionProps extends Omit<React.HTMLProps<HTMLButtonElement>, 'type' | 'ref'> {
  /** Additional classes added to the action button */
  className?: string;
  /** The action icon to use */
  icon?: 'favorites' | React.ReactNode;
  /** Callback on action click, can also specify onActionClick on the Menu instead */
  onClick?: (event?: any) => void;
  /** Accessibility label */
  'aria-label'?: string;
  /** Flag indicating if the item is favorited */
  isFavorited?: boolean;
  /** Disables action, can also be specified on the MenuItem instead */
  isDisabled?: boolean;
  /** Identifies the action item in the onActionClick on the Menu */
  actionId?: any;
  /** Forwarded ref */
  innerRef?: React.Ref<any>;
}

const MenuItemActionBase: React.FunctionComponent<MenuItemActionProps> = ({
  className = '',
  icon,
  onClick,
  'aria-label': ariaLabel,
  isFavorited = null,
  isDisabled,
  actionId,
  innerRef,
  ...props
}: MenuItemActionProps) => (
  <MenuContext.Consumer>
    {({ onActionClick }) => (
      <MenuItemContext.Consumer>
        {({ itemId, isDisabled: isDisabledContext }) => {
          const onClickButton = (event: any) => {
            // event specified on the MenuItemAction
            onClick && onClick(event);
            // event specified on the Menu
            onActionClick && onActionClick(event, itemId, actionId);
          };
          return (
            <button
              className={css(
                styles.menuItemAction,
                isFavorited !== null && styles.modifiers.favorite,
                isFavorited && styles.modifiers.favorited,
                className
              )}
              aria-label={ariaLabel}
              onClick={onClickButton}
              {...((isDisabled === true || isDisabledContext === true) && { disabled: true })}
              ref={innerRef}
              tabIndex={-1}
              {...props}
            >
              <span className={css(styles.menuItemActionIcon)}>
                {icon === 'favorites' || isFavorited !== null ? <StarIcon aria-hidden /> : icon}
              </span>
            </button>
          );
        }}
      </MenuItemContext.Consumer>
    )}
  </MenuContext.Consumer>
);

export const MenuItemAction = React.forwardRef((props: MenuItemActionProps, ref: React.Ref<HTMLButtonElement>) => (
  <MenuItemActionBase {...props} innerRef={ref} />
));
MenuItemAction.displayName = 'MenuItemAction';
