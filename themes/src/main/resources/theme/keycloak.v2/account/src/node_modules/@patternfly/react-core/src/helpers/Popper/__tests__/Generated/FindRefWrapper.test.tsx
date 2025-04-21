/**
 * This test was generated
 */
import * as React from 'react';
import { render } from '@testing-library/react';
import { FindRefWrapper } from '../../FindRefWrapper';

it('FindRefWrapper should match snapshot (auto-generated)', () => {
  const { asFragment } = render(<FindRefWrapper children={<div>ReactNode</div>} onFoundRef={(foundRef: any) => {}} />);
  expect(asFragment()).toMatchSnapshot();
});
