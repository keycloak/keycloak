/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { SimpleListItem } from '../../SimpleListItem';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('SimpleListItem should match snapshot (auto-generated)', () => {
  const view = shallow(
    <SimpleListItem
      children={<>ReactNode</>}
      className={"''"}
      component={'button'}
      componentClassName={"''"}
      componentProps={'any'}
      isCurrent={false}
      onClick={() => {}}
      type={'button'}
      href={"''"}
    />
  );
  expect(view).toMatchSnapshot();
});
