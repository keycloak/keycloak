import React from 'react';
import { Tabs, Tab, TabTitleText } from '@patternfly/react-core';

export const TabsDynamic: React.FunctionComponent = () => {
  const [activeTabKey, setActiveTabKey] = React.useState<number>(0);
  const [tabs, setTabs] = React.useState<string[]>(['Terminal 1', 'Terminal 2', 'Terminal 3']);
  const [newTabNumber, setNewTabNumber] = React.useState<number>(4);
  const tabComponentRef = React.useRef<any>();
  const firstMount = React.useRef(true);

  const onClose = (event: any, tabIndex: string | number) => {
    const tabIndexNum = tabIndex as number;
    let nextTabIndex = activeTabKey;
    if (tabIndexNum < activeTabKey) {
      // if a preceding tab is closing, keep focus on the new index of the current tab
      nextTabIndex = activeTabKey - 1 > 0 ? activeTabKey - 1 : 0;
    } else if (activeTabKey === tabs.length - 1) {
      // if the closing tab is the last tab, focus the preceding tab
      nextTabIndex = tabs.length - 2 > 0 ? tabs.length - 2 : 0;
    }
    setActiveTabKey(nextTabIndex);
    setTabs(tabs.filter((tab, index) => index !== tabIndex));
  };

  const onAdd = () => {
    setTabs([...tabs, `Terminal ${newTabNumber}`]);
    setActiveTabKey(tabs.length);
    setNewTabNumber(newTabNumber + 1);
  };

  React.useEffect(() => {
    if (firstMount.current) {
      firstMount.current = false;
      return;
    } else {
      const first = tabComponentRef.current.tabList.current.childNodes[activeTabKey];
      first && first.firstChild.focus();
    }
  }, [tabs]);

  return (
    <Tabs
      activeKey={activeTabKey}
      onSelect={(event: any, tabIndex: string | number) => setActiveTabKey(tabIndex as number)}
      onClose={onClose}
      onAdd={onAdd}
      aria-label="Tabs in the addable/closeable example"
      addButtonAriaLabel="Add new tab"
      ref={tabComponentRef}
    >
      {tabs.map((tab, index) => (
        <Tab
          key={index}
          eventKey={index}
          title={<TabTitleText>{tab}</TabTitleText>}
          closeButtonAriaLabel={`Close ${tab}`}
          isCloseDisabled={tabs.length === 1}
        >
          {tab}
        </Tab>
      ))}
    </Tabs>
  );
};
