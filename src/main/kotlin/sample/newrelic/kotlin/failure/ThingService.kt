package sample.newrelic.kotlin.failure

import io.micronaut.transaction.annotation.Transactional
import jakarta.inject.Singleton

@Singleton
open class ThingService(
    private val thingRepo: ThingRepo,
    private val otherThingClient: OtherThingClient,
) {
    @Transactional
    open suspend fun saveThing(thingVal: Int): Boolean {
        val thing = Thing(thingVal)
        thingRepo.save(thing)
        return true
    }

    suspend fun getBattleArmor(): String = otherThingClient.getBattleArmor()

    fun getFastString(): String = "Hey here is a string"
}