import * as React from 'react';
export interface AboutModalBoxContentProps extends React.HTMLProps<HTMLDivElement> {
    /** content rendered inside the AboutModalBoxContent  */
    children: React.ReactNode;
    /** additional classes added to the AboutModalBoxContent  */
    className?: string;
    /** id to use for About Modal Box aria described by  */
    id: string;
    /** The Trademark info for the product  */
    trademark: string;
    /** Prevents the about modal from rendering content inside a container; allows for more flexible layouts */
    noAboutModalBoxContentContainer?: boolean;
}
export declare const AboutModalBoxContent: React.FunctionComponent<AboutModalBoxContentProps>;
//# sourceMappingURL=AboutModalBoxContent.d.ts.map