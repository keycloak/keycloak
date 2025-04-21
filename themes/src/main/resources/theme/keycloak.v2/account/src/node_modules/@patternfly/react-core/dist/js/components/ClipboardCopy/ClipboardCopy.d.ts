import * as React from 'react';
import { PickOptional } from '../../helpers/typeUtils';
import { PopoverPosition } from '../Popover';
import { TooltipPosition } from '../Tooltip';
export declare const clipboardCopyFunc: (event: React.ClipboardEvent<HTMLDivElement>, text?: React.ReactNode) => void;
export declare enum ClipboardCopyVariant {
    inline = "inline",
    expansion = "expansion",
    inlineCompact = "inline-compact"
}
export interface ClipboardCopyState {
    text: string | number;
    expanded: boolean;
    copied: boolean;
}
export interface ClipboardCopyProps extends Omit<React.HTMLProps<HTMLDivElement>, 'onChange'> {
    /** Additional classes added to the clipboard copy container. */
    className?: string;
    /** Tooltip message to display when hover the copy button */
    hoverTip?: string;
    /** Tooltip message to display when clicking the copy button */
    clickTip?: string;
    /** Aria-label to use on the TextInput. */
    textAriaLabel?: string;
    /** Aria-label to use on the ClipboardCopyToggle. */
    toggleAriaLabel?: string;
    /** Flag to show if the input is read only. */
    isReadOnly?: boolean;
    /** Flag to determine if clipboard copy is in the expanded state initially */
    isExpanded?: boolean;
    /** Flag to determine if clipboard copy content includes code */
    isCode?: boolean;
    /** Flag to determine if inline clipboard copy should be block styling */
    isBlock?: boolean;
    /** Adds Clipboard Copy variant styles. */
    variant?: typeof ClipboardCopyVariant | 'inline' | 'expansion' | 'inline-compact';
    /** Copy button popover position. */
    position?: PopoverPosition | TooltipPosition | 'auto' | 'top' | 'bottom' | 'left' | 'right' | 'top-start' | 'top-end' | 'bottom-start' | 'bottom-end' | 'left-start' | 'left-end' | 'right-start' | 'right-end';
    /** Maximum width of the tooltip (default 150px). */
    maxWidth?: string;
    /** Delay in ms before the tooltip disappears. */
    exitDelay?: number;
    /** Delay in ms before the tooltip appears. */
    entryDelay?: number;
    /** Delay in ms before the tooltip message switch to hover tip. */
    switchDelay?: number;
    /** A function that is triggered on clicking the copy button. */
    onCopy?: (event: React.ClipboardEvent<HTMLDivElement>, text?: React.ReactNode) => void;
    /** A function that is triggered on changing the text. */
    onChange?: (text?: string | number) => void;
    /** The text which is copied. */
    children: React.ReactNode;
    /** Additional actions for inline clipboard copy. Should be wrapped with ClipboardCopyAction. */
    additionalActions?: React.ReactNode;
}
export declare class ClipboardCopy extends React.Component<ClipboardCopyProps, ClipboardCopyState> {
    static displayName: string;
    timer: number;
    constructor(props: ClipboardCopyProps);
    static defaultProps: PickOptional<ClipboardCopyProps>;
    componentDidUpdate: (prevProps: ClipboardCopyProps, prevState: ClipboardCopyState) => void;
    componentWillUnmount: () => void;
    expandContent: (_event: React.MouseEvent<Element, MouseEvent>) => void;
    updateText: (text: string | number) => void;
    render: () => JSX.Element;
}
//# sourceMappingURL=ClipboardCopy.d.ts.map