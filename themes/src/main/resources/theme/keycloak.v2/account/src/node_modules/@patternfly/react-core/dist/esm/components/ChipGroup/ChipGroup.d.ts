import * as React from 'react';
import { TooltipPosition } from '../Tooltip';
import { OUIAProps } from '../../helpers';
export interface ChipGroupProps extends React.HTMLProps<HTMLUListElement>, OUIAProps {
    /** Content rendered inside the chip group. Should be <Chip> elements. */
    children?: React.ReactNode;
    /** Additional classes added to the chip item */
    className?: string;
    /** Flag for having the chip group default to expanded */
    defaultIsOpen?: boolean;
    /** Customizable "Show Less" text string */
    expandedText?: string;
    /** Customizeable template string. Use variable "${remaining}" for the overflow chip count. */
    collapsedText?: string;
    /** Category name text for the chip group category.  If this prop is supplied the chip group with have a label and category styling applied */
    categoryName?: string;
    /** Aria label for chip group that does not have a category name */
    'aria-label'?: string;
    /** Set number of chips to show before overflow */
    numChips?: number;
    /** Flag if chip group can be closed*/
    isClosable?: boolean;
    /** Aria label for close button */
    closeBtnAriaLabel?: string;
    /** Function that is called when clicking on the chip group close button */
    onClick?: (event: React.MouseEvent) => void;
    /** Function that is called when clicking on the overflow (expand/collapse) chip button */
    onOverflowChipClick?: (event: React.MouseEvent) => void;
    /** Position of the tooltip which is displayed if the category name text is longer */
    tooltipPosition?: TooltipPosition | 'auto' | 'top' | 'bottom' | 'left' | 'right' | 'top-start' | 'top-end' | 'bottom-start' | 'bottom-end' | 'left-start' | 'left-end' | 'right-start' | 'right-end';
}
interface ChipGroupState {
    isOpen: boolean;
    isTooltipVisible: boolean;
}
export declare class ChipGroup extends React.Component<ChipGroupProps, ChipGroupState> {
    static displayName: string;
    constructor(props: ChipGroupProps);
    private headingRef;
    static defaultProps: ChipGroupProps;
    componentDidMount(): void;
    toggleCollapse: () => void;
    renderLabel(id: string): JSX.Element;
    render(): JSX.Element;
}
export {};
//# sourceMappingURL=ChipGroup.d.ts.map