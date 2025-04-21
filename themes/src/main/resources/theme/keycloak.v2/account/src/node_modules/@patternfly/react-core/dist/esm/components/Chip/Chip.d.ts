import * as React from 'react';
import { TooltipPosition } from '../Tooltip';
import { OUIAProps } from '../../helpers';
export interface ChipProps extends React.HTMLProps<HTMLDivElement>, OUIAProps {
    /** Content rendered inside the chip text */
    children?: React.ReactNode;
    /** Aria Label for close button */
    closeBtnAriaLabel?: string;
    /** Additional classes added to the chip item */
    className?: string;
    /** Flag indicating if the chip is an overflow chip */
    isOverflowChip?: boolean;
    /** Flag indicating if chip is read only */
    isReadOnly?: boolean;
    /** Function that is called when clicking on the chip close button */
    onClick?: (event: React.MouseEvent) => void;
    /** Component that will be used for chip. It is recommended that <button> or <li>  are used when the chip is an overflow chip. */
    component?: React.ReactNode;
    /** Position of the tooltip which is displayed if text is longer */
    tooltipPosition?: TooltipPosition | 'auto' | 'top' | 'bottom' | 'left' | 'right' | 'top-start' | 'top-end' | 'bottom-start' | 'bottom-end' | 'left-start' | 'left-end' | 'right-start' | 'right-end';
    /** Css property expressed in percentage or any css unit that overrides the default value of the max-width of the chip's text */
    textMaxWidth?: string;
}
interface ChipState {
    isTooltipVisible: boolean;
    ouiaStateId: string;
}
export declare class Chip extends React.Component<ChipProps, ChipState> {
    static displayName: string;
    constructor(props: ChipProps);
    span: React.RefObject<HTMLSpanElement>;
    static defaultProps: ChipProps;
    componentDidMount(): void;
    setChipStyle: () => {
        '--pf-c-chip__text--MaxWidth': string;
    };
    renderOverflowChip: () => JSX.Element;
    renderInnerChip(id: string): JSX.Element;
    renderChip: (randomId: string) => JSX.Element;
    render(): JSX.Element;
}
export {};
//# sourceMappingURL=Chip.d.ts.map