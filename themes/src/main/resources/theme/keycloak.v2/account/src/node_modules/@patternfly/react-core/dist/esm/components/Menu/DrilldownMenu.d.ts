import React from 'react';
export interface DrilldownMenuProps extends Omit<React.HTMLAttributes<HTMLDivElement>, 'ref' | 'onSelect'> {
    /** Items within drilldown sub-menu */
    children?: React.ReactNode;
    /** ID of the drilldown sub-menu */
    id?: string;
    /** Flag indicating whether the menu is drilled in */
    isMenuDrilledIn?: boolean;
    /** Optional callback to get the height of the sub menu */
    getHeight?: (height: string) => void;
}
export declare const DrilldownMenu: React.FunctionComponent<DrilldownMenuProps>;
//# sourceMappingURL=DrilldownMenu.d.ts.map