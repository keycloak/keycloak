import { Text, TextContent, Title } from "@patternfly/react-core";

import "./wizard-section-header.css";

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
        className={
          showDescription
            ? "kc-wizard-section-header__title--has-description"
            : "kc-wizard-section-header__title"
        }
      >
        {title}
      </Title>
      {showDescription && (
        <TextContent className="kc-wizard-section-header__description">
          <Text>{description}</Text>
        </TextContent>
      )}
    </>
  );
};
