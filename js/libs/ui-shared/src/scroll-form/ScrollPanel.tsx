/* eslint-disable react/jsx-no-useless-fragment */
// See: https://github.com/i18next/react-i18next/issues/1543
import { HTMLProps } from "react";
import { FormTitle } from "./FormTitle";

type ScrollPanelProps = HTMLProps<HTMLFormElement> & {
  title: string;
  scrollId: string;
};

export const ScrollPanel = (props: ScrollPanelProps) => {
  const { title, children, scrollId, ...rest } = props;
  return (
    <section {...rest} style={{ marginTop: "var(--pf-v5-global--spacer--lg)" }}>
      <>
        <FormTitle id={scrollId} title={title} />
        {children}
      </>
    </section>
  );
};
