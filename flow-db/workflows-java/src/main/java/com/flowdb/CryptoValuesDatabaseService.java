package com.flowdb;

import net.corda.core.node.ServiceHub;
import net.corda.core.node.services.CordaService;
import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static com.flowdb.Constants.TABLE_NAME;

@CordaService
public class CryptoValuesDatabaseService extends DatabaseService {

    public CryptoValuesDatabaseService(@NotNull ServiceHub services) throws SQLException {
        super(services);
        this.setUpStorage();
    }

    /**
     * Adds a crypto token and associated value to the table of crypto values.
     */
    protected void addtokenValue(String token, Integer value) throws SQLException {
        final String query = "insert into " + TABLE_NAME + " values (?, ?)";
        final Map<Integer, Object> params = new HashMap<>();
        params.put(1, token);
        params.put(2, value);

        executeUpdate(query, params);
        log.info("Token " + token + " added to crypto_values table.");
    }

    /**
     * Updates the value of a crypto token in the table of crypto values.
     */
    protected void updateTokenValue(String token, Integer value) throws SQLException {
        final String query = "update " + TABLE_NAME + " set value = ? where token = ?";
        final Map<Integer, Object> params = new HashMap<>();
        params.put(1, value);
        params.put(2, token);

        executeUpdate(query, params);
        log.info("Token " + token + " updated in crypto_values table.");
    }

    /**
     * Retrieves the value of a crypto token in the table of crypto values.
     */
    protected Integer queryTokenValue(String token) throws SQLException {
        final String query = "select value from " + TABLE_NAME + " where token = ?";
        final Map<Integer, Object> params = new HashMap<>();
        params.put(1, token);

        Function<ResultSet, Object> transformer = (it) -> {
            Integer i = null;
            try {
                i = it.getInt("value");
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return i;
        };

        final List<Object> results = executeQuery(query, params, transformer);

        if (results.isEmpty()) {
            throw new IllegalArgumentException("Token " + token + " not present in database");
        } else if (results.size() > 1) {
            throw new IllegalArgumentException("Error list has more than one element");
        }

        log.info("Token " + token + "read from crypto_values table.");
        return (Integer) results.get(0);
    }

    private void setUpStorage() throws SQLException {
        final String query = "create table if not exists " + TABLE_NAME +
                "(token varchar(64), value int)";
        executeUpdate(query, Collections.emptyMap());
        log.info("Created crypto_values table.");
    }

}
