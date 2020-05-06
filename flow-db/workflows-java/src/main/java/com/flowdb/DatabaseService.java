package com.flowdb;

import net.corda.core.node.ServiceHub;
import net.corda.core.node.services.CordaService;
import net.corda.core.serialization.SingletonSerializeAsToken;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * A generic database service superclass for handling for database update.
 *
 * @param services The node's service hub.
 */
@CordaService
public class DatabaseService extends SingletonSerializeAsToken {
    private final ServiceHub services;
    protected static Logger log = LoggerFactory.getLogger(DatabaseService.class);

    public DatabaseService(@NotNull  ServiceHub services) {
        this.services = services;
    }

    /**
     * Executes a database update.
     *
     * @param query The query string with blanks for the parameters.
     * @param params The parameters to fill the blanks in the query string.
     */
    protected void executeUpdate(String query, Map<Integer, Object> params) throws SQLException {

        try (PreparedStatement preparedStatement = prepareStatement(query, params)) {
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            log.error(e.getMessage());
            throw e;
        }
    }

    /**
     * Executes a database query.
     *
     * @param query The query string with blanks for the parameters.
     * @param params The parameters to fill the blanks in the query string.
     * @param transformer A function for processing the query's ResultSet.
     *
     * @return The list of transformed query results.
     */
    protected List<Object> executeQuery(String query, Map<Integer, Object> params, Function<ResultSet, Object> transformer) throws SQLException {
        final PreparedStatement preparedStatement = prepareStatement(query, params);
        final List<Object> results = new ArrayList<>();

        try {
            final ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                results.add(transformer.apply(resultSet));
            }
        } catch (SQLException e) {
            log.error(e.getMessage());
            throw e;
        } finally {
            preparedStatement.close();
        }

        return results;
    }

    /**
     * Creates a PreparedStatement - a precompiled SQL statement to be
     * executed against the database.
     *
     * @param query The query string with blanks for the parameters.
     * @param params The parameters to fill the blanks in the query string.
     *
     * @return The query string and params compiled into a PreparedStatement
     */
    private PreparedStatement prepareStatement(String query, Map<Integer, Object> params) throws SQLException {
        final Connection session = services.jdbcSession();
        final PreparedStatement preparedStatement = session.prepareStatement(query);

        params.forEach((key, value) -> {
            try {
                if (value instanceof String) {
                    preparedStatement.setString(key, (String) value);
                } else if (value instanceof Integer) {
                    preparedStatement.setInt(key, (Integer) value);
                } else if (value instanceof Long) {
                    preparedStatement.setLong(key, (Long) value);
                } else {
                    throw new IllegalArgumentException("Unsupported Type");
                }
            }  catch (SQLException e) { e.printStackTrace(); }
        });

        return preparedStatement;
    }
}
