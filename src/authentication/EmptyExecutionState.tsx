import React from "react";
import { useTranslation } from "react-i18next";
import {
  Button,
  Flex,
  FlexItem,
  Title,
  TitleSizes,
} from "@patternfly/react-core";

import { ListEmptyState } from "../components/list-empty-state/ListEmptyState";

import "./empty-execution-state.css";

const sections = ["addExecution", "addSubFlow"];

export const EmptyExecutionState = () => {
  const { t } = useTranslation("authentication");
  return (
    <>
      <ListEmptyState
        message={t("emptyExecution")}
        instructions={t("emptyExecutionInstructions")}
      />

      <div className="keycloak__empty-execution-state__block">
        {sections.map((section) => (
          <Flex key={section} className="keycloak__empty-execution-state__help">
            <FlexItem flex={{ default: "flex_1" }}>
              <Title headingLevel="h3" size={TitleSizes.md}>
                {t(`${section}Title`)}
              </Title>
              <p>{t(`authentication-help:${section}`)}</p>
            </FlexItem>
            <Flex alignSelf={{ default: "alignSelfCenter" }}>
              <FlexItem>
                <Button variant="tertiary">{t(section)}</Button>
              </FlexItem>
            </Flex>
          </Flex>
        ))}
      </div>
    </>
  );
};
