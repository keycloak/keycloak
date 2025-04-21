import * as React from 'react';
export declare const isVariantIcon: (icon: any) => icon is string;
export interface ModalBoxTitleProps {
    /** Content rendered inside the modal box header title. */
    title: React.ReactNode;
    /** Optional alert icon (or other) to show before the title of the Modal Header
     * When the predefined alert types are used the default styling
     * will be automatically applied */
    titleIconVariant?: 'success' | 'danger' | 'warning' | 'info' | 'default' | React.ComponentType<any>;
    /** Optional title label text for screen readers */
    titleLabel?: string;
    /** Additional classes added to the modal box header title. */
    className?: string;
    /** id of the modal box header title. */
    id: string;
}
export declare const ModalBoxTitle: React.FunctionComponent<ModalBoxTitleProps>;
//# sourceMappingURL=ModalBoxTitle.d.ts.map