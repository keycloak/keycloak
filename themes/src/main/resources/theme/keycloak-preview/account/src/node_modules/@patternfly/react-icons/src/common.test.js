import { IconSize, getSize } from './common';

Object.values(IconSize).forEach(size => {
  test(`getSize returns size for ${size} icons`, () => {
    expect(getSize(size)).toMatchSnapshot();
  });
});

test('getSize returns a default size for unknown sizes', () => {
  expect(getSize('unknown')).toMatchSnapshot();
});
