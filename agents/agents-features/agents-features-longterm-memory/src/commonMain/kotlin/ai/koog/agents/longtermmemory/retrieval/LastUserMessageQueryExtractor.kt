package ai.koog.agents.longtermmemory.retrieval

import ai.koog.agents.core.annotation.ExperimentalAgentsApi
import ai.koog.prompt.dsl.Prompt
import ai.koog.prompt.message.Message
import ai.koog.prompt.message.MessagePart

/**
 * Default [QueryExtractor] implementation that extracts the content of the last user message from the prompt.
 */
@ExperimentalAgentsApi
public class LastUserMessageQueryExtractor : QueryExtractor {
    override fun extract(prompt: Prompt): String? {
        return prompt.messages.lastOrNull { it.role == Message.Role.User }
            ?.parts?.filterIsInstance<MessagePart.Text>()?.joinToString("\n") { it.text }
    }
}
