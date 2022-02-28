/// <reference types="react" />
import { ButtonProps } from '../Button';
interface AlertActionCloseButtonProps extends ButtonProps {
    /** Additional classes added to the AlertActionCloseButton */
    className?: string;
    /** A callback for when the close button is clicked */
    onClose?: () => void;
    /** Aria Label for the Close button */
    'aria-label'?: string;
    /** Variant Label for the Close button */
    variantLabel?: string;
}
export declare const AlertActionCloseButton: ({ className, onClose, "aria-label": ariaLabel, variantLabel, ...props }: AlertActionCloseButtonProps) => JSX.Element;
export {};
