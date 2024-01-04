import {
  ActionGroup,
  Button,
  Chip,
  ChipGroup,
  Divider,
  Dropdown,
  DropdownToggle,
  Flex,
  FlexItem,
  Form,
  FormGroup,
  Select,
  SelectOption,
  SelectVariant,
} from "@patternfly/react-core";
import { pickBy } from "lodash-es";
import { useState } from "react";
import { Controller, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { adminClient } from "../../admin-client";
import { KeycloakTextInput } from "../../components/keycloak-text-input/KeycloakTextInput";
import { ListEmptyState } from "../../components/list-empty-state/ListEmptyState";
import { KeycloakDataTable } from "../../components/table-toolbar/KeycloakDataTable";
import { useRealm } from "../../context/realm-context/RealmContext";
import { useServerInfo } from "../../context/server-info/ServerInfoProvider";
import { useWhoAmI } from "../../context/whoami/WhoAmI";
import { DEFAULT_LOCALE } from "../../i18n/i18n";
import { localeToDisplayName } from "../../util";

type EffectiveMessageBundlesProps = {
  defaultSupportedLocales: string[];
};

type EffectiveMessageBundlesSearchForm = {
  theme: string;
  themeType: string;
  locale: string;
  hasWords: string[];
};

const defaultValues: EffectiveMessageBundlesSearchForm = {
  theme: "",
  themeType: "",
  locale: "",
  hasWords: [],
};

export const EffectiveMessageBundles = ({
  defaultSupportedLocales,
}: EffectiveMessageBundlesProps) => {
  const { t } = useTranslation();
  const { realm } = useRealm();
  const serverInfo = useServerInfo();
  const { whoAmI } = useWhoAmI();
  const [searchDropdownOpen, setSearchDropdownOpen] = useState(false);
  const [searchPerformed, setSearchPerformed] = useState(false);
  const [selectThemesOpen, setSelectThemesOpen] = useState(false);
  const [selectThemeTypeOpen, setSelectThemeTypeOpen] = useState(false);
  const [selectLanguageOpen, setSelectLanguageOpen] = useState(false);
  const [activeFilters, setActiveFilters] = useState<
    Partial<EffectiveMessageBundlesSearchForm>
  >({});
  const themes = serverInfo.themes;
  const themeKeys = themes
    ? Object.keys(themes).sort((a, b) => a.localeCompare(b))
    : [];
  const themeTypes = Object.values(themes!)
    .flatMap((theme) => theme.map((item) => item.name))
    .filter((value, index, self) => self.indexOf(value) === index)
    .sort((a, b) => a.localeCompare(b));
  const [key, setKey] = useState(0);

  const filterLabels: Record<keyof EffectiveMessageBundlesSearchForm, string> =
    {
      theme: t("theme"),
      themeType: t("themeType"),
      locale: t("language"),
      hasWords: t("hasWords"),
    };

  const {
    getValues,
    reset,
    formState: { isDirty },
    control,
  } = useForm<EffectiveMessageBundlesSearchForm>({
    mode: "onChange",
    defaultValues,
  });

  const loader = async () => {
    try {
      const filter = getValues();
      const messages = await adminClient.serverInfo.findEffectiveMessageBundles(
        {
          realm,
          ...filter,
          locale: filter.locale || DEFAULT_LOCALE,
          source: true,
        },
      );

      return filter.hasWords.length > 0
        ? messages.filter((m) =>
            filter.hasWords.some((f) => m.value.includes(f)),
          )
        : messages;
    } catch (error) {
      return [];
    }
  };

  function submitSearch() {
    setSearchDropdownOpen(false);
    commitFilters();
  }

  function resetSearch() {
    reset();
    commitFilters();
  }

  function removeFilter(key: keyof EffectiveMessageBundlesSearchForm) {
    const formValues: EffectiveMessageBundlesSearchForm = { ...getValues() };
    delete formValues[key];

    reset({ ...defaultValues, ...formValues });
    commitFilters();
  }

  function removeFilterValue(
    key: keyof EffectiveMessageBundlesSearchForm,
    valueToRemove: string,
  ) {
    const formValues = getValues();
    const fieldValue = formValues[key];
    const newFieldValue = Array.isArray(fieldValue)
      ? fieldValue.filter((val) => val !== valueToRemove)
      : fieldValue;

    reset({ ...formValues, [key]: newFieldValue });
    commitFilters();
  }

  function commitFilters() {
    const newFilters: Partial<EffectiveMessageBundlesSearchForm> = pickBy(
      getValues(),
      (value) => value !== "" || (Array.isArray(value) && value.length > 0),
    );

    setActiveFilters(newFilters);
    setKey(key + 1);
  }

  const effectiveMessageBunldesSearchFormDisplay = () => {
    return (
      <Flex
        direction={{ default: "column" }}
        spaceItems={{ default: "spaceItemsNone" }}
      >
        <FlexItem>
          <Dropdown
            id="effective-message-bundles-search-select"
            data-testid="EffectiveMessageBundlesSearchSelector"
            toggle={
              <DropdownToggle
                data-testid="effectiveMessageBundlesSearchSelectorToggle"
                onToggle={(isOpen) => setSearchDropdownOpen(isOpen)}
              >
                {t("searchForEffectiveMessageBundles")}
              </DropdownToggle>
            }
            isOpen={searchDropdownOpen}
          >
            <Form
              isHorizontal
              className="pf-c-form pf-u-mx-lg pf-u-mb-lg"
              data-testid="effectiveMessageBundlesSearchForm"
            >
              <FormGroup label={t("theme")} fieldId="kc-theme">
                <Controller
                  name="theme"
                  control={control}
                  render={({ field }) => (
                    <Select
                      name="theme"
                      data-testid="effective_message_bundles-theme-searchField"
                      chipGroupProps={{
                        numChips: 1,
                        expandedText: t("hide"),
                        collapsedText: t("showRemaining"),
                      }}
                      variant={SelectVariant.single}
                      typeAheadAriaLabel="Select"
                      onToggle={(isOpen) => setSelectThemesOpen(isOpen)}
                      selections={field.value}
                      onSelect={(_, selectedValue) => {
                        field.onChange(selectedValue.toString());
                      }}
                      onClear={(theme) => {
                        theme.stopPropagation();
                        field.onChange("");
                      }}
                      isOpen={selectThemesOpen}
                      aria-labelledby={t("theme")}
                      chipGroupComponent={
                        <ChipGroup>
                          <Chip
                            key={field.value}
                            onClick={(theme) => {
                              theme.stopPropagation();
                              field.onChange("");
                            }}
                          >
                            {field.value}
                          </Chip>
                        </ChipGroup>
                      }
                    >
                      {[
                        <SelectOption
                          key="theme_placeholder"
                          value="Select theme"
                          label={t("selectTheme")}
                          className="kc__effective_message_bundles_search_theme__placeholder"
                          isDisabled
                        />,
                      ].concat(
                        themeKeys.map((option) => (
                          <SelectOption key={option} value={option} />
                        )),
                      )}
                    </Select>
                  )}
                />
              </FormGroup>
              <FormGroup label={t("themeType")} fieldId="kc-themeType">
                <Controller
                  name="themeType"
                  control={control}
                  render={({ field }) => (
                    <Select
                      name="themeType"
                      data-testid="effective-message-bundles-feature-searchField"
                      chipGroupProps={{
                        numChips: 1,
                        expandedText: t("hide"),
                        collapsedText: t("showRemaining"),
                      }}
                      variant={SelectVariant.single}
                      typeAheadAriaLabel="Select"
                      onToggle={(isOpen) => setSelectThemeTypeOpen(isOpen)}
                      selections={field.value}
                      onSelect={(_, selectedValue) => {
                        field.onChange(selectedValue.toString());
                      }}
                      onClear={(themeType) => {
                        themeType.stopPropagation();
                        field.onChange("");
                      }}
                      isOpen={selectThemeTypeOpen}
                      aria-labelledby={t("themeType")}
                      chipGroupComponent={
                        <ChipGroup>
                          <Chip
                            key={field.value}
                            onClick={(themeType) => {
                              themeType.stopPropagation();
                              field.onChange("");
                            }}
                          >
                            {field.value}
                          </Chip>
                        </ChipGroup>
                      }
                    >
                      {[
                        <SelectOption
                          key="themeType_placeholder"
                          value="Select theme type"
                          label={t("selectThemeType")}
                          className="pf-m-plain"
                          isDisabled
                        />,
                      ].concat(
                        themeTypes.map((option) => (
                          <SelectOption key={option} value={option} />
                        )),
                      )}
                    </Select>
                  )}
                />
              </FormGroup>
              <FormGroup label={t("language")} fieldId="kc-language">
                <Controller
                  name="locale"
                  control={control}
                  render={({ field }) => (
                    <Select
                      name="language"
                      data-testid="effective-message-bundles-language-searchField"
                      chipGroupProps={{
                        numChips: 1,
                        expandedText: t("hide"),
                        collapsedText: t("showRemaining"),
                      }}
                      variant={SelectVariant.single}
                      typeAheadAriaLabel="Select"
                      onToggle={(isOpen) => setSelectLanguageOpen(isOpen)}
                      selections={field.value}
                      onSelect={(_, selectedValue) => {
                        field.onChange(selectedValue.toString());
                      }}
                      onClear={(language) => {
                        language.stopPropagation();
                        field.onChange("");
                      }}
                      isOpen={selectLanguageOpen}
                      aria-labelledby="language"
                      chipGroupComponent={
                        <ChipGroup>
                          {field.value ? (
                            <Chip
                              key={field.value}
                              onClick={(language) => {
                                language.stopPropagation();
                                field.onChange("");
                              }}
                            >
                              {localeToDisplayName(
                                field.value,
                                whoAmI.getLocale(),
                              )}
                            </Chip>
                          ) : null}
                        </ChipGroup>
                      }
                    >
                      {[
                        <SelectOption
                          key="language_placeholder"
                          value="Select language"
                          label={t("selectLanguage")}
                          className="pf-m-plain"
                          isDisabled
                        />,
                      ].concat(
                        defaultSupportedLocales.map((option) => (
                          <SelectOption key={option} value={option}>
                            {localeToDisplayName(option, whoAmI.getLocale())}
                          </SelectOption>
                        )),
                      )}
                    </Select>
                  )}
                />
              </FormGroup>
              <FormGroup label={t("hasWords")} fieldId="kc-hasWords">
                <Controller
                  name="hasWords"
                  control={control}
                  render={({ field }) => (
                    <div>
                      <KeycloakTextInput
                        id="kc-hasWords"
                        data-testid="effective-message-bundles-hasWords-searchField"
                        value={field.value.join(" ")}
                        onChange={(e) => {
                          const target = e.target as HTMLInputElement;
                          const words = target.value
                            .split(" ")
                            .map((word) => word.trim());
                          field.onChange(words);
                        }}
                      />
                      <ChipGroup>
                        {field.value.map((word, index) => (
                          <Chip
                            key={index}
                            onClick={(e) => {
                              e.stopPropagation();
                              const newWords = field.value.filter(
                                (_, i) => i !== index,
                              );
                              field.onChange(newWords);
                            }}
                          >
                            {word}
                          </Chip>
                        ))}
                      </ChipGroup>
                    </div>
                  )}
                />
              </FormGroup>
              <ActionGroup>
                <Button
                  variant={"primary"}
                  onClick={() => {
                    setSearchPerformed(true);
                    submitSearch();
                  }}
                  data-testid="search-effective-message-bundles-btn"
                  isDisabled={!isDirty}
                >
                  {t("search")}
                </Button>
                <Button
                  variant="secondary"
                  onClick={resetSearch}
                  data-testid="reset-search-effective-message-bundles-btn"
                  isDisabled={!isDirty}
                >
                  {t("reset")}
                </Button>
              </ActionGroup>
            </Form>
          </Dropdown>
        </FlexItem>
        <FlexItem>
          {Object.entries(activeFilters).length > 0 && (
            <div className="keycloak__searchChips pf-u-ml-md">
              {Object.entries(activeFilters).map((filter) => {
                const [key, value] = filter as [
                  keyof EffectiveMessageBundlesSearchForm,
                  string | string[],
                ];

                return (
                  <ChipGroup
                    className="pf-u-mt-md pf-u-mr-md"
                    key={key}
                    categoryName={filterLabels[key]}
                    isClosable
                    onClick={() => removeFilter(key)}
                  >
                    {typeof value === "string" ? (
                      <Chip isReadOnly>
                        {key === "locale"
                          ? localeToDisplayName(value, whoAmI.getLocale())
                          : value}
                      </Chip>
                    ) : (
                      value.map((entry) => (
                        <Chip
                          key={entry}
                          onClick={() => removeFilterValue(key, entry)}
                        >
                          {entry}
                        </Chip>
                      ))
                    )}
                  </ChipGroup>
                );
              })}
            </div>
          )}
        </FlexItem>
      </Flex>
    );
  };

  if (!searchPerformed) {
    return (
      <>
        <div className="pf-u-py-lg pf-u-pl-md">
          {effectiveMessageBunldesSearchFormDisplay()}
        </div>
        <Divider />
        <ListEmptyState
          message={t("emptyEffectiveMessageBundles")}
          instructions={t("emptyEffectiveMessageBundlesInstructions")}
          isSearchVariant
        />
      </>
    );
  }

  return (
    <KeycloakDataTable
      key={key}
      loader={loader}
      ariaLabelKey="effectiveMessageBundles"
      toolbarItem={effectiveMessageBunldesSearchFormDisplay()}
      columns={[
        {
          name: "key",
          displayKey: "key",
        },
        {
          name: "value",
          displayKey: "value",
        },
      ]}
      emptyState={
        <ListEmptyState
          message={t("noSearchResults")}
          instructions={t("noSearchResultsInstructions")}
        />
      }
      isSearching={Object.keys(activeFilters).length > 0}
    />
  );
};
