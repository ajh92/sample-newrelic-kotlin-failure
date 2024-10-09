package sample.newrelic.kotlin.failure

import io.micronaut.cache.annotation.CacheConfig
import io.micronaut.http.annotation.Get
import io.micronaut.http.client.annotation.Client
import io.micronaut.retry.annotation.Retryable

@Retryable(attempts = "5", multiplier = "1.2")
@Client("https://pokeapi.co/api/v2")
@CacheConfig("pokemon")
interface OtherThingClient {
    @Get("ability/battle-armor")
    suspend fun getBattleArmor(): String
}