import * as React from 'react';
export interface HelperTextProps extends React.HTMLProps<HTMLDivElement | HTMLUListElement> {
    /** Content rendered inside the helper text container. */
    children?: React.ReactNode;
    /** Additional classes applied to the helper text container. */
    className?: string;
    /** Component type of the helper text container */
    component?: 'div' | 'ul';
    /** ID for the helper text container. The value of this prop can be passed into a form component's
     * aria-describedby prop when you intend for all helper text items to be announced to
     * assistive technologies.
     */
    id?: string;
    /** Flag for indicating whether the helper text container is a live region. Use this prop when you
     * expect or intend for any helper text items within the container to be dynamically updated.
     */
    isLiveRegion?: boolean;
}
export declare const HelperText: React.FunctionComponent<HelperTextProps>;
//# sourceMappingURL=HelperText.d.ts.map