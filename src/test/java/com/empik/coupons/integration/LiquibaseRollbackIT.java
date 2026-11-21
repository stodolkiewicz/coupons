package com.empik.coupons.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import liquibase.command.CommandArgumentDefinition;
import liquibase.command.CommandScope;
import liquibase.command.core.RollbackCountCommandStep;
import liquibase.command.core.UpdateCountCommandStep;
import liquibase.command.core.helpers.DatabaseChangelogCommandStep;
import liquibase.command.core.helpers.DbUrlConnectionArgumentsCommandStep;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Checks that the rollback of each changeset restores the exact database state from before that changeset.
 * The test runs without Spring, so the database starts empty.
 * No context filter is set, so Liquibase runs the changesets of all contexts.
 * The test applies the changesets one by one.
 * For each changeset it compares pg_dump output (schema and data) from before the update and after the rollback.
 */
@Testcontainers
class LiquibaseRollbackIT {

	private static final String CHANGELOG_FILE = "db/changelog/db.changelog-master.yml";

	@Container
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

	@Test
	@DisplayName("The rollback of each changeset restores the schema and data from before that changeset")
	void givenEmptyDatabase_eachChangesetRollbackShouldRestorePreviousState() throws Exception {
		int appliedChangesets = 0;
		while (true) {
			String stateBeforeUpdate = dumpDatabase();
			runOneLiquibaseChangeset();
			int appliedAfterUpdate = countAppliedChangesets();
			boolean noChangesetWasApplied = appliedAfterUpdate == appliedChangesets;
			if (noChangesetWasApplied) {
				break;
			}

			rollbackOneLiquibaseChangeset();
			assertThat(dumpDatabase())
					.as("Database state after the rollback of changeset number %d", appliedAfterUpdate)
					.isEqualTo(stateBeforeUpdate);

			// The changeset is applied again, so the next iteration starts from the state with it applied.
			runOneLiquibaseChangeset();
			appliedChangesets = appliedAfterUpdate;
		}
		assertThat(appliedChangesets).isPositive();
	}

	/**
	 * Applies the next changeset that has not run yet.
	 */
	private void runOneLiquibaseChangeset() throws Exception {
		runLiquibaseCommand(
				UpdateCountCommandStep.COMMAND_NAME,
				UpdateCountCommandStep.COUNT_ARG
		);
	}

	/**
	 * Rolls back the most recently applied changeset.
	 */
	private void rollbackOneLiquibaseChangeset() throws Exception {
		runLiquibaseCommand(
				RollbackCountCommandStep.COMMAND_NAME,
				RollbackCountCommandStep.COUNT_ARG
		);
	}

	/**
	 * Runs a Liquibase command that applies or rolls back exactly one changeset.
	 */
	private void runLiquibaseCommand(
			String[] commandName,
			CommandArgumentDefinition<Integer> countArg
	) throws Exception {
		try (Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(
				new JdbcConnection(openConnection())
		)) {
			new CommandScope(commandName)
					.addArgumentValue(
							DbUrlConnectionArgumentsCommandStep.DATABASE_ARG,
							database
					)
					.addArgumentValue(
							DatabaseChangelogCommandStep.CHANGELOG_FILE_ARG,
							CHANGELOG_FILE
					)
					.addArgumentValue(
							countArg,
							1
					)
					.execute();
		}
	}

	/**
	 * Returns the schema and data of the database as SQL.
	 * The Liquibase tables are excluded, because before the first changeset they do not exist,
	 * and the rollback of the first changeset leaves them in the database.
	 * A fixed --restrict-key replaces the random key that pg_dump writes into every dump.
	 */
	private String dumpDatabase() throws Exception {
		ExecResult result = postgres.execInContainer(
				"pg_dump",
				"--username=" + postgres.getUsername(),
				"--dbname=" + postgres.getDatabaseName(),
				"--exclude-table=databasechangelog*",
				"--restrict-key=test"
		);
		assertThat(result.getExitCode())
				.as("pg_dump stderr: %s", result.getStderr())
				.isZero();
		return result.getStdout();
	}

	private int countAppliedChangesets() throws SQLException {
		try (
				Connection connection = openConnection();
				Statement statement = connection.createStatement();
				ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM databasechangelog")
		) {
			resultSet.next();
			return resultSet.getInt(1);
		}
	}

	private Connection openConnection() throws SQLException {
		return DriverManager.getConnection(
				postgres.getJdbcUrl(),
				postgres.getUsername(),
				postgres.getPassword()
		);
	}

}
