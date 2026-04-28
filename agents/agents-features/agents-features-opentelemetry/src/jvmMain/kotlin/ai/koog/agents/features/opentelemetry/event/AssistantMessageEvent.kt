package ai.koog.agents.features.opentelemetry.event

import ai.koog.agents.features.opentelemetry.attribute.CommonAttributes
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.message.Message
import ai.koog.prompt.message.MessagePart
import kotlinx.serialization.json.JsonObject

internal class AssistantMessageEvent(
    provider: LLMProvider,
    private val message: Message.Assistant,
    private val arguments: JsonObject? = null,
) : GenAIAgentEvent() {

    init {
        // Attributes
        addAttribute(CommonAttributes.System(provider))

        // Body Fields
        addBodyField(EventBodyFields.Role(role = message.role))

        var hasToolCalls = false
        message.parts.forEach { part ->
            when (part) {
                is MessagePart.Text -> {
                    addBodyField(EventBodyFields.Content(content = part.text))
                }
                is MessagePart.Reasoning -> {
                    part.content.forEach {
                        addBodyField(EventBodyFields.Content(content = it))
                    }
                }
                is MessagePart.Tool.Call -> {
                    addBodyField(EventBodyFields.ToolCalls(tools = listOf(part)))
                    hasToolCalls = true
                }
                is MessagePart.Attachment -> {
                    // Attachments are not included in event body
                }
            }
        }

        if (!hasToolCalls) {
            arguments?.let { addBodyField(EventBodyFields.Arguments(it)) }
        }
    }

    override val name: String = super.name.concatName("assistant.message")
}
