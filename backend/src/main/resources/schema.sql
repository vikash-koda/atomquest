create table if not exists users (
    id bigserial primary key,
    name varchar(255) not null,
    email varchar(255) not null unique,
    password varchar(255) not null,
    role varchar(40) not null,
    manager_id bigint references users(id),
    department varchar(255) not null,
    created_at timestamp with time zone not null
);

create table if not exists shared_goals (
    id bigserial primary key,
    title varchar(255) not null,
    description varchar(1200),
    thrust_area varchar(255) not null,
    uom_type varchar(40) not null,
    target double precision not null,
    deadline date not null,
    owner_id bigint not null references users(id),
    department varchar(255) not null
);

create table if not exists goals (
    id bigserial primary key,
    employee_id bigint not null references users(id),
    title varchar(255) not null,
    description varchar(1200),
    thrust_area varchar(255) not null,
    uom_type varchar(40) not null,
    target double precision not null,
    achievement double precision,
    weightage integer not null check (weightage >= 10),
    status varchar(40) not null,
    locked boolean not null default false,
    shared_goal_id bigint references shared_goals(id),
    deadline date not null,
    created_at timestamp with time zone not null
);

create table if not exists quarterly_updates (
    id bigserial primary key,
    goal_id bigint not null references goals(id),
    quarter varchar(10) not null,
    achievement double precision not null,
    comment varchar(1000),
    progress_score double precision not null,
    status varchar(40),
    completion_date date,
    updated_at timestamp with time zone not null,
    constraint uk_goal_quarter unique(goal_id, quarter)
);

create table if not exists manager_comments (
    id bigserial primary key,
    manager_id bigint not null references users(id),
    employee_id bigint not null references users(id),
    quarter varchar(10) not null,
    comment varchar(1200) not null,
    created_at timestamp with time zone not null
);

create table if not exists audit_logs (
    id bigserial primary key,
    user_id bigint references users(id),
    action varchar(255) not null,
    entity_type varchar(255) not null,
    entity_id bigint,
    old_value varchar(2000),
    new_value varchar(2000),
    timestamp timestamp with time zone not null
);
