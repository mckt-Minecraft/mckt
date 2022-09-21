package io.github.gaming32.mckt.items

import io.github.gaming32.mckt.objects.Vector3d
import io.github.gaming32.mckt.packet.play.s2c.SetEntityVelocityPacket
import kotlinx.coroutines.launch
import kotlin.random.Random

object FireworkRocketHandler : ItemEventHandler {
    const val DURATION = 2 // Hardcoded Duration 1 firework rockets for now

    override suspend fun use(event: ItemEventHandler.UseEvent): ItemEventHandler.Result {
        val lifetime = 10 * DURATION * Random.nextInt(6) + Random.nextInt(7)
        event.scope.launch {
            val client = event.client
            val server = event.server
            var velocity = Vector3d.ZERO
            for (i in 1..lifetime) {
                if (!client.data.isFallFlying) break
                val rotation = client.rotationVector
                velocity += Vector3d(
                    rotation.x * 0.01 + (rotation.x * 1.5 - velocity.x) * 0.5,
                    rotation.y * 0.01 + (rotation.y * 1.5 - velocity.y) * 0.5,
                    rotation.z * 0.01 + (rotation.z * 1.5 - velocity.z) * 0.5
                )
                client.sendPacket(SetEntityVelocityPacket(client.entityId, velocity.x, velocity.y, velocity.z))
                server.waitTicks()
            }
        }
        return ItemEventHandler.Result.USE_UP
    }

    override suspend fun useOnBlock(event: ItemEventHandler.BlockUseEvent): ItemEventHandler.Result {
        return ItemEventHandler.Result.PASS
    }
}
