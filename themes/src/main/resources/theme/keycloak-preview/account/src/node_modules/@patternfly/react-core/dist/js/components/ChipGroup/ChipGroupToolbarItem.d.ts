import * as React from 'react';
export interface ChipGroupToolbarItemProps extends React.HTMLProps<HTMLUListElement> {
    /**  Category name text */
    categoryName?: string;
    /** Content rendered inside the chip text */
    children: React.ReactNode;
    /** Additional classes added to the chip item */
    className?: string;
    /** Flag if chip group can be closed*/
    isClosable?: boolean;
    /** Function that is called when clicking on the chip group button */
    onClick?: (event: React.MouseEvent) => void;
    /** Aria label for close button */
    closeBtnAriaLabel?: string;
    /** Position of the tooltip which is displayed if the category name text is longer */
    tooltipPosition?: 'auto' | 'top' | 'bottom' | 'left' | 'right';
}
interface ChipGroupToolbarItemState {
    isTooltipVisible: boolean;
}
export declare class ChipGroupToolbarItem extends React.Component<ChipGroupToolbarItemProps, ChipGroupToolbarItemState> {
    constructor(props: ChipGroupToolbarItemProps);
    heading: React.RefObject<HTMLHeadingElement>;
    static defaultProps: ChipGroupToolbarItemProps;
    componentDidMount(): void;
    render(): JSX.Element;
}
export {};
