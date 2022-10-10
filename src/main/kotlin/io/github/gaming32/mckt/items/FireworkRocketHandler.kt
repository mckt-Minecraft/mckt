package io.github.gaming32.mckt.items

import io.github.gaming32.mckt.PlayClient
import io.github.gaming32.mckt.World
import io.github.gaming32.mckt.objects.*
import io.github.gaming32.mckt.packet.play.s2c.ParticlePacket
import io.github.gaming32.mckt.packet.play.s2c.PlaySoundPacket
import io.github.gaming32.mckt.packet.play.s2c.SetEntityVelocityPacket
import io.github.gaming32.mckt.packet.play.s2c.SoundCategory
import io.github.gaming32.mckt.util.GaussianGenerator
import kotlinx.coroutines.launch
import kotlin.random.Random

object FireworkRocketHandler : ItemHandler() {
    override suspend fun use(
        world: World,
        client: PlayClient,
        hand: Hand
    ): ActionResultInfo<ItemStack> {
        val stack = client.data.getHeldItem(hand)
        if (!client.data.isFallFlying) return stack.pass()
        client.server.broadcast(PlaySoundPacket(
            Identifier("entity.firework_rocket.launch"),
            SoundCategory.AMBIENT,
            client.position,
            3f, 1f
        ))
        val duration = 1 + stack.getOrCreateSubNbt("Fireworks").getByte("Flight")
        val lifetime = 10 * duration * Random.nextInt(6) + Random.nextInt(7)
        client.server.mainCoroutineScope.launch {
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
                server.broadcast(ParticlePacket(
                    ParticleType.FIREWORK,
                    false,
                    client.position + client.getHeldItemVector(Identifier("firework_rocket")),
                    (GaussianGenerator.next() * 0.05).toFloat(),
                    (-velocity.y * 0.5).toFloat(),
                    (GaussianGenerator.next() * 0.05).toFloat()
                ))
                client.sendPacket(SetEntityVelocityPacket(client.entityId, velocity))
                server.waitTicks()
            }
        }
        stack.decrement()
        return stack.success(swingHand = true)
    }
}
