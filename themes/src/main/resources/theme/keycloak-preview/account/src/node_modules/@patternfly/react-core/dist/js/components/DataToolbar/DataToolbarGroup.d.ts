import * as React from 'react';
import { DataToolbarBreakpointMod } from './DataToolbarUtils';
import { RefObject } from 'react';
export declare enum DataToolbarGroupVariant {
    'filter-group' = "filter-group",
    'icon-button-group' = "icon-button-group",
    'button-group' = "button-group"
}
export interface DataToolbarGroupProps extends Omit<React.HTMLProps<HTMLDivElement>, 'ref'> {
    /** Classes applied to root element of the data toolbar group */
    className?: string;
    /** A type modifier which modifies spacing specifically depending on the type of group */
    variant?: DataToolbarGroupVariant | 'filter-group' | 'icon-button-group' | 'button-group';
    /** Array of objects representing the various modifiers to apply to the data toolbar group at various breakpoints */
    breakpointMods?: DataToolbarBreakpointMod[];
    /** Content to be rendered inside the data toolbar group */
    children?: React.ReactNode;
    /** Reference to pass to this group if it has .pf-m-chip-container modifier */
    innerRef?: RefObject<any>;
}
export declare const DataToolbarGroup: React.ForwardRefExoticComponent<DataToolbarGroupProps & React.RefAttributes<unknown>>;
