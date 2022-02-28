import {
  Drawer,
  DrawerPanelContent,
  DrawerContent,
  DrawerHead,
  DrawerActions,
  DrawerCloseButton,
  DrawerContentBody,
  DrawerPanelBody
} from '../';
import React from 'react';
import { mount } from 'enzyme';

Object.values([
  { isExpanded: true, isInline: false, isStatic: false },
  { isExpanded: false, isInline: false, isStatic: false },
  { isExpanded: true, isInline: true, isStatic: false },
  { isExpanded: false, isInline: true, isStatic: false },
  { isExpanded: true, isInline: false, isStatic: true }
]).forEach(({ isExpanded, isInline, isStatic }) => {
  const panelContent = (
    <DrawerPanelContent>
      <DrawerHead>
        <span>drawer-panel</span>
        <DrawerActions>
          <DrawerCloseButton />
        </DrawerActions>
      </DrawerHead>
      <DrawerPanelBody>drawer-panel</DrawerPanelBody>
    </DrawerPanelContent>
  );
  const drawerContent =
    'Lorem ipsum dolor sit amet, consectetur adipiscing elit. Phasellus pretium est a porttitor vehicula. Quisque vel commodo urna. Morbi mattis rutrum ante, id vehicula ex accumsan ut. Morbi viverra, eros vel porttitor facilisis, eros purus aliquet erat,nec lobortis felis elit pulvinar sem. Vivamus vulputate, risus eget commodo eleifend, eros nibh porta quam, vitae lacinia leo libero at magna. Maecenas aliquam sagittis orci, et posuere nisi ultrices sit amet. Aliquam ex odio, malesuada sed posuere quis, pellentesque at mauris. Phasellus venenatis massa ex, eget pulvinar libero auctor pretium. Aliquam erat volutpat. Duis euismod justo in quam ullamcorper, in commodo massa vulputate.';
  test(`Drawer isExpanded = ${isExpanded} and isInline = ${isInline} and isStatic = ${isStatic}`, () => {
    const view = mount(
      <Drawer isExpanded={isExpanded}>
        <DrawerContent panelContent={panelContent}>
          <DrawerContentBody>{drawerContent}</DrawerContentBody>
        </DrawerContent>
      </Drawer>
    );
    expect(view).toMatchSnapshot();
  });
});
