package com.muse.companion.tools

import com.muse.companion.security.ToolSpec

/** Immutable set of registered tools. The broker reads [specs]; only code can add a tool here. */
class ToolRegistry(tools: List<Tool>) {
    private val byId: Map<String, Tool> = tools.associateBy { it.spec.id }
    val specs: Map<String, ToolSpec> = byId.mapValues { it.value.spec }

    fun get(id: String): Tool? = byId[id]
    fun ids(): List<String> = byId.keys.toList()
}
