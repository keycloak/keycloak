import React from 'react';
import {
  Drawer,
  DrawerPanelContent,
  DrawerContent,
  DrawerContentBody,
  DrawerHead,
  DrawerActions,
  DrawerCloseButton,
  DrawerPanelBody,
  Button
} from '@patternfly/react-core';

export const DrawerStackedContentBodyElements: React.FunctionComponent = () => {
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
    <DrawerPanelContent>
      <DrawerHead>
        <h3 className="pf-c-title pf-m-2xl" tabIndex={isExpanded ? 0 : -1} ref={drawerRef}>
          drawer title{' '}
        </h3>
        <DrawerActions>
          <DrawerCloseButton onClick={onCloseClick} />
        </DrawerActions>
        drawer-panel
      </DrawerHead>
      <DrawerPanelBody hasNoPadding>drawer-panel with no padding</DrawerPanelBody>
      <DrawerPanelBody>drawer-panel</DrawerPanelBody>
    </DrawerPanelContent>
  );

  return (
    <React.Fragment>
      <Button aria-expanded={isExpanded} onClick={onClick}>
        Toggle drawer
      </Button>
      <Drawer isExpanded={isExpanded} onExpand={onExpand}>
        <DrawerContent panelContent={panelContent}>
          <DrawerContentBody>content-body</DrawerContentBody>
          <DrawerContentBody hasPadding>content-body with padding</DrawerContentBody>
          <DrawerContentBody>content-body</DrawerContentBody>
        </DrawerContent>
      </Drawer>
    </React.Fragment>
  );
};
