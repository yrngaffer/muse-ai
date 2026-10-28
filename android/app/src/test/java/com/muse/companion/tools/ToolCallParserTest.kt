package com.muse.companion.tools

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ToolCallParserTest {
    @Test fun parsesSingleBlock() {
        val calls = ToolCallParser.parse("hi\n[TOOL]\ntool: open_url\nurl: https://x.com\n[/TOOL]\nbye")
        assertThat(calls).hasSize(1)
        assertThat(calls[0].toolId).isEqualTo("open_url")
        assertThat(calls[0].args["url"]).isEqualTo("https://x.com")
    }

    @Test fun parsesMultipleBlocks() {
        val calls = ToolCallParser.parse("[TOOL]\ntool: a\n[/TOOL][TOOL]\ntool: b\n[/TOOL]")
        assertThat(calls.map { it.toolId }).containsExactly("a", "b").inOrder()
    }

    @Test fun ignoresBlockWithoutToolId() {
        assertThat(ToolCallParser.parse("[TOOL]\nfoo: bar\n[/TOOL]")).isEmpty()
    }

    @Test fun stripsBlocks() {
        assertThat(ToolCallParser.stripToolBlocks("keep [TOOL]\ntool: x\n[/TOOL] this")).contains("keep")
    }
}
