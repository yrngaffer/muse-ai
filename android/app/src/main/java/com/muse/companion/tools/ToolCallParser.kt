package com.muse.companion.tools

/** Parses `[TOOL] ... [/TOOL]` blocks from model output (same wire format as the web Muse). Pure & tested. */
object ToolCallParser {
    private val BLOCK = Regex("\\[TOOL\\]\\s*([\\s\\S]*?)\\[/TOOL\\]", RegexOption.IGNORE_CASE)

    fun parse(text: String): List<ToolCall> {
        val calls = mutableListOf<ToolCall>()
        for (m in BLOCK.findAll(text)) {
            val body = m.groupValues[1].trim()
            var toolId = ""
            val args = linkedMapOf<String, String>()
            for (line in body.lineSequence()) {
                val i = line.indexOf(':')
                if (i <= 0) continue
                val key = line.substring(0, i).trim().lowercase()
                val value = line.substring(i + 1).trim()
                if (key == "tool") toolId = value else if (key.isNotEmpty()) args[key] = value
            }
            if (toolId.isNotEmpty()) calls.add(ToolCall(toolId, args))
        }
        return calls
    }

    fun stripToolBlocks(text: String): String = BLOCK.replace(text, "").trim()
}
