import { Card, CardBody, CardHeader, CardTitle } from "@patternfly/react-core";
import { PropsWithChildren, useId } from "react";
import { FormTitle } from "./FormTitle";

type FormPanelProps = {
  title: string;
  scrollId?: string;
  className?: string;
};

export const FormPanel = ({
  title,
  children,
  scrollId,
  className,
}: PropsWithChildren<FormPanelProps>) => {
  const id = useId();

  return (
    <Card id={id} className={className} isFlat>
      <CardHeader className="kc-form-panel__header">
        <CardTitle tabIndex={0}>
          <FormTitle id={scrollId} title={title} />
        </CardTitle>
      </CardHeader>
      <CardBody className="kc-form-panel__body">{children}</CardBody>
    </Card>
  );
};
