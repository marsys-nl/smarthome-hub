# ADR-002: Shared entity domain model across hub and app

**Status:** Accepted
**Date:** 2026-10-27

## Context

The SmartHome platform consists of a hub and (shared) mobile applications.

Both currently maintain separate domain models representing the same physical entities, their states, capabilities, requirements and context.

Although the models share many concepts, they have evolved independently to accommodate application-specific requirements.

The hub focuses on entity state management, integration communication, command processing, and event sourcing.

The mobile applications focus on entity presentation, user interactions, and maintaining locally synchronized entity state.

Maintaining separate models introduces unnecessary duplication and increases the risk of inconsistencies.

For example, capability contexts are currently implemented in the hub but are also required by the application.
Introducing them independently would duplicate both the implementation and its maintenance.

Furthermore, complex entity structures require extensive mapping between API representations and separate domain models.

A shared domain model can reduce this complexity while preserving the architectural independence of the hub and applications.

## Decision

We will introduce a shared domain model used by both the hub and mobile applications.

The shared model will define the common domain concepts and behavior required to represent and manipulate entities consistently.

Application-specific responsibilities will remain outside the shared model.

### 1. Shared entities and states

Entity definitions and their states will be shared.

This includes:

- Entity identifiers and semantic naming.
- Entity types and their supported capabilities.
- Known and unknown entity states.
- Strongly typed capability properties.
- Capability constraints and contexts.
- Common entity state operations and invariants.

Entity states will remain explicitly typed rather than represented through generic collections of capabilities.

### 2. Shared capabilities

Capability definitions will be shared between the hub and applications.

Each capability defines its domain meaning, current value, and applicable context.

Common capability behavior may be implemented directly in the shared model.

Presentation-specific properties and event-sourcing responsibilities will not be included.

### 3. Shared commands

Commands will be defined in the shared domain model.

Capability-specific commands may be associated with their corresponding capability definitions.

For example, the `OnOff` capability may define `TurnOn` and `TurnOff` commands.

Commands represent requests to perform an operation, not confirmation that the requested state change has occurred.

Command definitions will be independent of transport protocols, event sourcing, and integration implementations.

### 4. Hub-specific command processing

The hub remains responsible for processing commands and managing authoritative entity state.

Command processing includes:

- Validating commands against current entity state.
- Enforcing command-specific domain rules.
- Determining whether commands can be accepted.
- Coordinating execution through integrations.
- Processing resulting state changes.

Command processing may be implemented through dedicated domain services or deciders rather than directly within shared entity classes.

The exact processing implementation is not prescribed by this ADR.

### 5. Hub-specific event sourcing

Event sourcing remains an internal responsibility of the hub.

The hub is responsible for:

- Persisting domain events.
- Reconstructing entity state from events.
- Applying accepted events to entity state.
- Publishing relevant platform events.

Shared entities must not depend on event stores, event processors, or event-sourcing infrastructure.

The hub may introduce dedicated evolvers or aggregates to implement event-sourcing behavior.

### 6. Application specific presentation

Entity and capability descriptors will remain application-specific.

Descriptors translate domain state into presentation-oriented representations.

The application must provide explicit coverage for supported entity and capability types.

Where possible, exhaustive handling of sealed type hierarchies will be used to provide compile-time guarantees.

Descriptors may accept additional presentation context to support different representations, such as dashboards, detail screens, and historical views.

Presentation concerns must not become dependencies of the shared domain model.

### 7. Shared domain behavior

The shared model may contain behavior that is meaningful to both the hub and applications.

Examples include:

- Updating capability values.
- Replacing entity state.
- Accessing capabilities by type and context.
- Enforcing common structural invariants.
- Evaluating domain properties derived from current state.

Application-specific policies, such as rejecting or ignoring unsupported updates, remain the responsibility of the consuming application.

The shared model must not be reduced to passive data structures solely to accommodate architectural separation.

### 8. API independence

API models will remain separate from the shared domain model.

The API represents a transport contract and may intentionally use flatter structures than the domain.

Domain concepts that can be reconstructed from the entity type do not necessarily need to be transmitted.

The API must preserve information required to reconstruct meaningful domain state, including distinctions between supported, unsupported, and unavailable capability values.

Hub and application versions may evolve independently.

API compatibility must therefore not depend on both applications using the same version of the shared domain library.

### 9. Capability history and temporal metadata

Capability history will remain separate from current entity snapshots.

The shared model may eventually define common temporal metadata or history contracts.

However, this ADR does not prescribe their implementation.

The meaning and ownership of timestamps such as `observedAt`, `changedAt`, and `receivedAt` will be addressed separately.

### 10. Extensibility

The shared model will initially use strongly typed entity states and capabilities.

The architecture should avoid unnecessary restrictions that would prevent introducing more dynamic capability representations in the future.

Whether capability and entity hierarchies should be sealed will be determined based on the required balance between compile-time completeness guarantees and extensibility.

## Consequences

### Positive

- Entity and capability definitions are maintained in one location.
- Structural inconsistencies between hub and application models are reduced.
- Complex domain behavior can be reused.
- Capability contexts and constraints remain consistent.
- Less mapping code is required.
- Commands share a consistent domain language.
- The hub retains control over command processing and event sourcing.
- Applications retain control over presentation and user interaction.

### Negative

- The hub and applications become coupled to a shared domain library.
- Changes to shared models may require coordinated development.
- Shared domain definitions must remain platform-independent.
- Application-specific behavior must be separated from existing domain implementations.
- The migration requires refactoring existing entity and capability classes.

## Risks and mitigations

### Risk: Shared models accumulate application specific responsibilities

Mitigation: Keep infrastructure, presentation, and event-sourcing dependencies outside the shared module.

### Risk: Shared model changes break older application versions.

Mitigation: Maintain an independent API contract and explicitly manage backward compatibility.

### Risk: Compile-time descriptor completeness is lost.

Mitigation: Prefer exhaustive type handling where possible and introduce explicit verification where compile-time guarantees cannot be achieved.

## Risk: The refactor introduces regressions in entity state handling.

Mitigation: Migrate incrementally, beginning with Light and its capabilities, and preserve existing behavior through automated tests.

## Alternatives Considered

### Maintain separate domain models

Rejected because the models represent largely identical concepts and require duplicated maintenance.

### Share only capability definitions

Not selected because entity states and their strongly typed capability structures would remain duplicated.

### Share only entity states and capabilities

Not selected as the primary approach because a fully shared entity model also appears feasible when application-specific command processing and presentation behavior are separated.

### Share API models as domain models

Rejected because API contracts serve different purposes and intentionally omit certain domain concepts.
