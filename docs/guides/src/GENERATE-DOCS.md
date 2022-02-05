To generate the docs, we use a maven plugin that transforms the guides from FreeMarker enabled AsciiDoc to pure AsciiDoc. This includes linking the options from the Configuration to expose them to FreeMarker templates. 

FreeMarker macros are used heavily here to keep consistency throughout the guides, and to make the guides themselves as slim as possible.

To help debugging, for now use the `DocsBuildDebugUtil.java` which has a main method that allows running this step outside of Maven.

To build the guides, run: 
```
cd docs
mvn clean install
```
After that you will have the following artifacts:

- `docs/guides/target/generated-guides`: pure asciidoc generated versions of the guides
- `docs/guides/target/generated-docs/index.html`: all guides in a single html file generated with asciidoc maven plugins. 

_Note:_ The layout primarily serves as an example for now and is not how we will eventually present the documentation.