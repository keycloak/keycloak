/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { ContextSelectorToggle } from '../../ContextSelectorToggle';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('ContextSelectorToggle should match snapshot (auto-generated)', () => {
  const view = shallow(
    <ContextSelectorToggle
      id={'string'}
      className={"''"}
      toggleText={"''"}
      isOpen={false}
      onToggle={(event: any, value: boolean) => undefined as any}
      onEnter={() => undefined as any}
      parentRef={null}
      isFocused={false}
      isHovered={false}
      isActive={false}
    />
  );
  expect(view).toMatchSnapshot();
});
