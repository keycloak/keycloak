/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { ContextSelectorItem } from '../../ContextSelectorItem';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('ContextSelectorItem should match snapshot (auto-generated)', () => {
  const view = shallow(
    <ContextSelectorItem
      children={<>ReactNode</>}
      className={"''"}
      isDisabled={false}
      isHovered={false}
      onClick={(): any => undefined}
      index={42}
      sendRef={() => {}}
    />
  );
  expect(view).toMatchSnapshot();
});
