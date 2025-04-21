import React from 'react';

import { render, screen } from '@testing-library/react';

import { Accordion } from '../Accordion';
import { AccordionToggle } from '../AccordionToggle';
import { AccordionContent } from '../AccordionContent';
import { AccordionItem } from '../AccordionItem';
import { AccordionExpandedContentBody } from '../AccordionExpandedContentBody';

describe('Accordion', () => {
  test('Accordion default', () => {
    const { asFragment } = render(<Accordion aria-label="this is a simple accordion" />);
    expect(asFragment()).toMatchSnapshot();
  });

  test('Accordion with non-default headingLevel', () => {
    const { asFragment } = render(
      <Accordion asDefinitionList={false} headingLevel="h2">
        <AccordionItem>
          <AccordionToggle id="item-1">Item One</AccordionToggle>
          <AccordionContent>Item One Content</AccordionContent>
        </AccordionItem>
      </Accordion>
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('It should pass optional aria props', () => {
    render(
      <Accordion asDefinitionList>
        <AccordionToggle aria-label="Toggle details for" aria-labelledby="ex-toggle2 ex-item2" id="ex-toggle2" />
      </Accordion>
    );
    const button = screen.getByRole('button');

    expect(button).toHaveAttribute('aria-label', 'Toggle details for');
    expect(button).toHaveAttribute('aria-labelledby', 'ex-toggle2 ex-item2');
    expect(button).toHaveAttribute('aria-expanded', 'false');
  });

  test('Toggle expanded', () => {
    render(
      <Accordion asDefinitionList>
        <AccordionToggle aria-label="Toggle details for" id="ex-toggle2" isExpanded />
      </Accordion>
    );
    const button = screen.getByRole('button');

    expect(button).toHaveAttribute('aria-expanded', 'true');
    expect(button).toHaveClass('pf-m-expanded');
  });

  test('renders content with custom Toggle and Content containers', () => {
    const container = 'a';

    const { asFragment } = render(
      <Accordion headingLevel="h2">
        <AccordionItem>
          <AccordionToggle id="item-1" component={container}>
            Item One
          </AccordionToggle>
          <AccordionContent component={container}>Item One Content</AccordionContent>
        </AccordionItem>
      </Accordion>
    );

    expect(screen.getByText('Item One')).toBeInTheDocument();
    expect(screen.getByText('Item One Content')).toBeInTheDocument();
  });

  test('Accordion bordered', () => {
    const { asFragment } = render(
      <Accordion isBordered>
        <AccordionItem>
          <AccordionToggle id="item-1">Item One</AccordionToggle>
          <AccordionContent>Item One Content</AccordionContent>
        </AccordionItem>
      </Accordion>
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('Accordion display large', () => {
    const { asFragment } = render(
      <Accordion displaySize="large">
        <AccordionItem>
          <AccordionToggle id="item-1">Item One</AccordionToggle>
          <AccordionContent>Item One Content</AccordionContent>
        </AccordionItem>
      </Accordion>
    );
    expect(asFragment()).toMatchSnapshot();
  });

  test('Accordion custom content', () => {
    const { asFragment } = render(
      <Accordion>
        <AccordionItem>
          <AccordionToggle id="item-1">Item One</AccordionToggle>
          <AccordionContent isCustomContent>
            <AccordionExpandedContentBody>Item one content body 1</AccordionExpandedContentBody>
            <AccordionExpandedContentBody>Item one Content body 2</AccordionExpandedContentBody>
          </AccordionContent>
        </AccordionItem>
      </Accordion>
    );
    expect(asFragment()).toMatchSnapshot();
  });
});
