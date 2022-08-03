import { FunctionComponent } from "react";
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
  className?: string;
};

export const FormPanel: FunctionComponent<FormPanelProps> = ({
  title,
  children,
  scrollId,
  className,
}) => {
  return (
    <Card className={className} isFlat>
      <CardHeader className="kc-form-panel__header">
        <CardTitle tabIndex={0}>
          <Title
            headingLevel="h1"
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
