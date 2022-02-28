import * as React from 'react';
export interface ModalContentProps {
    /** Content rendered inside the Modal. */
    children: React.ReactNode;
    /** Additional classes added to the button */
    className?: string;
    /** Creates a large version of the Modal */
    isLarge?: boolean;
    /** Creates a small version of the Modal */
    isSmall?: boolean;
    /** Flag to show the modal */
    isOpen?: boolean;
    /** Complex header (more than just text), supersedes title for header content */
    header?: React.ReactNode;
    /** Description of the modal */
    description?: React.ReactNode;
    /** Simple text content of the Modal Header, also used for aria-label on the body */
    title: string;
    /** Flag to show the title (ignored for custom headers) */
    hideTitle?: boolean;
    /** Flag to show the close button in the header area of the modal */
    showClose?: boolean;
    /** Default width of the content. */
    width?: number | string;
    /** Custom footer */
    footer?: React.ReactNode;
    /** Action buttons to add to the standard Modal Footer, ignored if `footer` is given */
    actions?: any;
    /** Flag to indicate that the Footer content is left aligned */
    isFooterLeftAligned?: boolean;
    /** A callback for when the close button is clicked */
    onClose?: () => void;
    /** Id to use for Modal Box description */
    ariaDescribedById?: string;
    /** Id of the ModalBoxBody */
    id: string;
    /** Flag to disable focus trap */
    disableFocusTrap?: boolean;
}
export declare const ModalContent: React.FunctionComponent<ModalContentProps>;
