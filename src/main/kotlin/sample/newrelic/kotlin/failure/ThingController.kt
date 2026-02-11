package sample.newrelic.kotlin.failure

import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Controller("thing")
class ThingController(
    private val thingService: ThingService,
) {
    @Post("save")
    suspend fun saveThing(thingVal: Int): Boolean {
        thingService.saveThing(thingVal)
        return true
    }

    @Get("asyncGet")
    suspend fun asyncGet(): String = thingService.getBattleArmor()

    @Get("synchronousGet")
    fun synchronousGet(): String = thingService.getSlowString()

    @Get("asyncDbGet")
    suspend fun asyncDbGet(): List<Thing> = thingService.getAllThingsFromDb()

    @Get("asyncDbLaunch")
    suspend fun asyncDbGetLaunch() {
        val coroutineScope = CoroutineScope(Dispatchers.IO);

        val job = coroutineScope.launch {
            thingService.getOneThingFromDb()
        }

        job.join()
    }

    @Get(value = "asyncDbGetOne")
    suspend fun asyncDbGetOne(): Thing? = thingService.getOneThingFromDb()
}
