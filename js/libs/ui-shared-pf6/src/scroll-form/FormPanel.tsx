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
    <Card id={id} className={className} isCompact>
      <CardHeader>
        <CardTitle tabIndex={0}>
          <FormTitle id={scrollId} title={title} />
        </CardTitle>
      </CardHeader>
      <CardBody>{children}</CardBody>
    </Card>
  );
};
