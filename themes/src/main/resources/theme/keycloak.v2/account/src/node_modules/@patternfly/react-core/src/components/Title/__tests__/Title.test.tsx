import * as React from 'react';
import { render, screen } from '@testing-library/react';
import { Title, TitleSizes } from '..';

describe('Title', () => {
  Object.values(TitleSizes).forEach(size => {
    test(`${size} Title`, () => {
      const { asFragment } = render(
        <Title size={size} headingLevel="h1">
          {size} Title
        </Title>
      );
      expect(asFragment()).toMatchSnapshot();
    });
  });

  test('Heading level can be set using a string value', () => {
    render(
      <Title size="lg" headingLevel="h3">
        H3 Heading
      </Title>
    );
    expect(screen.getByRole('heading').parentElement.querySelector('h3')).toBeInTheDocument();
  });
});
