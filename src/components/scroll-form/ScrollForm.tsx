import React, { Children } from "react";
import { useTranslation } from "react-i18next";
import {
  Grid,
  GridItem,
  GridProps,
  JumpLinks,
  JumpLinksItem,
  PageSection,
} from "@patternfly/react-core";

import { mainPageContentId } from "../../App";
import { FormPanel } from "./FormPanel";
import "./scroll-form.css";

type ScrollFormProps = GridProps & {
  sections: string[];
  children: React.ReactNode;
};

const spacesToHyphens = (string: string): string => {
  return string.replace(/\s+/g, "-");
};

export const ScrollForm = ({
  sections,
  children,
  ...rest
}: ScrollFormProps) => {
  const { t } = useTranslation("common");

  const nodes = Children.toArray(children);
  return (
    <Grid hasGutter {...rest}>
      <GridItem span={8}>
        {sections.map((cat, index) => (
          <FormPanel scrollId={spacesToHyphens(cat)} key={cat} title={cat}>
            {nodes[index]}
          </FormPanel>
        ))}
      </GridItem>
      <GridItem span={4}>
        <PageSection className="kc-scroll-form--sticky">
          <JumpLinks
            isVertical
            // scrollableSelector has to point to the id of the element whose scrollTop changes
            // to scroll the entire main section, it has to be the pf-c-page__main
            scrollableSelector={`#${mainPageContentId}`}
            label={t("jumpToSection")}
            offset={100}
          >
            {sections.map((cat) => (
              // note that JumpLinks currently does not work with spaces in the href
              <JumpLinksItem key={cat} href={`#${spacesToHyphens(cat)}`}>
                {cat}
              </JumpLinksItem>
            ))}
          </JumpLinks>
        </PageSection>
      </GridItem>
    </Grid>
  );
};
