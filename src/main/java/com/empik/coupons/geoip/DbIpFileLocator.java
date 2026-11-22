package com.empik.coupons.geoip;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Builds the names of the DB-IP Lite files and finds them on disk.
 * DB-IP publishes one file per month, for example {@code dbip-country-lite-2026-11.mmdb.gz}.
 * The application stores it unpacked under the same name without {@code .gz}.
 */
public final class DbIpFileLocator {

	private static final Pattern LOCAL_FILE_NAME = Pattern.compile("dbip-country-lite-\\d{4}-\\d{2}\\.mmdb");

	private DbIpFileLocator() {
	}

	public static String localFileName(YearMonth month) {
		return "dbip-country-lite-" + month + ".mmdb";
	}

	public static String remoteFileName(YearMonth month) {
		return localFileName(month) + ".gz";
	}

	/**
	 * Lists the DB-IP Lite files in a directory.
	 * The month in the file name sorts the list from the oldest to the newest file.
	 */
	public static List<Path> listLocalFiles(Path directory) {
		if (!Files.isDirectory(directory)) {
			return List.of();
		}
		try (Stream<Path> files = Files.list(directory)) {
			return files
					.filter(file -> LOCAL_FILE_NAME.matcher(file.getFileName().toString()).matches())
					.sorted(Comparator.comparing(file -> file.getFileName().toString()))
					.toList();
		} catch (IOException e) {
			throw new UncheckedIOException(
					"Cannot list the DB-IP Lite files in " + directory,
					e
			);
		}
	}

	public static Optional<Path> findNewestLocalFile(Path directory) {
		List<Path> files = listLocalFiles(directory);
		return files.isEmpty()
				? Optional.empty()
				: Optional.of(files.getLast());
	}

}
