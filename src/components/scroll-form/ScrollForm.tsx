import React, { Children, useState } from "react";
import { useTranslation } from "react-i18next";
import {
  Grid,
  GridItem,
  JumpLinks,
  JumpLinksItem,
  Title,
} from "@patternfly/react-core";

import { FormPanel } from "./FormPanel";
import style from "./scroll-form.module.css";

type ScrollFormProps = {
  sections: string[];
  children: React.ReactNode;
};

export const ScrollForm = ({ sections, children }: ScrollFormProps) => {
  const { t } = useTranslation("common");
  const [active, setActive] = useState(sections[0]);

  const Nav = () => (
    <div className={style.sticky}>
      <Title headingLevel="h5" size="lg">
        {t("jumpToSection")}
      </Title>

      <JumpLinks isVertical>
        {sections.map((cat) => (
          <JumpLinksItem
            isActive={active === cat}
            key={cat}
            href={`#${cat}`}
            onClick={() => setActive(cat)}
          >
            {cat}
          </JumpLinksItem>
        ))}
      </JumpLinks>
    </div>
  );

  const nodes = Children.toArray(children);
  return (
    <Grid hasGutter>
      <GridItem span={8}>
        {sections.map((cat, index) => (
          <FormPanel id={cat} key={cat} title={cat}>
            {nodes[index]}
          </FormPanel>
        ))}
      </GridItem>
      <GridItem span={4}>
        <Nav />
      </GridItem>
    </Grid>
  );
};
