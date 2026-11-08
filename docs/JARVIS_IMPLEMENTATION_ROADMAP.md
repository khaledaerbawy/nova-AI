# NOVA JARVIS Engineering Program

Living implementation plan for evolving the existing Android assistant into a secure, multi-device personal AI system. This document distinguishes verified repository capabilities from planned work; a feature is not complete until its acceptance gate passes.

## 1. Product target

NOVA is a voice-first personal AI system with a shared reasoning and memory model, connected to permission-scoped clients and tools on Android, Windows, and supported smart-home systems. Android remains the first-class runtime. Cloud services are optional accelerators, not mandatory for core device safety or private local actions.

### Non-negotiable engineering principles

- Build on existing working code; do not duplicate the agent, memory, provider, action, or security systems.
- Introduce interfaces and adapters incrementally before broad rewrites.
- Keep device state, private memory, and local actions on-device whenever practical.
- Make cloud calls explicit, observable, configurable, and safely recoverable.
- Every tool needs a schema, risk classification, permission contract, timeout, cancellation path, and execution result.
- Treat web, notification, document, and OCR text as untrusted data, never privileged instructions.
- Require policy-based confirmation for destructive, financial, communication, and privacy-sensitive actions.
- Do not claim release readiness without CI, artifact inspection, and physical-device acceptance tests.

## 2. Repository baseline verified at PR #2 head d612a407

The current codebase is a substantial Android application in a single Gradle module named :app, not a greenfield project.

| Capability | Existing implementation | Baseline assessment |
|---|---|---|
| Agent and planning | AgentLoop, PlanManager, ActionSequenceExecutor, PlanValidator, ReEvaluationEngine | Present; end-to-end quality and reliability need measurement |
| Intent and Arabic | IntentClassifier, ArabicCommandHeuristics, AliasResolver | Present; expand Egyptian Arabic evaluation set |
| LLM routing | LLMProviderFactory, ProviderSelectionPolicy, HybridOnDeviceProvider, cloud and local providers | Present; formalize routing policy and telemetry |
| Memory | Working, Episodic, Semantic, Procedural memory and PersonalKnowledgeGraph | Present; evaluate retrieval, conflict resolution and user controls |
| Voice | WakeWordDetector, SherpaKwsWakeWordDetector, SpeechRecognitionEngine, TextToSpeechEngine | Present; verify lifecycle, interruption, latency and Egyptian voice quality |
| Device actions | ActionDispatcher plus system, communication, media, calendar, finance, routines and other handlers | Present; audit Android-version and permission behavior |
| Automation | Accessibility service, app automators, routines, macros and approvals | Present; strengthen safety, recovery and compatibility tests |
| Smart home | SmartHomeActions and tests | Partial; connector/protocol coverage and real-device tests required |
| Vision | VisionEngine for screen/multimodal flows; LiveCameraDialog with CameraX and ML Kit | Partial; live-camera frame-to-VisionEngine pipeline and privacy lifecycle required |
| Security | Keystore-backed credential/profile stores, risk policy, approval policy, backup exclusions | Present foundation; threat-model and permission audit required |
| UI | Jetpack Compose screens for chat, memory, plans, settings and camera | Present; visual consistency and accessibility review required |
| Backend | No dedicated backend service/module found in repository tree | Not implemented as a deployable service |
| Desktop | No Windows/Desktop agent found in repository tree | Not implemented |
| Multi-device sync | No complete cross-device identity/session/sync service found | Not implemented |

### Immediate baseline issue

The Android manifest declares android.permission.CAMERA twice. Phase 0 removes the duplicate declaration. At audit time GitHub Actions run 158 is still executing the Gradle test/build step and has no uploaded artifact yet.

## 3. Target architecture

The target is a modular monolith first, then separately deployable companion clients/services where justified. Avoid premature microservices.

