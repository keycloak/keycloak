import * as React from 'react';
import { shallow } from 'enzyme';

import { TitleLevel } from '../../Title';
import { ModalBoxHeader } from '../ModalBoxHeader';

test('ModalBoxHeader Test', () => {
  const view = shallow(<ModalBoxHeader>This is a ModalBox header</ModalBoxHeader>);
  expect(view).toMatchSnapshot();
});

test('ModalBoxHeader Test with H3', () => {
  const view = shallow(<ModalBoxHeader headingLevel={TitleLevel.h3}>This is a ModalBox header</ModalBoxHeader>);
  expect(view).toMatchSnapshot();
});

test('ModalBoxHeader Test hideTitle', () => {
  const view = shallow(<ModalBoxHeader hideTitle>This is a ModalBox header</ModalBoxHeader>);
  expect(view).toMatchSnapshot();
});
