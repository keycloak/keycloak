import * as React from 'react';
import { ButtonProps } from '../Button';
export interface ChipButtonProps extends ButtonProps {
    /** Aria label for chip button */
    ariaLabel?: string;
    /** Content rendered inside the chip item */
    children?: React.ReactNode;
    /** Additional classes added to the chip item */
    className?: string;
    /** Function that is called when clicking on the chip button */
    onClick?: (event: React.MouseEvent) => void;
}
export declare const ChipButton: React.FunctionComponent<ChipButtonProps>;
