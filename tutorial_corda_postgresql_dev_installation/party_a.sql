CREATE USER "party_a" WITH LOGIN PASSWORD 'my_password';
CREATE SCHEMA "party_a_schema";
GRANT USAGE, CREATE ON SCHEMA "party_a_schema" TO "party_a";
GRANT SELECT, INSERT, UPDATE, DELETE, REFERENCES ON ALL tables IN SCHEMA "party_a_schema" TO "party_a";
ALTER DEFAULT privileges IN SCHEMA "party_a_schema" GRANT SELECT, INSERT, UPDATE, DELETE, REFERENCES ON tables TO "party_a";
GRANT USAGE, SELECT ON ALL sequences IN SCHEMA "party_a_schema" TO "party_a";
ALTER DEFAULT privileges IN SCHEMA "party_a_schema" GRANT USAGE, SELECT ON sequences TO "party_a";
ALTER ROLE "party_a" SET search_path = "party_a_schema";
