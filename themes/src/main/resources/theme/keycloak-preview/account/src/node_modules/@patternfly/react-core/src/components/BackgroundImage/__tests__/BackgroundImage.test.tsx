import { BackgroundImage, BackgroundImageSrc } from '../BackgroundImage';
import * as React from 'react';
import { shallow } from 'enzyme';

const images = {
  [BackgroundImageSrc.lg]: '/assets/images/pfbg_1200.jpg',
  [BackgroundImageSrc.sm]: '/assets/images/pfbg_768.jpg',
  [BackgroundImageSrc.sm2x]: '/assets/images/pfbg_768@2x.jpg',
  [BackgroundImageSrc.xs]: '/assets/images/pfbg_576.jpg',
  [BackgroundImageSrc.xs2x]: '/assets/images/pfbg_576@2x.jpg',
  [BackgroundImageSrc.filter]: '/assets/images/background-filter.svg'
};

// eslint-disable-next-line @typescript-eslint/no-unused-vars
Object.values([true, false]).forEach(isRead => {
  test('BackgroundImage', () => {
    const view = shallow(<BackgroundImage src={images} />);
    expect(view).toMatchSnapshot();
  });
});

test('allows passing in a single string as the image src', () => {
  const view = shallow(<BackgroundImage src={images.lg} />);
  expect(view).toMatchSnapshot();
});
