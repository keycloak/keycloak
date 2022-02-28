import * as React from 'react';
import { PickOptional } from '../../helpers/typeUtils';
export interface ModalProps extends React.HTMLProps<HTMLDivElement> {
    /** Content rendered inside the Modal. */
    children: React.ReactNode;
    /** Additional classes added to the Modal */
    className?: string;
    /** Flag to show the modal */
    isOpen?: boolean;
    /** Complex header (more than just text), supersedes title for header content */
    header?: React.ReactNode;
    /** Simple text content of the Modal Header, also used for aria-label on the body */
    title: string;
    /** Flag to hide the title */
    hideTitle?: boolean;
    /** Flag to show the close button in the header area of the modal */
    showClose?: boolean;
    /** Id to use for Modal Box description */
    ariaDescribedById?: string;
    /** Custom footer */
    footer?: React.ReactNode;
    /** Action buttons to add to the standard Modal Footer, ignored if `footer` is given */
    actions?: any;
    /** Flag to indicate that the Footer content is left aligned */
    isFooterLeftAligned?: boolean;
    /** A callback for when the close button is clicked */
    onClose?: () => void;
    /** Default width of the Modal. */
    width?: number | string;
    /** Creates a large version of the Modal */
    isLarge?: boolean;
    /** Creates a small version of the Modal */
    isSmall?: boolean;
    /** The parent container to append the modal to. Defaults to document.body */
    appendTo?: HTMLElement | (() => HTMLElement);
    /** Flag to disable focus trap */
    disableFocusTrap?: boolean;
    /** Description of the modal */
    description?: React.ReactNode;
}
interface ModalState {
    container: HTMLElement;
}
export declare class Modal extends React.Component<ModalProps, ModalState> {
    static currentId: number;
    id: string;
    static defaultProps: PickOptional<ModalProps>;
    constructor(props: ModalProps);
    handleEscKeyClick: (event: KeyboardEvent) => void;
    getElement: (appendTo: HTMLElement | (() => HTMLElement)) => HTMLElement;
    toggleSiblingsFromScreenReaders: (hide: boolean) => void;
    componentDidMount(): void;
    componentDidUpdate(): void;
    componentWillUnmount(): void;
    render(): React.ReactPortal;
}
export {};
