import * as React from 'react';
export interface AboutModalBoxCloseButtonProps extends React.HTMLProps<HTMLDivElement> {
    /** additional classes added to the About Modal Close button  */
    className?: string;
    /** A callback for when the close button is clicked  */
    onClose?: () => void;
    /** Set close button aria label */
    'aria-label'?: string;
}
export declare const AboutModalBoxCloseButton: React.FunctionComponent<AboutModalBoxCloseButtonProps>;
//# sourceMappingURL=AboutModalBoxCloseButton.d.ts.map