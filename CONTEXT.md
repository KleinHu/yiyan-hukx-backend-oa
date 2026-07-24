# AI Assistant glossary

## AI Gateway Configuration

A user's saved connection to one OpenAI-compatible AI provider. It identifies where requests are sent and carries the user's preferred default model.

## Model Cache

The user-scoped, locally persisted snapshot of models discovered from that user's AI Gateway. It is the source for normal model selection and is refreshed only when the user explicitly requests a refresh or a newly saved gateway has no snapshot yet.

## Enabled Model

A model in the Model Cache that is currently available for the user to select and use in a chat.
