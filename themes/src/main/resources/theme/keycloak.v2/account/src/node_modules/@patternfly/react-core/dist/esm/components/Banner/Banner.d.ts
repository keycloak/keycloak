import * as React from 'react';
export interface BannerProps extends React.HTMLProps<HTMLDivElement> {
    /** Content rendered inside the banner */
    children?: React.ReactNode;
    /** Additional classes added to the banner */
    className?: string;
    /** Variant styles for the banner */
    variant?: 'default' | 'info' | 'danger' | 'success' | 'warning';
    /** If set to true, the banner sticks to the top of its container */
    isSticky?: boolean;
    /** Text announced by screen readers to indicate the type of banner. Defaults to "${variant} banner" if this prop is not passed in */
    screenReaderText?: string;
}
export declare const Banner: React.FunctionComponent<BannerProps>;
//# sourceMappingURL=Banner.d.ts.map