-- ══════════════════════════════════════════════════════════════
-- Task API — Schema PostgreSQL
-- ══════════════════════════════════════════════════════════════

-- ── Tipos ENUM ────────────────────────────────────────────────
CREATE TYPE task_status   AS ENUM ('PENDING', 'IN_PROGRESS', 'COMPLETED');
CREATE TYPE task_priority AS ENUM ('LOW', 'MEDIUM', 'HIGH');

-- ── users ─────────────────────────────────────────────────────
CREATE TABLE users (
    id            BIGSERIAL    PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- ── categories (por usuário) ──────────────────────────────────
CREATE TABLE categories (
    id      BIGSERIAL   PRIMARY KEY,
    user_id BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name    VARCHAR(50) NOT NULL,
    color   VARCHAR(7)  NOT NULL DEFAULT '#6366f1',  -- cor hex ex: #ff5733

    CONSTRAINT uq_category_name_per_user UNIQUE (user_id, name)
);

-- ── tags (por usuário) ────────────────────────────────────────
CREATE TABLE tags (
    id      BIGSERIAL   PRIMARY KEY,
    user_id BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name    VARCHAR(50) NOT NULL,

    CONSTRAINT uq_tag_name_per_user UNIQUE (user_id, name)
);

-- ── tasks ─────────────────────────────────────────────────────
CREATE TABLE tasks (
    id             BIGSERIAL     PRIMARY KEY,
    user_id        BIGINT        NOT NULL REFERENCES users(id)      ON DELETE CASCADE,
    parent_task_id BIGINT                 REFERENCES tasks(id)      ON DELETE CASCADE,
    category_id    BIGINT                 REFERENCES categories(id) ON DELETE SET NULL,
    title          VARCHAR(255)  NOT NULL,
    description    TEXT,
    status         task_status   NOT NULL DEFAULT 'PENDING',
    priority       task_priority NOT NULL DEFAULT 'MEDIUM',
    due_date       TIMESTAMP,
    completed_at   TIMESTAMP,
    created_at     TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP     NOT NULL DEFAULT NOW()
);

-- ── task_tags (pivô N:N) ──────────────────────────────────────
CREATE TABLE task_tags (
    task_id BIGINT NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    tag_id  BIGINT NOT NULL REFERENCES tags(id)  ON DELETE CASCADE,

    PRIMARY KEY (task_id, tag_id)
);

-- ══════════════════════════════════════════════════════════════
-- Índices
-- ══════════════════════════════════════════════════════════════

-- Buscas mais comuns: tarefas de um usuário filtradas por status/prioridade
CREATE INDEX idx_tasks_user_status   ON tasks (user_id, status);
CREATE INDEX idx_tasks_user_priority ON tasks (user_id, priority);

-- Listagem de subtarefas de uma tarefa pai
CREATE INDEX idx_tasks_parent ON tasks (parent_task_id) WHERE parent_task_id IS NOT NULL;

-- Tarefas com prazo (overdue)
CREATE INDEX idx_tasks_due_date ON tasks (due_date) WHERE due_date IS NOT NULL;

-- Tarefas por categoria
CREATE INDEX idx_tasks_category ON tasks (category_id) WHERE category_id IS NOT NULL;

-- Tags de uma tarefa (frequente na listagem)
CREATE INDEX idx_task_tags_task ON task_tags (task_id);

-- ══════════════════════════════════════════════════════════════
-- Trigger 1: garante apenas 1 nível de subtarefa
-- (o PostgreSQL não permite subquery em CHECK, então usamos trigger)
-- ══════════════════════════════════════════════════════════════

CREATE OR REPLACE FUNCTION enforce_one_level_subtask()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.parent_task_id IS NOT NULL THEN
        IF (SELECT parent_task_id FROM tasks WHERE id = NEW.parent_task_id) IS NOT NULL THEN
            RAISE EXCEPTION 'Subtarefa não pode ter subtarefas (apenas 1 nível permitido)';
        END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_tasks_one_level
    BEFORE INSERT OR UPDATE ON tasks
    FOR EACH ROW EXECUTE FUNCTION enforce_one_level_subtask();

-- ══════════════════════════════════════════════════════════════
-- Trigger 2: atualiza updated_at automaticamente
-- ══════════════════════════════════════════════════════════════

CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_tasks_updated_at
    BEFORE UPDATE ON tasks
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();