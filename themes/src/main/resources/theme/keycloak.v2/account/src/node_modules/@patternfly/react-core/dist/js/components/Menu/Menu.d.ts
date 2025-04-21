import * as React from 'react';
import { OUIAProps } from '../../helpers';
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
    onSearchInputChange?: (event: React.FormEvent<HTMLInputElement> | React.SyntheticEvent<HTMLButtonElement>, value: string) => void;
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
export declare const Menu: React.ForwardRefExoticComponent<MenuProps & React.RefAttributes<HTMLDivElement>>;
//# sourceMappingURL=Menu.d.ts.map