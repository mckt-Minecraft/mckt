package io.github.gaming32.mckt.items

import io.github.gaming32.mckt.PlayClient
import io.github.gaming32.mckt.World
import io.github.gaming32.mckt.objects.*
import io.github.gaming32.mckt.packet.play.s2c.SetEntityVelocityPacket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.random.Random

object FireworkRocketHandler : ItemHandler() {
    const val DURATION = 1 // Hardcoded Duration 1 firework rockets for now

    override suspend fun use(
        world: World,
        client: PlayClient,
        hand: Hand,
        scope: CoroutineScope
    ): ActionResultInfo<ItemStack> {
        val stack = client.data.getHeldItem(hand)
        if (!client.data.isFallFlying) return stack.pass()
        val lifetime = 10 * DURATION * Random.nextInt(6) + Random.nextInt(7)
        scope.launch {
            val server = client.server
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
        stack.decrement()
        return stack.success(swingHand = true)
    }
}
