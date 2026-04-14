# Datadog exporter

Koog provides built-in support for exporting agent traces to [Datadog](https://www.datadoghq.com/), a monitoring and analytics platform with dedicated LLM Observability capabilities. With Datadog integration, you can visualize, analyze, and debug how your Koog agents interact with LLMs, APIs, and other components.

For background on Koog's OpenTelemetry support, see the [OpenTelemetry support](https://docs.koog.ai/opentelemetry-support/).

______________________________________________________________________

## Setup instructions

1. Create a Datadog account at <https://www.datadoghq.com/>

1. Get your API key from [Organization Settings > API Keys](https://app.datadoghq.com/organization-settings/api-keys)

1. Pass the Datadog API key to the Datadog exporter. This can be done by providing it as a parameter to the `addDatadogExporter()` function, or by setting an environment variable:

```
export DD_API_KEY="<your-api-key>"
```

4. (Optional) Configure the Datadog site. Datadog operates in multiple regions. By default, the exporter sends traces to `datadoghq.com` (US1 region). To use a different region, set the `DD_SITE` environment variable or pass the `datadogSite` parameter to `addDatadogExporter()`:

```
export DD_SITE="datadoghq.eu"
```

Common site values:

| Site                | Region        |
| ------------------- | ------------- |
| `datadoghq.com`     | US1 (default) |
| `datadoghq.eu`      | EU1           |
| `us3.datadoghq.com` | US3           |
| `us5.datadoghq.com` | US5           |
| `ap1.datadoghq.com` | AP1 (Japan)   |

## Configuration

To enable Datadog export, install the **OpenTelemetry feature** and add the `DatadogExporter`. The exporter uses `OtlpHttpSpanExporter` under the hood to send traces directly to Datadog's OTLP intake endpoint.

### Example: agent with Datadog tracing

```
fun main() = runBlocking {
    val agent = AIAgent(
        promptExecutor = promptExecutor,
        llmModel = OpenAIModels.Chat.GPT4oMini,
        systemPrompt = "You are a code assistant. Provide concise code examples."
    ) {
        install(OpenTelemetry) {
            addDatadogExporter()
        }
    }

    println("Running agent with Datadog tracing")

    val result = agent.run("Tell me a joke about programming")
    println("Result: $result\nSee traces in Datadog LLM Observability")
}
```

```
public static void main(String[] args) {
    var agent = AIAgent.builder()
        .promptExecutor(promptExecutor)
        .llmModel(OpenAIModels.Chat.GPT4oMini)
        .systemPrompt("You are a code assistant. Provide concise code examples.")
        .install(OpenTelemetry.Feature, config ->
            config.addDatadogExporter()
        )
        .build();

    System.out.println("Running agent with Datadog tracing");

    var result = agent.run("Tell me a joke about programming");
    System.out.println("Result: " + result + "\nSee traces in Datadog LLM Observability");
}
```

## Trace attributes

The `addDatadogExporter` function supports a `traceAttributes` parameter that accepts a map of resource-level attributes. These attributes are added to all exported spans and are useful for tagging traces with application-specific metadata.

Common attributes:

- **env**: Environment name (e.g., `production`, `staging`, `development`)
- **service.name**: Name of your service or application
- **version**: Application version for tracking deployments

### Example with trace attributes

```
fun main() = runBlocking {
    val agent = AIAgent(
        promptExecutor = promptExecutor,
        llmModel = OpenAIModels.Chat.GPT4oMini,
        systemPrompt = "You are a helpful assistant."
    ) {
        install(OpenTelemetry) {
            addDatadogExporter(
                datadogSite = "datadoghq.eu",  // Use EU region
                traceAttributes = mapOf(
                    "env" to "production",
                    "service.name" to "my-agent",
                    "version" to "1.0.0"
                )
            )
        }
    }

    println("Running agent with Datadog tracing")

    agent.run("What is Kotlin?")
}
```

```
public static void main(String[] args) {
    var agent = AIAgent.builder()
        .promptExecutor(promptExecutor)
        .systemPrompt("You are a helpful assistant.")
        .llmModel(OpenAIModels.Chat.GPT4oMini)
        .install(OpenTelemetry.Feature, config ->
            config.addDatadogExporter(
                null,                           // Use DD_API_KEY env var
                "datadoghq.eu",                 // Use EU region
                null,                           // Default timeout
                Map.of(
                    "env", "production",
                    "service.name", "my-agent",
                    "version", "1.0.0"
                )
            ))
        .build();

    System.out.println("Running agent with Datadog tracing");

    agent.run("What is Kotlin?");
}
```

## Custom exporter wrapping

The `buildDatadogExporter()` function is available if you need to wrap the exporter with custom decorators before registering it:

```
install(OpenTelemetry) {
    val exporter = buildDatadogExporter()
    val wrapped = MyCustomSpanExporter(exporter) // e.g. attribute post-processing
    addSpanExporter(wrapped)
}
```

## What gets traced

When enabled, the Datadog exporter captures the same spans as Koog's general OpenTelemetry integration, including:

- **Agent lifecycle events**: agent start, stop, errors
- **LLM interactions**: prompts, responses, token usage, latency
- **Tool calls**: execution traces for tool invocations
- **System context**: metadata such as model name, environment, Koog version

The exporter includes the `dd-otlp-source: llmobs` header to route spans to Datadog's LLM Observability product.

For security reasons, some content of OpenTelemetry spans is masked by default. To make the content available in Datadog, use the [setVerbose](opentelemetry-support.md#setverbose) method in the OpenTelemetry configuration and set its `verbose` argument to `true` as follows:

```
install(OpenTelemetry) {
    addDatadogExporter()
    setVerbose(true)
}
```

```
install(OpenTelemetry.Feature, config -> {
    config.addDatadogExporter();
    config.setVerbose(true);
})
```

For more details on Datadog's LLM Observability and OpenTelemetry support, see:

- [Datadog LLM Observability Docs](https://docs.datadoghq.com/llm_observability/)
- [Datadog OTLP API Intake](https://docs.datadoghq.com/opentelemetry/guide/otlp_api/)

______________________________________________________________________

## Troubleshooting

### No traces appear in Datadog

- Double-check that `DD_API_KEY` is set in your environment.
- Verify that you're using the correct `DD_SITE` for your Datadog region (`datadoghq.com` for US, `datadoghq.eu` for EU).
- Ensure that your API key has the necessary permissions to send traces.

### Authentication errors

- Check that your `DD_API_KEY` is valid and active.
- API keys can be verified in [Organization Settings > API Keys](https://app.datadoghq.com/organization-settings/api-keys).

### Connection issues

- Make sure your environment has network access to the Datadog OTLP intake endpoint (`https://otlp.<site>/v1/traces`).
- Check for any firewall or proxy settings that might block outbound connections.
