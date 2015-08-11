# runrightfast-vertx-demo

## The demo features and key points

### 2015-08-01
1. co.runrightfast.vertx.demo.VertxApp
    - main class
    - shows how easy it is to start a RunRightFast Vertx app
    - the code to launch the app is centralized in co.runrightfast.vertx.core.application.RunRightFastVertxApplicationLauncher
    - provide a CLI - current options supported are:

      -c,--config    Show the application configuration as JSON
      -h,--help      Print usage
      -v,--version   Show the application version

    - registers an application MBean under the co.runrightfast JMX domain
      - used to view the deployed application version
      - exposes an operation to shutdown the app
2. Framework provides support to make it easy to develop, configure, and deploy verticles     
3. Vertx is embedded
    - see co.runrightfast.vertx.core.VertxService and its implementation
3. Application configuration is managed via TypeSafe config
4. Dagger 2 is used for DI
    - component interfaces package: co.runrightfast.vertx.core.components
      - the RunRightFastVertxApplication component interface is the main application component
5. Hazelcast is used for Vertx clustering
6. DropWizard is used for metrics and healthchecks
   - metrics are enabled by default and exposed via JMX under domain co.runrightfast.vertx.metrics
7. JDK built in logging is used

### 2015-08-02
1. First class citizen support added for HealthChecks
   - HealthChecks can be discovered and run via JMX
   - Each verticle has its owne HealthCheck registry
   - Each verticle is responsible for registering its own health checks
     - this is supported by the RunRightFastVerticle base class
     - sub-classes must implement: abstract Set<RunRightFastHealthCheck> getHealthChecks() 

### 2015-08-04
1. Integrated Docker plugin   
   - appDocker Gradle task
     - generates a Dockerfile - located within build/docker dir
     - creates a Docker image named : runrightfast/runrightfast-vertx-demo
     - requires Docker to be installed locally
   - to create a container and be able to run remote debug (port 4000) and connect to it via JMX (port 7410), run the following docker command
   
        docker create --name=runrightfast-vertx-demo -p 7410:7410 -p 4000:4000 <image_id>

### 2015-08-08
1. Added new DemoMXBean
    - used to get deployed verticles, using a ProtobufMessageProducer
    - metrics are available via JMX unser JMX domain DemoMXBean.metrics