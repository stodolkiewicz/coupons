package com.empik.coupons.unit.geoip;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.empik.coupons.exceptions.GeoIpLookupException;
import com.empik.coupons.geoip.DbIpFileLocator;
import com.empik.coupons.geoip.resolver.IpCountryResolverProperties;
import com.empik.coupons.geoip.resolver.IpCountryResolverService;
import com.maxmind.db.InvalidDatabaseException;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class IpCountryResolverServiceTest {

	private static final InetAddress GOOGLE_DNS = parseIpAddress("8.8.8.8");
	private static final InetAddress LOOPBACK = parseIpAddress("127.0.0.1");

	// The repository always contains one DB-IP Lite file in geoip/.
	private static final Path REPOSITORY_GEOIP_DIRECTORY = Path.of("geoip");
	private static final Path LOCAL_DB_IP_DATABASE_FILE = DbIpFileLocator.findNewestLocalFile(REPOSITORY_GEOIP_DIRECTORY)
			.orElseThrow(() -> new IllegalStateException("The repository has no DB-IP Lite file in geoip/"));

	@Test
	void givenDirectoryWithFile_shouldResolveCountryOfPublicIp() {
		// given
		IpCountryResolverService ipCountryResolverService = new IpCountryResolverService(new IpCountryResolverProperties(REPOSITORY_GEOIP_DIRECTORY));

		// when
		Optional<String> countryCode = ipCountryResolverService.resolveCountryCode(GOOGLE_DNS);

		// then
		assertThat(countryCode).contains("US");
	}

	@Test
	void givenDirectoryWithFile_shouldReturnEmptyForLoopbackIp() {
		// given
		IpCountryResolverService ipCountryResolverService = new IpCountryResolverService(new IpCountryResolverProperties(REPOSITORY_GEOIP_DIRECTORY));

		// when
		Optional<String> countryCode = ipCountryResolverService.resolveCountryCode(LOOPBACK);

		// then
		assertThat(countryCode).isEmpty();
	}

	@Test
	void givenEmptyDirectory_shouldReturnEmpty(@TempDir Path directory) {
		// given
		IpCountryResolverService ipCountryResolverService = new IpCountryResolverService(new IpCountryResolverProperties(directory));

		// when
		Optional<String> countryCode = ipCountryResolverService.resolveCountryCode(GOOGLE_DNS);

		// then
		assertThat(countryCode).isEmpty();
	}

	@Test
	void givenFileLoadedAfterStart_shouldResolveCountryFromIt(@TempDir Path directory) throws IOException {
		// given
		IpCountryResolverService ipCountryResolverService = new IpCountryResolverService(new IpCountryResolverProperties(directory));

		// when
		ipCountryResolverService.loadDbIpLiteDatabaseIntoMemory(LOCAL_DB_IP_DATABASE_FILE);

		// then
		assertThat(ipCountryResolverService.resolveCountryCode(GOOGLE_DNS)).contains("US");
	}

	private static InetAddress parseIpAddress(String address) {
		try {
			return InetAddress.getByName(address);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

}
