# ⚡ DFIR Orchestrator (Mini-SPL)

> **Desktop Incident Response & Digital Forensics Playbook Engine**  
> Built with **JavaFX (FXML)**, **SQLite (JDBC)**, and **Maven** for the Design Patterns Lab (SE 2215), Institute of Information Technology (IIT), University of Dhaka.

---

## 🎯 Problem Statement & What It Does

Security Operations Center (SOC) and Digital Forensics & Incident Response (DFIR) teams responding to active cyber incidents (such as ransomware outbreaks or credential phishing) must execute rapid, disciplined workflows while preserving an immutable chain of custody for legal and audit scrutiny. Mishandling either aspect risks uncontained threats or evidence dismissal in court.

**DFIR Orchestrator** automates and enforces strict incident response lifecycles:

1. **🚨 Incident Intake & Triage:** Analysts catalog incidents and automatically compute threat severity scores using pluggable algorithmic models.
2. **📖 Enforced Playbooks (Template Method):** Enforces an invariant 5-phase investigation lifecycle (**Triage → Containment → Evidence Collection → Eradication → Post-Mortem**). Phases cannot be skipped out of order; threat-specific engines branch into concrete, tailored remediation steps.
3. **↺ Reversible Remediation Actions (Command Pattern):** Actions such as isolating a host, blocking an IP, or revoking credentials are encapsulated into reversible commands with one-click rollback capabilities recorded in an audit ledger.
4. **📦 Evidence Chain of Custody (State Pattern):** Cryptographic SHA-256 digests and physical artifacts are strictly tracked through custody states (**Seized → In Analysis → Court Hold → Archived**).
5. **🔒 Cryptographic SHA-256 Integrity Verifier:** Real-time hash computation over actual disk artifacts to certify that evidence has not been tampered with or corrupted.
6. **🖥️ Infrastructure Asset Inventory:** Endpoint management with full CRUD, criticality classification, and instant network quarantine controls.
7. **📊 Real-time MTTC Analytics & Reporting:** Dynamically calculates true **Mean Time to Contain (MTTC)** across historical incidents and provides 1-click export to Markdown Dossiers and compliance CSV logs.

---

## 🧩 Design Patterns Catalog

