package ai.koog.agents.features.opentelemetry.event

import ai.koog.agents.features.opentelemetry.attribute.CommonAttributes
import ai.koog.agents.features.opentelemetry.attribute.GenAIAttributes
import ai.koog.agents.features.opentelemetry.mock.MockLLMProvider
import ai.koog.prompt.message.FinishReason
import ai.koog.prompt.message.Message
import ai.koog.prompt.message.MessagePart
import ai.koog.prompt.message.ResponseMetaInfo
import ai.koog.utils.time.KoogClock
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class ChoiceEventTest {

    //region Attributes

    @Test
    fun testChoiceAttributes() {
        val expectedContent = "Test message"
        val expectedMessage = createTestAssistantMessage(expectedContent)
        val llmProvider = MockLLMProvider()

        val choiceEvent = ChoiceEvent(
            provider = llmProvider,
            message = expectedMessage,
            index = 0,
        )

        val expectedAttributes = listOf(
            CommonAttributes.System(llmProvider)
        )

        assertEquals(expectedAttributes.size, choiceEvent.attributes.size)
        assertContentEquals(expectedAttributes, choiceEvent.attributes)
    }

    //endregion Attributes

    //region Body Fields

    @Test
    fun testAssistantMessageWithFinishReason() {
        val expectedContent = "Test message"
        val expectedMessage = createTestAssistantMessage(expectedContent, "stop")

        val choiceEvent = ChoiceEvent(
            provider = MockLLMProvider(),
            message = expectedMessage,
            index = 0,
        )

        val expectedBodyFields = listOf(
            EventBodyFields.Index(0),
            EventBodyFields.FinishReason("stop"),
            EventBodyFields.Message(expectedMessage.role, expectedContent)
        )

        assertEquals(expectedBodyFields.size, choiceEvent.bodyFields.size)
        assertContentEquals(expectedBodyFields, choiceEvent.bodyFields)
    }

    @Test
    fun testToolCallMessage() {
        val toolCallPart = MessagePart.Tool.Call(id = "test-id", tool = "test-tool", args = "Test message")
        val expectedMessage = createTestAssistantMessageWithToolCall(toolCallPart)

        val choiceEvent = ChoiceEvent(
            provider = MockLLMProvider(),
            message = expectedMessage,
            index = 0,
        )

        val expectedBodyFields = listOf(
            EventBodyFields.Index(0),
            EventBodyFields.Role(role = Message.Role.Assistant),
            EventBodyFields.ToolCalls(tools = listOf(toolCallPart)),
            EventBodyFields.FinishReason(reason = GenAIAttributes.Response.FinishReasonType.ToolCalls.id)
        )

        assertEquals(expectedBodyFields.size, choiceEvent.bodyFields.size)
        assertContentEquals(expectedBodyFields, choiceEvent.bodyFields)
    }

    @Test
    fun testAssistantMessage() {
        val expectedContent = "Test message"
        val expectedMessage = createTestAssistantMessage(expectedContent)

        val choiceEvent = ChoiceEvent(
            provider = MockLLMProvider(),
            message = expectedMessage,
            index = 0,
        )

        val expectedBodyFields = listOf(
            EventBodyFields.Index(0),
            EventBodyFields.Message(
                role = expectedMessage.role,
                content = expectedContent
            )
        )

        assertEquals(expectedBodyFields.size, choiceEvent.bodyFields.size)
        assertContentEquals(expectedBodyFields, choiceEvent.bodyFields)
    }

    //endregion Body Fields

    //region Arguments Tests

    @Test
    fun testAssistantMessageWithArguments() {
        val expectedContent = "Test message"
        val expectedMessage = createTestAssistantMessage(expectedContent)
        val args = buildJsonObject {
            put("string", "value")
            put("integer", 42)
        }

        val choiceEvent = ChoiceEvent(
            provider = MockLLMProvider(),
            message = expectedMessage,
            arguments = args,
            index = 0,
        )

        val expectedBodyFields = listOf(
            EventBodyFields.Index(0),
            EventBodyFields.Message(expectedMessage.role, expectedContent),
            EventBodyFields.Arguments(args)
        )

        assertEquals(expectedBodyFields.size, choiceEvent.bodyFields.size)
        assertContentEquals(expectedBodyFields, choiceEvent.bodyFields)
    }

    @Test
    fun testToolCallMessageIgnoresArguments() {
        val toolCallPart = MessagePart.Tool.Call(id = "test-id", tool = "test-tool", args = "Test message")
        val expectedMessage = createTestAssistantMessageWithToolCall(toolCallPart)
        val args = buildJsonObject { put("ignored", true) }

        val choiceEvent = ChoiceEvent(
            provider = MockLLMProvider(),
            message = expectedMessage,
            arguments = args,
            index = 0,
        )

        val expectedBodyFields = listOf(
            EventBodyFields.Index(0),
            EventBodyFields.Role(role = Message.Role.Assistant),
            EventBodyFields.ToolCalls(tools = listOf(toolCallPart)),
            EventBodyFields.FinishReason(reason = GenAIAttributes.Response.FinishReasonType.ToolCalls.id)
        )

        assertEquals(expectedBodyFields.size, choiceEvent.bodyFields.size)
        assertContentEquals(expectedBodyFields, choiceEvent.bodyFields)
    }

    //endregion Arguments Tests

    //region Private Methods

    private fun createTestAssistantMessage(content: String, finishReason: String? = null): Message.Assistant =
        Message.Assistant(
            content = content,
            metaInfo = ResponseMetaInfo(KoogClock.System.now()),
            finishReason = finishReason?.let { FinishReason(it) }
        )

    private fun createTestAssistantMessageWithToolCall(toolCallPart: MessagePart.Tool.Call): Message.Assistant =
        Message.Assistant(
            parts = listOf(toolCallPart),
            metaInfo = ResponseMetaInfo(KoogClock.System.now()),
            finishReason = FinishReason.ToolCall
        )

    //endregion Private Methods
}
