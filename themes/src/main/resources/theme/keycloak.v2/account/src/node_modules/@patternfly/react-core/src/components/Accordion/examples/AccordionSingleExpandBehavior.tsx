import React from 'react';
import { Accordion, AccordionItem, AccordionContent, AccordionToggle } from '@patternfly/react-core';

export const AccordionSingleExpandBehavior: React.FunctionComponent = () => {
  const [expanded, setExpanded] = React.useState('ex-toggle2');

  const onToggle = (id: string) => {
    if (id === expanded) {
      setExpanded('');
    } else {
      setExpanded(id);
    }
  };

  return (
    <Accordion asDefinitionList={false}>
      <AccordionItem>
        <AccordionToggle
          onClick={() => {
            onToggle('ex-toggle1');
          }}
          isExpanded={expanded === 'ex-toggle1'}
          id="ex-toggle1"
        >
          Item one
        </AccordionToggle>
        <AccordionContent id="ex-expand1" isHidden={expanded !== 'ex-toggle1'}>
          <p>
            Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut labore et
            dolore magna aliqua.
          </p>
        </AccordionContent>
      </AccordionItem>

      <AccordionItem>
        <AccordionToggle
          onClick={() => {
            onToggle('ex-toggle2');
          }}
          isExpanded={expanded === 'ex-toggle2'}
          id="ex-toggle2"
        >
          Item two
        </AccordionToggle>
        <AccordionContent id="ex-expand2" isHidden={expanded !== 'ex-toggle2'}>
          <p>
            Vivamus et tortor sed arcu congue vehicula eget et diam. Praesent nec dictum lorem. Aliquam id diam
            ultrices, faucibus erat id, maximus nunc.
          </p>
        </AccordionContent>
      </AccordionItem>

      <AccordionItem>
        <AccordionToggle
          onClick={() => {
            onToggle('ex-toggle3');
          }}
          isExpanded={expanded === 'ex-toggle3'}
          id="ex-toggle3"
        >
          Item three
        </AccordionToggle>
        <AccordionContent id="ex-expand3" isHidden={expanded !== 'ex-toggle3'}>
          <p>Morbi vitae urna quis nunc convallis hendrerit. Aliquam congue orci quis ultricies tempus.</p>
        </AccordionContent>
      </AccordionItem>

      <AccordionItem>
        <AccordionToggle
          onClick={() => {
            onToggle('ex-toggle4');
          }}
          isExpanded={expanded === 'ex-toggle4'}
          id="ex-toggle4"
        >
          Item four
        </AccordionToggle>
        <AccordionContent id="ex-expand4" isHidden={expanded !== 'ex-toggle4'}>
          <p>
            Donec vel posuere orci. Phasellus quis tortor a ex hendrerit efficitur. Aliquam lacinia ligula pharetra,
            sagittis ex ut, pellentesque diam. Vestibulum ante ipsum primis in faucibus orci luctus et ultrices posuere
            cubilia Curae; Vestibulum ultricies nulla nibh. Etiam vel dui fermentum ligula ullamcorper eleifend non quis
            tortor. Morbi tempus ornare tempus. Orci varius natoque penatibus et magnis dis parturient montes, nascetur
            ridiculus mus. Mauris et velit neque. Donec ultricies condimentum mauris, pellentesque imperdiet libero
            convallis convallis. Aliquam erat volutpat. Donec rutrum semper tempus. Proin dictum imperdiet nibh, quis
            dapibus nulla. Integer sed tincidunt lectus, sit amet auctor eros.
          </p>
        </AccordionContent>
      </AccordionItem>

      <AccordionItem>
        <AccordionToggle
          onClick={() => {
            onToggle('ex-toggle5');
          }}
          isExpanded={expanded === 'ex-toggle5'}
          id="ex-toggle5"
        >
          Item five
        </AccordionToggle>
        <AccordionContent id="ex-expand5" isHidden={expanded !== 'ex-toggle5'}>
          <p>Vivamus finibus dictum ex id ultrices. Mauris dictum neque a iaculis blandit.</p>
        </AccordionContent>
      </AccordionItem>
    </Accordion>
  );
};
