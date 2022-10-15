@file:UseSerializers(BitSetSerializer::class)

package io.github.gaming32.mckt.world

import io.github.gaming32.mckt.nbt.*
import io.github.gaming32.mckt.objects.BitSetSerializer
import io.github.gaming32.mckt.objects.BlockPosition
import kotlinx.serialization.UseSerializers

val DEFAULT_REGISTRY_CODEC = buildNbtCompound {
    putNbtCompound("minecraft:dimension_type") {
        put("type", "minecraft:dimension_type")
        putNbtList<NbtCompound>("value") {
            addNbtCompound {
                put("name", "minecraft:overworld")
                put("id", 0)
                putNbtCompound("element") {
                    put("name", "minecraft:overworld")
                    put("ultrawarm", false)
                    put("natural", true)
                    put("coordinate_scale", 1f)
                    put("has_skylight", true)
                    put("has_ceiling", false)
                    put("ambient_light", 1f)
                    put("monster_spawn_light_level", 0)
                    put("monster_spawn_block_light_limit", 0)
                    put("piglin_safe", false)
                    put("bed_works", true)
                    put("respawn_anchor_works", false)
                    put("has_raids", true)
                    put("logical_height", 4064)
                    put("min_y", -2032)
                    put("height", 4064)
                    put("infiniburn", "#minecraft:infiniburn_overworld")
                    put("effects", "minecraft:overworld")
                }
            }
        }
    }
    putNbtCompound("minecraft:worldgen/biome") {
        put("type", "minecraft:worldgen/biome")
        putNbtList<NbtCompound>("value") {
            addNbtCompound {
                put("name", "minecraft:plains")
                put("id", 0)
                putNbtCompound("element") {
                    put("name", "minecraft:plains")
                    put("precipitation", "rain")
                    put("depth", 0.125f)
                    put("temperature", 0.8f)
                    put("scale", 0.05f)
                    put("downfall", 0.4f)
                    put("category", "plains")
                    putNbtCompound("effects") {
                        put("sky_color", 0x78A7FFL)
                        put("water_fog_color", 0x050533L)
                        put("fog_color", 0xC0D8FFL)
                        put("water_color", 0x3F76E4L)
                    }
                    putNbtCompound("mood_sound") {
                        put("tick_delay", 6000)
                        put("offset", 2f)
                        put("sound", "minecraft:ambient_cave")
                        put("block_search_extent", 8)
                    }
                }
            }
        }
    }
    putNbtCompound("minecraft:chat_type") {
        put("type", "minecraft:chat_type")
        putNbtList<NbtCompound>("value") {
            addNbtCompound {
                put("name", "minecraft:chat")
                put("id", 0)
                putNbtCompound("element") {
                    putNbtCompound("chat") {
                        put("translation_key", "chat.type.text")
                        putNbtList<NbtString>("parameters") {
                            add("sender")
                            add("content")
                        }
                    }
                    putNbtCompound("narration") {
                        put("translation_key", "chat.type.text.narrate")
                        putNbtList<NbtString>("parameters") {
                            add("sender")
                            add("content")
                        }
                    }
                }
            }
            addNbtCompound {
                put("name", "minecraft:say_command")
                put("id", 1)
                putNbtCompound("element") {
                    putNbtCompound("chat") {
                        put("translation_key", "chat.type.announcement")
                        putNbtList<NbtString>("parameters") {
                            add("sender")
                            add("content")
                        }
                    }
                    putNbtCompound("narration") {
                        put("translation_key", "chat.type.text.narrate")
                        putNbtList<NbtString>("parameters") {
                            add("sender")
                            add("content")
                        }
                    }
                }
            }
            addNbtCompound {
                put("name", "minecraft:msg_command_incoming")
                put("id", 2)
                putNbtCompound("element") {
                    putNbtCompound("chat") {
                        put("translation_key", "commands.message.display.incoming")
                        putNbtCompound("style") {
                            put("color", "gray")
                            put("italic", true)
                        }
                        putNbtList<NbtString>("parameters") {
                            add("sender")
                            add("content")
                        }
                    }
                    putNbtCompound("narration") {
                        put("translation_key", "chat.type.text.narrate")
                        putNbtList<NbtString>("parameters") {
                            add("sender")
                            add("content")
                        }
                    }
                }
            }
            addNbtCompound {
                put("name", "minecraft:msg_command_outgoing")
                put("id", 3)
                putNbtCompound("element") {
                    putNbtCompound("chat") {
                        put("translation_key", "commands.message.display.outgoing")
                        putNbtCompound("style") {
                            put("color", "gray")
                            put("italic", true)
                        }
                        putNbtList<NbtString>("parameters") {
                            add("sender")
                            add("content")
                        }
                    }
                    putNbtCompound("narration") {
                        put("translation_key", "chat.type.text.narrate")
                        putNbtList<NbtString>("parameters") {
                            add("sender")
                            add("content")
                        }
                    }
                }
            }
            addNbtCompound {
                put("name", "minecraft:team_msg_command_incoming")
                put("id", 4)
                putNbtCompound("element") {
                    putNbtCompound("chat") {
                        put("translation_key", "chat.type.team.text")
                        putNbtList<NbtString>("parameters") {
                            add("target")
                            add("sender")
                            add("content")
                        }
                    }
                    putNbtCompound("narration") {
                        put("translation_key", "chat.type.text.narrate")
                        putNbtList<NbtString>("parameters") {
                            add("sender")
                            add("content")
                        }
                    }
                }
            }
            addNbtCompound {
                put("name", "minecraft:team_msg_command_outgoing")
                put("id", 5)
                putNbtCompound("element") {
                    putNbtCompound("chat") {
                        put("translation_key", "chat.type.team.sent")
                        putNbtList<NbtString>("parameters") {
                            add("target")
                            add("sender")
                            add("content")
                        }
                    }
                    putNbtCompound("narration") {
                        put("translation_key", "chat.type.text.narrate")
                        putNbtList<NbtString>("parameters") {
                            add("sender")
                            add("content")
                        }
                    }
                }
            }
            addNbtCompound {
                put("name", "minecraft:emote_command")
                put("id", 6)
                putNbtCompound("element") {
                    putNbtCompound("chat") {
                        put("translation_key", "chat.type.emote")
                        putNbtList<NbtString>("parameters") {
                            add("sender")
                            add("content")
                        }
                    }
                    putNbtCompound("narration") {
                        put("translation_key", "chat.type.emote")
                        putNbtList<NbtString>("parameters") {
                            add("sender")
                            add("content")
                        }
                    }
                }
            }
        }
    }
}

val BlockPosition.isInBuildLimit get() = y in -2032 until 2032 && isValidHorizontally
val BlockPosition.isValidForWorld get() = y in -20000000 until 20000000 && isValidHorizontally
private val BlockPosition.isValidHorizontally inline get() =
    x in -30000000 until 30000000 && z in -30000000 until 30000000
