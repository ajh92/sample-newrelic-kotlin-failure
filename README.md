## sample-newrelic-kotlin-failure

This is a Micronaut application using Kotlin that demonstrates problems integrating with newer versions of
[newrelic-java-kotlin-coroutines](https://github.com/newrelic/newrelic-java-kotlin-coroutines), 
[newrelic-java-micronaut-http](https://github.com/newrelic/newrelic-java-micronaut-http), 
[newrelic-micronaut-java-core](https://github.com/newrelic/newrelic-java-micronaut-core) and the 
[latest Java agent](https://github.com/newrelic/newrelic-java-agent/releases/tag/v8.20.0).

With the latest Java agent, 8.20.0, we are still seeing Unknown transactions appearing. These seem to be related to netty 
and requests using the Micronaut HTTP client. 
This occurs when adding the below to `newrelic.yml` as noted
[here](https://docs.newrelic.com/docs/release-notes/agent-release-notes/java-release-notes/java-agent-8200/#fixes), 
or omitting the `netty` section altogether.
```
  netty:
    http2:
      frame_read_listener:
        start_transaction: false
```

In a separate issue, we see `NullPointerException`s when including the `Kotlin-Coroutines-Suspends.jar` extension and
accessing an R2DBC database.

This appears to be related to the `null` return on [Line 20 of SuspendTracerFactory](https://github.com/newrelic/newrelic-java-kotlin-coroutines/blob/f168d295d51a708dbb38617d308b0ba1cddb4911/Kotlin-Coroutines-Suspends/src/main/java/com/newrelic/instrumentation/kotlin/coroutines/tracing/SuspendTracerFactory.java#L20).
Replacing this with `return new NoOpTracer();` fixes the NPE issue, but I am not sure what further implications doing so might have.

---
### Test Environment: 
```shell
$ uname -a
Darwin macbookpro.lan 24.4.0 Darwin Kernel Version 24.4.0: Fri Apr 11 18:33:47 PDT 2025; root:xnu-11417.101.15~117/RELEASE_ARM64_T6000 arm64

$ sw_vers
ProductName:		macOS
ProductVersion:		15.4.1
BuildVersion:		24E263

$ java --version
openjdk 21.0.7 2025-04-15 LTS
OpenJDK Runtime Environment Corretto-21.0.7.6.1 (build 21.0.7+6-LTS)
OpenJDK 64-Bit Server VM Corretto-21.0.7.6.1 (build 21.0.7+6-LTS, mixed mode, sharing)

```

---

### In all cases:
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

---

### For Unknown Transactions
1. Add the Micronaut instrumentation JARs
    ```shell
    $ ./gradlew addMicronautInstrumentation
    ```
1. Ensure no other process is listening on port `8080`
1. Start the application with the NewRelic Java Agent
    ```shell
    $ ./gradlew run -PjvmArgs="-javaagent:$(pwd)/newrelic.jar"
    ```
1. Send requests to `http://localhost:8080/thing/asyncGet` (e.g. using [hey](https://github.com/rakyll/hey))
    ```
    $ hey -n 1000 -c 50 http://localhost:8080/thing/asyncGet
    ```
1. Observe the appearance of `Unknown` / `WebTransaction/Uri/Unknown` in New Relic Transactions

### For Kotlin Coroutine / Suspend Function NPE issue
1. Stop the running application
1. Add the Kotlin Coroutine Instrumentation extension JARs
    ```shell
    $ ./gradlew addCoroutineInstrumentation
    ```
1. Restart the application
    ```shell
    $ ./gradlew run -PjvmArgs="-javaagent:$(pwd)/newrelic.jar"
    ```
1. Send requests to `http://localhost:8080/thing/save` (e.g. using [hey](https://github.com/rakyll/hey))
    ```shell
    $ hey -n 1000 -c 50 -m POST -H "Content-Type: application/json" -d '{"thingVal": 1}' http://localhost:8080/thing/save
    ```
    Example stacktrace:
    ```
    14:50:48.425 [reactor-tcp-nio-8] ERROR i.m.http.server.RouteExecutor - Unexpected error occurred: Cannot invoke "com.newrelic.agent.Transaction.finishSegment(com.newrelic.agent.Segment, java.lang.Throwable, com.newrelic.agent.tracers.Tracer, String)" because the return value of "com.newrelic.agent.TransactionActivity.getTransaction()" is null
    java.lang.NullPointerException: Cannot invoke "com.newrelic.agent.Transaction.finishSegment(com.newrelic.agent.Segment, java.lang.Throwable, com.newrelic.agent.tracers.Tracer, String)" because the return value of "com.newrelic.agent.TransactionActivity.getTransaction()" is null
        at com.newrelic.agent.Segment$1.run(Segment.java:203)
        at com.newrelic.agent.ExpirationService.expireSegmentInline(ExpirationService.java:47)
        at com.newrelic.agent.Segment.finish(Segment.java:215)
        at com.newrelic.agent.Segment.end(Segment.java:144)
        at io.r2dbc.postgresql.R2dbcUtils.lambda$reportExecution$1(R2dbcUtils.java:66)
        at reactor.core.publisher.FluxPeek$PeekSubscriber.onSubscribe(FluxPeek.java:162)
        at reactor.core.publisher.FluxMap$MapSubscriber.onSubscribe(FluxMap.java:92)
        at reactor.core.publisher.FluxJust.subscribe(FluxJust.java:68)
        at reactor.core.publisher.InternalFluxOperator.subscribe(InternalFluxOperator.java:68)
        at reactor.core.publisher.FluxDefer.subscribe(FluxDefer.java:54)
        at reactor.core.publisher.InternalMonoOperator.subscribe(InternalMonoOperator.java:76)
        at reactor.core.publisher.MonoFlatMap$FlatMapMain.onNext(MonoFlatMap.java:165)
        at reactor.core.publisher.MonoFlatMap$FlatMapMain.secondComplete(MonoFlatMap.java:245)
        at reactor.core.publisher.MonoFlatMap$FlatMapInner.onNext(MonoFlatMap.java:305)
        at reactor.core.publisher.Operators$ScalarSubscription.request(Operators.java:2571)
        at reactor.core.publisher.MonoFlatMap$FlatMapInner.onSubscribe(MonoFlatMap.java:291)
        at reactor.core.publisher.MonoJust.subscribe(MonoJust.java:55)
        at reactor.core.publisher.InternalMonoOperator.subscribe(InternalMonoOperator.java:76)
        at reactor.core.publisher.MonoDeferContextual.subscribe(MonoDeferContextual.java:55)
        at reactor.core.publisher.InternalMonoOperator.subscribe(InternalMonoOperator.java:76)
        at reactor.core.publisher.MonoFlatMap$FlatMapMain.onNext(MonoFlatMap.java:165)
        at reactor.core.publisher.MonoFlatMap$FlatMapMain.secondComplete(MonoFlatMap.java:245)
        at reactor.core.publisher.MonoFlatMap$FlatMapInner.onNext(MonoFlatMap.java:305)
        at reactor.core.publisher.Operators$ScalarSubscription.request(Operators.java:2571)
        at reactor.core.publisher.MonoFlatMap$FlatMapInner.onSubscribe(MonoFlatMap.java:291)
        at reactor.core.publisher.MonoJust.subscribe(MonoJust.java:55)
        at reactor.core.publisher.InternalMonoOperator.subscribe(InternalMonoOperator.java:76)
        at reactor.core.publisher.MonoDeferContextual.subscribe(MonoDeferContextual.java:55)
        at reactor.core.publisher.InternalMonoOperator.subscribe(InternalMonoOperator.java:76)
        at reactor.core.publisher.MonoFlatMap$FlatMapMain.onNext(MonoFlatMap.java:165)
        at reactor.core.publisher.Operators$ScalarSubscription.request(Operators.java:2571)
        at reactor.core.publisher.MonoFlatMap$FlatMapMain.request(MonoFlatMap.java:194)
        at reactor.core.publisher.MonoFlatMap$FlatMapMain.request(MonoFlatMap.java:194)
        at reactor.core.publisher.MonoFlatMap$FlatMapMain.request(MonoFlatMap.java:194)
        at reactor.core.publisher.FluxFilterFuseable$FilterFuseableConditionalSubscriber.request(FluxFilterFuseable.java:411)
        at reactor.core.publisher.FluxMapFuseable$MapFuseableConditionalSubscriber.request(FluxMapFuseable.java:360)
        at reactor.core.publisher.FluxContextWrite$ContextWriteSubscriber.request(FluxContextWrite.java:136)
        at reactor.core.publisher.MonoToCompletableFuture.onSubscribe(MonoToCompletableFuture.java:53)
        at reactor.core.publisher.FluxContextWrite$ContextWriteSubscriber.onSubscribe(FluxContextWrite.java:101)
        at reactor.core.publisher.FluxMapFuseable$MapFuseableConditionalSubscriber.onSubscribe(FluxMapFuseable.java:265)
        at reactor.core.publisher.FluxFilterFuseable$FilterFuseableConditionalSubscriber.onSubscribe(FluxFilterFuseable.java:305)
        at reactor.core.publisher.MonoFlatMap$FlatMapMain.onSubscribe(MonoFlatMap.java:117)
        at reactor.core.publisher.MonoFlatMap$FlatMapMain.onSubscribe(MonoFlatMap.java:117)
        at reactor.core.publisher.MonoFlatMap$FlatMapMain.onSubscribe(MonoFlatMap.java:117)
        at reactor.core.publisher.MonoJust.subscribe(MonoJust.java:55)
        at reactor.core.publisher.InternalMonoOperator.subscribe(InternalMonoOperator.java:76)
        at reactor.core.publisher.MonoDeferContextual.subscribe(MonoDeferContextual.java:55)
        at reactor.core.publisher.InternalMonoOperator.subscribe(InternalMonoOperator.java:76)
        at reactor.core.publisher.MonoDeferContextual.subscribe(MonoDeferContextual.java:55)
        at reactor.core.publisher.Mono.subscribe(Mono.java:4576)
        at reactor.core.publisher.Mono.subscribeWith(Mono.java:4642)
        at reactor.core.publisher.Mono.toFuture(Mono.java:5154)
        at io.micronaut.data.runtime.operations.ReactorToAsyncOperationsAdaptor.toCompletionStage(ReactorToAsyncOperationsAdaptor.java:181)
        at io.micronaut.data.runtime.operations.ReactorToAsyncOperationsAdaptor.persist(ReactorToAsyncOperationsAdaptor.java:114)
        at io.micronaut.data.runtime.intercept.async.DefaultSaveEntityInterceptor.interceptCompletionStage(DefaultSaveEntityInterceptor.java:47)
        at io.micronaut.data.runtime.intercept.async.AbstractCountConvertCompletionStageInterceptor.intercept(AbstractCountConvertCompletionStageInterceptor.java:55)
        at io.micronaut.data.runtime.intercept.async.AbstractCountConvertCompletionStageInterceptor.intercept(AbstractCountConvertCompletionStageInterceptor.java:32)
        at io.micronaut.data.runtime.intercept.DataIntroductionAdvice.interceptCompletionStage(DataIntroductionAdvice.java:94)
        at io.micronaut.data.runtime.intercept.DataIntroductionAdvice.intercept(DataIntroductionAdvice.java:82)
        at io.micronaut.aop.chain.MethodInterceptorChain.proceed(MethodInterceptorChain.java:143)
        at sample.newrelic.kotlin.failure.ThingRepo$Intercepted.save(Unknown Source)
        at sample.newrelic.kotlin.failure.ThingService.saveThing$suspendImpl(ThingService.kt:16)
        at sample.newrelic.kotlin.failure.ThingService.saveThing(ThingService.kt)
        at sample.newrelic.kotlin.failure.$ThingService$Definition$Intercepted.$$access$$saveThing(Unknown Source)
        at sample.newrelic.kotlin.failure.$ThingService$Definition$Exec.dispatch(Unknown Source)
        at io.micronaut.context.AbstractExecutableMethodsDefinition$DispatchedExecutableMethod.invoke(AbstractExecutableMethodsDefinition.java:456)
        at io.micronaut.aop.chain.MethodInterceptorChain.proceed(MethodInterceptorChain.java:134)
        at io.micronaut.aop.internal.intercepted.KotlinInterceptedMethodImpl.interceptResultAsCompletionStage(KotlinInterceptedMethodImpl.java:116)
        at io.micronaut.aop.internal.intercepted.KotlinInterceptedMethodImpl.interceptResultAsCompletionStage(KotlinInterceptedMethodImpl.java:43)
        at io.micronaut.transaction.interceptor.TransactionalInterceptor.lambda$intercept$4(TransactionalInterceptor.java:135)
        at io.micronaut.transaction.async.AsyncUsingReactiveTransactionOperations.lambda$withTransaction$0(AsyncUsingReactiveTransactionOperations.java:69)
        at reactor.core.publisher.Mono.lambda$fromCompletionStage$0(Mono.java:563)
        at reactor.core.publisher.MonoDefer.subscribe(MonoDefer.java:45)
        at reactor.core.publisher.InternalMonoOperator.subscribe(InternalMonoOperator.java:76)
        at reactor.core.publisher.MonoDeferContextual.subscribe(MonoDeferContextual.java:55)
        at reactor.core.publisher.Mono.subscribe(Mono.java:4576)
        at reactor.core.publisher.FluxFlatMap$FlatMapMain.onNext(FluxFlatMap.java:430)
        at reactor.core.publisher.Operators$ScalarSubscription.request(Operators.java:2571)
        at reactor.core.publisher.FluxFlatMap$FlatMapMain.onSubscribe(FluxFlatMap.java:373)
        at reactor.core.publisher.FluxJust.subscribe(FluxJust.java:68)
        at reactor.core.publisher.Flux.subscribe(Flux.java:8848)
        at reactor.core.publisher.FluxFlatMap$FlatMapMain.onNext(FluxFlatMap.java:430)
        at reactor.core.publisher.FluxConcatArray$ConcatArraySubscriber.onNext(FluxConcatArray.java:180)
        at reactor.core.publisher.Operators$ScalarSubscription.request(Operators.java:2571)
        at reactor.core.publisher.FluxConcatArray$ConcatArraySubscriber.onSubscribe(FluxConcatArray.java:172)
        at reactor.core.publisher.MonoJust.subscribe(MonoJust.java:55)
        at reactor.core.publisher.Mono.subscribe(Mono.java:4576)
        at reactor.core.publisher.FluxConcatArray$ConcatArraySubscriber.onComplete(FluxConcatArray.java:238)
        at reactor.core.publisher.MonoIgnoreElements$IgnoreElementsSubscriber.onComplete(MonoIgnoreElements.java:89)
        at reactor.core.publisher.FluxConcatArray$ConcatArraySubscriber.onComplete(FluxConcatArray.java:209)
        at reactor.core.publisher.MonoIgnoreElements$IgnoreElementsSubscriber.onComplete(MonoIgnoreElements.java:89)
        at io.r2dbc.postgresql.util.FluxDiscardOnCancel$FluxDiscardOnCancelSubscriber.onComplete(FluxDiscardOnCancel.java:104)
        at reactor.core.publisher.FluxPeek$PeekSubscriber.onComplete(FluxPeek.java:260)
        at reactor.core.publisher.FluxPeek$PeekSubscriber.onComplete(FluxPeek.java:260)
        at reactor.core.publisher.FluxHandle$HandleSubscriber.onComplete(FluxHandle.java:223)
        at io.r2dbc.postgresql.util.FluxDiscardOnCancel$FluxDiscardOnCancelSubscriber.onComplete(FluxDiscardOnCancel.java:104)
        at reactor.core.publisher.FluxContextWrite$ContextWriteSubscriber.onComplete(FluxContextWrite.java:126)
        at reactor.core.publisher.FluxCreate$BaseSink.complete(FluxCreate.java:465)
        at reactor.core.publisher.FluxCreate$BufferAsyncSink.drain(FluxCreate.java:871)
        at reactor.core.publisher.FluxCreate$BufferAsyncSink.complete(FluxCreate.java:819)
        at reactor.core.publisher.FluxCreate$SerializedFluxSink.drainLoop(FluxCreate.java:249)
        at reactor.core.publisher.FluxCreate$SerializedFluxSink.drain(FluxCreate.java:215)
        at reactor.core.publisher.FluxCreate$SerializedFluxSink.complete(FluxCreate.java:206)
        at io.r2dbc.postgresql.client.ReactorNettyClient$Conversation.complete(ReactorNettyClient.java:668)
        at io.r2dbc.postgresql.client.ReactorNettyClient$BackendMessageSubscriber.emit(ReactorNettyClient.java:934)
        at io.r2dbc.postgresql.client.ReactorNettyClient$BackendMessageSubscriber.onNext(ReactorNettyClient.java:810)
        at io.r2dbc.postgresql.client.ReactorNettyClient$BackendMessageSubscriber.onNext(ReactorNettyClient.java:716)
        at reactor.core.publisher.FluxHandle$HandleSubscriber.onNext(FluxHandle.java:129)
        at reactor.core.publisher.FluxPeekFuseable$PeekConditionalSubscriber.onNext(FluxPeekFuseable.java:854)
        at reactor.core.publisher.FluxMap$MapConditionalSubscriber.onNext(FluxMap.java:224)
        at reactor.core.publisher.FluxMap$MapConditionalSubscriber.onNext(FluxMap.java:224)
        at reactor.netty.channel.FluxReceive.drainReceiver(FluxReceive.java:294)
        at reactor.netty.channel.FluxReceive.onInboundNext(FluxReceive.java:403)
        at reactor.netty.channel.ChannelOperations.onInboundNext(ChannelOperations.java:425)
        at reactor.netty.channel.ChannelOperationsHandler.channelRead(ChannelOperationsHandler.java:115)
        at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:444)
        at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:420)
        at io.netty.channel.AbstractChannelHandlerContext.fireChannelRead(AbstractChannelHandlerContext.java:412)
        at io.netty.handler.codec.ByteToMessageDecoder.fireChannelRead(ByteToMessageDecoder.java:346)
        at io.netty.handler.codec.ByteToMessageDecoder.channelRead(ByteToMessageDecoder.java:318)
        at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:444)
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

Note: This does not occur when replacing [Line 20 of SuspendTracerFactory](https://github.com/newrelic/newrelic-java-kotlin-coroutines/blob/f168d295d51a708dbb38617d308b0ba1cddb4911/Kotlin-Coroutines-Suspends/src/main/java/com/newrelic/instrumentation/kotlin/coroutines/tracing/SuspendTracerFactory.java#L20).
with `return new NoOpTracer();`, but I am not sure the implications of this.

---

Removing the `Kotlin-Coroutines-Suspends` extension also prevents this issue.
However, it is unclear to me if we still properly trace transactions that hit a suspension point in this case.
    ```
    $ rm ./extensions/Kotlin-Coroutines-Suspends.jar
    ```
    
---
For New Relic engineers/support agents, here is a [link](https://onenr.io/0BR6OkmD2jO) to an APM service that demonstrates these issues.
