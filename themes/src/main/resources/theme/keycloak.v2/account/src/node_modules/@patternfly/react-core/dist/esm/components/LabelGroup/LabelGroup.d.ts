import * as React from 'react';
import { TooltipPosition } from '../Tooltip';
export interface LabelGroupProps extends React.HTMLProps<HTMLUListElement> {
    /** Content rendered inside the label group. Should be <Label> elements. */
    children?: React.ReactNode;
    /** Additional classes added to the label item */
    className?: string;
    /** Flag for having the label group default to expanded */
    defaultIsOpen?: boolean;
    /** Customizable "Show Less" text string */
    expandedText?: string;
    /** Customizeable template string. Use variable "${remaining}" for the overflow label count. */
    collapsedText?: string;
    /** Category name text for the label group category.  If this prop is supplied the label group with have a label and category styling applied */
    categoryName?: string;
    /** Aria label for label group that does not have a category name */
    'aria-label'?: string;
    /** Set number of labels to show before overflow */
    numLabels?: number;
    /** Flag if label group can be closed */
    isClosable?: boolean;
    /** Flag indicating the labels in the group are compact */
    isCompact?: boolean;
    /** Aria label for close button */
    closeBtnAriaLabel?: string;
    /** Function that is called when clicking on the label group close button */
    onClick?: (event: React.MouseEvent) => void;
    /** Position of the tooltip which is displayed if the category name text is longer */
    tooltipPosition?: TooltipPosition | 'auto' | 'top' | 'bottom' | 'left' | 'right' | 'top-start' | 'top-end' | 'bottom-start' | 'bottom-end' | 'left-start' | 'left-end' | 'right-start' | 'right-end';
    /** Flag to implement a vertical layout */
    isVertical?: boolean;
    /** @beta Flag indicating contained labels are editable. Allows spacing for a text input after the labels. */
    isEditable?: boolean;
    /** @beta Flag indicating the editable label group should be appended with a textarea. */
    hasEditableTextArea?: boolean;
    /** @beta Additional props passed to the editable textarea. */
    editableTextAreaProps?: any;
    /** @beta Control for adding new labels */
    addLabelControl?: React.ReactNode;
}
interface LabelGroupState {
    isOpen: boolean;
    isTooltipVisible: boolean;
}
export declare class LabelGroup extends React.Component<LabelGroupProps, LabelGroupState> {
    static displayName: string;
    constructor(props: LabelGroupProps);
    private headingRef;
    static defaultProps: LabelGroupProps;
    componentDidMount(): void;
    toggleCollapse: () => void;
    renderLabel(id: string): JSX.Element;
    render(): JSX.Element;
}
export {};
//# sourceMappingURL=LabelGroup.d.ts.map