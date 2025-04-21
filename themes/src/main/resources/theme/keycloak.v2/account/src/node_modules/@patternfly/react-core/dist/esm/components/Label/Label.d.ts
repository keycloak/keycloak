import * as React from 'react';
import { TooltipPosition } from '../Tooltip';
export interface LabelProps extends React.HTMLProps<HTMLSpanElement> {
    /** Content rendered inside the label. */
    children?: React.ReactNode;
    /** Additional classes added to the label. */
    className?: string;
    /** Color of the label. */
    color?: 'blue' | 'cyan' | 'green' | 'orange' | 'purple' | 'red' | 'grey' | 'gold';
    /** Variant of the label. */
    variant?: 'outline' | 'filled';
    /** Flag indicating the label is compact. */
    isCompact?: boolean;
    /** @beta Flag indicating the label is editable. */
    isEditable?: boolean;
    /** @beta Additional props passed to the editable label text div. Optionally passing onInput and onBlur callbacks will allow finer custom text input control. */
    editableProps?: any;
    /** @beta Callback when an editable label completes an edit. */
    onEditComplete?: (newText: string) => void;
    /** @beta Callback when an editable label cancels an edit. */
    onEditCancel?: (previousText: string) => void;
    /** Flag indicating the label text should be truncated. */
    isTruncated?: boolean;
    /** Position of the tooltip which is displayed if text is truncated */
    tooltipPosition?: TooltipPosition | 'auto' | 'top' | 'bottom' | 'left' | 'right' | 'top-start' | 'top-end' | 'bottom-start' | 'bottom-end' | 'left-start' | 'left-end' | 'right-start' | 'right-end';
    /** Icon added to the left of the label text. */
    icon?: React.ReactNode;
    /** Close click callback for removable labels. If present, label will have a close button. */
    onClose?: (event: React.MouseEvent) => void;
    /** Node for custom close button. */
    closeBtn?: React.ReactNode;
    /** Aria label for close button */
    closeBtnAriaLabel?: string;
    /** Additional properties for the default close button. */
    closeBtnProps?: any;
    /** Href for a label that is a link. If present, the label will change to an anchor element. */
    href?: string;
    /** Flag indicating if the label is an overflow label */
    isOverflowLabel?: boolean;
    /** Forwards the label content and className to rendered function.  Use this prop for react router support.*/
    render?: ({ className, content, componentRef }: {
        className: string;
        content: React.ReactNode;
        componentRef: any;
    }) => React.ReactNode;
}
export declare const Label: React.FunctionComponent<LabelProps>;
//# sourceMappingURL=Label.d.ts.map