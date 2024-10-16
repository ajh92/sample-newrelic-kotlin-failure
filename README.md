## sample-newrelic-kotlin-failure

This is a Micronaut application using Kotlin that demonstrates a problem integrating with newer versions of
[newrelic-java-kotlin-coroutines](https://github.com/newrelic/newrelic-java-kotlin-coroutines) and the latest Java
agent.

In what appears to be the fault of the kotlin-coroutines-instrumentation extension, it appears that R2DBC doesn't play 
nicely with Kotlin.

In a separate issue, it also appears that starting with newrelic-java 8.14.0, we're seeing Unknown traces associated
with certain controller entry points.

---

### To reproduce the R2DBC issue:

1. Download and unzip the newrelic java agent.
   ```shell
   wget https://download.newrelic.com/newrelic/java-agent/newrelic-agent/8.15.0/newrelic-java.zip && unzip -j "newrelic-java.zip" "newrelic/newrelic.jar"
   ```
2. Add the following New Relic extensions to the extensions directory:
   ```shell
   mkdir extensions
   wget https://github.com/newrelic/newrelic-java-micronaut-core/releases/download/v1.1.4/micronaut-core-instrumentation-v1.1.4.zip && unzip -j "micronaut-core-instrumentation-v1.1.4.zip" -d extensions
   wget https://github.com/newrelic/newrelic-java-micronaut-http/releases/download/v1.1.4/micronaut-http-instrumentation-v1.1.4.zip && unzip -j "micronaut-http-instrumentation-v1.1.4.zip" -d extensions
   wget https://github.com/newrelic/newrelic-java-kotlin-coroutines/releases/download/v1.0.4/kotlin-coroutines-instrumentation-v1.0.4.zip && unzip -j kotlin-coroutines-instrumentation-v1.0.4.zip -d extensions
   ```
3. If you're using IntelliJ, run this application with javaagent set to your newrelic.jar. Also provide 
   NEW_RELIC_ACCOUNT_ID, NEW_RELIC_APP_NAME, NEW_RELIC_INSERT_KEY, and NEW_RELIC_LICENSE_KEY as environment variables
   to hook in to your New Relic account.
4. Run the basic POST endpoint in the controller **more that once** (as it succeeds the first time).
   ```shell
   curl --location 'http://localhost:8080/thing/save' --header 'Content-Type: application/json' --data '{ "thingVal": 1 }'
   ```
5. Review your application's distributed tracing in New Relic.
   
---

### To reproduce the Unknown traces issue:

1. Download and unzip a newrelic java agent with versions 8.14.0 or 8.15.0.
   
   **IMPORTANT NOTE**: This issue with _not_ happen on 8.13.0, so you can later test with that agent. 
   ```shell
   wget https://download.newrelic.com/newrelic/java-agent/newrelic-agent/8.14.0/newrelic-java.zip && unzip -j "newrelic-java.zip" "newrelic/newrelic.jar"
   ``` 
2. Add the following New Relic extensions to the extensions directory:
   ```shell
   mkdir extensions
   wget https://github.com/newrelic/newrelic-java-micronaut-http/releases/download/v1.0.4/micronaut-http-instrumentation-v1.0.4.zip && unzip -j "micronaut-http-instrumentation-v1.0.4.zip" -d extensions
   wget https://github.com/newrelic/newrelic-java-micronaut-core/releases/download/v1.1.3/micronaut-core-instrumentation-v1.1.3.zip && unzip -j "micronaut-core-instrumentation-v1.1.3.zip" -d extensions
   wget https://github.com/newrelic/newrelic-java-kotlin-coroutines/releases/download/1.3/Kotlin-Coroutines_1.4.jar -P extensions
   ```
3. If you're using IntelliJ, run this application with javaagent set to your newrelic.jar. Also provide
   NEW_RELIC_ACCOUNT_ID, NEW_RELIC_APP_NAME, NEW_RELIC_INSERT_KEY, and NEW_RELIC_LICENSE_KEY as environment variables
   to hook in to your New Relic account.
4. Run the basic POST endpoints in the controller a few times
   ```shell
   curl --location 'http://localhost:8080/thing/save' --header 'Content-Type: application/json' --data '{ "thingVal": 1 }'
   curl --location 'http://localhost:8080/thing/asyncGet'
   ```
5. Review your application's distributed tracing in New Relic.

---

### Further details on R2DBC issue

Example of a real stage application experiencing this issue: https://onenr.io/08jq2xpoxQl

Example stacktrace:
```
10:38:10.740 [reactor-tcp-nio-1] ERROR i.m.http.server.RouteExecutor - Unexpected error occurred: Cannot invoke "com.newrelic.agent.Transaction.finishSegment(com.newrelic.agent.Segment, java.lang.Throwable, com.newrelic.agent.tracers.Tracer, String)" because the return value of "com.newrelic.agent.TransactionActivity.getTransaction()" is null
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
	at reactor.core.publisher.FluxMapFuseable$MapFuseableSubscriber.onNext(FluxMapFuseable.java:129)
	at reactor.core.publisher.FluxMapFuseable$MapFuseableSubscriber.onNext(FluxMapFuseable.java:129)
	at reactor.core.publisher.Operators$ScalarSubscription.request(Operators.java:2571)
	at reactor.core.publisher.FluxMapFuseable$MapFuseableSubscriber.request(FluxMapFuseable.java:171)
	at reactor.core.publisher.FluxMapFuseable$MapFuseableSubscriber.request(FluxMapFuseable.java:171)
	at reactor.core.publisher.MonoFlatMap$FlatMapMain.request(MonoFlatMap.java:194)
	at reactor.core.publisher.FluxFilterFuseable$FilterFuseableConditionalSubscriber.request(FluxFilterFuseable.java:411)
	at reactor.core.publisher.FluxMapFuseable$MapFuseableConditionalSubscriber.request(FluxMapFuseable.java:360)
	at reactor.core.publisher.FluxContextWrite$ContextWriteSubscriber.request(FluxContextWrite.java:136)
	at reactor.core.publisher.MonoToCompletableFuture.onSubscribe(MonoToCompletableFuture.java:53)
	at reactor.core.publisher.FluxContextWrite$ContextWriteSubscriber.onSubscribe(FluxContextWrite.java:101)
	at reactor.core.publisher.FluxMapFuseable$MapFuseableConditionalSubscriber.onSubscribe(FluxMapFuseable.java:265)
	at reactor.core.publisher.FluxFilterFuseable$FilterFuseableConditionalSubscriber.onSubscribe(FluxFilterFuseable.java:305)
	at reactor.core.publisher.MonoFlatMap$FlatMapMain.onSubscribe(MonoFlatMap.java:117)
	at reactor.core.publisher.FluxMapFuseable$MapFuseableSubscriber.onSubscribe(FluxMapFuseable.java:96)
	at reactor.core.publisher.FluxMapFuseable$MapFuseableSubscriber.onSubscribe(FluxMapFuseable.java:96)
	at reactor.core.publisher.MonoJust.subscribe(MonoJust.java:55)
	at reactor.core.publisher.InternalMonoOperator.subscribe(InternalMonoOperator.java:76)
	at reactor.core.publisher.MonoDeferContextual.subscribe(MonoDeferContextual.java:55)
	at reactor.core.publisher.Mono.subscribe(Mono.java:4568)
	at reactor.core.publisher.Mono.subscribeWith(Mono.java:4634)
	at reactor.core.publisher.Mono.toFuture(Mono.java:5146)
	at io.micronaut.data.runtime.operations.ReactorToAsyncOperationsAdaptor.toCompletionStage(ReactorToAsyncOperationsAdaptor.java:181)
	at io.micronaut.data.runtime.operations.ReactorToAsyncOperationsAdaptor.persist(ReactorToAsyncOperationsAdaptor.java:114)
	at io.micronaut.data.runtime.intercept.async.DefaultSaveEntityInterceptor.interceptCompletionStage(DefaultSaveEntityInterceptor.java:47)
	at io.micronaut.data.runtime.intercept.async.AbstractCountConvertCompletionStageInterceptor.intercept(AbstractCountConvertCompletionStageInterceptor.java:55)
	at io.micronaut.data.runtime.intercept.async.AbstractCountConvertCompletionStageInterceptor.intercept(AbstractCountConvertCompletionStageInterceptor.java:32)
	at io.micronaut.data.runtime.intercept.DataIntroductionAdvice.interceptCompletionStage(DataIntroductionAdvice.java:94)
	at io.micronaut.data.runtime.intercept.DataIntroductionAdvice.intercept(DataIntroductionAdvice.java:82)
	at io.micronaut.aop.chain.MethodInterceptorChain.proceed(MethodInterceptorChain.java:138)
	at sample.newrelic.kotlin.failure.ThingRepo$Intercepted.save(Unknown Source)
	at sample.newrelic.kotlin.failure.ThingService.saveThing$suspendImpl(ThingService.kt:14)
	at sample.newrelic.kotlin.failure.ThingService.saveThing(ThingService.kt)
	at sample.newrelic.kotlin.failure.$ThingService$Definition$Intercepted.$$access$$saveThing(Unknown Source)
	at sample.newrelic.kotlin.failure.$ThingService$Definition$Exec.dispatch(Unknown Source)
	at io.micronaut.context.AbstractExecutableMethodsDefinition$DispatchedExecutableMethod.invoke(AbstractExecutableMethodsDefinition.java:456)
	at io.micronaut.aop.chain.MethodInterceptorChain.proceed(MethodInterceptorChain.java:129)
	at io.micronaut.aop.internal.intercepted.KotlinInterceptedMethodImpl.interceptResultAsCompletionStage(KotlinInterceptedMethodImpl.java:115)
	at io.micronaut.aop.internal.intercepted.KotlinInterceptedMethodImpl.interceptResultAsCompletionStage(KotlinInterceptedMethodImpl.java:42)
	at io.micronaut.transaction.interceptor.TransactionalInterceptor.lambda$intercept$4(TransactionalInterceptor.java:135)
	at io.micronaut.transaction.async.AsyncUsingReactiveTransactionOperations.lambda$withTransaction$0(AsyncUsingReactiveTransactionOperations.java:69)
	at reactor.core.publisher.Mono.lambda$fromCompletionStage$0(Mono.java:563)
	at reactor.core.publisher.MonoDefer.subscribe(MonoDefer.java:45)
	at reactor.core.publisher.InternalMonoOperator.subscribe(InternalMonoOperator.java:76)
	at reactor.core.publisher.MonoDeferContextual.subscribe(MonoDeferContextual.java:55)
	at reactor.core.publisher.Flux.subscribe(Flux.java:8840)
	at reactor.core.publisher.FluxFlatMap$FlatMapMain.onNext(FluxFlatMap.java:430)
	at reactor.core.publisher.FluxConcatArray$ConcatArraySubscriber.onNext(FluxConcatArray.java:202)
	at reactor.core.publisher.Operators$ScalarSubscription.request(Operators.java:2571)
	at reactor.core.publisher.FluxConcatArray$ConcatArraySubscriber.onSubscribe(FluxConcatArray.java:194)
	at reactor.core.publisher.MonoJust.subscribe(MonoJust.java:55)
	at reactor.core.publisher.Mono.subscribe(Mono.java:4568)
	at reactor.core.publisher.FluxConcatArray$ConcatArraySubscriber.onComplete(FluxConcatArray.java:260)
	at reactor.core.publisher.MonoIgnoreElements$IgnoreElementsSubscriber.onComplete(MonoIgnoreElements.java:89)
	at reactor.core.publisher.FluxConcatArray$ConcatArraySubscriber.onComplete(FluxConcatArray.java:231)
	at reactor.core.publisher.MonoPeekTerminal$MonoTerminalPeekSubscriber.onComplete(MonoPeekTerminal.java:299)
	at reactor.core.publisher.FluxPeekFuseable$PeekConditionalSubscriber.onComplete(FluxPeekFuseable.java:940)
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
	at reactor.netty.channel.ChannelOperations.onInboundNext(ChannelOperations.java:426)
	at reactor.netty.channel.ChannelOperationsHandler.channelRead(ChannelOperationsHandler.java:114)
	at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:444)
	at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:420)
	at io.netty.channel.AbstractChannelHandlerContext.fireChannelRead(AbstractChannelHandlerContext.java:412)
	at io.netty.handler.codec.ByteToMessageDecoder.fireChannelRead(ByteToMessageDecoder.java:346)
	at io.netty.handler.codec.ByteToMessageDecoder.channelRead(ByteToMessageDecoder.java:318)
	at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:444)
	at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:420)
	at io.netty.channel.AbstractChannelHandlerContext.fireChannelRead(AbstractChannelHandlerContext.java:412)
	at io.netty.channel.DefaultChannelPipeline$HeadContext.channelRead(DefaultChannelPipeline.java:1410)
	at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:440)
	at io.netty.channel.AbstractChannelHandlerContext.invokeChannelRead(AbstractChannelHandlerContext.java:420)
	at io.netty.channel.DefaultChannelPipeline.fireChannelRead(DefaultChannelPipeline.java:919)
	at io.netty.channel.nio.AbstractNioByteChannel$NioByteUnsafe.read(AbstractNioByteChannel.java:166)
	at io.netty.channel.nio.NioEventLoop.processSelectedKey(NioEventLoop.java:788)
	at io.netty.channel.nio.NioEventLoop.processSelectedKeysOptimized(NioEventLoop.java:724)
	at io.netty.channel.nio.NioEventLoop.processSelectedKeys(NioEventLoop.java:650)
	at io.netty.channel.nio.NioEventLoop.run(NioEventLoop.java:562)
	at io.netty.util.concurrent.SingleThreadEventExecutor$4.run(SingleThreadEventExecutor.java:997)
	at io.netty.util.internal.ThreadExecutorMap$2.run(ThreadExecutorMap.java:74)
	at io.netty.util.concurrent.FastThreadLocalRunnable.run(FastThreadLocalRunnable.java:30)
	at java.base/java.lang.Thread.run(Thread.java:840)
```

New Relic Agent logs with error:

```
2024-10-10T10:38:08,809-0500 [42060 81] com.newrelic ERROR: Tracer Debug: An error occurred calling Transaction.tracerFinished() for class reactor.netty.channel.ChannelOperationsHandler : java.lang.NullPointerException: Cannot invoke "com.newrelic.agent.Transaction.activityFailedOrIgnored(com.newrelic.agent.TransactionActivity, int)" because "this.transaction" is null : this Tracer = com.newrelic.agent.tracers.OtherRootTracer@3b08f53c
2024-10-10T10:38:09,916-0500 [42060 81] com.newrelic ERROR: Tracer Debug: Inconsistent state! tracer (actual tracer popped off stack) != lastTracer (pointer to top of stack) for com.newrelic.agent.TransactionActivity@7fffffff (com.newrelic.agent.tracers.OtherRootTracer@720ae8f9 != com.newrelic.agent.tracers.DefaultTracer@7e854540)
2024-10-10T10:38:09,916-0500 [42060 81] com.newrelic ERROR: Tracer Debug: An error occurred calling Transaction.tracerFinished() for class reactor.netty.channel.ChannelOperationsHandler : java.lang.NullPointerException: Cannot invoke "com.newrelic.agent.Transaction.activityFailedOrIgnored(com.newrelic.agent.TransactionActivity, int)" because "this.transaction" is null : this Tracer = com.newrelic.agent.tracers.OtherRootTracer@720ae8f9
2024-10-10T10:38:10,742-0500 [42060 81] com.newrelic ERROR: Tracer Debug: Inconsistent state! tracer (actual tracer popped off stack) != lastTracer (pointer to top of stack) for com.newrelic.agent.TransactionActivity@7fffffff (com.newrelic.agent.tracers.OtherRootTracer@5d693b50 != com.newrelic.agent.tracers.DefaultTracer@5d6d9454)
```

---

### Further details on the Unknown trace issue:

For New Relic engineers/support agents, here is a link to some distributed tracing for a sample app that shows the 
problem: https://onenr.io/0gR7VlD4xwo 

As an aside, these traces don't include always the Redis/database calls either. Once this Unknown trace issue is
resolved with the most recent versions of the NR Java agent, figuring out the root cause of *that* will be my next goal.
I suspect that this is likely a Micronaut/Micronaut extensions problem, though.