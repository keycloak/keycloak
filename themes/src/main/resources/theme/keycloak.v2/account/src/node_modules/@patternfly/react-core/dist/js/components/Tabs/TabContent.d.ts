import * as React from 'react';
import { OUIAProps } from '../../helpers';
export interface TabContentProps extends Omit<React.HTMLProps<HTMLElement>, 'ref'>, OUIAProps {
    /** content rendered inside the tab content area if used outside Tabs component */
    children?: any;
    /** Child to show in the content area */
    child?: React.ReactElement<any>;
    /** class of tab content area if used outside Tabs component */
    className?: string;
    /** Identifies the active Tab  */
    activeKey?: number | string;
    /** uniquely identifies the controlling Tab if used outside Tabs component */
    eventKey?: number | string;
    /** Callback for the section ref */
    innerRef?: React.Ref<any>;
    /** id passed from parent to identify the content section */
    id: string;
    /** title of controlling Tab if used outside Tabs component */
    'aria-label'?: string;
}
export declare const TabContent: React.ForwardRefExoticComponent<TabContentProps & React.RefAttributes<HTMLElement>>;
//# sourceMappingURL=TabContent.d.ts.map