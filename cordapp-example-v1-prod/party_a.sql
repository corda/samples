CREATE USER "party_a_normal" WITH LOGIN PASSWORD 'my_password';
GRANT USAGE ON SCHEMA "party_a_schema" TO "party_a_normal";
GRANT SELECT, INSERT, UPDATE, DELETE, REFERENCES ON ALL tables IN SCHEMA "party_a_schema" TO "party_a_normal";
ALTER DEFAULT privileges IN SCHEMA "party_a_schema" GRANT SELECT, INSERT, UPDATE, DELETE, REFERENCES ON tables TO "party_a_normal";
GRANT USAGE, SELECT ON ALL sequences IN SCHEMA "party_a_schema" TO "party_a_normal";
ALTER DEFAULT privileges IN SCHEMA "party_a_schema" GRANT USAGE, SELECT ON sequences TO "party_a_normal";
ALTER ROLE "party_a_normal" SET search_path = "party_a_schema";
