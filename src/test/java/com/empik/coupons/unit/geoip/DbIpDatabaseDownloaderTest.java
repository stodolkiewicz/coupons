package com.empik.coupons.unit.geoip;

import static org.assertj.core.api.Assertions.assertThat;

import com.empik.coupons.geoip.DbIpFileLocator;
import com.empik.coupons.geoip.download.DbIpDatabaseDownloader;
import com.empik.coupons.geoip.download.DbIpDatabaseDownloaderProperties;
import com.empik.coupons.geoip.resolver.IpCountryResolverProperties;
import com.empik.coupons.geoip.resolver.IpCountryResolverService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * {@link #fakeDbIpServer} is a local HTTP server that plays the DB-IP server.
 * It serves the gzipped DB-IP Lite file committed to the repository under the names in {@link #gzippedFilesPublishedOnFakeDbIpServer}.
 */
class DbIpDatabaseDownloaderTest {
	// How a test publishes a file on the fake DB-IP server? -> it adds an entry to gzippedFilesPublishedOnFakeDbIpServer.
	// servePublishedFileFromFakeDbIpServer answers each request from this map with 200 and the bytes, or with 404.
	//
	// How the downloader is wired?
	// createDbIpDatabaseDownloaderPropertiesForTempDirectoryAndFakeDbIpServer sets the directory to the @TempDir directory
	// and the base URL to fakeDbIpServer.
	// So the downloader downloads from fakeDbIpServer and saves the files in the temporary directory.

	private static final YearMonth CURRENT_MONTH = YearMonth.now(ZoneOffset.UTC);
	private static final YearMonth PREVIOUS_MONTH = CURRENT_MONTH.minusMonths(1);
	private static final String FAKE_DB_IP_SERVER_HOST = "127.0.0.1";

	// The repository always contains one DB-IP Lite file in geoip/.
	private static final Path LOCAL_DB_IP_DATABASE_FILE = DbIpFileLocator.findNewestLocalFile(Path.of("geoip"))
			.orElseThrow(() -> new IllegalStateException("The repository has no DB-IP Lite file in geoip/"));

	private static byte[] gzippedLocalDbIpDatabaseFile;

	private final Map<String, byte[]> gzippedFilesPublishedOnFakeDbIpServer = new ConcurrentHashMap<>();
	private final List<String> fileNamesRequestedFromFakeDbIpServer = new CopyOnWriteArrayList<>();

	// Before each test, JUnit creates a new empty temporary directory and puts its path into this field.
	// After the test, JUnit deletes the directory together with its content.
	@TempDir
	Path directoryPath;

	HttpServer fakeDbIpServer;

	/**
	 * The repository always contains one DB-IP Lite file in {@code geoip/}, so every test can use it.
	 */
	@BeforeAll
	static void gzipLocalDbIpDatabaseFile() throws IOException {
		gzippedLocalDbIpDatabaseFile = gzipBytes(Files.readAllBytes(LOCAL_DB_IP_DATABASE_FILE));
	}

	@BeforeEach
	void startFakeDbIpServer() throws IOException {
		fakeDbIpServer = HttpServer.create(
				new InetSocketAddress(
						FAKE_DB_IP_SERVER_HOST,
						0
				),
				0
		);
		// Sends every request whose path starts with /free/ to the handler method.
		fakeDbIpServer.createContext(
				"/free/",
				// The handler: the server calls this method for every request.
				// It passes an HttpExchange, which is one request together with its response.
				this::servePublishedFileFromFakeDbIpServer
		);
		fakeDbIpServer.start();
	}

	@AfterEach
	void stopFakeDbIpServer() {
		fakeDbIpServer.stop(0);
	}

	@Test
	void givenNoLocalFile_shouldDownloadCurrentMonthAndLoadIt() throws IOException {
		// given
		publishLocalDbIpDatabaseFileOnFakeDbIpServer(CURRENT_MONTH);
		IpCountryResolverService ipCountryResolverService = createIpCountryResolverServiceForTempDirectory();
		DbIpDatabaseDownloader downloader = createDbIpDatabaseDownloaderForTempDirectoryAndFakeDbIpServer(ipCountryResolverService);

		// when
		downloader.downloadDbIpLiteFileIfCurrentMonthIsMissing();

		// then
		assertThat(listFileNamesInTempDirectory()).containsExactly(DbIpFileLocator.localFileName(CURRENT_MONTH));
		assertThat(ipCountryResolverService.resolveCountryCode(InetAddress.getByName("8.8.8.8"))).contains("US");
	}

	@Test
	void givenCurrentMonthFileOnDisk_shouldSendNoRequest() throws IOException {
		// given
		copyLocalDbIpDatabaseFileAs(CURRENT_MONTH);
		publishLocalDbIpDatabaseFileOnFakeDbIpServer(CURRENT_MONTH);
		IpCountryResolverService ipCountryResolverService = createIpCountryResolverServiceForTempDirectory();
		DbIpDatabaseDownloader downloader = createDbIpDatabaseDownloaderForTempDirectoryAndFakeDbIpServer(ipCountryResolverService);

		// when
		downloader.downloadDbIpLiteFileIfCurrentMonthIsMissing();

		// then
		assertThat(fileNamesRequestedFromFakeDbIpServer).isEmpty();
	}

	@Test
	void givenNewMonthPublished_shouldDeleteFileOfPreviousMonth() throws IOException {
		// given
		copyLocalDbIpDatabaseFileAs(PREVIOUS_MONTH);
		publishLocalDbIpDatabaseFileOnFakeDbIpServer(CURRENT_MONTH);
		IpCountryResolverService ipCountryResolverService = createIpCountryResolverServiceForTempDirectory();
		DbIpDatabaseDownloader downloader = createDbIpDatabaseDownloaderForTempDirectoryAndFakeDbIpServer(ipCountryResolverService);

		// when
		downloader.downloadDbIpLiteFileIfCurrentMonthIsMissing();

		// then
		assertThat(listFileNamesInTempDirectory()).containsExactly(DbIpFileLocator.localFileName(CURRENT_MONTH));
	}

	@Test
	void givenCurrentMonthNotPublishedAndOldFileOnDisk_shouldKeepOldFile() throws IOException {
		// given
		copyLocalDbIpDatabaseFileAs(PREVIOUS_MONTH);
		IpCountryResolverService ipCountryResolverService = createIpCountryResolverServiceForTempDirectory();
		DbIpDatabaseDownloader downloader = createDbIpDatabaseDownloaderForTempDirectoryAndFakeDbIpServer(ipCountryResolverService);

		// when
		downloader.downloadDbIpLiteFileIfCurrentMonthIsMissing();

		// then
		assertThat(fileNamesRequestedFromFakeDbIpServer).containsExactly(DbIpFileLocator.remoteFileName(CURRENT_MONTH));
		assertThat(listFileNamesInTempDirectory()).containsExactly(DbIpFileLocator.localFileName(PREVIOUS_MONTH));
	}

	@Test
	void givenCurrentMonthNotPublishedAndNoLocalFile_shouldDownloadPreviousMonth() throws IOException {
		// given
		publishLocalDbIpDatabaseFileOnFakeDbIpServer(PREVIOUS_MONTH);
		IpCountryResolverService ipCountryResolverService = createIpCountryResolverServiceForTempDirectory();
		DbIpDatabaseDownloader downloader = createDbIpDatabaseDownloaderForTempDirectoryAndFakeDbIpServer(ipCountryResolverService);

		// when
		downloader.downloadDbIpLiteFileIfCurrentMonthIsMissing();

		// then
		assertThat(listFileNamesInTempDirectory()).containsExactly(DbIpFileLocator.localFileName(PREVIOUS_MONTH));
		assertThat(ipCountryResolverService.resolveCountryCode(InetAddress.getByName("8.8.8.8"))).contains("US");
	}

	@Test
	void givenPublishedFileIsNotDatabase_shouldDeleteItAndKeepOldFile() throws IOException {
		// given
		copyLocalDbIpDatabaseFileAs(PREVIOUS_MONTH);
		gzippedFilesPublishedOnFakeDbIpServer.put(
				DbIpFileLocator.remoteFileName(CURRENT_MONTH),
				gzipBytes("not a database".getBytes())
		);
		IpCountryResolverService ipCountryResolverService = createIpCountryResolverServiceForTempDirectory();
		DbIpDatabaseDownloader downloader = createDbIpDatabaseDownloaderForTempDirectoryAndFakeDbIpServer(ipCountryResolverService);

		// when
		downloader.downloadDbIpLiteFileIfCurrentMonthIsMissing();

		// then
		assertThat(listFileNamesInTempDirectory()).containsExactly(DbIpFileLocator.localFileName(PREVIOUS_MONTH));
		assertThat(ipCountryResolverService.resolveCountryCode(InetAddress.getByName("8.8.8.8"))).contains("US");
	}

	@Test
	void givenNoFilePublished_shouldLeaveDirectoryEmpty() throws IOException {
		// given
		IpCountryResolverService ipCountryResolverService = createIpCountryResolverServiceForTempDirectory();
		DbIpDatabaseDownloader downloader = createDbIpDatabaseDownloaderForTempDirectoryAndFakeDbIpServer(ipCountryResolverService);

		// when
		downloader.downloadDbIpLiteFileIfCurrentMonthIsMissing();

		// then
		assertThat(fileNamesRequestedFromFakeDbIpServer).containsExactly(
				DbIpFileLocator.remoteFileName(CURRENT_MONTH),
				DbIpFileLocator.remoteFileName(PREVIOUS_MONTH)
		);
		assertThat(listFileNamesInTempDirectory()).isEmpty();
	}

	private IpCountryResolverService createIpCountryResolverServiceForTempDirectory() {
		return new IpCountryResolverService(new IpCountryResolverProperties(directoryPath));
	}

	private DbIpDatabaseDownloader createDbIpDatabaseDownloaderForTempDirectoryAndFakeDbIpServer(IpCountryResolverService ipCountryResolverService) {
		return new DbIpDatabaseDownloader(
				ipCountryResolverService,
				createDbIpDatabaseDownloaderPropertiesForTempDirectoryAndFakeDbIpServer()
		);
	}

	private DbIpDatabaseDownloaderProperties createDbIpDatabaseDownloaderPropertiesForTempDirectoryAndFakeDbIpServer() {
		return new DbIpDatabaseDownloaderProperties(
				directoryPath,
				new DbIpDatabaseDownloaderProperties.Download(
						URI.create("http://" + FAKE_DB_IP_SERVER_HOST + ":" + fakeDbIpServer.getAddress().getPort() + "/free"),
						Duration.ofSeconds(5)
				)
		);
	}

	private void publishLocalDbIpDatabaseFileOnFakeDbIpServer(YearMonth month) {
		gzippedFilesPublishedOnFakeDbIpServer.put(
				DbIpFileLocator.remoteFileName(month),
				gzippedLocalDbIpDatabaseFile
		);
	}

	private static byte[] gzipBytes(byte[] content) throws IOException {
		ByteArrayOutputStream gzipped = new ByteArrayOutputStream();
		try (OutputStream out = new GZIPOutputStream(gzipped)) {
			out.write(content);
		}
		return gzipped.toByteArray();
	}

	private void copyLocalDbIpDatabaseFileAs(YearMonth month) throws IOException {
		Files.copy(
				LOCAL_DB_IP_DATABASE_FILE,
				directoryPath.resolve(DbIpFileLocator.localFileName(month))
		);
	}

	private List<String> listFileNamesInTempDirectory() throws IOException {
		try (Stream<Path> files = Files.list(directoryPath)) {
			return files
					.map(file -> file.getFileName().toString())
					.toList();
		}
	}

	private void servePublishedFileFromFakeDbIpServer(HttpExchange exchange) throws IOException {
		// The downloader requests <base URL>/<file name>, for example /free/dbip-country-lite-2026-11.mmdb.gz.
		// Cutting off /free/ leaves the file name, for example dbip-country-lite-2026-11.mmdb.gz.
		String fileName = exchange.getRequestURI().getPath().substring("/free/".length());
		fileNamesRequestedFromFakeDbIpServer.add(fileName);
		byte[] body = gzippedFilesPublishedOnFakeDbIpServer.get(fileName);
		try (exchange) {
			if (body == null) {
				exchange.sendResponseHeaders(
						404,
						-1
				);
				return;
			}
			exchange.sendResponseHeaders(
					200,
					body.length
			);
			exchange.getResponseBody().write(body);
		}
	}

}
