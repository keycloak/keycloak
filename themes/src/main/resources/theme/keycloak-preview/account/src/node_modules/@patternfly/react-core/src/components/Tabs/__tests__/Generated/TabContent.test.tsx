/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { TabContent } from '../../TabContent';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('TabContent should match snapshot (auto-generated)', () => {
  const view = shallow(
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
  expect(view).toMatchSnapshot();
});
