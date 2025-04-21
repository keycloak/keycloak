/**
 * This test was generated
 */
import * as React from 'react';
import { render } from '@testing-library/react';
import { TabContent } from '../../TabContent';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('TabContent should match snapshot (auto-generated)', () => {
  const { asFragment } = render(
    <TabContent
      children={'any'}
      child={<p>ReactElement</p>}
      className={'string'}
      activeKey={1}
      eventKey={1}
      innerRef={() => {}}
      id={'string'}
      aria-label={'string'}
    />
  );
  expect(asFragment()).toMatchSnapshot();
});
