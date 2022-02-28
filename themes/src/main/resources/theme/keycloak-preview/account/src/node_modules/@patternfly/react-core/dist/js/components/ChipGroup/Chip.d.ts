import * as React from 'react';
import { InjectedOuiaProps } from '../withOuia';
export interface ChipProps extends React.HTMLProps<HTMLDivElement> {
    /** Content rendered inside the chip text */
    children?: React.ReactNode;
    /** Aria Label for close button */
    closeBtnAriaLabel?: string;
    /** Additional classes added to the chip item */
    className?: string;
    /** Flag indicating if the chip has overflow */
    isOverflowChip?: boolean;
    /** Flag if chip is read only */
    isReadOnly?: boolean;
    /** Function that is called when clicking on the chip button */
    onClick?: (event: React.MouseEvent) => void;
    /** Internal flag for which component will be used for chip */
    component?: React.ReactNode;
    /** Position of the tooltip which is displayed if text is longer */
    tooltipPosition?: 'auto' | 'top' | 'bottom' | 'left' | 'right';
}
declare const ChipWithOuiaContext: React.FunctionComponent<ChipProps & InjectedOuiaProps>;
export { ChipWithOuiaContext as Chip };
