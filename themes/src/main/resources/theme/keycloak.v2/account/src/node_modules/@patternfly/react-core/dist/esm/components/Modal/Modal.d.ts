import * as React from 'react';
import { PickOptional } from '../../helpers';
import { OUIAProps } from '../../helpers';
export interface ModalProps extends React.HTMLProps<HTMLDivElement>, OUIAProps {
    /** Content rendered inside the Modal. */
    children: React.ReactNode;
    /** Additional classes added to the Modal */
    className?: string;
    /** Flag to show the modal */
    isOpen?: boolean;
    /** Complex header (more than just text), supersedes title for header content */
    header?: React.ReactNode;
    /** Optional help section for the Modal Header */
    help?: React.ReactNode;
    /** Simple text content of the Modal Header, also used for aria-label on the body */
    title?: string;
    /** Optional alert icon (or other) to show before the title of the Modal Header
     * When the predefined alert types are used the default styling
     * will be automatically applied */
    titleIconVariant?: 'success' | 'danger' | 'warning' | 'info' | 'default' | React.ComponentType<any>;
    /** Optional title label text for screen readers */
    titleLabel?: string;
    /** Id to use for Modal Box label */
    'aria-labelledby'?: string | null;
    /** Accessible descriptor of modal */
    'aria-label'?: string;
    /** Id to use for Modal Box descriptor */
    'aria-describedby'?: string;
    /** Accessible label applied to the modal box body. This should be used to communicate important information about the modal box body div if needed, such as that it is scrollable */
    bodyAriaLabel?: string;
    /** Accessible role applied to the modal box body. This will default to region if a body aria label is applied. Set to a more appropriate role as applicable based on the modal content and context */
    bodyAriaRole?: string;
    /** Flag to show the close button in the header area of the modal */
    showClose?: boolean;
    /** Custom footer */
    footer?: React.ReactNode;
    /** Action buttons to add to the standard Modal Footer, ignored if `footer` is given */
    actions?: any;
    /** A callback for when the close button is clicked */
    onClose?: () => void;
    /** Default width of the Modal. */
    width?: number | string;
    /** The parent container to append the modal to. Defaults to document.body */
    appendTo?: HTMLElement | (() => HTMLElement);
    /** Flag to disable focus trap */
    disableFocusTrap?: boolean;
    /** Description of the modal */
    description?: React.ReactNode;
    /** Variant of the modal */
    variant?: 'small' | 'medium' | 'large' | 'default';
    /** Alternate position of the modal */
    position?: 'top';
    /** Offset from alternate position. Can be any valid CSS length/percentage */
    positionOffset?: string;
    /** Flag indicating if modal content should be placed in a modal box body wrapper */
    hasNoBodyWrapper?: boolean;
    /** An ID to use for the ModalBox container */
    id?: string;
    /** Modal handles pressing of the Escape key and closes the modal. If you want to handle this yourself you can use this callback function */
    onEscapePress?: (event: KeyboardEvent) => void;
}
export declare enum ModalVariant {
    small = "small",
    medium = "medium",
    large = "large",
    default = "default"
}
interface ModalState {
    container: HTMLElement;
    ouiaStateId: string;
}
export declare class Modal extends React.Component<ModalProps, ModalState> {
    static displayName: string;
    static currentId: number;
    boxId: string;
    labelId: string;
    descriptorId: string;
    static defaultProps: PickOptional<ModalProps>;
    constructor(props: ModalProps);
    handleEscKeyClick: (event: KeyboardEvent) => void;
    getElement: (appendTo: HTMLElement | (() => HTMLElement)) => HTMLElement;
    toggleSiblingsFromScreenReaders: (hide: boolean) => void;
    isEmpty: (value: string | null) => boolean;
    componentDidMount(): void;
    componentDidUpdate(): void;
    componentWillUnmount(): void;
    render(): React.ReactElement<any, string | React.JSXElementConstructor<any>>;
}
export {};
//# sourceMappingURL=Modal.d.ts.map