| Design Pattern | Key Classes | Problem Addressed & Architectural Benefit |
| :--- | :--- | :--- |
| **Observer Pattern** | [`IncidentEventPublisher`](src/main/java/com/minispl/application/observer/IncidentEventPublisher.java), [`IncidentEventListener`](src/main/java/com/minispl/application/observer/IncidentEventListener.java), [`AuditTrailListener`](src/main/java/com/minispl/application/observer/AuditTrailListener.java), UI Controllers | Decouples state machines and command executors from JavaFX presentation views and the SQLite audit trail logger. Allows background state mutations to notify multiple independent observers without tight coupling. |
| **Template Method Pattern** | [`InvestigationEngine`](src/main/java/com/minispl/application/playbook/InvestigationEngine.java), [`RansomwarePlaybookEngine`](src/main/java/com/minispl/application/playbook/RansomwarePlaybookEngine.java), [`PhishingPlaybookEngine`](src/main/java/com/minispl/application/playbook/PhishingPlaybookEngine.java) | Enforces the invariant 5-phase investigation lifecycle (`executePlaybook`) while allowing threat subclasses to define concrete execution steps, phase transitions, and post-mortem reporting. |
| **State Pattern (Incident)** | [`IncidentStateMachine`](src/main/java/com/minispl/application/incident/IncidentStateMachine.java), [`NewIncidentState`](src/main/java/com/minispl/application/incident/NewIncidentState.java), [`TriagedIncidentState`](src/main/java/com/minispl/application/incident/TriagedIncidentState.java), [`ContainedIncidentState`](src/main/java/com/minispl/application/incident/ContainedIncidentState.java), [`ClosedIncidentState`](src/main/java/com/minispl/application/incident/ClosedIncidentState.java) | Replaces complex conditional spaghetti code (`if-else`/`switch`) with distinct state classes enforcing legal lifecycle transitions (`NEW` → `TRIAGED` → `CONTAINED` → `CLOSED`). |
| **State Pattern (Evidence)** | [`EvidenceCustodyStateMachine`](src/main/java/com/minispl/application/evidence/EvidenceCustodyStateMachine.java), [`SeizedCustodyState`](src/main/java/com/minispl/application/evidence/SeizedCustodyState.java), [`InAnalysisCustodyState`](src/main/java/com/minispl/application/evidence/InAnalysisCustodyState.java), [`CourtHoldCustodyState`](src/main/java/com/minispl/application/evidence/CourtHoldCustodyState.java), [`ArchivedCustodyState`](src/main/java/com/minispl/application/evidence/ArchivedCustodyState.java) | Guarantees legal evidentiary integrity by enforcing chain of custody rules (e.g. archived artifacts cannot be analyzed; court-held evidence cannot be archived directly). |
| **Strategy Pattern** | [`RiskScoringStrategy`](src/main/java/com/minispl/application/strategy/RiskScoringStrategy.java), [`CvssRiskScoringStrategy`](src/main/java/com/minispl/application/strategy/CvssRiskScoringStrategy.java), [`NistRiskScoringStrategy`](src/main/java/com/minispl/application/strategy/NistRiskScoringStrategy.java), [`RiskScoringContext`](src/main/java/com/minispl/application/strategy/RiskScoringContext.java) | Enables pluggable risk calculation algorithms (CVSS v3.1 mathematical scoring vs. NIST SP 800-61 criticality matrices) interchangeable at runtime without modifying incident entities. |
| **Command Pattern** | [`RemediationCommand`](src/main/java/com/minispl/application/remediation/RemediationCommand.java), [`IsolateHostCommand`](src/main/java/com/minispl/application/remediation/IsolateHostCommand.java), [`BlockIPCommand`](src/main/java/com/minispl/application/remediation/BlockIPCommand.java), [`RevokeCredentialsCommand`](src/main/java/com/minispl/application/remediation/RevokeCredentialsCommand.java), [`CommandInvoker`](src/main/java/com/minispl/application/remediation/CommandInvoker.java) | Encapsulates remediation actions into standalone objects with `execute()` and `undo()` methods, powering automated audit ledger logging and reversible rollback capabilities. |
| **Factory Pattern** | [`RemediationCommandFactory`](src/main/java/com/minispl/application/remediation/RemediationCommandFactory.java), [`PlaybookEngineFactory`](src/main/java/com/minispl/application/playbook/PlaybookEngineFactory.java) | Decouples object instantiation from callers, parsing parameters and constructing appropriate Command or PlaybookEngine instances dynamically. |
| **Singleton Pattern** | [`DatabaseManager`](src/main/java/com/minispl/persistence/DatabaseManager.java), [`IncidentEventPublisher`](src/main/java/com/minispl/application/observer/IncidentEventPublisher.java) | Controls global connection management to SQLite with enforced foreign keys, and provides a unified event bus instance for pub/sub messaging. |

---

## 🏛️ Application Architecture

```text
com.minispl/
├── MainApp.java                       # JavaFX Application Entrypoint
├── presentation/                      # Presentation Layer (JavaFX / FXML)
│   ├── MainController.java            # App shell with view caching & tab refresh
│   ├── IncidentViewController.java    # Screen 1: Incidents, Triage & Search
│   ├── EvidenceViewController.java    # Screen 2: Chain of Custody & Hash Verifier
│   ├── AssetViewController.java       # Screen 3: Asset Inventory & Quarantine CRUD
│   ├── PlaybookViewController.java    # Screen 4: Playbook Engine Runner
│   ├── ReportsViewController.java     # Screen 5: MTTC Analytics & Export
│   └── Refreshable.java               # Lifecycle interface to prevent stale views
├── application/                       # Application & Business Logic Layer
│   ├── observer/                      # Observer Pattern (Event bus, listeners)
│   ├── playbook/                      # Template Method Pattern (Engines, reports)
│   ├── incident/                      # State Pattern for Incident Lifecycle
│   ├── evidence/                      # State Pattern for Chain of Custody
│   ├── strategy/                      # Strategy Pattern for Risk Scoring
│   └── remediation/                   # Command & Factory Patterns for Remediation
├── domain/                            # Enterprise Domain Model Layer
│   ├── model/                         # Entity classes (Incident, EvidenceItem, Asset, User, ...)
│   └── enums/                         # Strongly-typed enumerations
└── persistence/                       # Persistence Layer (Repository / DAO)
    ├── DatabaseManager.java           # SQLite Connection & PRAGMA foreign_keys
    ├── DatabaseSeeder.java            # Automatic schema generation & seed data
    └── dao/                           # Isolated DAO accessors (IncidentDAO, EvidenceDAO, AssetDAO, ...)
```

