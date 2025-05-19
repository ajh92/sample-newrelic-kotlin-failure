package sample.newrelic.kotlin.failure

import io.micronaut.transaction.annotation.Transactional
import jakarta.inject.Singleton
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.toList

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

    fun getSlowString(): String {
        Thread.sleep(5000)
        return "Hey here is a string"
    }

    suspend fun getAllThingsFromDb(): List<Thing> {
        delay(1000)
        val things = thingRepo.findAll().toList()
        delay(1000)
        return things
    }
}
