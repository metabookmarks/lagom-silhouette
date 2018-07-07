# Lagom silhouette

This is a [sihouette](https://www.silhouette.rocks/) integration attempt for lagom.
  
## Usage

Add dependency

```sbtshell libraryDependencies += "io.metabookmarks.lagom" %% "security" % securityVersion```

Add in routes :

```
->          /auth                            silhouette.Routes

```

Then follow the [sihouette instructions](https://www.silhouette.rocks/docs).

## Kubernetes deployment (on progress)

Following [Official docuumentation](https://developer.lightbend.com/guides/lagom-kubernetes-k8s-deploy-microservices/)

```bash
sbt -mem 2048 -DbuildTarget=kubernetes
```

### Kafka deployment

https://github.com/ramhiser/kafka-kubernetes

