import React from 'react';
import {
  Drawer,
  DrawerPanelContent,
  DrawerHead,
  DrawerActions,
  DrawerCloseButton,
  Button,
  DrawerContent,
  DrawerContentBody
} from '@patternfly/react-core';

export const DrawerResizableOnInline: React.FunctionComponent = () => {
  const [isExpanded, setIsExpanded] = React.useState(false);
  const drawerRef = React.useRef<HTMLDivElement>();

  const onExpand = () => {
    drawerRef.current && drawerRef.current.focus();
  };

  const onClick = () => {
    setIsExpanded(!isExpanded);
  };

  const onCloseClick = () => {
    setIsExpanded(false);
  };
  const panelContent = (
    <DrawerPanelContent isResizable defaultSize={'500px'} minSize={'150px'}>
      <DrawerHead>
        <span tabIndex={isExpanded ? 0 : -1} ref={drawerRef}>
          drawer-panel
        </span>
        <DrawerActions>
          <DrawerCloseButton onClick={onCloseClick} />
        </DrawerActions>
      </DrawerHead>
    </DrawerPanelContent>
  );

  const drawerContent =
    'Lorem ipsum dolor sit amet, consectetur adipiscing elit. Phasellus pretium est a porttitor vehicula. Quisque vel commodo urna. Morbi mattis rutrum ante, id vehicula ex accumsan ut. Morbi viverra, eros vel porttitor facilisis, eros purus aliquet erat,nec lobortis felis elit pulvinar sem. Vivamus vulputate, risus eget commodo eleifend, eros nibh porta quam, vitae lacinia leo libero at magna. Maecenas aliquam sagittis orci, et posuere nisi ultrices sit amet. Aliquam ex odio, malesuada sed posuere quis, pellentesque at mauris. Phasellus venenatis massa ex, eget pulvinar libero auctor pretium. Aliquam erat volutpat. Duis euismod justo in quam ullamcorper, in commodo massa vulputate.';

  return (
    <React.Fragment>
      <Button aria-expanded={isExpanded} onClick={onClick}>
        Toggle drawer
      </Button>
      <Drawer isExpanded={isExpanded} onExpand={onExpand} isInline>
        <DrawerContent panelContent={panelContent}>
          <DrawerContentBody>{drawerContent}</DrawerContentBody>
        </DrawerContent>
      </Drawer>
    </React.Fragment>
  );
};
