CREATE USER "party_b_normal" WITH LOGIN PASSWORD 'my_password';
GRANT USAGE ON SCHEMA "party_b_schema" TO "party_b_normal";
GRANT SELECT, INSERT, UPDATE, DELETE, REFERENCES ON ALL tables IN SCHEMA "party_b_schema" TO "party_b_normal";
ALTER DEFAULT privileges IN SCHEMA "party_b_schema" GRANT SELECT, INSERT, UPDATE, DELETE, REFERENCES ON tables TO "party_b_normal";
GRANT USAGE, SELECT ON ALL sequences IN SCHEMA "party_b_schema" TO "party_b_normal";
ALTER DEFAULT privileges IN SCHEMA "party_b_schema" GRANT USAGE, SELECT ON sequences TO "party_b_normal";
ALTER ROLE "party_b_normal" SET search_path = "party_b_schema";
