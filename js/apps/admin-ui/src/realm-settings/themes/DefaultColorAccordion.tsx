import {
  Accordion,
  AccordionContent,
  AccordionItem,
  AccordionToggle,
} from "@patternfly/react-core";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { resolveColorToHex } from "./PatternflyVars";
import { ColorControl, ColorControlProps } from "./ColorControl";

type DefaultColorAccordionProps = ColorControlProps & {
  colorName?: string;
  onOverride?: (colorName: string) => void;
};

export const DefaultColorAccordion = (props: DefaultColorAccordionProps) => {
  const { t } = useTranslation();
  const [expanded, setExpanded] = useState(false);
  const { color, colorName, onOverride, ...rest } = props;

  const handleOverride = () => {
    if (colorName && onOverride) {
      onOverride(colorName);
    }
  };

  return (
    <Accordion asDefinitionList={false} isBordered togglePosition="start">
      <AccordionItem>
        <AccordionToggle
          onClick={() => setExpanded(!expanded)}
          isExpanded={expanded}
          id="default-color-toggle"
        >
          {t(props.label || "defaultColor")}
        </AccordionToggle>
        <AccordionContent id="default-color-content" isHidden={!expanded}>
          <ColorControl
            {...rest}
            color={resolveColorToHex(color)}
            onUserChange={handleOverride}
          />
        </AccordionContent>
      </AccordionItem>
    </Accordion>
  );
};
