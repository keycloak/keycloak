import * as React from 'react';
import { ButtonProps } from '../../components/Button';
export interface PageToggleButtonProps extends ButtonProps {
    /** Content of the page toggle button */
    children?: React.ReactNode;
    /** True if the side nav is shown  */
    isNavOpen?: boolean;
    /** Callback function to handle the side nav toggle button, managed by the Page component if the Page isManagedSidebar prop is set to true */
    onNavToggle?: () => void;
}
export declare const PageToggleButton: React.FunctionComponent<PageToggleButtonProps>;
//# sourceMappingURL=PageToggleButton.d.ts.map