import React, { ReactElement, useContext, useState } from "react";
import {
  Text,
  PageSection,
  TextContent,
  Divider,
  Level,
  LevelItem,
  Switch,
  Toolbar,
  ToolbarContent,
  ToolbarItem,
  Badge,
  ButtonProps,
  Dropdown,
  DropdownToggle,
  DropdownPosition,
  Title,
} from "@patternfly/react-core";

export type WizardSectionHeaderProps = {
  title: string;
  description?: string;
  showDescription?: boolean;
};

export const WizardSectionHeader = ({
  title,
  description,
  showDescription = false,
}: WizardSectionHeaderProps) => {
  return (
    <>
      <Title
        size={"xl"}
        headingLevel={"h2"}
        className={showDescription ? "pf-u-mb-sm" : "pf-u-mb-lg"}
      >
        {title}
      </Title>
      {showDescription && (
        <TextContent className="pf-u-mb-lg">
          <Text>{description}</Text>
        </TextContent>
      )}
    </>
  );
};
