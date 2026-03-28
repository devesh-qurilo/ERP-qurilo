-- projects
CREATE TABLE IF NOT EXISTS projects (
                          id BIGSERIAL PRIMARY KEY,
                          short_code varchar(50),
                          name varchar(255) NOT NULL,
                          start_date date,
                          deadline date,
                          no_deadline boolean DEFAULT false,
                          category varchar(100),
                          department_id bigint,
                          client_id bigint,
                          summary text,
                          tasks_need_admin_approval boolean DEFAULT false,
                          currency varchar(10),
                          budget numeric(19,2),
                          hours_estimate integer,
                          allow_manual_time_logs boolean DEFAULT false,
                          added_by varchar(100),
                          assigned_employees text, -- store array as a comma-separated string OR create join table in future
                          project_status varchar(50),
                          progress_percent integer,
                          calculate_through_tasks boolean DEFAULT false,
                          created_by varchar(100),
                          created_at timestamptz DEFAULT now(),
                          updated_by varchar(100),
                          updated_at timestamptz
);

-- tasks
CREATE TABLE IF NOT EXISTS tasks (
                       id BIGSERIAL PRIMARY KEY,
                       title varchar(1000) NOT NULL,
                       category varchar(255),
                       project_id bigint NOT NULL,
                       start_date date,
                       due_date date,
                       no_due_date boolean DEFAULT false,
                       status_enum varchar(100),
                       assigned_employees text,
                       description text,
                       milestone_id bigint,
                       priority varchar(50),
                       is_private boolean DEFAULT false,
                       time_estimate_minutes integer,
                       is_dependent boolean DEFAULT false,
                       dependent_task_id bigint,
                       duplicate_of_task_id bigint,
                       created_by varchar(100),
                       created_at timestamptz DEFAULT now(),
                       updated_by varchar(100),
                       updated_at timestamptz
);

-- recurring_tasks
CREATE TABLE IF NOT EXISTS recurring_tasks (
                                 id BIGSERIAL PRIMARY KEY,
                                 title varchar(1000) NOT NULL,
                                 project_id bigint NOT NULL,
                                 category varchar(255),
                                 start_date date,
                                 due_date date,
                                 no_due_date boolean DEFAULT false,
                                 status_id bigint,
                                 assigned_employees text,
                                 description text,
                                 milestone_id bigint,
                                 priority varchar(50),
                                 is_private boolean DEFAULT false,
                                 repeat_flag boolean DEFAULT false,
                                 repeat_every integer,
                                 repeat_unit varchar(20),
                                 cycle integer,
                                 recurring_status varchar(50) DEFAULT 'ACTIVE',
                                 last_generated_at timestamptz,
                                 created_by varchar(100),
                                 created_at timestamptz DEFAULT now(),
                                 updated_by varchar(100),
                                 updated_at timestamptz
);

-- milestones
CREATE TABLE IF NOT EXISTS project_milestones (
                                   id BIGSERIAL PRIMARY KEY,
                                   project_id bigint,
                                   title varchar(500),
                                   milestone_cost numeric(19,2),
                                   status varchar(50),
                                   summary text,
                                   start_date date,
                                   end_date date,
                                   created_by varchar(100),
                                   created_at timestamptz DEFAULT now()
);

-- labels
CREATE TABLE IF NOT EXISTS labels (
                       id BIGSERIAL PRIMARY KEY,
                       name varchar(255),
                       color_code varchar(50),
                       project_id bigint,
                       description text,
                       created_by varchar(100),
                       created_at timestamptz DEFAULT now()
);

-- task_stages
CREATE TABLE IF NOT EXISTS task_stages (
                            id BIGSERIAL PRIMARY KEY,
                            name varchar(255),
                            position integer,
                            label_color varchar(50),
                            project_id bigint,
                            created_by varchar(100),
                            created_at timestamptz DEFAULT now()
);

-- task_category
CREATE TABLE IF NOT EXISTS task_category (
                               id BIGSERIAL PRIMARY KEY,
                               name varchar(255),
                               created_by varchar(100),
                               created_at timestamptz DEFAULT now()
);

-- subtask
CREATE TABLE IF NOT EXISTS subtask (
                         id BIGSERIAL PRIMARY KEY,
                         task_id bigint,
                         title varchar(1000),
                         description text,
                         is_done boolean DEFAULT false,
                         created_by varchar(100),
                         created_at timestamptz DEFAULT now()
);

-- task_note & project_note
CREATE TABLE IF NOT EXISTS task_notes (
                           id BIGSERIAL PRIMARY KEY,
                           task_id bigint,
                           title varchar(255),
                           content text,
                           is_public boolean DEFAULT true,
                           owner_employee_id varchar(100),
                           created_by varchar(100),
                           created_at timestamptz DEFAULT now()
);

CREATE TABLE IF NOT EXISTS project_notes (
                              id BIGSERIAL PRIMARY KEY,
                              project_id bigint,
                              title varchar(255),
                              content text,
                              is_public boolean DEFAULT true,
                              owner_employee_id varchar(100),
                              created_by varchar(100),
                              created_at timestamptz DEFAULT now()
);

-- time logs
CREATE TABLE IF NOT EXISTS time_logs (
                          id BIGSERIAL PRIMARY KEY,
                          project_id bigint,
                          task_id bigint,
                          employee_id varchar(100),
                          start_date date,
                          start_time time,
                          end_date date,
                          end_time time,
                          memo text,
                          duration_minutes bigint,
                          created_by varchar(100),
                          created_at timestamptz DEFAULT now()
);

-- weekly time log
CREATE TABLE IF NOT EXISTS weekly_time_logs (
                                 id BIGSERIAL PRIMARY KEY,
                                 employee_id varchar(100),
                                 task_id bigint,
                                 week_start_date date,
                                 day1_hours integer,
                                 day2_hours integer,
                                 day3_hours integer,
                                 day4_hours integer,
                                 day5_hours integer,
                                 day6_hours integer,
                                 day7_hours integer,
                                 total_hours integer,
                                 created_by varchar(100),
                                 created_at timestamptz DEFAULT now()
);

-- project_activity
CREATE TABLE IF NOT EXISTS project_activity (
                                  id BIGSERIAL PRIMARY KEY,
                                  project_id bigint,
                                  actor_employee_id varchar(100),
                                  action varchar(255),
                                  metadata text,
                                  created_at timestamptz DEFAULT now()
);

-- file_meta (if not already created)
CREATE TABLE IF NOT EXISTS file_meta (
                           id BIGSERIAL PRIMARY KEY,
                           project_id bigint,
                           task_id bigint,
                           milestone_id bigint,
                           recurring_task_id bigint,
                           filename varchar(1024) NOT NULL,
                           bucket varchar(255) NOT NULL,
                           path varchar(2000) NOT NULL,
                           url varchar(2000),
                           mime_type varchar(255),
                           size bigint,
                           uploaded_by varchar(100),
                           created_at timestamptz DEFAULT now()
);

-- task_copy table for copy-link
CREATE TABLE IF NOT EXISTS task_copy (
                           id uuid PRIMARY KEY,
                           task_id bigint,
                           project_id bigint,
                           snapshot_json text,
                           created_at timestamptz DEFAULT now()
);

-- simple indexes
CREATE INDEX IF NOT EXISTS idx_project_id ON projects(id);
CREATE INDEX IF NOT EXISTS idx_task_project ON tasks(project_id);
CREATE INDEX IF NOT EXISTS idx_filemeta_project ON file_meta(project_id);
