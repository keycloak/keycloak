import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Tabs/tabs';
import { OUIAProps } from '../../helpers';
import { TabButton } from './TabButton';
import { TabsContext } from './TabsContext';
import { css } from '@patternfly/react-styles';
import { Tooltip } from '../Tooltip';
import { Button } from '../Button';
import TimesIcon from '@patternfly/react-icons/dist/esm/icons/times-icon';

export interface TabProps extends Omit<React.HTMLProps<HTMLAnchorElement | HTMLButtonElement>, 'title'>, OUIAProps {
  /** content rendered inside the Tab content area. */
  children?: React.ReactNode;
  /** additional classes added to the Tab */
  className?: string;
  /** URL associated with the Tab. A Tab with an href will render as an <a> instead of a <button>. A Tab inside a <Tabs component="nav"> should have an href. */
  href?: string;
  /** Content rendered in the tab title. Should be <TabTitleText> and/or <TabTitleIcon> for proper styling. */
  title: React.ReactNode;
  /** uniquely identifies the tab */
  eventKey: number | string;
  /** child id for case in which a TabContent section is defined outside of a Tabs component */
  tabContentId?: string | number;
  /** child reference for case in which a TabContent section is defined outside of a Tabs component */
  tabContentRef?: React.RefObject<any>;
  /** whether to render the tab or not */
  isHidden?: boolean;
  /** Adds disabled styling and disables the button using the disabled html attribute */
  isDisabled?: boolean;
  /** Adds disabled styling and communicates that the button is disabled using the aria-disabled html attribute */
  isAriaDisabled?: boolean;
  /** Events to prevent when the button is in an aria-disabled state */
  inoperableEvents?: string[];
  /** Forwarded ref */
  innerRef?: React.Ref<any>;
  /** Optional Tooltip rendered to a Tab. Should be <Tooltip> with appropriate props for proper rendering. */
  tooltip?: React.ReactElement<any>;
  /** @beta Aria-label for the close button added by passing the onClose property to Tabs. */
  closeButtonAriaLabel?: string;
  /** @beta Flag indicating the close button should be disabled */
  isCloseDisabled?: boolean;
}

const TabBase: React.FunctionComponent<TabProps> = ({
  title,
  eventKey,
  tabContentRef,
  id: childId,
  tabContentId,
  className: childClassName = '',
  ouiaId: childOuiaId,
  isDisabled,
  isAriaDisabled,
  inoperableEvents = ['onClick', 'onKeyPress'],
  href,
  innerRef,
  tooltip,
  closeButtonAriaLabel,
  isCloseDisabled = false,
  ...props
}: TabProps) => {
  const preventedEvents = inoperableEvents.reduce(
    (handlers, eventToPrevent) => ({
      ...handlers,
      [eventToPrevent]: (event: React.SyntheticEvent<HTMLButtonElement>) => {
        event.preventDefault();
      }
    }),
    {}
  );
  const { mountOnEnter, localActiveKey, unmountOnExit, uniqueId, handleTabClick, handleTabClose } = React.useContext(
    TabsContext
  );
  let ariaControls = tabContentId ? `${tabContentId}` : `pf-tab-section-${eventKey}-${childId || uniqueId}`;
  if ((mountOnEnter || unmountOnExit) && eventKey !== localActiveKey) {
    ariaControls = undefined;
  }
  const isButtonElement = Boolean(!href);
  const getDefaultTabIdx = () => {
    if (isDisabled) {
      return isButtonElement ? null : -1;
    } else if (isAriaDisabled) {
      return null;
    }
  };

  const tabButton = (
    <TabButton
      parentInnerRef={innerRef}
      className={css(
        styles.tabsLink,
        isDisabled && href && styles.modifiers.disabled,
        isAriaDisabled && styles.modifiers.ariaDisabled
      )}
      disabled={isButtonElement ? isDisabled : null}
      aria-disabled={isDisabled || isAriaDisabled}
      tabIndex={getDefaultTabIdx()}
      onClick={(event: any) => handleTabClick(event, eventKey, tabContentRef)}
      {...(isAriaDisabled ? preventedEvents : null)}
      id={`pf-tab-${eventKey}-${childId || uniqueId}`}
      aria-controls={ariaControls}
      tabContentRef={tabContentRef}
      ouiaId={childOuiaId}
      href={href}
      role="tab"
      aria-selected={eventKey === localActiveKey}
      {...props}
    >
      {title}
    </TabButton>
  );

  return (
    <li
      className={css(
        styles.tabsItem,
        eventKey === localActiveKey && styles.modifiers.current,
        handleTabClose && styles.modifiers.action,
        handleTabClose && (isDisabled || isAriaDisabled) && styles.modifiers.disabled,
        childClassName
      )}
      role="presentation"
    >
      {tooltip ? <Tooltip {...tooltip.props}>{tabButton}</Tooltip> : tabButton}
      {handleTabClose !== undefined && (
        <span className={css(styles.tabsItemClose)}>
          <Button
            variant="plain"
            aria-label={closeButtonAriaLabel || 'Close tab'}
            onClick={(event: any) => handleTabClose(event, eventKey, tabContentRef)}
            isDisabled={isCloseDisabled}
          >
            <span className={css(styles.tabsItemCloseIcon)}>
              <TimesIcon />
            </span>
          </Button>
        </span>
      )}
    </li>
  );
};

export const Tab = React.forwardRef((props: TabProps, ref: React.Ref<any>) => <TabBase innerRef={ref} {...props} />);
Tab.displayName = 'Tab';
