## sample-newrelic-kotlin-failure

This is a Micronaut application using Kotlin that demonstrates problems integrating with newer versions of
[newrelic-java-kotlin-coroutines](https://github.com/newrelic/newrelic-java-kotlin-coroutines),
[latest Java agent](https://github.com/newrelic/newrelic-java-agent/releases/tag/v8.20.0).

With the latest Java agent, 8.24.0, and kotlin-coroutines-instrumentation-v1.0.8., we are receiving java.lang.UnsupportedOperationException in some circumstances.

This may be related to the [implementation](https://github.com/newrelic/newrelic-java-kotlin-coroutines/blob/f9b827e5d5cb73bb7de3f207cddf9ebac0513918/Kotlin-Coroutines_1.9/src/main/java/com/newrelic/instrumentation/kotlin/coroutines_19/NRDelayCancellableContinuation.java)
of the  `CancellableContinuation<T>` interface to instrument delays. This interface should be considered [private for implementation](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/-cancellable-continuation/#:~:text=The%20interface%20itself%20is%20public%20for%20use%20and%20private%20for%20implementation.),
and third-party implementations are [specifically checked for and rejected](https://github.com/Kotlin/kotlinx.coroutines/blob/8062e9f6c21bc2672528c5e63dcff7e9057a0989/kotlinx-coroutines-core/common/src/CancellableContinuation.kt#L315).

---
### Test Environment: 
```shell
$ uname -a
Darwin macbookpro.lan 24.4.0 Darwin Kernel Version 24.4.0: Fri Apr 11 18:33:47 PDT 2025; root:xnu-11417.101.15~117/RELEASE_ARM64_T6000 arm64

$ sw_vers
ProductName:		macOS
ProductVersion:		15.6.1
BuildVersion:		24G90

$ java --version
openjdk 21.0.8 2025-07-15 LTS
OpenJDK Runtime Environment Corretto-21.0.8.9.1 (build 21.0.8+9-LTS)
OpenJDK 64-Bit Server VM Corretto-21.0.8.9.1 (build 21.0.8+9-LTS, mixed mode, sharing)

```

### Steps to Reproduce
1. Ensure the `NEW_RELIC_LICENSE_KEY` environment variable is set and exported with a valid license key
1. Set and export the `NEW_RELIC_APP_NAME` with an appropriate value
1. Start supporting infrastructure using Docker Compose
    ```shell
    $ docker-compose up -d
    ```
1. Add the New Relic Java agent to the project
    ```shell
    $ ./gradlew addAgent
    ```
1. Add the Kotlin Coroutine Instrumentation extension JARs
    ```shell
    $ ./gradlew addCoroutineInstrumentation
    ```
1. Add the Additional Micronaut Instrumentation extension JARs
    ```shell
    $ ./gradlew addMicronautInstrumentation
    ```
1. Start the application with the NewRelic Java Agent
    ```shell
    $ ./gradlew run -PjvmArgs="-Dnewrelic.config.class_transformer.clear_return_stacks=true -javaagent:$(pwd)/newrelic.jar"
    ```
1. Send requests to `http://localhost:8080/thing/save` (e.g. using [hey](https://github.com/rakyll/hey))
    ```shell
    $ hey -n 1000 -c 50 -m POST -H "Content-Type: application/json" -d '{"thingVal": 1}' http://localhost:8080/thing/save 
   ```
1. Send requests to `http://localhost:8080/thing/asyncDbGetOne` (e.g. using [hey](https://github.com/rakyll/hey))
    ```shell
    $  hey -n 1000 -c 1000 http://localhost:8080/thing/asyncDbGetOne
   ```
1. No Errors!
