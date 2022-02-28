/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { OptionsMenuItem } from '../../OptionsMenuItem';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('OptionsMenuItem should match snapshot (auto-generated)', () => {
  const view = shallow(
    <OptionsMenuItem
      children={<>ReactNode</>}
      className={'string'}
      isSelected={false}
      isDisabled={true}
      onSelect={() => null as any}
      id={"''"}
    />
  );
  expect(view).toMatchSnapshot();
});
