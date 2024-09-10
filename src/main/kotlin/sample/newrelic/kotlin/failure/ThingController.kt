package sample.newrelic.kotlin.failure

import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Post

@Controller("thing")
class ThingController(
    private val thingService: ThingService,
) {
    @Post
    suspend fun saveThing(thingVal: Int) {
        thingService.makeAndSaveThing(thingVal)
    }
}