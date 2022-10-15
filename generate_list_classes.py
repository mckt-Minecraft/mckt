import json
from typing import cast, Any


with open('src/main/resources/blockStates.json') as fp:
    in_json = cast(dict[str, Any], json.load(fp))

with open('src/main/kotlin/io/github/gaming32/mckt/world/Blocks.kt', 'w') as fp:
    def p(line: str) -> None:
        print(line, file=fp)

    p('package io.github.gaming32.mckt.world')
    p('')
    p('import io.github.gaming32.mckt.GlobalPalette.DEFAULT_BLOCKSTATES')
    p('import io.github.gaming32.mckt.objects.Identifier')
    p('')
    p('@Suppress("unused")')
    p('object Blocks {')
    p('    private fun getBlock(id: String) =')
    p('        DEFAULT_BLOCKSTATES[Identifier.parse(id)] ?: throw Error("Standard block $id not found")')
    p('')
    for block in in_json.keys():
        if not block.startswith("minecraft:"):
            continue
        block = block.removeprefix("minecraft:")
        p(f'    val {block.upper()} = getBlock("{block}")')
    p('}')


with open('src/main/resources/dataexport/materials.json') as fp:
    in_json = cast(dict[str, Any], json.load(fp))

with open('src/main/kotlin/io/github/gaming32/mckt/world/Materials.kt', 'w') as fp:
    def p(line: str) -> None:
        print(line, file=fp)

    p('package io.github.gaming32.mckt.world')
    p('')
    p('import io.github.gaming32.mckt.BLOCK_MATERIALS')
    p('')
    p('@Suppress("unused")')
    p('object Materials {')
    p('    private fun getMaterial(name: String) =')
    p('        BLOCK_MATERIALS[name] ?: throw Error("Standard material $name not found")')
    p('')
    for material in in_json.keys():
        p(f'    val {material.upper()} = getMaterial("{material}")')
    p('}')
