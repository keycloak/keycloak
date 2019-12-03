# Keycloak Governance

* [Vision](#vision) 
* [Maintainers](#maintainers) 
* [Contributing](#contributing)

## Vision

Keycloak aims to be easy to use and lightweight. The project was founded to make it easy for application developers 
to secure modern applications and services.

The 80/20 rule, that states 80% of requirements come from around 20% of use cases, is a core part of the vision behind 
Keycloak. We strongly believe if Keycloak would support all use cases by default it would become bloated and hard to use.

Keycloak aims to be opinionated and make it as easy as possible to achieve the common use cases, while still
enabling the less common use cases through custom extensions.


## Projects

Keycloak consists of several projects:

* [Keycloak](https://github.com/keycloak/keycloak) - Keycloak Server and Java adapters
* [Keycloak Documentation](https://github.com/keycloak/keycloak-documentation) - Documentation for Keycloak
* [Keycloak QuickStarts](https://github.com/keycloak/keycloak-quickstarts) - QuickStarts for getting started with Keycloak
* [Keycloak Containers](https://github.com/keycloak/keycloak-containers) - Container images for Keycloak
* [Keycloak Gatekeeper](https://github.com/keycloak/keycloak-gatekeeper) - Proxy service to secure apps and services with Keycloak
* [Keycloak Node.js Connect](https://github.com/keycloak/keycloak-nodejs-connect) - Node.js adapter for Keycloak
* [Keycloak Node.js Admin Client](https://github.com/keycloak/keycloak-nodejs-admin-client) - Node.js library for Keycloak Admin REST API

The same governance model applies to all projects. However, the list of maintainers may vary per project. 


## Maintainers

The list of maintainers can be found in the [MAINTAINERS.md](MAINTAINERS.md) file in the repository for the individual 
projects listed in the [Projects](#projects) section.

### Maintainer Responsibilities

A maintainer is someone who has shown deep knowledge of vision, features and codebase. It is their 
responsibility to drive the project forward, encourage collaboration and contributions, and generally help the 
community.

Responsibilities of a maintainer include, but are not limited to:

* Engage in design discussions
* Actively monitor mailing lists, user forum and chat
* Contribute high quality code
* Maintain deep knowledge of vision, features and codebase
* Review pull requests either personally or delegate to experts in the relevant area
* Helping the community

### Becoming a Maintainer

To become a maintainer, you need to demonstrate the following:

* Good understanding of vision, features and codebase
* Contribution of larger features
* Contribution of bug fixes
* Participation in design discussions
* Participation in pull request reviews
* Ability to collaborate with the team
* Helping the community

A new maintainer must be proposed by sending an email to the [developer mailing list](https://groups.google.com/forum/#!forum/keycloak-dev).
The email should include evidence of the above list.

The existing maintainers will then discuss the proposal. If anyone objects or wants more information, the maintainers 
will reach out to the nominee directly for further discussion. 

For the nominee to be accepted as a maintainer at least 2/3 of existing maintainers have to approve the nominee.


### Changes in Maintainership

Maintainers can be removed if at least 2/3 of existing maintainers agree.


## Contributing Changes

The process of reviewing proposed changes differs depending of the size and impact of the change.

### Minor Changes

A minor change is a bug fix, a smaller enhancement or a smaller addition to existing features.

To propose a minor change, simply create an issue in our [issue tracker](https://issues.jboss.org/browse/KEYCLOAK) and
send a pull request.

A maintainer will be responsible for ultimately approving the pull request. The maintainer may do a deep review of the
pull request or delegate to an expert in the corresponding area.

If the change has a bigger impact it has to follow the process for larger changes.

### Larger Changes

For larger changes all maintainers and contributors should have a chance of reviewing the change. This is done by
sending an email to the [developer mailing list](https://groups.google.com/forum/#!forum/keycloak-dev) to start a
discussion around the change.

For new features we highly recommend creating a design proposal. There is no strict requirement of the content or layout,
but it should at least cover motivation, use cases and how the feature will be used. A design proposal is created by
sending a PR to [design proposals repository](https://github.com/keycloak/keycloak-community/tree/master/design).

The contributor can decide to send a pull request prior to discussion on the mailing list, and the creation of a design
proposal. However, the change will not be accepted until it has been discussed on the mailing list.

If there are any objections to the change they can in most cases be resolved through discussions on the mailing list or
on the pull request. If a resolution can not be made it can be accepted if at least 2/3 of maintainers approve the change. 