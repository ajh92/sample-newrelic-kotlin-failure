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

1. Send requests to `http://localhost:8080/thing/save` (e.g. using [hey](https://github.com/rakyll/hey))
    ```shell
    $ hey -n 1000 -c 50 -m POST -H "Content-Type: application/json" -d '{"thingVal": 1}' http://localhost:8080/thing/save 
   ```
1. Send requests to `http://localhost:8080/thing/asyncDbGetOne` (e.g. using [hey](https://github.com/rakyll/hey))
    ```shell
    $  hey -n 1000 -c 1000 http://localhost:8080/thing/asyncDbGetOne
   ```
1. Observe Error
```
[default-nioEventLoopGroup-1-17] ERROR i.m.http.server.RouteExecutor - Unexpected error occurred: third-party implementation of CancellableContinuation is not supported
java.lang.UnsupportedOperationException: third-party implementation of CancellableContinuation is not supported
        at kotlinx.coroutines.CancellableContinuationKt.invokeOnCancellation(CancellableContinuation.kt:240)
        at kotlinx.coroutines.CancellableContinuationKt.disposeOnCancellation(CancellableContinuation.kt:417)
        at kotlinx.coroutines.EventLoopImplBase.scheduleResumeAfterDelay(EventLoop.common.kt:238)
        at kotlinx.coroutines.DelayKt.delay(Delay.kt:126)
        at sample.newrelic.kotlin.failure.ThingService.getOneThingFromDb(ThingService.kt:35)
        at sample.newrelic.kotlin.failure.ThingController.asyncDbGetOne(ThingController.kt:27)
        at sample.newrelic.kotlin.failure.$ThingController$Definition$Exec.dispatch(Unknown Source)
        at io.micronaut.context.AbstractExecutableMethodsDefinition$DispatchedExecutableMethod.invokeUnsafe(AbstractExecutableMethodsDefinition.java:461)
        at io.micronaut.context.DefaultBeanContext$BeanContextUnsafeExecutionHandle.invokeUnsafe(DefaultBeanContext.java:4350)
        at io.micronaut.web.router.AbstractRouteMatch.execute(AbstractRouteMatch.java:237)
        at io.micronaut.web.router.DefaultUriRouteMatch.execute(DefaultUriRouteMatch.java:38)
        at io.micronaut.http.server.RouteExecutor.executeRouteAndConvertBody(RouteExecutor.java:498)
        at io.micronaut.http.server.RouteExecutor.lambda$callRoute$7(RouteExecutor.java:482)
        at reactor.core.publisher.MonoDeferContextual.subscribe(MonoDeferContextual.java:47)
        at reactor.core.publisher.InternalMonoOperator.subscribe(InternalMonoOperator.java:76)
        at io.micronaut.http.reactive.execution.ReactorExecutionFlowImpl.onComplete(ReactorExecutionFlowImpl.java:89)
        at io.micronaut.http.server.netty.NettyRequestLifecycle.handleNormal(NettyRequestLifecycle.java:98)
        at io.micronaut.http.server.netty.RoutingInBoundHandler.accept(RoutingInBoundHandler.java:235)
        at io.micronaut.http.server.netty.websocket.NettyServerWebSocketUpgradeHandler.accept(NettyServerWebSocketUpgradeHandler.java:156)
        at io.micronaut.http.server.netty.handler.PipeliningServerHandler$MessageInboundHandler.read(PipeliningServerHandler.java:415)
        at io.micronaut.http.server.netty.handler.PipeliningServerHandler.channelRead(PipeliningServerHandler.java:221)
        at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:444)
        at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:420)
        at io.netty.channel.AbstractChannelHandlerContext.fireChannelRead(AbstractChannelHandlerContext.java:412)
        at io.netty.channel.ChannelInboundHandlerAdapter.channelRead(ChannelInboundHandlerAdapter.java:93)
        at io.netty.handler.codec.http.websocketx.extensions.WebSocketServerExtensionHandler.onHttpRequestChannelRead(WebSocketServerExtensionHandler.java:158)
        at io.netty.handler.codec.http.websocketx.extensions.WebSocketServerExtensionHandler.channelRead(WebSocketServerExtensionHandler.java:82)
        at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:442)
        at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:420)
        at io.netty.channel.AbstractChannelHandlerContext.fireChannelRead(AbstractChannelHandlerContext.java:412)
        at io.netty.channel.CombinedChannelDuplexHandler$DelegatingChannelHandlerContext.fireChannelRead(CombinedChannelDuplexHandler.java:436)
        at io.netty.handler.codec.ByteToMessageDecoder.fireChannelRead(ByteToMessageDecoder.java:346)
        at io.netty.handler.codec.ByteToMessageDecoder.channelRead(ByteToMessageDecoder.java:318)
        at io.netty.channel.CombinedChannelDuplexHandler.channelRead(CombinedChannelDuplexHandler.java:251)
        at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:442)
        at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:420)
        at io.netty.channel.AbstractChannelHandlerContext.fireChannelRead(AbstractChannelHandlerContext.java:412)
        at io.netty.handler.timeout.IdleStateHandler.channelRead(IdleStateHandler.java:289)
        at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:442)
        at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:420)
        at io.netty.channel.AbstractChannelHandlerContext.fireChannelRead(AbstractChannelHandlerContext.java:412)
        at io.netty.channel.DefaultChannelPipeline$HeadContext.channelRead(DefaultChannelPipeline.java:1407)
        at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:440)
        at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:420)
        at io.netty.channel.DefaultChannelPipeline.fireChannelRead(DefaultChannelPipeline.java:918)
        at io.netty.channel.nio.AbstractNioByteChannel$NioByteUnsafe.read(AbstractNioByteChannel.java:166)
        at io.netty.channel.nio.NioEventLoop.processSelectedKey(NioEventLoop.java:788)
        at io.netty.channel.nio.NioEventLoop.processSelectedKeysOptimized(NioEventLoop.java:724)
        at io.netty.channel.nio.NioEventLoop.processSelectedKeys(NioEventLoop.java:650)
        at io.netty.channel.nio.NioEventLoop.run(NioEventLoop.java:562)
        at io.netty.util.concurrent.SingleThreadEventExecutor$4.run(SingleThreadEventExecutor.java:994)
        at io.netty.util.internal.ThreadExecutorMap$2.run(ThreadExecutorMap.java:74)
        at io.netty.util.concurrent.FastThreadLocalRunnable.run(FastThreadLocalRunnable.java:30)
        at java.base/java.lang.Thread.run(Thread.java:1583)
```

Setting `-Dnewrelic.config.class_transformer.clear_return_stacks=true` (noted [here](https://github.com/newrelic/newrelic-java-agent/pull/2307)) does not appear to make a difference.
