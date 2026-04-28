package ai.koog.agents.features.opentelemetry.event

import ai.koog.agents.features.opentelemetry.attribute.CommonAttributes
import ai.koog.agents.features.opentelemetry.attribute.GenAIAttributes
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.message.Message
import ai.koog.prompt.message.MessagePart
import kotlinx.serialization.json.JsonObject

internal class ChoiceEvent(
    provider: LLMProvider,
    private val message: Message.Assistant,
    private val arguments: JsonObject? = null,
    val index: Int? = null,
) : GenAIAgentEvent() {

    init {
        // Attributes
        addAttribute(CommonAttributes.System(provider))

        // Body Fields
        index?.let { index -> addBodyField(EventBodyFields.Index(index)) }

        val toolCalls = message.parts.filterIsInstance<MessagePart.Tool.Call>()
        if (toolCalls.isNotEmpty()) {
            addBodyField(EventBodyFields.Role(role = message.role))
            addBodyField(EventBodyFields.ToolCalls(tools = toolCalls))
            addBodyField(EventBodyFields.FinishReason(GenAIAttributes.Response.FinishReasonType.ToolCalls.id))
        } else {
            message.finishReason?.let { reason ->
                addBodyField(EventBodyFields.FinishReason(reason))
            }
            val textContent = message.parts.filterIsInstance<MessagePart.Text>().joinToString("\n") { it.text }
            addBodyField(EventBodyFields.Message(role = message.role, content = textContent))
            arguments?.let { addBodyField(EventBodyFields.Arguments(it)) }
        }
    }

    override val name: String = super.name.concatName("choice")
}
