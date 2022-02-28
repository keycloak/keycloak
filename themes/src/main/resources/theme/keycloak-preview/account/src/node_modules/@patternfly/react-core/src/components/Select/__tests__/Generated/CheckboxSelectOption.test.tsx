/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { CheckboxSelectOption } from '../../CheckboxSelectOption';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('CheckboxSelectOption should match snapshot (auto-generated)', () => {
  const view = shallow(
    <CheckboxSelectOption
      children={<div>ReactNode</div>}
      className={"''"}
      index={0}
      value={"''"}
      isDisabled={false}
      isChecked={false}
      sendRef={() => {}}
      keyHandler={() => {}}
      onClick={() => {}}
    />
  );
  expect(view).toMatchSnapshot();
});
