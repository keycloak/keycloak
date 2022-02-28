import * as React from 'react';
export declare const ChipGroupContext: React.Context<string>;
export interface ChipGroupProps extends React.HTMLProps<HTMLDivElement> {
    /** Content rendered inside the chip text */
    children?: React.ReactNode;
    /** Additional classes added to the chip item */
    className?: string;
    /** Flag for having the chip group default to expanded */
    defaultIsOpen?: boolean;
    /** Customizable "Show Less" text string */
    expandedText?: string;
    /** Customizeable template string. Use variable "${remaining}" for the overflow chip count. */
    collapsedText?: string;
    /** Flag for grouping with a toolbar & category name */
    withToolbar?: boolean;
    /** Set heading level to the chip item label */
    headingLevel?: 'h1' | 'h2' | 'h3' | 'h4' | 'h5' | 'h6';
    /** Set number of chips to show before overflow */
    numChips?: number;
}
interface ChipGroupState {
    isOpen: boolean;
}
export declare class ChipGroup extends React.Component<ChipGroupProps, ChipGroupState> {
    constructor(props: ChipGroupProps);
    static defaultProps: ChipGroupProps;
    toggleCollapse: () => void;
    renderToolbarGroup(): JSX.Element;
    renderChipGroup(): JSX.Element;
    render(): JSX.Element;
}
export {};
