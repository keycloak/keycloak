// https://www.i18next.com/overview/typescript
import "i18next";

declare module "i18next" {
  interface CustomTypeOptions {
    returnNull: false;
  }
}
