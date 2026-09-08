-- ==========================================================
-- DFIR Orchestrator / Mini-SPL Schema (SQLite DDL)
-- Course: Design Patterns Lab (SE 2215), IIT, University of Dhaka
-- ==========================================================

PRAGMA foreign_keys = ON;

-- 1. Users Table (Analysts, Custodians, Administrators)
CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    full_name TEXT NOT NULL,
    role TEXT NOT NULL CHECK(role IN ('ANALYST', 'SENIOR_ANALYST', 'ADMIN')),
    email TEXT UNIQUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Assets Table (Target Systems, Hosts, Critical Infrastructure)
CREATE TABLE IF NOT EXISTS assets (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    hostname TEXT UNIQUE NOT NULL,
    ip_address TEXT NOT NULL,
    criticality_tier TEXT NOT NULL CHECK(criticality_tier IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    status TEXT NOT NULL DEFAULT 'ONLINE' CHECK(status IN ('ONLINE', 'ISOLATED', 'QUARANTINED')),
    description TEXT
);

-- 3. Playbooks Table (High-Level Incident Investigation Playbooks)
CREATE TABLE IF NOT EXISTS playbooks (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    threat_type TEXT UNIQUE NOT NULL,
    description TEXT NOT NULL
);

-- 4. Playbook Steps Table (Enforced Phases and Concrete Remediation Steps)
CREATE TABLE IF NOT EXISTS playbook_steps (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    playbook_id INTEGER NOT NULL,
    phase TEXT NOT NULL CHECK(phase IN ('TRIAGE', 'CONTAINMENT', 'EVIDENCE_COLLECTION', 'ERADICATION', 'POST_MORTEM')),
    sequence_order INTEGER NOT NULL,
    step_name TEXT NOT NULL,
    action_type TEXT,
    pass_fail_criteria TEXT NOT NULL,
    FOREIGN KEY (playbook_id) REFERENCES playbooks(id) ON DELETE CASCADE
);

-- 5. Incidents Table (Security Breaches, Threat Alerts, Investigations)
CREATE TABLE IF NOT EXISTS incidents (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    title TEXT NOT NULL,
    threat_type TEXT NOT NULL,
    severity TEXT NOT NULL CHECK(severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    status TEXT NOT NULL DEFAULT 'NEW' CHECK(status IN ('NEW', 'TRIAGED', 'CONTAINED', 'CLOSED')),
    asset_id INTEGER NOT NULL,
    assigned_analyst_id INTEGER,
    playbook_id INTEGER,
    risk_score REAL NOT NULL DEFAULT 0.0,
    current_phase TEXT NOT NULL DEFAULT 'TRIAGE' CHECK(current_phase IN ('TRIAGE', 'CONTAINMENT', 'EVIDENCE_COLLECTION', 'ERADICATION', 'POST_MORTEM', 'CLOSED')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    closed_at TIMESTAMP,
    FOREIGN KEY (asset_id) REFERENCES assets(id),
    FOREIGN KEY (assigned_analyst_id) REFERENCES users(id),
    FOREIGN KEY (playbook_id) REFERENCES playbooks(id)
);

-- 6. Evidence Items Table (Chain of Custody Tracking, Forensic Artifacts)
CREATE TABLE IF NOT EXISTS evidence_items (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    incident_id INTEGER NOT NULL,
    source_asset_id INTEGER NOT NULL,
    evidence_name TEXT NOT NULL,
    evidence_type TEXT NOT NULL CHECK(evidence_type IN ('DISK_IMAGE', 'MEMORY_DUMP', 'NETWORK_PCAP', 'LOG_FILE', 'CREDENTIAL_CACHE')),
    file_hash TEXT NOT NULL,
    custody_status TEXT NOT NULL DEFAULT 'SEIZED' CHECK(custody_status IN ('SEIZED', 'IN_ANALYSIS', 'COURT_HOLD', 'ARCHIVED')),
    current_custodian_id INTEGER NOT NULL,
    collected_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (incident_id) REFERENCES incidents(id) ON DELETE CASCADE,
    FOREIGN KEY (source_asset_id) REFERENCES assets(id),
    FOREIGN KEY (current_custodian_id) REFERENCES users(id)
);

-- 7. Action Audit Logs Table (Command Pattern Rollback and Execution Audit)
CREATE TABLE IF NOT EXISTS action_audit_logs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    incident_id INTEGER NOT NULL,
    command_type TEXT NOT NULL,
    parameters TEXT,
    executed_by_id INTEGER NOT NULL,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    can_undo INTEGER NOT NULL DEFAULT 1,
    status TEXT NOT NULL CHECK(status IN ('EXECUTED', 'UNDONE', 'FAILED')),
    FOREIGN KEY (incident_id) REFERENCES incidents(id) ON DELETE CASCADE,
    FOREIGN KEY (executed_by_id) REFERENCES users(id)
);

-- Indices for performance
CREATE INDEX IF NOT EXISTS idx_incidents_status ON incidents(status);
CREATE INDEX IF NOT EXISTS idx_incidents_assigned_analyst ON incidents(assigned_analyst_id);
CREATE INDEX IF NOT EXISTS idx_evidence_incident ON evidence_items(incident_id);
CREATE INDEX IF NOT EXISTS idx_audit_incident ON action_audit_logs(incident_id);
CREATE INDEX IF NOT EXISTS idx_steps_playbook ON playbook_steps(playbook_id);
