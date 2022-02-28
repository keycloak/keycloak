import * as React from 'react';
import { PickOptional } from '../../helpers/typeUtils';
import { PopoverPosition } from '../Popover';
export declare const clipboardCopyFunc: (event: React.ClipboardEvent<HTMLDivElement>, text?: React.ReactNode) => void;
export declare enum ClipboardCopyVariant {
    inline = "inline",
    expansion = "expansion"
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
    /** Custom flag to show that the input requires an associated id or aria-label. */
    textAriaLabel?: string;
    /** Custom flag to show that the toggle button requires an associated id or aria-label. */
    toggleAriaLabel?: string;
    /** Flag to show if the input is read only. */
    isReadOnly?: boolean;
    /** Flag to determine if clipboard copy is in the expanded state initially */
    isExpanded?: boolean;
    /** Flag to determine if clipboard copy content includes code */
    isCode?: boolean;
    /** Adds Clipboard Copy variant styles. */
    variant?: typeof ClipboardCopyVariant | 'inline' | 'expansion';
    /** Copy button popover position. */
    position?: PopoverPosition | 'auto' | 'top' | 'bottom' | 'left' | 'right';
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
}
export declare class ClipboardCopy extends React.Component<ClipboardCopyProps, ClipboardCopyState> {
    timer: number;
    constructor(props: ClipboardCopyProps);
    static defaultProps: PickOptional<ClipboardCopyProps>;
    componentDidUpdate: (prevProps: ClipboardCopyProps, prevState: ClipboardCopyState) => void;
    expandContent: (_event: React.MouseEvent<Element, MouseEvent>) => void;
    updateText: (text: React.ReactText) => void;
    render: () => JSX.Element;
}
