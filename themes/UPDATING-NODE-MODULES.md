# Updating dependencies

The dependencies will be downloaded at build time, based on the contents of `package-lock.json`. You should verify the new set of packages don't break anything before committing the new `package-lock.json`.

## For login, admin console, and old account console

```bash
cd src/main/resources/theme/keycloak/common/resources
npm install some-package-name@version
git add package-lock.json
cd -
```

## For the new account console

```bash
cd src/main/resources/theme/keycloak.v2/account/src
npm install some-package-name@version
git add package-lock.json
cd -
```

## License Information

Make sure to enter license information for new dependencies, as specified in `docs/dependency-license-information.md`. Javascript dependencies are included as `other` elements.


## Tips

You can use `npm outdated --latest` in the same directory where the `package.json` file resides to see which dependencies are outdated 
