import React from 'react';
import createIcon from './createIcon';
import { shallow } from 'enzyme';
import { IconSize } from './common';

const iconDef = {
  name: 'IconName',
  width: 10,
  height: 20,
  svgPath: 'svgPath'
};

const Icon = createIcon(iconDef);

test('sets correct viewBox', () => {
  const view = shallow(<Icon />);
  expect(view.find('svg').prop('viewBox')).toBe(`0 0 ${iconDef.width} ${iconDef.height}`);
});

test('sets correct svgPath', () => {
  const view = shallow(<Icon />);
  expect(view.find('path').prop('d')).toBe(iconDef.svgPath);
});

test('sets correct displayName', () => {
  expect(Icon.displayName).toBe(iconDef.name);
});

test('height and width are set from size', () => {
  const view = shallow(<Icon size={IconSize.sm} />);
  expect(view.find('svg').prop('width')).toMatchSnapshot('width');
  expect(view.find('svg').prop('height')).toMatchSnapshot('height');
});

test('aria-hidden is true if no title is specified', () => {
  const view = shallow(<Icon />);
  expect(view.find('svg').prop('aria-hidden')).toBe(true);
});

test('title is not renderd if a title is not passed', () => {
  const view = shallow(<Icon />);
  expect(view.find('title').exists()).toBe(false);
});

test('aria-labelledby is null if a title is not passed', () => {
  const view = shallow(<Icon />);
  expect(view.find('svg').prop('aria-labelledby')).toBe(null);
});

test('title is rendered', () => {
  const title = 'icon title';
  const view = shallow(<Icon title={title} />);
  expect(view.find('title').text()).toBe(title);
});

test('aria-labelledby matches title id', () => {
  const view = shallow(<Icon title="icon title" />);
  const labelledby = view.find('svg').prop('aria-labelledby');
  const id = view.find('title').prop('id');
  expect(labelledby).toBe(id);
});

test('ids should be unique for each rendered icon', () => {
  const first = shallow(<Icon title="icon title" />);
  const second = shallow(<Icon title="icon title" />);
  expect(first.find('title').prop('id')).not.toBe(second.find('title').prop('id'));
});

test('additional props should be spread to the root svg element', () => {
  const view = shallow(<Icon data-testid="icon" />);
  expect(view.find('svg')).toMatchSnapshot();
});
