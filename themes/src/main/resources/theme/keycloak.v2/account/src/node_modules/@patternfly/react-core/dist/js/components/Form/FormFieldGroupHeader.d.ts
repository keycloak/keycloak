import * as React from 'react';
export interface FormFiledGroupHeaderTitleTextObject {
    /** Title text. */
    text: React.ReactNode;
    /** The applied to the title div for accessibility */
    id: string;
}
export interface FormFieldGroupHeaderTitleTextObject extends FormFiledGroupHeaderTitleTextObject {
}
export interface FormFieldGroupHeaderProps extends React.HTMLProps<HTMLDivElement> {
    /** Additional classes added to the section */
    className?: string;
    /** Field group header title text */
    titleText?: FormFieldGroupHeaderTitleTextObject;
    /** Field group header title description */
    titleDescription?: React.ReactNode;
    /** Field group header actions */
    actions?: React.ReactNode;
}
export declare const FormFieldGroupHeader: React.FunctionComponent<FormFieldGroupHeaderProps>;
//# sourceMappingURL=FormFieldGroupHeader.d.ts.map