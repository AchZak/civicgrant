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

// Strings because Bicep has no decimals. On Consumption, 0.5 vCPU goes with 1Gi.
@description('vCPUs per replica, written as a decimal string.')
param cpu string = '0.5'

@description('Memory per replica, paired with the CPU above.')
param memory string = '1Gi'

@description('Fewest replicas running.')
param minReplicas int = 0

@description('Most replicas running.')
param maxReplicas int = 1

var appPort = 8080

resource containerApp 'Microsoft.App/containerApps@2026-07-01' = {
  name: name
  location: location
  tags: tags

  identity: {
    type: 'UserAssigned'
    userAssignedIdentities: {
      '${identityId}': {}
    }
  }

  properties: {
    environmentId: environmentId
    workloadProfileName: 'Consumption'

    configuration: {
      activeRevisionsMode: 'Single'

      ingress: {
        external: true
        targetPort: appPort
        transport: 'auto'
        allowInsecure: false
      }

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

          // Consumption in a workload profiles environment adds no default probes.
          probes: [
            {
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
        cooldownPeriod: 300

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

output name string = containerApp.name
output fqdn string = containerApp.properties.configuration.ingress.fqdn
