import * as React from 'react';
import { PickOptional } from '../../helpers/typeUtils';
export interface AboutModalProps {
    /** Content rendered inside the about modal */
    children: React.ReactNode;
    /** Additional classes added to the about modal */
    className?: string;
    /** Flag to show the about modal  */
    isOpen?: boolean;
    /** A callback for when the close button is clicked  */
    onClose?: () => void;
    /** Product name  */
    productName?: string;
    /** Trademark information  */
    trademark?: string;
    /** The URL of the image for the brand  */
    brandImageSrc: string;
    /** The alternate text of the brand image  */
    brandImageAlt: string;
    /** The URL of the image for the background  */
    backgroundImageSrc?: string;
    /** Prevents the about modal from rendering content inside a container; allows for more flexible layouts  */
    noAboutModalBoxContentContainer?: boolean;
    /** The parent container to append the modal to. Defaults to document.body */
    appendTo?: HTMLElement | (() => HTMLElement);
    /** Set aria label to the close button */
    closeButtonAriaLabel?: string;
    /** Flag to disable focus trap */
    disableFocusTrap?: boolean;
}
interface ModalState {
    container: HTMLElement;
}
export declare class AboutModal extends React.Component<AboutModalProps, ModalState> {
    static displayName: string;
    private static currentId;
    private id;
    ariaLabelledBy: string;
    ariaDescribedBy: string;
    static defaultProps: PickOptional<AboutModalProps>;
    constructor(props: AboutModalProps);
    handleEscKeyClick: (event: KeyboardEvent) => void;
    toggleSiblingsFromScreenReaders: (hide: boolean) => void;
    getElement: (appendTo: HTMLElement | (() => HTMLElement)) => HTMLElement;
    componentDidMount(): void;
    componentDidUpdate(): void;
    componentWillUnmount(): void;
    render(): React.ReactElement<any, string | React.JSXElementConstructor<any>>;
}
export {};
//# sourceMappingURL=AboutModal.d.ts.map