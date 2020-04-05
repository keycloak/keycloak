## Updating dependencies for login, admin console, and old account console

Edit `src/main/package.json` to update the dependency versions. Then run the following commands to download the new dependencies:

    cd themes
    mvn clean install -Pnpm-update

The above will download the full NPM dependencies to `src/main/resources/theme/keycloak/common/resources/node_modules`. The main purpose of this directory is that we have the full source code available for dependencies in the future. This will be removed in the future as the internal build systems will take care of this. 

Before committing changes review changes in `src/main/resources/theme/keycloak/common/resources/node_modules` making sure that it hasn't added new unused dependencies (transitive dependencies) and added any files that are not needed in the distribution (this is importat as the full node_modules downloaded are 176M while the filtered dependencies are 42M).


## Updating dependencies for the new account console

The node dependencies will be downloaded at build time, based on the content of `package-lock.json`. To update `package-lock.json`:

    cd src/main/resources/theme/keycloak-preview/account/resources/
    npm install
    git add package-lock.json
    cd -

You should verify the new set of packages don't break anything before commiting the new `package-lock.json`. Do not commit the `node_modules` directory for the new account console.

## License Information

Make sure to enter license information for new dependencies, as specified in `docs/dependency-license-information.md`. Javascript dependencies are included as `other` elements.
