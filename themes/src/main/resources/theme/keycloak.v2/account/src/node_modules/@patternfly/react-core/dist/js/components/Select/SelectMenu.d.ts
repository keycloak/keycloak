import * as React from 'react';
import { SelectOptionObject } from './SelectOption';
import { SelectPosition } from './selectConstants';
export interface SelectMenuProps extends Omit<React.HTMLProps<HTMLElement>, 'checked' | 'selected' | 'ref'> {
    /** Content rendered inside the SelectMenu */
    children: React.ReactElement[] | React.ReactNode;
    /** Flag indicating that the children is custom content to render inside the SelectMenu.  If true, variant prop is ignored. */
    isCustomContent?: boolean;
    /** Additional classes added to the SelectMenu control */
    className?: string;
    /** Flag indicating the Select is expanded */
    isExpanded?: boolean;
    /** Flag indicating the Select options are grouped */
    isGrouped?: boolean;
    /** Currently selected option (for single, typeahead variants) */
    selected?: string | SelectOptionObject | (string | SelectOptionObject)[];
    /** Currently checked options (for checkbox variant) */
    checked?: (string | SelectOptionObject)[];
    /** @hide Internal flag for specifiying how the menu was opened */
    openedOnEnter?: boolean;
    /** Flag to specify the  maximum height of the menu, as a string percentage or number of pixels */
    maxHeight?: string | number;
    /** Indicates where menu will be alligned horizontally */
    position?: SelectPosition | 'right' | 'left';
    /** Inner prop passed from parent */
    noResultsFoundText?: string;
    /** Inner prop passed from parent */
    createText?: string;
    /** @hide Internal callback for ref tracking */
    sendRef?: (ref: React.ReactNode, favoriteRef: React.ReactNode, index: number) => void;
    /** @hide Internal callback for keyboard navigation */
    keyHandler?: (index: number, innerIndex: number, position: string) => void;
    /** Flag indicating select has an inline text input for filtering */
    hasInlineFilter?: boolean;
    innerRef?: any;
    /** Content rendered in the footer of the select menu */
    footer?: React.ReactNode;
    /** The menu footer element */
    footerRef?: React.RefObject<HTMLDivElement>;
    /** @hide callback to check if option is the last one in the menu when there is a footer  */
    isLastOptionBeforeFooter?: (index: number) => void;
}
export declare const SelectMenu: React.ForwardRefExoticComponent<{
    children?: React.ReactNode;
} & React.RefAttributes<unknown>>;
//# sourceMappingURL=SelectMenu.d.ts.map