---

## 🖥️ Major Application Consoles (5 Screens)

1. **🚨 Incidents & Triage Console:** Lifecycle state transitions (`Triage`, `Contain`, `Close`), multi-criteria real-time keyword search, dual status/severity filtering, and CVSS vs. NIST scoring recalculation.
2. **📦 Evidence & Chain of Custody Console:** Forensic evidence cataloging, custody transitions (`Begin Analysis`, `Place Court Hold`, `Release`, `Archive`), and cryptographic SHA-256 integrity verification against actual disk files.
3. **🖥️ Infrastructure Assets & Inventory Console:** Complete endpoint CRUD (Create, Read, Update, Delete), criticality tiering, and 1-click network quarantine/isolation toggling.
4. **📖 IR Playbooks Console:** Interactive Template Method execution for Ransomware and Phishing threats, step-by-step pass/fail verification, and execution summaries.
5. **📊 Audit & Analytics Console:** Real Mean Time to Contain (MTTC) calculation, reversible command rollback ledger, 1-click **Export Forensic Dossier (.md)**, and **Export Audit CSV (.csv)**.

---

## 🗄️ Database Schema & Entities

The SQLite database (`dfir_orchestrator.db`) is automatically seeded on startup with relational constraints and foreign keys (`PRAGMA foreign_keys = ON;`):

- **`users`**: Investigators, Analysts, and System Custodians.
- **`assets`**: Mission-critical servers, databases, and analyst workstations.
- **`incidents`**: Security cases linked to assets and analysts.
- **`evidence_items`**: Digital artifacts linked to incidents, source hosts, and custodians.
- **`playbooks`**: Threat workflow templates.
- **`playbook_steps`**: Sequential actions per playbook phase.
- **`action_audit_logs`**: Immutable audit ledger recording commands, parameters, and rollback status.

---

## 🧪 Comprehensive Automated Test Suite (49 Tests)

Run the full suite:

```bash
mvn clean test
```

All 49 unit and integration tests pass cleanly:

* **Observer Pattern Tests (5 tests):** [`ObserverPatternTest.java`](src/test/java/com/minispl/application/ObserverPatternTest.java)
* **Template Method Playbook Tests (5 tests):** [`TemplateMethodPlaybookTest.java`](src/test/java/com/minispl/application/TemplateMethodPlaybookTest.java)
* **State Machine Tests (9 tests):** [`IncidentStateMachineTest.java`](src/test/java/com/minispl/application/IncidentStateMachineTest.java), [`EvidenceCustodyStateMachineTest.java`](src/test/java/com/minispl/application/EvidenceCustodyStateMachineTest.java)
* **Strategy & Command Tests (8 tests):** [`RiskScoringStrategyTest.java`](src/test/java/com/minispl/application/RiskScoringStrategyTest.java), [`RemediationCommandAndFactoryTest.java`](src/test/java/com/minispl/application/RemediationCommandAndFactoryTest.java)
* **Additional Workflows & Export Tests (5 tests):** [`AdditionalWorkflowsTest.java`](src/test/java/com/minispl/application/AdditionalWorkflowsTest.java)
* **Analytics & MTTC Tests (4 tests):** [`MTTCCalculationTest.java`](src/test/java/com/minispl/presentation/MTTCCalculationTest.java)
* **Persistence & DAO Tests (7 tests):** [`DatabaseAndDAOTest.java`](src/test/java/com/minispl/persistence/DatabaseAndDAOTest.java)
* **JavaFX FXML UI Loading Tests (6 tests):** [`FXMLLoadingTest.java`](src/test/java/com/minispl/presentation/FXMLLoadingTest.java)

---

## 🚀 Running the Application

### Prerequisites
* **Java JDK 21+**
* **Apache Maven 3.8+**

### Execution Steps
```bash
# 1. Clone repository
git clone https://github.com/nafisa1602/Mini-SPL.git
cd Mini-SPL

# 2. Build and run tests
mvn clean test

# 3. Launch JavaFX Desktop Application
mvn javafx:run
```
