import * as React from 'react';
export interface AboutModalContainerProps extends React.HTMLProps<HTMLDivElement> {
    /** content rendered inside the About Modal Box Content.  */
    children: React.ReactNode;
    /** additional classes added to the About Modal Box  */
    className?: string;
    /** Flag to show the About Modal  */
    isOpen?: boolean;
    /** A callback for when the close button is clicked  */
    onClose?: () => void;
    /** Product Name  */
    productName?: string;
    /** Trademark information  */
    trademark?: string;
    /** the URL of the image for the Brand.  */
    brandImageSrc: string;
    /** the alternate text of the Brand image.  */
    brandImageAlt: string;
    /** the URL of the image for the background.  */
    backgroundImageSrc?: string;
    /** id to use for About Modal Box aria labeled by  */
    aboutModalBoxHeaderId: string;
    /** id to use for About Modal Box aria described by  */
    aboutModalBoxContentId: string;
    /** Set close button aria label */
    closeButtonAriaLabel?: string;
    /** Flag to disable focus trap */
    disableFocusTrap?: boolean;
}
export declare const AboutModalContainer: React.FunctionComponent<AboutModalContainerProps>;
//# sourceMappingURL=AboutModalContainer.d.ts.map