import * as React from 'react';
export interface DescriptionListTermProps extends React.HTMLProps<HTMLElement> {
    /** Anything that can be rendered inside of list term */
    children: React.ReactNode;
    /** Icon that is rendered inside of list term to the left side of the children */
    icon?: React.ReactNode;
    /** Additional classes added to the DescriptionListTerm */
    className?: string;
}
export declare const DescriptionListTerm: React.FunctionComponent<DescriptionListTermProps>;
//# sourceMappingURL=DescriptionListTerm.d.ts.map