# kc-create

Create a new Keycloak ui project based on a template

## Usage

```bash
npm create keycloak-theme <name> [type]
```

### Arguments

* `<name>` name of the project (required)
* `[type]` the type of ui to be created: `account` or `admin` (prompted if omitted)

### Options

* `-t, --type <type>` same as the `[type]` argument

When using `npm create`, prefer the positional `[type]` form. npm treats `-t` as its own `--tag` flag, so pass `--type` after `--` if you use the option form. If neither is provided, the CLI asks which type to create.

### Example

```bash
npm create keycloak-theme my-project admin
```

Or with the option form:

```bash
npm create keycloak-theme my-project -- --type admin
```

This will create a new project called `my-project` with an admin ui based on the template from the quickstarts repo.
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
