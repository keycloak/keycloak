import * as React from 'react';
import { OUIAProps } from '../../helpers';
export interface ModalContentProps extends OUIAProps {
    /** Content rendered inside the Modal. */
    children: React.ReactNode;
    /** Additional classes added to the button */
    className?: string;
    /** Variant of the modal */
    variant?: 'small' | 'medium' | 'large' | 'default';
    /** Alternate position of the modal */
    position?: 'top';
    /** Offset from alternate position. Can be any valid CSS length/percentage */
    positionOffset?: string;
    /** Flag to show the modal */
    isOpen?: boolean;
    /** Complex header (more than just text), supersedes title for header content */
    header?: React.ReactNode;
    /** Optional help section for the Modal Header */
    help?: React.ReactNode;
    /** Description of the modal */
    description?: React.ReactNode;
    /** Simple text content of the Modal Header, also used for aria-label on the body */
    title?: string;
    /** Optional alert icon (or other) to show before the title of the Modal Header
     * When the predefined alert types are used the default styling
     * will be automatically applied */
    titleIconVariant?: 'success' | 'danger' | 'warning' | 'info' | 'default' | React.ComponentType<any>;
    /** Optional title label text for screen readers */
    titleLabel?: string;
    /** Id of Modal Box label */
    'aria-labelledby'?: string | null;
    /** Accessible descriptor of modal */
    'aria-label'?: string;
    /** Id of Modal Box description */
    'aria-describedby'?: string;
    /** Accessible label applied to the modal box body. This should be used to communicate important information about the modal box body div if needed, such as that it is scrollable */
    bodyAriaLabel?: string;
    /** Accessible role applied to the modal box body. This will default to region if a body aria label is applied. Set to a more appropriate role as applicable based on the modal content and context */
    bodyAriaRole?: string;
    /** Flag to show the close button in the header area of the modal */
    showClose?: boolean;
    /** Default width of the content. */
    width?: number | string;
    /** Custom footer */
    footer?: React.ReactNode;
    /** Action buttons to add to the standard Modal Footer, ignored if `footer` is given */
    actions?: any;
    /** A callback for when the close button is clicked */
    onClose?: () => void;
    /** Id of the ModalBox container */
    boxId: string;
    /** Id of the ModalBox title */
    labelId: string;
    /** Id of the ModalBoxBody */
    descriptorId: string;
    /** Flag to disable focus trap */
    disableFocusTrap?: boolean;
    /** Flag indicating if modal content should be placed in a modal box body wrapper */
    hasNoBodyWrapper?: boolean;
}
export declare const ModalContent: React.FunctionComponent<ModalContentProps>;
//# sourceMappingURL=ModalContent.d.ts.map