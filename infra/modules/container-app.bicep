// ============================================================
// MODULE: CONTAINER APP
// ============================================================
//
// Creates the Container App that runs the application image:
//
//   Container App   one container from the shared registry, with the environment's
//                   app identity, public HTTPS ingress, health probes and scaling
//
// Each deployment of a new image creates a new revision: an immutable snapshot of
// the image and its settings. In single revision mode, traffic moves to the new
// revision once its readiness probe passes, and the old one is stopped.
//
// Sized for the free allowance: 180,000 vCPU-seconds and 360,000 GiB-seconds a
// month per subscription, about 100 hours of one replica at the size below. With
// no traffic the app scales to zero replicas and costs nothing.


// ------------------------------------------------------------
// PARAMETERS
// Values are passed by main.bicep.
// ------------------------------------------------------------

@description('Name of the Container App.')
param name string

@description('Azure region of the app: its environment\'s region.')
param location string

@description('Tags applied to the app.')
param tags object

@description('Resource id of the Container Apps environment the app runs in.')
param environmentId string

@description('Full image name: <registry>/<repository>:<tag>.')
param image string

@description('Login server of the registry the image is pulled from.')
param registryLoginServer string

@description('Resource id of the user-assigned identity the app runs with.')
param identityId string

@description('Environment variables of the container: name and value pairs.')
param environmentVariables array

// Strings, because Bicep has no decimal numbers: json() turns the CPU into one below.
// On the Consumption profile, CPU and memory come in fixed pairs: 0.5 vCPU goes with 1Gi.
@description('vCPUs per replica, written as a decimal string.')
param cpu string = '0.5'

@description('Memory per replica, paired with the CPU above.')
param memory string = '1Gi'

// 0: with no traffic, no replica runs and nothing is billed. The price is a cold
// start: the first request after a quiet period waits for the JVM to start.
@description('Fewest replicas running.')
param minReplicas int = 0

// 1: one replica is plenty for a test environment, and caps what it can cost.
@description('Most replicas running.')
param maxReplicas int = 1


// ------------------------------------------------------------
// VARIABLES
// ------------------------------------------------------------

// The port Spring Boot listens on inside the container.
var appPort = 8080


// ------------------------------------------------------------
// RESOURCES
// ------------------------------------------------------------

// 1. The Container App.
resource containerApp 'Microsoft.App/containerApps@2026-07-01' = {
  name: name
  location: location
  tags: tags

  // The environment's app identity, created by shared.bicep. It pulls the image and
  // signs in to PostgreSQL and Application Insights. User-assigned, so it outlives
  // the app: deleting and recreating the app keeps its database role and rights.
  identity: {
    type: 'UserAssigned'
    userAssignedIdentities: {
      '${identityId}': {}
    }
  }

  properties: {
    environmentId: environmentId
    workloadProfileName: 'Consumption'  // The environment's built-in serverless profile.

    configuration: {
      // One revision at a time: a new image replaces the old one.
      activeRevisionsMode: 'Single'

      ingress: {
        external: true          // Reachable from the internet, not only inside the environment.
        targetPort: appPort     // Where the container listens. Visitors use HTTPS on 443.
        transport: 'auto'       // HTTP/1.1 or HTTP/2, whatever the client speaks.
        allowInsecure: false    // Plain HTTP is redirected to HTTPS.
      }

      // Pull the image with the app identity: no registry password anywhere.
      // Its role on the registry allows reading the civicgrant repository only.
      registries: [
        {
          server: registryLoginServer
          identity: identityId
        }
      ]
    }

    template: {
      containers: [
        {
          name: 'civicgrant'
          image: image
          resources: {
            cpu: json(cpu)
            memory: memory
          }
          env: environmentVariables

          // Spring Boot's health groups, public in SecurityConfiguration:
          //   /actuator/health/liveness   is the JVM alive? Restart the replica if not.
          //   /actuator/health/readiness  may it receive requests? Hold traffic if not.
          // Declared explicitly: Container Apps doesn't add default probes on the
          // Consumption profile of a workload profiles environment.
          probes: [
            {
              // Until it succeeds, the other two probes wait. 36 tries 5 seconds
              // apart give Spring Boot 3 minutes to start on half a vCPU, Flyway
              // migrations included. Then the replica is restarted.
              type: 'Startup'
              httpGet: {
                path: '/actuator/health/liveness'
                port: appPort
              }
              periodSeconds: 5
              timeoutSeconds: 3
              failureThreshold: 36
            }
            {
              // After startup: 3 failures in a row, 30 seconds, restart the replica.
              type: 'Liveness'
              httpGet: {
                path: '/actuator/health/liveness'
                port: appPort
              }
              periodSeconds: 10
              timeoutSeconds: 3
              failureThreshold: 3
            }
            {
              // After startup: 3 failures in a row stop traffic to the replica
              // until it passes again. No restart.
              type: 'Readiness'
              httpGet: {
                path: '/actuator/health/readiness'
                port: appPort
              }
              periodSeconds: 5
              timeoutSeconds: 3
              failureThreshold: 3
            }
          ]
        }
      ]

      scale: {
        minReplicas: minReplicas
        maxReplicas: maxReplicas

        // Seconds without traffic before the last replica stops. 300 is Azure's default.
        cooldownPeriod: 300

        // Scale on HTTP traffic: a replica starts when a request arrives, and another
        // would start above 10 concurrent requests per replica, up to maxReplicas.
        rules: [
          {
            name: 'http-requests'
            http: {
              metadata: {
                concurrentRequests: '10'
              }
            }
          }
        ]
      }
    }
  }
}


// ------------------------------------------------------------
// OUTPUTS
// ------------------------------------------------------------

output name string = containerApp.name                                   // For checking its revisions after a deployment.
output fqdn string = containerApp.properties.configuration.ingress.fqdn  // <name>.<unique>.<region>.azurecontainerapps.io
