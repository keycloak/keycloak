import { PageSection, Text, TextContent, Title } from "@patternfly/react-core";
import { PropsWithChildren } from "react";

type PageProps = {
  title: string;
  description: string;
};

export const Page = ({
  title,
  description,
  children,
}: PropsWithChildren<PageProps>) => {
  return (
    <>
      <PageSection variant="light">
        <TextContent>
          <Title headingLevel="h1" data-testid="page-heading">
            {title}
          </Title>
          <Text component="p">{description}</Text>
        </TextContent>
      </PageSection>
      <PageSection variant="light">{children}</PageSection>
    </>
  );
};
