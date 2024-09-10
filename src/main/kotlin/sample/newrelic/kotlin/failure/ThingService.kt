package sample.newrelic.kotlin.failure

import io.micronaut.transaction.annotation.Transactional
import jakarta.inject.Singleton

@Singleton
open class ThingService(
    private val thingRepo: ThingRepo,
) {
    @Transactional
    open suspend fun makeAndSaveThing(thingVal: Int) {
        val thing = Thing(thingVal)
        thingRepo.save(thing)
    }
}