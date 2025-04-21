import React from 'react';
import {
  Checkbox,
  Drawer,
  DrawerPanelContent,
  DrawerContent,
  DrawerContentBody,
  DrawerHead,
  DrawerActions,
  DrawerCloseButton,
  DrawerSection,
  Button,
  DrawerColorVariant
} from '@patternfly/react-core';

export const DrawerLightGray: React.FunctionComponent = () => {
  const [isExpanded, setIsExpanded] = React.useState(false);
  const [panelGray, setPanelGray] = React.useState(true);
  const [contentGray, setContentGray] = React.useState(false);
  const [sectionGray, setSectionGray] = React.useState(false);

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

  const togglePanelGray = (checked: boolean) => {
    setPanelGray(checked);
  };

  const toggleSectionGray = (checked: boolean) => {
    setSectionGray(checked);
  };

  const toggleContentGray = (checked: boolean) => {
    setContentGray(checked);
  };

  const panelContent = (
    <DrawerPanelContent colorVariant={panelGray ? DrawerColorVariant.light200 : DrawerColorVariant.default}>
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
      <Checkbox
        label="Gray panel"
        isChecked={panelGray}
        onChange={togglePanelGray}
        aria-label="Gray panel checkbox"
        id="toggle-gray-panel"
        name="toggle-gray-panel"
      />
      <Checkbox
        label="Gray content"
        isChecked={contentGray}
        onChange={toggleContentGray}
        aria-label="Gray content checkbox"
        id="toggle-gray-content"
        name="toggle-gray-content"
      />
      <Checkbox
        label="Gray section"
        isChecked={sectionGray}
        onChange={toggleSectionGray}
        aria-label="Gray section checkbox"
        id="toggle-gray-section"
        name="toggle-gray-section"
      />
      <br />
      <Button aria-expanded={isExpanded} onClick={onClick}>
        Toggle drawer
      </Button>
      <Drawer isExpanded={isExpanded} onExpand={onExpand}>
        <DrawerSection colorVariant={sectionGray ? DrawerColorVariant.light200 : DrawerColorVariant.default}>
          drawer-section
        </DrawerSection>
        <DrawerContent
          colorVariant={contentGray ? DrawerColorVariant.light200 : DrawerColorVariant.default}
          panelContent={panelContent}
        >
          <DrawerContentBody>{drawerContent}</DrawerContentBody>
        </DrawerContent>
      </Drawer>
    </React.Fragment>
  );
};
