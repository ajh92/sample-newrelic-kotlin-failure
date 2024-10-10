package sample.newrelic.kotlin.failure

import io.micronaut.cache.annotation.CacheConfig
import io.micronaut.http.annotation.Get
import io.micronaut.http.client.annotation.Client

@Client("poke-api", path = "/api/v2")
@CacheConfig("pokemon")
interface OtherThingClient {
    @Get("ability/battle-armor")
    suspend fun getBattleArmor(): String

    @Get("ability/cloud-nine")
    fun getCloudNine(): String
}