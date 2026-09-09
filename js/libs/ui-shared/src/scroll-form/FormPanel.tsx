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
    <Card id={scrollId || id} className={className} isFlat tabIndex={-1}>
      <CardHeader className="kc-form-panel__header">
        <CardTitle tabIndex={0}>
          <FormTitle title={title} />
        </CardTitle>
      </CardHeader>
      <CardBody className="kc-form-panel__body">{children}</CardBody>
    </Card>
  );
};
