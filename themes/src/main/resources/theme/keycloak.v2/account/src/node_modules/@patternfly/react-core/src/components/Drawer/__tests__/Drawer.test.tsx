import {
  Drawer,
  DrawerActions,
  DrawerCloseButton,
  DrawerContent,
  DrawerContentBody,
  DrawerHead,
  DrawerPanelBody,
  DrawerColorVariant,
  DrawerPanelContent
} from '../';
import React from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

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
    const { asFragment } = render(
      <Drawer isExpanded={isExpanded}>
        <DrawerContent panelContent={panelContent}>
          <DrawerContentBody>{drawerContent}</DrawerContentBody>
        </DrawerContent>
      </Drawer>
    );
    expect(asFragment()).toMatchSnapshot();
  });
});

test(`Drawer expands from bottom`, () => {
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

  const { asFragment } = render(
    <Drawer isExpanded={true} position="bottom">
      <DrawerContent panelContent={panelContent}>
        <DrawerContentBody>{drawerContent}</DrawerContentBody>
      </DrawerContent>
    </Drawer>
  );
  expect(asFragment()).toMatchSnapshot();
});

test(`Drawer has resizable css and color variants`, () => {
  const panelContent = (
    <DrawerPanelContent
      isResizable
      minSize={'200px'}
      defaultSize={'300px'}
      maxSize={'400px'}
      colorVariant={DrawerColorVariant.light200}
    >
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

  const { asFragment } = render(
    <Drawer isExpanded={true} position="left">
      <DrawerContent panelContent={panelContent}>
        <DrawerContentBody>{drawerContent}</DrawerContentBody>
      </DrawerContent>
    </Drawer>
  );
  expect(asFragment()).toMatchSnapshot();
});

test(`Drawer has resizable callback and id`, () => {
  const panelContent = (
    <DrawerPanelContent isResizable onResize={jest.fn()} id="test-id">
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

  const { asFragment } = render(
    <Drawer isExpanded={true} position="left">
      <DrawerContent panelContent={panelContent}>
        <DrawerContentBody>{drawerContent}</DrawerContentBody>
      </DrawerContent>
    </Drawer>
  );
  expect(asFragment()).toMatchSnapshot();
});

test('Resizeable DrawerPanelContent can be wrapped in a context without causing an error', () => {
  const TestContext = React.createContext({});

  const consoleError = jest.spyOn(console, 'error').mockImplementation();

  const panelContent = (
    <TestContext.Provider value={{}}>
      <DrawerPanelContent
        isResizable
      >
        <DrawerHead>
          <span>
            drawer-panel
          </span>
          <DrawerActions>
            <DrawerCloseButton />
          </DrawerActions>
        </DrawerHead>
      </DrawerPanelContent>
    </TestContext.Provider>
  );

  render(
    <Drawer isExpanded={true} position="left">
      <DrawerContent panelContent={panelContent}>
        <DrawerContentBody>Drawer content text</DrawerContentBody>
      </DrawerContent>
    </Drawer>
  );

  userEvent.tab();
  userEvent.keyboard('{arrowleft}');

  expect(consoleError).not.toHaveBeenCalled();
})