- Android app: composition root, navigation, lifecycle and UI.
- Core domain: use cases, entities, ports and policy contracts.
- Core agent: intent, context, planning, orchestration and task state.
- Core memory: memory models, retrieval, retention, consent and encryption boundaries.
- Core AI: provider contracts, model registry, routing, streaming and budgets.
- Core voice: wake word, audio session, STT, TTS, interruption and voice selection.
- Core vision: camera/screen/image inputs, frame sampling, OCR, detection and multimodal analysis.
- Core security: permission broker, risk policy, approvals, audit log and secret storage.
- Core tools: typed tool registry, validation, dispatch, cancellation and result verification.
- Feature modules: independently testable Android UI/features.
- Integration modules: Home Assistant, MQTT and external provider adapters.
- Backend: optional sync, identity, device registry and push relay; only after a threat model.
- Windows companion: opt-in desktop agent with explicit capabilities and authenticated local IPC.
- Docs: architecture decisions, threat model, API contracts, test plans and release evidence.

Migration rule: extract one boundary at a time while preserving compatibility. Do not move the whole application into Gradle modules in one risky change.

## 4. Delivery phases and acceptance gates

### Phase 0 — Baseline, stabilization and architecture
Inspect PR/CI, map existing code, remove manifest duplication, document capability status, establish architecture decisions and test gates.

Exit gate: manifest/static checks clean; unit tests and debug APK build pass on the same commit; device-test checklist documented; no unexplained permission or dependency additions.

### Phase 1 — Intelligence core and memory
Reuse AgentLoop, IntentClassifier, PlanManager, ReEvaluationEngine, LLMProviderFactory, MemoryManager and memory graph.

Build an explicit AssistantRequest to Intent to Context to Plan to ToolCalls to VerifiedResult flow; Egyptian Arabic benchmark fixtures; provider capability registry; deterministic local/cloud routing policy; memory provenance, confidence, expiration, conflict resolution, search evaluation, and user inspect/export/delete controls.

Exit gate: golden tests for Arabic/English intent; plans validate before execution; memory retrieval tests meet an agreed target; provider outage produces safe, visible fallback.

### Phase 2 — Voice experience
Reuse existing wake-word, speech-recognition and TTS engines.

Build an audio-session state machine; configurable wake phrase; streaming partial transcripts; barge-in/interruption; echo/noise handling; voice catalog and locale/quality selection; foreground-service lifecycle and battery policy.

Exit gate: real-device tests across supported Android versions; measured wake false-accept/false-reject, response latency, interruption and battery budgets. Advertise an Egyptian feminine voice only where a tested provider actually supports it.

### Phase 3 — Unified vision
Reuse VisionEngine, CameraX LiveCameraDialog, ML Kit detection and existing screen capture.

Build a common VisionInput abstraction for still image, sampled live frames and screen capture; frame throttling/backpressure; preprocessing; OCR/detection plus multimodal model adapter; grounded confidence; consent indicator; pause/stop; cache cleanup and retention controls.

Exit gate: a captured frame flows through VisionEngine and produces a grounded result; permission denial and camera loss recover cleanly; no background capture; latency, thermal and battery tests pass.

### Phase 4 — Android device control
Reuse ActionDispatcher, typed action models, native Android actions, accessibility service and permission screens.

Build a canonical Tool Registry; declared input/output schemas; capability discovery per Android version; permission broker; dry-run/preview for risky actions; idempotency keys; cancellation and post-action verification; audit trail.

Exit gate: every supported action has positive, negative, permission-denied and cancellation tests; high-risk actions cannot bypass confirmation; unsupported APIs are never reported as successful.

### Phase 5 — Reliable autonomous agent
Reuse AgentLoop, PlanManager, ActionSequenceExecutor, approvals and re-evaluation.

Build durable task state machine; resumable WorkManager jobs where appropriate; deadlines and budgets; retry classification; checkpointing; dependency graph; user pause/stop; tool isolation; prompt-injection boundaries; result verification and truthful final summaries.

Exit gate: multi-tool scenarios survive process death/network loss; retries do not duplicate side effects; cancellation is honored; agent cannot elevate tool permissions.

