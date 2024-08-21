# ContentGrid configuration discovery

Dynamic configuration discovery for multi-tenant ContentGrid platform services

## Usage

Your application should depend on at least 3 modules:

1. Runtime dependency on the autoconfiguration module: `com.contentgrid.configuration:contentgrid-configuration-autoconfigure`
2. Runtime dependency on a configuration discovery module (or multiple if discovering from multiple sources)
3. Implementation dependency on one (or more) configuration definitions module

A full configuration is composed from multiple ConfigurationFragments based on their composition key.
The way that different fragments are composed into a whole is definition-dependent, but typically lists are merged together.
Singular values should typically only be present once, otherwise an arbitrary value is selected.

### Configuration discovery modules

Currently, there are 2 different discovery modules available: Kubernetes and Static Spring properties.

#### Kubernetes discovery module

Module: `com.contentgrid.configuration:contentgrid-configuration-kubernetes-fabric8`

Uses the fabric8 `KubernetesClient` to discover configuration from kubernetes secrets and configmaps.
Configuration is automatically updated when changes are made in Kubernetes.

This module can be automatically configured when a `KubernetesClient` bean is available. (e.g. when using spring-cloud-kubernetes)

| Property                                                   | Type      | Description                                                                                                                                                                                                             |
|------------------------------------------------------------|-----------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `contentgrid.configuration.discovery.kubernetes.enabled`   | `boolean` | Enables configuration discovery through Kubernetes (default `true`)                                                                                                                                                     |
| `contentgrid.configuration.discovery.kubernetes.namespace` | `string`  | Sets the kubernetes namespace in which configuration discovery will be done. If unset, defaults to the namespace that the application is deployed in (or `default` if the application is running outside of Kubernetes) |


Other configuration for the Kubernetes client should be done in [spring-cloud-kubernetes](https://docs.spring.io/spring-cloud-kubernetes/docs/current/reference/html/appendix.html), or a custom created `KubernetesClient`.

#### Static spring properties discovery module

Module: `com.contentgrid.configuration:contentgrid-configuration-properties-spring`

Reads spring properties underneath `contentgrid.configuration.static`. These properties are read once on startup and are assumed not to change.

Different sub-properties are available, depending on the chosen configuration definitions modules.

| Property                                                                         | Type                  | Description                                        |
|----------------------------------------------------------------------------------|-----------------------|----------------------------------------------------|
| `contentgrid.configuration.static.<definition-name>.[config-id].composition-key` | `string`              | Composition key to combine multiple configurations |
| `contentgrid.configuration.static.<definition-name>.[config-id].configuration`   | `map<string, string>` | Configuration properties                           |

The `definition-name` is dependent on the configuration definition module.
The `config-id` is an arbitrary, unique identifier of a configuration fragment. This allows partially overriding configurations from different configuration files.

### Configuration definitions modules

Currently, there is one configuration definitions module: Contentgrid Apps.

#### ContentGrid Apps definitions module

Module: `com.contentgrid.configuration:contentgrid-configuration-contentgrid-apps`

Configuration for ContentGrid Apps.

| Configuration key               | Type           | Description                                                                   |
|---------------------------------|----------------|-------------------------------------------------------------------------------|
| `contentgrid.idp.client-id`     | `string`       | OIDC client ID for authenticating users to the application                    |
| `contentgrid.idp.client-secret` | `string`       | OIDC client Secret for confidential clients                                   |
| `contentgrid.idp.issuer-uri`    | `uri`          | OIDC issuer for authenticating users and JWT bearer tokens                    |
| `contentgrid.routing.domains`   | `list<string>` | Comma-separated list of domain names that the application will listen on      |
| `contentgrid.cors.origins`      | `list<string>` | Comma-separated list of origins that are trusted for CORS for the application |


In Kubernetes, the configuration is read from ConfigMap and Service with labels:
 * `app.contentgrid.com/service-type=gateway`
 * `app.contentgrid.com/application-id`

The `app.contentgrid.com/application-id` label is used as the composition key.

For static configuration, the `definition-name` is `contentgrid-apps`.