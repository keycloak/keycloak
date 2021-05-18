import React from "react";
import type { Meta } from "@storybook/react";
import { ScrollForm } from "../components/scroll-form/ScrollForm";

export default {
  title: "Scroll spy scroll form",
  component: ScrollForm,
} as Meta;

export const View = () => {
  return (
    <ScrollForm sections={["Revocation", "Clustering", "Fine grain stuff"]}>
      <div style={{ height: "2400px" }}>One</div>
      <div style={{ height: "2400px" }}>Two</div>
      <div style={{ height: "2400px" }}>fine grain</div>
    </ScrollForm>
  );
};
