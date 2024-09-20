# kc-create

Create a new Keycloak ui project based on a template

## Usage

```bash
npm create keycloak-theme <name> [options]
```

### Options

* `-t, --type <name>` the type of ui to be created either account or admin (currently only account is supported)

### Example

```bash
npm create keycloak-theme my-project -t account
```

This will create a new project called `my-project` with an account ui based on the template from the quickstarts repo.
After the project is created, the following commands can be used to start the server and open the ui in a browser:

```bash
cd my-project
npm run dev
```
And then run keycloak in the background:

```bash
npm run start-keycloak
```

Then open the ui in a browser:

```bash
open http://localhost:8080
```