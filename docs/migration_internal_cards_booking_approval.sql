-- Run this in PostgreSQL/Supabase SQL Editor before using the internal card booking flow.

begin;

create table if not exists cards (
  card_id bigserial primary key,
  user_id bigint not null unique references users(user_id),
  card_code varchar(64) not null unique,
  status varchar(30) not null default 'ACTIVE',
  issued_at timestamp not null default now(),
  expired_at timestamp null,
  created_at timestamp not null default now(),
  updated_at timestamp not null default now()
);

insert into cards (user_id, card_code, status, issued_at, created_at, updated_at)
select
  u.user_id,
  concat('CARD-', lpad(u.user_id::text, 6, '0')),
  'ACTIVE',
  now(),
  now(),
  now()
from users u
where not exists (
  select 1
  from cards c
  where c.user_id = u.user_id
);

alter table bookings
  add column if not exists card_id bigint references cards(card_id),
  add column if not exists payment_status varchar(30) not null default 'NOT_ALLOWED',
  add column if not exists staff_note text,
  add column if not exists rejected_at timestamp,
  add column if not exists paid_at timestamp,
  add column if not exists parking_id bigint references parking_facilities(parking_id),
  add column if not exists floor_id bigint references parking_floors(floor_id),
  add column if not exists vehicle_type_id bigint references vehicle_types(vehicle_type_id),
  add column if not exists zone_id bigint references parking_zones(zone_id),
  add column if not exists accepted_by bigint references users(user_id),
  add column if not exists accepted_at timestamp;

update bookings b
set card_id = c.card_id
from cards c
where b.card_id is null
  and c.user_id = b.user_id;

update bookings
set status = case
  when status = 'PENDING' then 'WAITING_STAFF_APPROVAL'
  when status = 'CONFIRMED' then 'APPROVED_WAITING_PAYMENT'
  else status
end
where status in ('PENDING', 'CONFIRMED');

update bookings
set payment_status = case
  when status = 'APPROVED_WAITING_PAYMENT' then 'UNPAID'
  when status in ('PAID', 'COMPLETED') then 'PAID'
  else 'NOT_ALLOWED'
end
where payment_status is null
   or payment_status = 'NOT_ALLOWED';

create index if not exists idx_cards_user_id on cards(user_id);
create index if not exists idx_cards_status on cards(status);
create index if not exists idx_bookings_card_id on bookings(card_id);
create index if not exists idx_bookings_status_created_at on bookings(status, created_at);
create index if not exists idx_bookings_user_created_at on bookings(user_id, created_at desc);

commit;
