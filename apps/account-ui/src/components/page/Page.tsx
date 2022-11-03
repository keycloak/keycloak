import { PageSection, Text, TextContent, Title } from "@patternfly/react-core";
import { FunctionComponent } from "react";

type PageProps = {
  title: string;
  description: string;
};

export const Page: FunctionComponent<PageProps> = ({
  title,
  description,
  children,
}) => {
  return (
    <>
      <PageSection variant="light">
        <TextContent>
          <Title headingLevel="h1">{title}</Title>
          <Text component="p">{description}</Text>
        </TextContent>
      </PageSection>
      <PageSection variant="light">{children}</PageSection>
    </>
  );
};
