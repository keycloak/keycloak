/**
 * This test was generated
 */
import * as React from 'react';
import { shallow } from 'enzyme';
import { AboutModalBoxCloseButton } from '../../AboutModalBoxCloseButton';
// any missing imports can usually be resolved by adding them here
import {} from '../..';

it('AboutModalBoxCloseButton should match snapshot (auto-generated)', () => {
  const view = shallow(<AboutModalBoxCloseButton className={"''"} onClose={() => undefined as any} />);
  expect(view).toMatchSnapshot();
});
