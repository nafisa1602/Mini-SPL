#Mini-SPL
DFIR Orchestrator
A desktop incident response & digital forensics playbook engine.Built with JavaFX, SQLite, and Maven for the Design Patterns Lab (SE 2215), IIT, University of Dhaka.

What it does

Security teams responding to an incident (a breach, ransomware, phishing) need to move fast and keep a clean paper trail — mishandling either one means an uncontained threat or evidence that can't hold up in court. DFIR Orchestrator guides an analyst through the whole response instead of leaving it to memory and spreadsheets:

Log an incident — the system scores it by asset criticality and threat type.
Follow a guided playbook — Triage → Containment → Evidence Collection → Eradication → Post-Mortem. Phases can't be skipped; ransomware and phishing cases share the same shape but branch into different concrete steps.
Take reversible actions — isolate a host, block an IP, revoke credentials — each one logged and undoable, like a text editor's undo stack.
Track evidence custody — artifacts (disk images, memory dumps, logs) are hashed and tracked through Seized → In Analysis → Court Hold → Archived, so custody is provable.
View reporting — Mean Time to Contain, vulnerability trends, and an exportable chain-of-custody report.
Tech stack
Layer	Tech
UI	JavaFX (FXML)
Build	Apache Maven
Persistence	SQLite (JDBC driver)
Testing	JUnit
Architecture
presentation/   → JavaFX views, bound to observable application state
application/     → orchestration engine, Command objects, State machines, Strategy implementations
persistence/     → Repository/DAO layer — the only code that talks to SQLite

Business logic never issues SQL directly; all database access goes through the repository layer, so it can be swapped or mocked for testing.

Design patterns
Pattern	Why it's here
Command	Wraps each remediation action (IsolateHostCommand, BlockIPCommand, ...) in an object with execute()/undo(), powering the audit log and one-click rollback.
Factory Method	Builds the right Command from a playbook step at runtime — new actions plug in without touching the execution engine.
State	Governs valid transitions for both the incident lifecycle and the evidence custody lifecycle, rejecting invalid moves instead of relying on conditionals.
Template Method	Fixes the overall investigation lifecycle while letting threat-specific playbooks (RansomwarePlaybook, PhishingPlaybook) define their own steps.
Strategy	Swaps risk-scoring models (NIST SP 800-61 vs. CVSS) and hash algorithms (SHA-256, SHA-512) without touching core workflow code.
Observer	Lets a single state change notify both the UI and the SQLite audit logger, decoupled from each other.
Database

Core tables: users, incidents, assets, evidence_items, playbooks, playbook_steps, action_audit_logs. Analyst/custodian/executor fields are foreign keys into users to preserve chain-of-custody integrity. See docs/er-diagram.png (or link once added) for the full schema.

Getting started
bash
git clone https://github.com/<org-or-user>/dfir-orchestrator.git
cd dfir-orchestrator
mvn clean install
mvn javafx:run

The seeder scripts create the schema and load sample data on first run.
