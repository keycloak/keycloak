import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Tabs/tabs';
import { TabButton } from './TabButton';
import { TabsContext } from './TabsContext';
import { css } from '@patternfly/react-styles';
import { Tooltip } from '../Tooltip';
import { Button } from '../Button';
import TimesIcon from '@patternfly/react-icons/dist/esm/icons/times-icon';
const TabBase = (_a) => {
    var { title, eventKey, tabContentRef, id: childId, tabContentId, className: childClassName = '', ouiaId: childOuiaId, isDisabled, isAriaDisabled, inoperableEvents = ['onClick', 'onKeyPress'], href, innerRef, tooltip, closeButtonAriaLabel, isCloseDisabled = false } = _a, props = __rest(_a, ["title", "eventKey", "tabContentRef", "id", "tabContentId", "className", "ouiaId", "isDisabled", "isAriaDisabled", "inoperableEvents", "href", "innerRef", "tooltip", "closeButtonAriaLabel", "isCloseDisabled"]);
    const preventedEvents = inoperableEvents.reduce((handlers, eventToPrevent) => (Object.assign(Object.assign({}, handlers), { [eventToPrevent]: (event) => {
            event.preventDefault();
        } })), {});
    const { mountOnEnter, localActiveKey, unmountOnExit, uniqueId, handleTabClick, handleTabClose } = React.useContext(TabsContext);
    let ariaControls = tabContentId ? `${tabContentId}` : `pf-tab-section-${eventKey}-${childId || uniqueId}`;
    if ((mountOnEnter || unmountOnExit) && eventKey !== localActiveKey) {
        ariaControls = undefined;
    }
    const isButtonElement = Boolean(!href);
    const getDefaultTabIdx = () => {
        if (isDisabled) {
            return isButtonElement ? null : -1;
        }
        else if (isAriaDisabled) {
            return null;
        }
    };
    const tabButton = (React.createElement(TabButton, Object.assign({ parentInnerRef: innerRef, className: css(styles.tabsLink, isDisabled && href && styles.modifiers.disabled, isAriaDisabled && styles.modifiers.ariaDisabled), disabled: isButtonElement ? isDisabled : null, "aria-disabled": isDisabled || isAriaDisabled, tabIndex: getDefaultTabIdx(), onClick: (event) => handleTabClick(event, eventKey, tabContentRef) }, (isAriaDisabled ? preventedEvents : null), { id: `pf-tab-${eventKey}-${childId || uniqueId}`, "aria-controls": ariaControls, tabContentRef: tabContentRef, ouiaId: childOuiaId, href: href, role: "tab", "aria-selected": eventKey === localActiveKey }, props), title));
    return (React.createElement("li", { className: css(styles.tabsItem, eventKey === localActiveKey && styles.modifiers.current, handleTabClose && styles.modifiers.action, handleTabClose && (isDisabled || isAriaDisabled) && styles.modifiers.disabled, childClassName), role: "presentation" },
        tooltip ? React.createElement(Tooltip, Object.assign({}, tooltip.props), tabButton) : tabButton,
        handleTabClose !== undefined && (React.createElement("span", { className: css(styles.tabsItemClose) },
            React.createElement(Button, { variant: "plain", "aria-label": closeButtonAriaLabel || 'Close tab', onClick: (event) => handleTabClose(event, eventKey, tabContentRef), isDisabled: isCloseDisabled },
                React.createElement("span", { className: css(styles.tabsItemCloseIcon) },
                    React.createElement(TimesIcon, null)))))));
};
export const Tab = React.forwardRef((props, ref) => React.createElement(TabBase, Object.assign({ innerRef: ref }, props)));
Tab.displayName = 'Tab';
//# sourceMappingURL=Tab.js.map