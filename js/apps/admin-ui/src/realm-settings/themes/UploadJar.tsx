import { Button } from "@patternfly/react-core";
import JSZip from "jszip";
import { ChangeEvent } from "react";
import { useTranslation } from "react-i18next";

type UploadJarProps = {
  onUpload: (theme: string) => void;
};

export const UploadJar = ({ onUpload }: UploadJarProps) => {
  const { t } = useTranslation();

  const triggerUpload = () => {
    const input = document.getElementById("jarUpload") as HTMLInputElement;
    if (input) {
      input.click();
    }
  };

  const handleAcceptedFiles = async (files: ChangeEvent<HTMLInputElement>) => {
    const file = files.target.files?.[0];
    if (!file) {
      return;
    }

    const jsZip = new JSZip();
    const zipFile = await jsZip.loadAsync(file);
    const themeFile = await zipFile
      .file("theme-settings.json")
      ?.async("string");

    onUpload(themeFile || "{}");
  };

  return (
    <>
      <input
        id="jarUpload"
        type="file"
        accept=".jar"
        style={{ display: "none" }}
        onChange={(acceptedFiles) => handleAcceptedFiles(acceptedFiles)}
      />
      <Button variant="secondary" onClick={triggerUpload}>
        {t("uploadGeneratedThemeJar")}
      </Button>
    </>
  );
};