### Phase 6 — Smart-home ecosystem
Reuse SmartHomeActions.

Build Home Assistant integration first, then MQTT adapter; device/entity discovery; secure local endpoint pairing; scoped tokens; confirmation for locks/security-critical devices; state reconciliation and offline handling.

Exit gate: tests against a real Home Assistant instance and at least one physical device; no broad network scanning; credentials encrypted; stale state is never presented as live.

### Phase 7 — Windows companion
Build a separately packaged Windows agent with signed installation, local permission/capability registry, allowlisted tools, user-visible activity, secure pairing and authenticated encrypted transport. Start with read-only file search and app launch; add screen capture and UI automation only after consent and threat review.

Exit gate: paired-device authentication, revocation, least-privilege tests, network-loss recovery and signed build pipeline pass. Android remains functional if desktop is offline.

### Phase 8 — Release hardening and operations
Build release signing outside the repository, SBOM/dependency review, vulnerability scanning, crash/privacy review, accessibility review, performance and battery budgets, migration/rollback plan, staged rollout, support diagnostics and release notes.

Exit gate: CI green on release candidate; APK signature verified; regression suite passes; physical-device matrix signed off; known limitations documented; no critical/high security finding remains open.

## 5. Cross-cutting quality gates

- Unit tests for domain policies and parsers.
- Integration tests for Room, provider adapters, tool dispatch and background work.
- Instrumented tests for camera, audio, permissions, lifecycle and accessibility.
- Contract tests for each external integration.
- Security tests for prompt injection, credential redaction, approval bypass, replay and privilege escalation.
- Performance tests for cold start, memory, model load, camera thermal load, voice latency and battery.
- CI runs formatting/static checks, unit tests, lint, debug build and artifact upload.
- Device acceptance is separate from CI; emulator success is not proof of OEM/physical-device behavior.

## 6. Execution policy

1. Work only on feat/nova-memory-agent-persona or a dedicated follow-up branch; never push directly to main.
2. Keep changes small, reviewable and phase-labelled in commit messages.
3. Do not add a backend, database, paid model, cloud relay or new permission without a concrete requirement and cost/privacy review.
4. Never commit secrets, signing keys, user data, camera frames or transcripts.
5. A phase is complete only when its exit gate is evidenced in CI and/or device-test records.
6. PRs remain open for review; merging is a separate user decision.

## 7. Current execution ledger

| Phase | Status at baseline |
|---|---|
| 0 — Baseline | In progress; repository map and plan committed; duplicate CAMERA declaration removed in 4fac54f; latest validation run pending |
| 1 — Intelligence and memory | Existing foundations; evaluation and targeted integration work needed |
| 2 — Voice | Existing foundations; device validation and UX reliability work needed |
| 3 — Vision | Partial; live camera exists, unified live-frame multimodal pipeline not verified |
| 4 — Device control | Broad existing action layer; capability/permission/verification audit needed |
| 5 — Autonomous agent | Existing planner/agent; durable execution and failure-injection validation needed |
| 6 — Smart home | Guarded: unconfigured actions now fail honestly instead of claiming device control (50a7c45); real Home Assistant/MQTT adapter and physical-device acceptance pending |
| 7 — Windows | Not implemented |
| 8 — Release hardening | CI pipeline exists; current run and physical-device/release gates pending |

Update this ledger with commit links, CI run IDs, test evidence and explicit blockers as work progresses.


## 8. Latest verified implementation activity

- Roadmap and baseline audit: commit 416b16976ce4e7ce6d0c0c7f84cf296867c95fa5.
- Duplicate CAMERA permission removed: commit 4fac54f702e11e5a6aef33ecb4891f648816d9c4.
- Smart-home false-success behavior replaced with fail-closed behavior and regression tests: commit 50a7c453c4b224ee9f7880b70835e52302056bb2.
- CI run for the latest code at the time of this update: https://github.com/khaledaerbawy/nova-AI/actions/runs/42450158781 (queued/pending when checked; no artifact available).
