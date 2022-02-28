/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { ProgressContainer } from '../../ProgressContainer';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('ProgressContainer should match snapshot (auto-generated)', () => {
  const view = shallow(
    <ProgressContainer
      ariaProps={undefined}
      parentId={'string'}
      title={"''"}
      label={null}
      variant={'danger'}
      measureLocation={'outside'}
      value={42}
    />
  );
  expect(view).toMatchSnapshot();
});
