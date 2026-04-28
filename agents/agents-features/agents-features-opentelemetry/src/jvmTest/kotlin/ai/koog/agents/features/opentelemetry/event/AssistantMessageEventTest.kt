package ai.koog.agents.features.opentelemetry.event

import ai.koog.agents.features.opentelemetry.attribute.CommonAttributes
import ai.koog.agents.features.opentelemetry.mock.MockLLMProvider
import ai.koog.prompt.message.Message
import ai.koog.prompt.message.MessagePart
import ai.koog.prompt.message.ResponseMetaInfo
import ai.koog.utils.time.KoogClock
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class AssistantMessageEventTest {

    //region Attributes

    @Test
    fun testAssistantMessageAttributes() {
        val expectedMessage = createTestAssistantMessage("Test message")
        val llmProvider = MockLLMProvider()

        val assistantMessageEvent = AssistantMessageEvent(
            provider = llmProvider,
            message = expectedMessage
        )

        val expectedAttributes = listOf(
            CommonAttributes.System(llmProvider)
        )

        assertEquals(expectedAttributes.size, assistantMessageEvent.attributes.size)
        assertContentEquals(expectedAttributes, assistantMessageEvent.attributes)
    }

    //endregion Attributes

    //region Body Fields

    @Test
    fun testToolCallMessage() {
        val toolCallPart = MessagePart.Tool.Call(id = "test-id", tool = "test-tool", args = "Test message")
        val expectedMessage = createTestAssistantMessageWithToolCall(toolCallPart)

        val assistantMessageEvent = AssistantMessageEvent(
            provider = MockLLMProvider(),
            message = expectedMessage,
        )

        val expectedBodyFields = listOf(
            EventBodyFields.Role(role = Message.Role.Assistant),
            EventBodyFields.ToolCalls(tools = listOf(toolCallPart))
        )

        assertEquals(expectedBodyFields.size, assistantMessageEvent.bodyFields.size)
        assertContentEquals(expectedBodyFields, assistantMessageEvent.bodyFields)
    }

    @Test
    fun testAssistantMessage() {
        val content = "Test message"
        val expectedMessage = createTestAssistantMessage(content)

        val assistantMessageEvent = AssistantMessageEvent(
            provider = MockLLMProvider(),
            message = expectedMessage,
        )

        val expectedBodyFields = listOf(
            EventBodyFields.Role(role = expectedMessage.role),
            EventBodyFields.Content(content = content)
        )

        assertEquals(expectedBodyFields.size, assistantMessageEvent.bodyFields.size)
        assertContentEquals(expectedBodyFields, assistantMessageEvent.bodyFields)
    }

    //endregion Body Fields

    //region Arguments Tests

    @Test
    fun testAssistantMessageWithArguments() {
        val content = "Test message"
        val expectedMessage = createTestAssistantMessage(content)
        val args = buildJsonObject {
            put("string", "value")
            put("integer", 42)
        }

        val assistantMessageEvent = AssistantMessageEvent(
            provider = MockLLMProvider(),
            message = expectedMessage,
            arguments = args,
        )

        val expectedBodyFields = listOf(
            EventBodyFields.Role(role = expectedMessage.role),
            EventBodyFields.Content(content = content),
            EventBodyFields.Arguments(args)
        )

        assertEquals(expectedBodyFields.size, assistantMessageEvent.bodyFields.size)
        assertContentEquals(expectedBodyFields, assistantMessageEvent.bodyFields)
    }

    @Test
    fun testToolCallMessageIgnoresArguments() {
        val toolCallPart = MessagePart.Tool.Call(id = "test-id", tool = "test-tool", args = "Test message")
        val expectedMessage = createTestAssistantMessageWithToolCall(toolCallPart)
        val args = buildJsonObject { put("ignored", true) }

        val assistantMessageEvent = AssistantMessageEvent(
            provider = MockLLMProvider(),
            message = expectedMessage,
            arguments = args,
        )

        val expectedBodyFields = listOf(
            EventBodyFields.Role(role = Message.Role.Assistant),
            EventBodyFields.ToolCalls(tools = listOf(toolCallPart))
        )

        assertEquals(expectedBodyFields.size, assistantMessageEvent.bodyFields.size)
        assertContentEquals(expectedBodyFields, assistantMessageEvent.bodyFields)
    }

    //endregion Arguments Tests

    //region Private Methods

    private fun createTestAssistantMessage(content: String): Message.Assistant = Message.Assistant(
        content = content,
        metaInfo = ResponseMetaInfo(KoogClock.System.now())
    )

    private fun createTestAssistantMessageWithToolCall(toolCallPart: MessagePart.Tool.Call): Message.Assistant =
        Message.Assistant(
            parts = listOf(toolCallPart),
            metaInfo = ResponseMetaInfo(KoogClock.System.now())
        )

    //endregion Private Methods
}
