import * as React from 'react';
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
export declare const MenuItemAction: React.ForwardRefExoticComponent<MenuItemActionProps & React.RefAttributes<HTMLButtonElement>>;
//# sourceMappingURL=MenuItemAction.d.ts.map