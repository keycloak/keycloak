import React from 'react';
import { render } from '@testing-library/react';
import { Tab } from '../Tab';
import { TabTitleText } from '../TabTitleText';

test('should not render anything', () => {
  const { asFragment } = render(
    <Tab eventKey={0} title={<TabTitleText>"Tab item 1"</TabTitleText>}>
      Tab 1 section
    </Tab>
  );
  expect(asFragment()).toMatchSnapshot();
});
