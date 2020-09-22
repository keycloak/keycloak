import React, { Children, useEffect, useState } from "react";
import { Grid, GridItem, Title } from "@patternfly/react-core";

import { FormPanel } from "./FormPanel";
import style from "./scroll-form.module.css";

type ScrollFormProps = {
  sections: string[];
  children: React.ReactNode;
};

export const ScrollForm = ({ sections, children }: ScrollFormProps) => {
  const [active, setActive] = useState(sections[0]);
  useEffect(() => {
    const getCurrentSection = () => {
      for (let sectionName of sections) {
        const section = document.getElementById(sectionName)!;
        const startAt = section.offsetTop;
        const endAt = startAt + section.offsetHeight;
        const currentPosition =
          document.documentElement.scrollTop || document.body.scrollTop;
        const isInView = currentPosition >= startAt && currentPosition < endAt;
        if (isInView) {
          return sectionName;
        }
      }
    };

    window.addEventListener("scroll", () => {
      const active = getCurrentSection();
      if (active) {
        setActive(active);
      }
    });
  }, [active, sections]);

  const Nav = () => (
    <div className={style.sticky}>
      <Title headingLevel="h5" size="lg">
        Jump to Section
      </Title>
      <div className="pf-c-tabs pf-m-vertical">
        <ul className="pf-c-tabs__list">
          {sections.map((cat) => (
            <li
              className={
                "pf-c-tabs__item" + (active === cat ? " pf-m-current" : "")
              }
              key={cat}
            >
              <button
                className="pf-c-tabs__link"
                id={`link-${cat}`}
                onClick={() =>
                  document
                    .getElementById(cat)
                    ?.scrollIntoView({ behavior: "smooth" })
                }
              >
                <span className="pf-c-tabs__item-text">{cat}</span>
              </button>
            </li>
          ))}
        </ul>
      </div>
    </div>
  );
  const nodes = Children.toArray(children);
  return (
    <>
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
    </>
  );
};
