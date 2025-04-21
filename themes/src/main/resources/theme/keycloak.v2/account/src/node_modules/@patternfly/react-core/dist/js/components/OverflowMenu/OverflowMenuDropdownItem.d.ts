import * as React from 'react';
import { DropdownItemProps } from '../Dropdown';
export interface OverflowMenuDropdownItemProps extends DropdownItemProps {
    /** Indicates when a dropdown item shows and hides the corresponding list item */
    isShared?: boolean;
    /** Indicates the index of the list item */
    index?: number;
}
export declare const OverflowMenuDropdownItem: React.FunctionComponent<OverflowMenuDropdownItemProps>;
//# sourceMappingURL=OverflowMenuDropdownItem.d.ts.map