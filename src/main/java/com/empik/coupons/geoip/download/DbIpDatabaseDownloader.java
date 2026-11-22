package com.empik.coupons.geoip.download;

import com.empik.coupons.geoip.DbIpFileLocator;
import com.empik.coupons.geoip.resolver.IpCountryResolverService;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.zip.GZIPInputStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Downloads the DB-IP Lite file of the current month and loads it into {@link IpCountryResolverService}.
 * DB-IP publishes a new file every month, usually on the 1st.
 * {@link com.empik.coupons.geoip.triggers.DbIpDatabaseStartupUpdater} runs the check at startup and {@link com.empik.coupons.geoip.triggers.DbIpDatabaseScheduledUpdater} runs it every day,
 * so the check catches the new file whatever day it comes out.
 * When the file of the current month is already on disk, the check sends no request.
 */
@Slf4j
@Component
public class DbIpDatabaseDownloader {

	private static final int HTTP_OK = 200;
	private static final int HTTP_NOT_FOUND = 404;

	private final IpCountryResolverService ipCountryResolverService;
	private final DbIpDatabaseDownloaderProperties properties;
	private final HttpClient httpClient;

	public DbIpDatabaseDownloader(
			IpCountryResolverService ipCountryResolverService,
			DbIpDatabaseDownloaderProperties properties
	) {
		this.ipCountryResolverService = ipCountryResolverService;
		this.properties = properties;
		this.httpClient = HttpClient.newBuilder()
				.connectTimeout(properties.download().timeout())
				.followRedirects(HttpClient.Redirect.NORMAL)
				.build();
	}

	/**
	 * Downloads the file of the current month when it is not on disk yet.
	 * When DB-IP has not published it yet and no file is on disk at all, it downloads the file of the previous month.
	 * A failed download keeps the file that is already loaded.
	 */
	public synchronized void downloadDbIpLiteFileIfCurrentMonthIsMissing() {
		YearMonth currentMonth = YearMonth.now(ZoneOffset.UTC);
		if (Files.exists(localDbIpLiteFilePath(currentMonth))) {
			log.debug(
					"The DB-IP Lite file for {} is already on disk",
					currentMonth
			);
			return;
		}
		try {
			if (downloadAndLoadDbIpLiteFileForMonth(currentMonth)) {
				return;
			}
			log.info(
					"DB-IP has not published the Lite file for {} yet",
					currentMonth
			);
			if (DbIpFileLocator.findNewestLocalFile(properties.directory()).isPresent()) {
				return;
			}
			YearMonth previousMonth = currentMonth.minusMonths(1);
			if (!downloadAndLoadDbIpLiteFileForMonth(previousMonth)) {
				log.warn(
						"DB-IP has no Lite file for {} either. The country of every IP address stays unknown.",
						previousMonth
				);
			}
		} catch (IOException e) {
			log.warn(
					"Cannot download the DB-IP Lite file. The application keeps the loaded file.",
					e
			);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			log.warn(
					"The download of the DB-IP Lite file was interrupted",
					e
			);
		}
	}

	/**
	 * Downloads the file of the given month, loads it and deletes older files.
	 *
	 * @return {@code false} when DB-IP has no file for the month
	 */
	private boolean downloadAndLoadDbIpLiteFileForMonth(YearMonth month) throws IOException, InterruptedException {
		URI uri = URI.create(properties.download().baseUrl() + "/" + DbIpFileLocator.remoteFileName(month));
		HttpRequest request = HttpRequest.newBuilder(uri)
				.timeout(properties.download().timeout())
				.GET()
				.build();
		HttpResponse<InputStream> response = httpClient.send(
				request,
				HttpResponse.BodyHandlers.ofInputStream()
		);
		try (InputStream body = response.body()) {
			if (response.statusCode() == HTTP_NOT_FOUND) {
				return false;
			}
			if (response.statusCode() != HTTP_OK) {
				throw new IOException("DB-IP returned HTTP " + response.statusCode() + " for " + uri);
			}
			Path target = unpackAndSaveDbIpLiteFile(
					body,
					month
			);
			loadDbIpLiteFileIntoResolverOrDeleteIt(target);
			deleteDbIpLiteFilesOtherThan(target);
			return true;
		}
	}

	/**
	 * Writes the unpacked file to a temporary file first and then renames it.
	 * Other code never sees a partly written file under the final name.
	 */
	private Path unpackAndSaveDbIpLiteFile(
			InputStream gzippedBody,
			YearMonth month
	) throws IOException {
		Files.createDirectories(properties.directory());
		Path temporaryFile = Files.createTempFile(
				properties.directory(),
				"dbip-country-lite-",
				".download"
		);
		try {
			try (InputStream unpacked = new GZIPInputStream(gzippedBody)) {
				Files.copy(
						unpacked,
						temporaryFile,
						StandardCopyOption.REPLACE_EXISTING
				);
			}
			Path target = localDbIpLiteFilePath(month);
			Files.move(
					temporaryFile,
					target,
					StandardCopyOption.ATOMIC_MOVE
			);
			return target;
		} finally {
			Files.deleteIfExists(temporaryFile);
		}
	}

	/**
	 * Deletes the downloaded file when it cannot be read, so the next check downloads it again.
	 */
	private void loadDbIpLiteFileIntoResolverOrDeleteIt(Path file) throws IOException {
		try {
			ipCountryResolverService.loadDbIpLiteDatabaseIntoMemory(file);
		} catch (IOException e) {
			Files.deleteIfExists(file);
			throw e;
		}
	}

	private void deleteDbIpLiteFilesOtherThan(Path fileToKeep) throws IOException {
		for (Path file : DbIpFileLocator.listLocalFiles(properties.directory())) {
			if (!file.equals(fileToKeep)) {
				Files.delete(file);
				log.info(
						"Deleted the old DB-IP Lite file {}",
						file
				);
			}
		}
	}

	private Path localDbIpLiteFilePath(YearMonth month) {
		return properties.directory().resolve(DbIpFileLocator.localFileName(month));
	}

}
