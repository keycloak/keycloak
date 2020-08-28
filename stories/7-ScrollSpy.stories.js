import React from "react";
import { storiesOf } from "@storybook/react";
import { ScrollForm } from "../src/components/scroll-form/ScrollForm";

storiesOf("Scroll Spy form", module).add("view", () => {
  return (
    <ScrollForm sections={["Revocation", "Clustering", "Fine grain stuff"]}>
      <div style={{ height: "2400px" }}>One</div>
      <div style={{ height: "2400px" }}>Two</div>
      <div style={{ height: "2400px" }}>fine grain</div>
    </ScrollForm>
  );
});
