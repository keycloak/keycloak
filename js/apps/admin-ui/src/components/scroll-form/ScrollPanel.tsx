/* eslint-disable react/jsx-no-useless-fragment */
// See: https://github.com/i18next/react-i18next/issues/1543
import { Title } from "@patternfly/react-core";
import { HTMLProps } from "react";

import "./form-panel.css";

type ScrollPanelProps = HTMLProps<HTMLFormElement> & {
  title: string;
  scrollId: string;
};

export const ScrollPanel = (props: ScrollPanelProps) => {
  const { title, children, scrollId, ...rest } = props;
  return (
    <section {...rest} className="kc-form-panel__panel">
      <>
        <Title
          headingLevel="h1"
          size="xl"
          className="kc-form-panel__title"
          id={scrollId}
          tabIndex={0} // so that jumpLink sends focus to the section for a11y
        >
          {title}
        </Title>
        {children}
      </>
    </section>
  );
};
