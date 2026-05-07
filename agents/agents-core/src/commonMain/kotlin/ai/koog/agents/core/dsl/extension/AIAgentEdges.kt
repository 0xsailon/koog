package ai.koog.agents.core.dsl.extension

import ai.koog.agents.core.dsl.builder.AIAgentEdgeBuilderIntermediate
import ai.koog.agents.core.dsl.builder.EdgeTransformationDslMarker
import ai.koog.agents.core.environment.SafeTool
import ai.koog.agents.core.tools.Tool
import ai.koog.prompt.message.Message
import ai.koog.prompt.message.MessagePart
import ai.koog.serialization.kotlinx.toKoogJSONObject
import kotlin.coroutines.cancellation.CancellationException
import kotlin.reflect.KClass

/**
 * Creates an edge that filters outputs based on their type.
 *
 * @param klass The class to check instance against (not actually used, see implementation comment)
 */
@EdgeTransformationDslMarker
public inline infix fun <IncomingOutput, IntermediateOutput, OutgoingInput, reified T : Any> AIAgentEdgeBuilderIntermediate<IncomingOutput, IntermediateOutput, OutgoingInput>.onIsInstance(
    /*
     klass is not used, but we need to use this trick to avoid passing all generic parameters on the usage side.
     Removing this parameter and just passing the correct type via generic reified parameter won't work, it requires all
     generic types in this case, which is not nice from the API perspective (trust me, I tried).
     */
    @Suppress("unused")
    klass: KClass<T>
): AIAgentEdgeBuilderIntermediate<IncomingOutput, T, OutgoingInput> {
    return onCondition { output -> output is T }
        .transformed { it as T }
}

/**
 * Creates an edge that filters outputs based on their MessagePart subtype.
 *
 * @param klass The MessagePart subclass to filter against
 */
@Suppress("UNCHECKED_CAST")
@EdgeTransformationDslMarker
public inline infix fun <IncomingOutput, IntermediateOutput, OutgoingInput, reified T : MessagePart> AIAgentEdgeBuilderIntermediate<IncomingOutput, IntermediateOutput, OutgoingInput>.onMessagePart(
    klass: KClass<T>
): AIAgentEdgeBuilderIntermediate<IncomingOutput, Message, OutgoingInput> {
    return onIsInstance(Message::class)
        .onCondition { message -> message.parts.any { it is T } }
}

/**
 * Creates an edge that transforms an intermediate output into a [Message.User] using the provided transform.
 *
 * @param transform A function that converts the intermediate output to a String for the user message.
 */
@EdgeTransformationDslMarker
public infix fun <IncomingOutput, IntermediateOutput, OutgoingInput> AIAgentEdgeBuilderIntermediate<IncomingOutput, IntermediateOutput, OutgoingInput>.toUserMessage(
    transform: suspend (IntermediateOutput) -> String
): AIAgentEdgeBuilderIntermediate<IncomingOutput, Message.User, OutgoingInput> {
    return transformed { llm.writeSession { userMessage(transform(it)) } }
}

/**
 * Creates an edge that transforms an intermediate output into a [Message.User] using the provided transform.
 *
 * @param transform A function that converts the intermediate output to a String for the user message.
 */
@EdgeTransformationDslMarker
public infix fun <IncomingOutput, IntermediateOutput, OutgoingInput> AIAgentEdgeBuilderIntermediate<IncomingOutput, IntermediateOutput, OutgoingInput>.toText(
    transform: suspend (String) -> String,
): AIAgentEdgeBuilderIntermediate<IncomingOutput, String, OutgoingInput> {
    return getMessageParts(MessagePart.Text::class)
        .transformed { it.joinToString("\n") { part -> part.text } }
}

/**
 * Creates an edge that filters outputs based on their MessagePart subtype.
 *
 * @param klass The MessagePart subclass to filter against
 */
@Suppress("UNCHECKED_CAST")
@EdgeTransformationDslMarker
public inline infix fun <IncomingOutput, IntermediateOutput, OutgoingInput, reified T : MessagePart> AIAgentEdgeBuilderIntermediate<IncomingOutput, IntermediateOutput, OutgoingInput>.getMessageParts(
    klass: KClass<T>
): AIAgentEdgeBuilderIntermediate<IncomingOutput, List<T>, OutgoingInput> {
    return onIsInstance(Message::class)
        .transformed { message -> message.parts.filterIsInstance<T>() }
}

