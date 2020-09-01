import React from "react";
import { Title } from "@patternfly/react-core";

import style from "./form-panel.module.css";

interface FormPanelProps extends React.HTMLProps<HTMLFormElement> {
  title: string;
}

export const FormPanel = (props: FormPanelProps) => {
  const { title, children, ...rest } = props;
  return (
    <section {...rest} className={style.panel}>
      <Title headingLevel="h4" size="xl" className={style.title}>
        {title}
      </Title>
      {children}
    </section>
  );
};
