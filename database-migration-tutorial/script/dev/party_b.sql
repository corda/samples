--CREATE USER "party_b" WITH LOGIN PASSWORD 'my_password';
CREATE SCHEMA "party_b_schema";
GRANT USAGE, CREATE ON SCHEMA "party_b_schema" TO "party_b";
GRANT SELECT, INSERT, UPDATE, DELETE, REFERENCES ON ALL tables IN SCHEMA "party_b_schema" TO "party_b";
ALTER DEFAULT privileges IN SCHEMA "party_b_schema" GRANT SELECT, INSERT, UPDATE, DELETE, REFERENCES ON tables TO "party_b";
GRANT USAGE, SELECT ON ALL sequences IN SCHEMA "party_b_schema" TO "party_b";
ALTER DEFAULT privileges IN SCHEMA "party_b_schema" GRANT USAGE, SELECT ON sequences TO "party_b";
ALTER ROLE "party_b" SET search_path = "party_b_schema";