/**
 * Creates an edge that filters assistant messages containing tool calls, based on a custom condition.
 * The default condition onToolCalls { true } will create a conditional edge checking that there at list one tool call
 * The custom condition onToolCalls { it.tool == "__exit__" } will create a conditional edge checking that there is tool call with the name "__exit__"
 *
 * @param block A function that evaluates whether to accept the tool call
 */
@EdgeTransformationDslMarker
public infix fun <IncomingOutput, IntermediateOutput, OutgoingInput> AIAgentEdgeBuilderIntermediate<IncomingOutput, IntermediateOutput, OutgoingInput>.onToolCall(
    block: suspend (MessagePart.Tool.Call) -> Boolean
): AIAgentEdgeBuilderIntermediate<IncomingOutput, Message.Assistant, OutgoingInput> {
    return onIsInstance(Message.Assistant::class)
        .onCondition { message ->
            message.parts.any { it is MessagePart.Tool.Call && block(it) }
        }
}

/**
 * Creates an edge that filters assistant messages containing tool calls, based on a custom condition.
 * The default condition onNoneToolCalls { true } will create a conditional edge checking that there are no tool calls in the assistant message
 * The custom condition onNoneToolCalls { it.tool == "__exit__" } will create a conditional edge checking that there are no tool call with the name "__exit__"
 *
 * @param block A function that evaluates whether to accept the tool call
 */
@EdgeTransformationDslMarker
public infix fun <IncomingOutput, IntermediateOutput, OutgoingInput> AIAgentEdgeBuilderIntermediate<IncomingOutput, IntermediateOutput, OutgoingInput>.onNoneToolCall(
    block: suspend (MessagePart.Tool.Call) -> Boolean
): AIAgentEdgeBuilderIntermediate<IncomingOutput, Message.Assistant, OutgoingInput> {
    return onIsInstance(Message.Assistant::class)
        .onCondition { message ->
            message.parts.none { it is MessagePart.Tool.Call && block(it) }
        }
}

/**
 * Creates an edge that filters assistant messages containing tool calls, based on a custom condition.
 *
 * @param block A function that evaluates whether to accept the assistant message
 */
@EdgeTransformationDslMarker
public infix fun <IncomingOutput, IntermediateOutput, OutgoingInput> AIAgentEdgeBuilderIntermediate<IncomingOutput, IntermediateOutput, OutgoingInput>.getToolCalls(
    block: suspend (MessagePart.Tool.Call) -> Boolean
): AIAgentEdgeBuilderIntermediate<IncomingOutput, List<MessagePart.Tool.Call>, OutgoingInput> {
    return onIsInstance(Message.Assistant::class)
        .transformed { message -> message.parts.filterIsInstance<MessagePart.Tool.Call>().filter { block(it) } }
}

/**
 * Creates an edge that filters assistant messages containing tool calls, based on a custom condition.
 *
 * @param block A function that evaluates whether to accept the assistant message
 */
@EdgeTransformationDslMarker
public infix fun <IncomingOutput, IntermediateOutput, OutgoingInput> AIAgentEdgeBuilderIntermediate<IncomingOutput, IntermediateOutput, OutgoingInput>.onToolResults(
    block: suspend (List<MessagePart.Tool.Result>) -> Boolean
): AIAgentEdgeBuilderIntermediate<IncomingOutput, Message.User, OutgoingInput> {
    return onIsInstance(Message.User::class)
        .onCondition { message ->
            val toolCalls = message.parts.filterIsInstance<MessagePart.Tool.Result>()
            block(toolCalls)
        }
}

/**
 * Creates an edge that filters assistant messages containing tool calls, based on a custom condition.
 *
 * @param block A function that evaluates whether to accept the assistant message
 */
@EdgeTransformationDslMarker
public infix fun <IncomingOutput, IntermediateOutput, OutgoingInput> AIAgentEdgeBuilderIntermediate<IncomingOutput, IntermediateOutput, OutgoingInput>.getToolResults(
    block: suspend (MessagePart.Tool.Result) -> Boolean
): AIAgentEdgeBuilderIntermediate<IncomingOutput, List<MessagePart.Tool.Result>, OutgoingInput> {
    return onIsInstance(Message.User::class)
        .transformed { message -> message.parts.filterIsInstance<MessagePart.Tool.Result>().filter { block(it) } }
}

/**
 * Creates an edge that filters tool call messages for a specific tool and arguments condition.
 *
 * @param tool The tool to match against
 * @param block A function that evaluates the tool arguments to determine if the edge should accept the message
 */
