/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { AlertGroup } from '../../AlertGroup';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('AlertGroup should match snapshot (auto-generated)', () => {
  const view = shallow(
    <AlertGroup className={'string'} children={<div>ReactNode</div>} isToast={true} appendTo={undefined} />
  );
  expect(view).toMatchSnapshot();
});
