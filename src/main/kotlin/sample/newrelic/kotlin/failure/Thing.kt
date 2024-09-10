package sample.newrelic.kotlin.failure

import io.micronaut.data.annotation.AutoPopulated
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.serde.annotation.Serdeable
import java.util.UUID

@Serdeable
@MappedEntity("thing")
class Thing(
    var thingVal: Int,
) {
    @AutoPopulated
    @field:Id
    lateinit var id: UUID
}