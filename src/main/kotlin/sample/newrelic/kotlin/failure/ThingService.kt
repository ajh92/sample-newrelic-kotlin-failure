package sample.newrelic.kotlin.failure

import io.micronaut.transaction.annotation.Transactional
import jakarta.inject.Singleton
import java.time.Instant
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@Singleton
open class ThingService(
    private val thingRepo: ThingRepo,
    private val otherThingClient: OtherThingClient,
) {
    @Transactional
    open suspend fun makeAndSaveThing(thingVal: Int): Boolean {
        val armor = otherThingClient.getBattleArmor()
        validateNotNull(armor) { "Armor not found" }

        val sampleInstant: Instant? = Instant.now()

        sampleInstant?.let {
            validate(true) {
                "Oops"
            }
        }

        val thing = Thing(thingVal)
        thingRepo.save(thing)
        return true
    }
}

@OptIn(ExperimentalContracts::class)
inline fun validate(value: Boolean, lazyMessage: () -> String) {
    contract {
        returns() implies value
    }
    if (!value) {
        val message = lazyMessage()
        throw Exception(
            message
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun <T : Any> validateNotNull(value: T?, lazyMessage: () -> String): T {
    contract {
        returns() implies (value != null)
    }

    if (value == null) {
        val message = lazyMessage()
        throw Exception(message)
    } else {
        return value
    }
}