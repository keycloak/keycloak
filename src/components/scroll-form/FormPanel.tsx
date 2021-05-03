import React, { ReactNode } from "react";
import {
  Card,
  CardBody,
  CardHeader,
  CardTitle,
  Title,
} from "@patternfly/react-core";

import "./form-panel.css";

type FormPanelProps = {
  title: string;
  scrollId?: string;
  children: ReactNode;
  className?: string;
};

export const FormPanel = ({
  title,
  children,
  scrollId,
  className,
}: FormPanelProps) => {
  return (
    <Card className={className} isFlat>
      <CardHeader className="kc-form-panel__header">
        <CardTitle tabIndex={0}>
          <Title
            headingLevel="h4"
            size="xl"
            className="kc-form-panel__title"
            id={scrollId}
            tabIndex={0} // so that jumpLink sends focus to the section for a11y
          >
            {title}
          </Title>
        </CardTitle>
      </CardHeader>
      <CardBody className="kc-form-panel__body">{children}</CardBody>
    </Card>
  );
};
