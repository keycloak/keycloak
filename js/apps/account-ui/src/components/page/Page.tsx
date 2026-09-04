import { PageSection, Content, Title } from "@patternfly/react-core";
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
      <PageSection hasBodyWrapper={false}>
        <Content>
          <Title headingLevel="h1" data-testid="page-heading">
            {title}
          </Title>
          <Content component="p">{description}</Content>
        </Content>
      </PageSection>
      <PageSection hasBodyWrapper={false}>{children}</PageSection>
    </>
  );
};
