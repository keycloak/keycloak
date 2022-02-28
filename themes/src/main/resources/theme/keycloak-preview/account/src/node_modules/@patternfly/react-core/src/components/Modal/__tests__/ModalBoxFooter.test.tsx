import * as React from 'react';
import { shallow } from 'enzyme';
import { ModalBoxFooter } from '../ModalBoxFooter';

test('ModalBoxFooter Test', () => {
  const view = shallow(<ModalBoxFooter className="test-box-footer-class">This is a ModalBox Footer</ModalBoxFooter>);
  expect(view).toMatchSnapshot();
});
