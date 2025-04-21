import React from 'react';
import { Accordion, AccordionItem, AccordionContent, AccordionToggle } from '@patternfly/react-core';

export const AccordionFixedWithMultipleExpandBehavior: React.FunctionComponent = () => {
  const [expanded, setExpanded] = React.useState(['ex2-toggle4']);

  const toggle = id => {
    const index = expanded.indexOf(id);
    const newExpanded: string[] =
      index >= 0 ? [...expanded.slice(0, index), ...expanded.slice(index + 1, expanded.length)] : [...expanded, id];
    setExpanded(newExpanded);
  };

  return (
    <Accordion asDefinitionList={false}>
      <AccordionItem>
        <AccordionToggle
          onClick={() => toggle('ex2-toggle1')}
          isExpanded={expanded.includes('ex2-toggle1')}
          id="ex2-toggle1"
        >
          Item one
        </AccordionToggle>
        <AccordionContent id="ex2-expand1" isHidden={!expanded.includes('ex2-toggle1')} isFixed>
          <p>
            Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut labore et
            dolore magna aliqua.
          </p>
        </AccordionContent>
      </AccordionItem>
      <AccordionItem>
        <AccordionToggle
          onClick={() => toggle('ex2-toggle2')}
          isExpanded={expanded.includes('ex2-toggle2')}
          id="ex2-toggle2"
        >
          Item two
        </AccordionToggle>
        <AccordionContent id="ex2-expand2" isHidden={!expanded.includes('ex2-toggle2')} isFixed>
          <p>
            Vivamus et tortor sed arcu congue vehicula eget et diam. Praesent nec dictum lorem. Aliquam id diam
            ultrices, faucibus erat id, maximus nunc.
          </p>
        </AccordionContent>
      </AccordionItem>
      <AccordionItem>
        <AccordionToggle
          onClick={() => toggle('ex2-toggle3')}
          isExpanded={expanded.includes('ex2-toggle3')}
          id="ex2-toggle3"
        >
          Item three
        </AccordionToggle>
        <AccordionContent id="ex2-expand3" isHidden={!expanded.includes('ex2-toggle3')} isFixed>
          <p>Morbi vitae urna quis nunc convallis hendrerit. Aliquam congue orci quis ultricies tempus.</p>
        </AccordionContent>
      </AccordionItem>
      <AccordionItem>
        <AccordionToggle
          onClick={() => toggle('ex2-toggle4')}
          isExpanded={expanded.includes('ex2-toggle4')}
          id="ex2-toggle4"
        >
          Item four
        </AccordionToggle>
        <AccordionContent id="ex2-expand4" isHidden={!expanded.includes('ex2-toggle4')} isFixed>
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
          onClick={() => toggle('ex2-toggle5')}
          isExpanded={expanded.includes('ex2-toggle5')}
          id="ex2-toggle5"
        >
          Item five
        </AccordionToggle>
        <AccordionContent id="ex2-expand5" isHidden={!expanded.includes('ex2-toggle5')} isFixed>
          <p>Vivamus finibus dictum ex id ultrices. Mauris dictum neque a iaculis blandit.</p>
        </AccordionContent>
      </AccordionItem>
    </Accordion>
  );
};