@EdgeTransformationDslMarker
public inline fun <IncomingOutput, IntermediateOutput, OutgoingInput, reified Args> AIAgentEdgeBuilderIntermediate<IncomingOutput, IntermediateOutput, OutgoingInput>.getToolCall(
    tool: Tool<Args, *>,
    crossinline block: suspend (Args) -> Boolean
): AIAgentEdgeBuilderIntermediate<IncomingOutput, MessagePart.Tool.Call, OutgoingInput> {
    return onIsInstance(Message.Assistant::class)
        .onCondition { message -> message.parts.any { it is MessagePart.Tool.Call && it.tool == tool.name } }
        .transformed { message ->
            message.parts.filterIsInstance<MessagePart.Tool.Call>().first { it.tool == tool.name }
        }
        .onCondition { toolCall ->
            val args = try {
                tool.decodeArgs(
                    rawArgs = toolCall.argsJson.toKoogJSONObject(),
                    serializer = config.serializer,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                return@onCondition false
            }
            block(args)
        }
}

/**
 * Creates an edge that filters tool call messages for a specific tool.
 *
 * @param tool The tool to match against
 */
@EdgeTransformationDslMarker
public infix fun <IncomingOutput, IntermediateOutput, OutgoingInput> AIAgentEdgeBuilderIntermediate<IncomingOutput, IntermediateOutput, OutgoingInput>.getToolCall(
    tool: Tool<*, *>,
): AIAgentEdgeBuilderIntermediate<IncomingOutput, MessagePart.Tool.Call, OutgoingInput> {
    return onIsInstance(Message.Assistant::class)
        .onCondition { message -> message.parts.any { it is MessagePart.Tool.Call && it.tool == tool.name } }
        .transformed { message ->
            message.parts.filterIsInstance<MessagePart.Tool.Call>().first { it.tool == tool.name }
        }
}

/**
 * Filters and transforms the intermediate outputs of the AI agent node based on the success results of a tool operation.
 *
 * This method is used to create a conditional path in the agent's execution by selecting only the successful results
 * of type [SafeTool.Result.Success] and evaluating them against a provided condition.
 *
 * @param condition A suspending lambda function that accepts a result of type [TResult]
 *                  and evaluates it to a Boolean value. Returns `true` if the condition is satisfied,
 *                  and `false` otherwise.
 * @return An instance of [AIAgentEdgeBuilderIntermediate] configured to handle only successful tool results
 *         that satisfy the specified condition, with output type adjusted to [SafeTool.Result.Success].
 */
@Suppress("UNCHECKED_CAST")
@EdgeTransformationDslMarker
public inline infix fun <IncomingOutput, OutgoingInput, reified TResult> AIAgentEdgeBuilderIntermediate<IncomingOutput, SafeTool.Result<TResult>, OutgoingInput>.onSuccessful(
    crossinline condition: suspend (TResult) -> Boolean
): AIAgentEdgeBuilderIntermediate<IncomingOutput, SafeTool.Result.Success<TResult>, OutgoingInput> =
    onIsInstance(SafeTool.Result.Success::class).transformed { it as SafeTool.Result.Success<TResult> }
        .onCondition {
            condition(it.result)
        }

/**
 * Defines a handler to process failure cases in a directed edge strategy by applying a condition
 * to filter intermediate results of type `SafeTool.Result.Failure`. This method is used to specialize
 * processing for failure results and to propagate or transform them based on the provided condition.
 *
 * @param condition A suspending lambda function that takes an error message string as input and returns a boolean.
 *                  It specifies whether the error should be further processed based on the condition provided.
 * @return A new instance of `AIAgentEdgeBuilderIntermediate`, where the intermediate output type is restricted
 *         to `SafeTool.Result.Failure` containing the specified `TResult` for failure results that match the condition.
 */
@Suppress("UNCHECKED_CAST")
@EdgeTransformationDslMarker
public inline infix fun <IncomingOutput, OutgoingInput, reified TResult> AIAgentEdgeBuilderIntermediate<IncomingOutput, SafeTool.Result<TResult>, OutgoingInput>.onFailure(
    crossinline condition: suspend (error: String) -> Boolean
): AIAgentEdgeBuilderIntermediate<IncomingOutput, SafeTool.Result.Failure<TResult>, OutgoingInput> =
    onIsInstance(SafeTool.Result.Failure::class).transformed { it as SafeTool.Result.Failure<TResult> }
        .onCondition {
            condition(it.message)
        }
