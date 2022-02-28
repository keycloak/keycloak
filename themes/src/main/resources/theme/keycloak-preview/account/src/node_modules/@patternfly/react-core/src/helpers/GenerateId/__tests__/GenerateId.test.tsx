import React from 'react';
import { shallow } from 'enzyme';
import GenerateId from '../GenerateId';

test('generates id', () => {
  const view = shallow(<GenerateId>{id => <div id={id}>div with random ID</div>}</GenerateId>);

  expect(view).toMatchSnapshot();
});
