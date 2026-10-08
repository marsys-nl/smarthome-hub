# ADR-001: Entity identity and semantic metadata

**Status:** Approved
**Date:** 2026-10-08

## Context

SmartHome integrates entities from different external systems, each with its own identification mechanisms and available metadata.

Each integration has its own identification mechanisms and may expose different amounts of descriptive information.

Historically, entity identifiers may contain semantic information, such as an entity's location or function (e.g. `living-room.ceiling-light`).

However, technical identity should remain independent of properties that may change throughout an entity's lifetime, including an entity's name, zone, or semantic classification.

SmartHome also aims to provide automatically localized entity names without requiring users to manually configure names for individual entities.

This requires a clear separation between:

- Technical entity identity.
- Integration-provided semantic metadata.
- User-defined metadata overrides.
- Client-side localization and presentation.

## Decision

### 1. Stable entity identity

Entity identity is managed internally by the SmartHome hub.

Each integration is responsible for providing an appropriate `EntityIdentifier` for its entities.

Identifiers may be:

- Explicitly defined in integration configuration.
- Derived from stable identifiers provided by external systems.
- Deterministically defined by the integration implementation.

Identifiers must be unique within the hub and should remain stable throughout an entity's lifetime.

Identifiers must not depend on mutable semantic properties, such as names, zones, or semantic classifications.

Consumers may receive entity identifiers for referencing entities but must not depend on their internal structure or meaning.

### 2. Discovered metadata

Entities may contain semantic metadata describing their classification and optional distinguishing characteristics.

Metadata may include, but is not limited to:

- Semantic key: A language-independent translation key describing the entity's generic type or function.
- Qualifier key: A optional language-independent translation key providing additional information to distinguish the entity from similar entities.
- Zone: A suggested zone association.

Semantic metadata does not contain a human-readable display name.

Integrations should provide as much, and the most specific reliable semantic classification available and may provide a qualifier when additional information is known.

Discovered metadata represents information provided by the integration and is not necessarily the final effective configuration.

### 3. Semantic classification

The semantic key represents a generic, language-independent classification.

For example, an integration may provide `ceiling-light` when the entity's function is known, or `light` when only its general type can be determined.

Semantic keys:

- Are not required to be unique across entities.
- Must not be used as entity identifiers.
- Must remain independent of zone membership and display language.
- Should describe an entity's type or function rather than its installation-specific identity.
- May be overridden through user configuration.

When no semantic key is provided by the integration, the hub derives a default semantic key from the entity type.

### 4. Semantic qualifier

The qualifier key provides optional additional semantic context.

For example, two ceiling lights within the same zone may share the semantic key `ceiling-light`, while using different qualifiers such as `dining-table` and `seating-area`.

Qualifiers:

- Are optional.
- Are language-independent translation keys.
- Are not required to be unique.
- Must not be used as entity identifiers.
- May be supplied by integrations or overridden through user configuration.
- Must not prescribe how or where the translated text is displayed.

A qualifier does not replace the entity's zone association.

### 5. User-defined metadata overrides

SmartHome allows users to override individual semantic metadata properties through hub configuration or, in the future, through consumers.

Overrides are maintained independently from integration-provided metadata.

Integration updates must not overwrite explicitly configured values.

Removing an override restores the latest available integration-provided value or applicable default.

The hub is responsible for managing and persisting user configuration. The persistence mechanism is an implementation detail.

### 6. Effective metadata resolution

Effective metadata is resolved using the following precedence:

1. Explicit user override.
2. Integration-provided metadata.
3. System-defined default, where applicable.
4. No value, if no suitable value is available.

Resolution is performed independently for each metadata property.

Consumers receive effective metadata without requiring knowledge of its origin or resolution process.

### 7. Zone associations

Entities may have an optional association with a zone.

The hub manages this association.

The internal representation of zone membership is not prescribed by this ADR. Consumers may receive the effective zone association as an entity property.

### 8. Metadata changes

Metadata changes are represented through domain events in accordance with SmartHome's event-sourced architecture.

Changes originating from integrations and changes originating from user configuration remain distinguishable.

The exact event types, payload structures and projection mechanisms are implementation details.

Metadata changes must not alter an entity's technical identity.

### 9. Localization and display name resolution

Entity display names are resolved at runtime by client applications.

The hub provides language-independent semantic metadata rather than localized display names.

The consumer resolves the semantic key, optional qualifier key, and zone independently into localized text.

For example:

- Semantic key: `ceiling-light`
- Zone: `living-room`

The preferred translation lookup order is:

1. `living-room.ceiling-light`
2. `ceiling-light`
3. `living-room`

The preferred language is evaluated before falling back to English.

If no suitable name or translation is available, the entity identifier may be used as a final fallback.

The ownership, storage, and distribution of translation catalogs are outside the scope of this ADR.

### 10. Context-aware presentation

Consumers may present entity information differently depending on the context in which an entity is displayed.

Consumers may present qualifiers and zones as subtitles, badges or components of a composed name.

The hub does not prescribe how these values are presented.

Neither semantic keys nor qualifiers are required to produce unique display names. Clients are responsible for presenting sufficient contextual information to distinguish similar entities.

## Consequences

### Positive

- Entity identity remains stable independently of semantic properties.
- Integrations retain responsibility for identifying their entities.
- Integrations can provide semantic metadata automatically.
- Users can override metadata without losing integration-provided values.
- Entities can be localized automatically without requiring manually assigned names.
- Generic classifications and distinguishing qualifiers are modeled separately.
- Localization does not require storing translated entity names.
- Consumers can adapt entity presentation to different contexts.
- The hub remains independent of display languages and presentation strategies.
- The model can be extended without changing entity identity.

### Negative

- Metadata management introduces additional domain concepts and resolution logic.
- Discovered metadata and user configuration must be maintained separately.
- Metadata changes require additional event processing.
- Consumers require access to suitable translation data.
- Generic classifications and qualifiers may still produce ambiguous names.
- Automatically generated names may be less descriptive than integration-provided names.
- Entity identifier changes are treated as new entities, potentially affecting references and history.

## Alternatives Considered

### Use external identifiers directly

Not adopted as a universal requirement. Integrations may reuse suitable external identifiers but remain responsible for providing stable internal identifiers.

### Encode semantic meaning in entity identifiers

Rejected because location, function, and display names may change independently of technical identity.

### Store human-readable names in entity metadata

Rejected in favor of semantic translation keys that enable automatic localization.

### Resolve display names in the hub

Rejected because localization and context-dependent presentation are client responsibilities.

### Let consumers manage all metadata

Rejected because integrations and the hub possess domain knowledge that should be shared consistently across clients.

### Store only effective metadata

Rejected because it would prevent reliable restoration of integration-provided values after removing user overrides.

### Use a suffix instead of a qualifier

Rejected because a suffix prescribes a presentation position, while a qualifier represents semantic information independent of presentation.

### Require unique semantic keys or qualifiers

Rejected because these values describe semantic properties rather than individual entity identities.

## Open Questions

- What domain events should represent integration-provided metadata changes and user-defined overrides?
- How should zone associations be represented internally within the hub?
- How should metadata overrides be loaded and persisted?
- How should translation catalogs be registered, stored, and distributed?
- How should clients handle missing translations and compose localized semantic components across different languages?

## Scope

This ADR establishes the architectural principles for entity identity, semantic metadata, user-defined overrides, semantic classification, qualifiers, and client-side localization and presentation.

Detailed event definitions, persistence implementations, translation distribution, and user-facing configuration interfaces will be addressed separately.
