package com.empik.coupons.geoip.resolver;

import com.empik.coupons.exceptions.GeoIpLookupException;
import com.empik.coupons.geoip.DbIpFileLocator;
import com.maxmind.db.Reader.FileMode;
import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Finds the country of an IP address in the DB-IP Lite file.
 * {@link com.empik.coupons.geoip.download.DbIpDatabaseDownloader} calls {@link #loadDbIpLiteDatabaseIntoMemory(Path)} after it downloads a new file.
 */
@Slf4j
@Component
public class IpCountryResolverService implements IpCountryResolver {

	private final AtomicReference<DatabaseReader> reader = new AtomicReference<>();

	public IpCountryResolverService(IpCountryResolverProperties properties) {
		Optional<Path> newestFile = DbIpFileLocator.findNewestLocalFile(properties.directory());
		if (newestFile.isEmpty()) {
			log.warn(
					"No DB-IP Lite file in {}. The country of every IP address is unknown until a download succeeds.",
					properties.directory().toAbsolutePath()
			);
			return;
		}
		try {
			loadDbIpLiteDatabaseIntoMemory(newestFile.get());
		} catch (IOException e) {
			log.error(
					"Cannot read the DB-IP Lite file {}",
					newestFile.get(),
					e
			);
		}
	}

	/**
	 * Replaces the loaded file with the given one.
	 * {@link FileMode#MEMORY} copies the whole file into memory, so the file can be deleted afterwards.
	 * The previous reader stays open, so lookups that already use it can finish.
	 * The garbage collector frees it.
	 */
	public void loadDbIpLiteDatabaseIntoMemory(Path dbIpLiteFile) throws IOException {
		DatabaseReader newReader = new DatabaseReader.Builder(dbIpLiteFile.toFile())
				.fileMode(FileMode.MEMORY)
				.build();
		reader.set(newReader);
		log.info(
				"Loaded the DB-IP Lite file {}",
				dbIpLiteFile
		);
	}

	public boolean isDbIpLiteDatabaseLoadedIntoMemory() {
		return reader.get() != null;
	}

	@Override
	public Optional<String> resolveCountryCode(InetAddress ip) {
		DatabaseReader currentReader = reader.get();
		if (currentReader == null) {
			return Optional.empty();
		}
		try {
			return currentReader.tryCountry(ip)
					.map(response -> response.country().isoCode());
		} catch (IOException | GeoIp2Exception e) {
			throw new GeoIpLookupException(
					ip,
					e
			);
		}
	}

}
