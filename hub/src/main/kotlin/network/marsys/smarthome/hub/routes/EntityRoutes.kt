package network.marsys.smarthome.hub.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import network.marsys.smarthome.api.models.entity.EntityResponse
import network.marsys.smarthome.api.models.entity.LightEntity
import network.marsys.smarthome.api.models.entity.SystemEntity
import network.marsys.smarthome.hub.feature.entity.application.usecase.GetEntities
import network.marsys.smarthome.hub.feature.entity.domain.capability.Capability
import network.marsys.smarthome.hub.feature.entity.domain.entity.Entity
import network.marsys.smarthome.hub.feature.entity.domain.entity.Light
import network.marsys.smarthome.hub.feature.entity.domain.entity.System
import org.koin.ktor.ext.inject

fun Route.entityRoutes() {
    val getEntitiesUseCase by inject<GetEntities>()

    get("/api/entities") {
        val entities = getEntitiesUseCase.invoke()
            .map(EntityResponse::map)

        call.respond(HttpStatusCode.OK, entities)
    }
}

private fun EntityResponse.Companion.map(entity: Entity): EntityResponse =
    when (entity) {
        is Light -> LightEntity(
            identifier = entity.identifier.value,
            state = entity.state.map(),
        )

        is System -> SystemEntity(
            identifier = entity.identifier.value,
            state = entity.state.map(),
        )
    }

private inline fun <reified F : Entity.State, reified T : EntityResponse.State> F.map(): T? =
    when (this) {
        is Light.State.Known -> map() as T
        is System.State.Known -> map() as T
        else -> null
    }

private fun Light.State.Known.map(): LightEntity.State =
    LightEntity.State(
        onOff = onOff.value.current,
        brightness = brightness.value?.let { it.current.value },
    )

private fun System.State.Known.map(): SystemEntity.State =
    SystemEntity.State(
        host = SystemEntity.Host(
            device = SystemEntity.Device(
                manufacturer = info.device.manufacturer,
                model = info.device.model,
                architecture = info.device.architecture,
                physicalCores = info.device.physicalCores,
                logicalCores = info.device.logicalCores,
            ),
            operatingSystem = SystemEntity.OperatingSystem(
                description = info.operatingSystem.description,
                family = info.operatingSystem.family,
                version = info.operatingSystem.version,
                bitness = info.operatingSystem.bitness,
            ),
        ),
        processor = SystemEntity.Processor(
            load = processor.load.value.current.value,
            temperature = processor.temperature.value?.let { it.current.value },
        ),
        memory = SystemEntity.Memory(
            total = memory.total.value.current.value,
            available = memory.available.value.current.value,
            swap = SystemEntity.Swap(
                total = memory.swap.total.value.current.value,
                used = memory.swap.used.value.current.value,
            ),
        ),
        uptime = SystemEntity.Uptime(
            host = uptime.host,
            application = uptime.application,
        ),
    )

@Suppress("UNCHECKED_CAST", "UseIfInsteadOfWhen")
private val <T : Capability<*>> Capability.Optional<T>.value: T? get() =
    when (this) {
        is Capability.Available<*> -> value as T
        else -> null
    }
