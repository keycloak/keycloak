/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { ChipGroupToolbarItem } from '../../ChipGroupToolbarItem';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('ChipGroupToolbarItem should match snapshot (auto-generated)', () => {
  const view = shallow(
    <ChipGroupToolbarItem
      categoryName={"''"}
      children={<>ReactNode</>}
      className={"''"}
      isClosable={false}
      onClick={(_e: React.MouseEvent) => undefined as any}
      closeBtnAriaLabel={"'Close chip group'"}
    />
  );
  expect(view).toMatchSnapshot();
});
