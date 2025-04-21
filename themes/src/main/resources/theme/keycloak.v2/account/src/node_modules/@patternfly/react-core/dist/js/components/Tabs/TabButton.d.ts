import * as React from 'react';
import { OUIAProps } from '../../helpers';
export interface TabButtonProps extends Omit<React.HTMLProps<HTMLAnchorElement | HTMLButtonElement>, 'ref'>, OUIAProps {
    /** content rendered inside the Tab content area. */
    children?: React.ReactNode;
    /** additional classes added to the Tab */
    className?: string;
    /** URL associated with the Tab. A Tab with an href will render as an <a> instead of a <button>. A Tab inside a <Tabs component="nav"> should have an href. */
    href?: string;
    /** child reference for case in which a TabContent section is defined outside of a Tabs component */
    tabContentRef?: React.Ref<any>;
    /** Parents' innerRef passed down for properly displaying Tooltips */
    parentInnerRef?: React.Ref<any>;
}
export declare const TabButton: React.FunctionComponent<TabButtonProps>;
//# sourceMappingURL=TabButton.d.ts.map