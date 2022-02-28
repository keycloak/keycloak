/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { EmptyStateIcon } from '../../EmptyStateIcon';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('EmptyStateIcon should match snapshot (auto-generated)', () => {
  const view = shallow(
    <EmptyStateIcon
      color={'string'}
      size={'sm'}
      title={'string'}
      className={"''"}
      icon={'div'}
      component={null}
      variant={'icon'}
    />
  );
  expect(view).toMatchSnapshot();
});

it('EmptyStateIcon should match snapshot for variant container', () => {
  const view = shallow(
    <EmptyStateIcon
      color={'string'}
      size={'sm'}
      title={'string'}
      className={"''"}
      icon={'div'}
      component={null}
      variant={'container'}
    />
  );
  expect(view).toMatchSnapshot();
});
