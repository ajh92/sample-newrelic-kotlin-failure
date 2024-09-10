package sample.newrelic.kotlin.failure

import io.micronaut.data.annotation.Repository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.kotlin.CoroutineCrudRepository
import java.util.UUID

@R2dbcRepository(dialect = Dialect.POSTGRES)
interface ThingRepo : CoroutineCrudRepository<Thing, UUID>