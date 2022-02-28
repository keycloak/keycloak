/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { SimpleList } from '../../SimpleList';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('SimpleList should match snapshot (auto-generated)', () => {
  const view = shallow(
    <SimpleList
      children={<>ReactNode</>}
      className={"''"}
      onSelect={(
        ref: React.RefObject<HTMLButtonElement> | React.RefObject<HTMLAnchorElement>,
        props: SimpleListItemProps
      ) => undefined as void}
    />
  );
  expect(view).toMatchSnapshot();
});
