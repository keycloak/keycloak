import {
  Grid,
  GridItem,
  GridProps,
  JumpLinks,
  JumpLinksItem,
  PageSection,
} from "@patternfly/react-core";
import { Fragment, ReactNode, useEffect, useMemo, useState } from "react";
import { FormPanel } from "./FormPanel";
import { ScrollPanel } from "./ScrollPanel";

import style from "./scroll-form.module.css";

export const mainPageContentId = "kc-main-content-page-container";

type ScrollSection = {
  title: string;
  panel: ReactNode;
  isHidden?: boolean;
};

type ScrollFormProps = GridProps & {
  label: string;
  sections: ScrollSection[];
  borders?: boolean;
};

const spacesToHyphens = (string: string): string => {
  return string.replace(/\s+/g, "-");
};

export const ScrollForm = ({
  label,
  sections,
  borders = false,
  ...rest
}: ScrollFormProps) => {
  const shownSections = useMemo(
    () => sections.filter(({ isHidden }) => !isHidden),
    [sections],
  );

  const [activeSection, setActiveSection] = useState(0);

  useEffect(() => {
    const scroller = document.getElementById(mainPageContentId);
    if (!scroller) return;

    const offset = 100;
    const updateActive = () => {
      const scrollTop = scroller.scrollTop + offset;
      let active = 0;
      shownSections.forEach(({ title }, index) => {
        const id = spacesToHyphens(title.toLowerCase());
        const el = document.getElementById(id);
        if (el && scrollTop >= el.offsetTop) {
          active = index;
        }
      });
      setActiveSection(active);
    };

    updateActive();
    scroller.addEventListener("scroll", updateActive);
    return () => scroller.removeEventListener("scroll", updateActive);
  }, [shownSections]);

  return (
    <Grid hasGutter {...rest}>
      <GridItem md={8} sm={12}>
        {shownSections.map(({ title, panel }) => {
          const scrollId = spacesToHyphens(title.toLowerCase());

          return (
            <Fragment key={title}>
              {borders ? (
                <FormPanel
                  scrollId={scrollId}
                  title={title}
                  className={style.panel}
                >
                  {panel}
                </FormPanel>
              ) : (
                <ScrollPanel scrollId={scrollId} title={title}>
                  {panel}
                </ScrollPanel>
              )}
            </Fragment>
          );
        })}
      </GridItem>
      <GridItem md={4} sm={12} order={{ default: "-1", md: "1" }}>
        <PageSection className={style.sticky}>
          <JumpLinks isVertical label={label}>
            {shownSections.map(({ title }, index) => {
              const scrollId = spacesToHyphens(title.toLowerCase());

              return (
                <JumpLinksItem
                  key={title}
                  isActive={activeSection === index}
                  onClick={() => {
                    const element = document.getElementById(scrollId);
                    if (element) {
                      element.scrollIntoView({
                        behavior: "smooth",
                        block: "start",
                      });
                    }
                  }}
                  data-testid={`jump-link-${scrollId}`}
                >
                  {title}
                </JumpLinksItem>
              );
            })}
          </JumpLinks>
        </PageSection>
      </GridItem>
    </Grid>
  );
};
