package ninja.mspp.operation.jpost;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ninja.mspp.operation.jpost.model.JpostFile;
import ninja.mspp.operation.jpost.model.JpostProject;

/**
 * Thin HTTP client for the public jPOST repository (https://repository.jpostdb.org).
 *
 * <p>The {@code /_api} endpoints reject requests that lack a same-site
 * {@code Referer} header, so every request carries one (plus the
 * {@code X-Requested-With} header the web client sends).</p>
 */
class JpostClient {
	private static final String SITE = "https://repository.jpostdb.org";
	private static final String BASE_URL = SITE + "/_api";
	private static final String DATA_URL = SITE + "/data";
	private static final String STORAGE_URL = "https://storage.jpostdb.org";
	private static final ObjectMapper MAPPER = new ObjectMapper();

	private final HttpClient http;

	JpostClient() {
		this.http = HttpClient.newBuilder()
				.connectTimeout(Duration.ofSeconds(15))
				.build();
	}

	/** A single page of projects together with the total number of matching projects. */
	static final class ProjectPage {
		final List<JpostProject> projects;
		final int total;

		ProjectPage(List<JpostProject> projects, int total) {
			this.projects = projects;
			this.total = total;
		}
	}

	/**
	 * Fetches one page of public projects, optionally filtered by {@code keyword}
	 * (server-side search over all fields). Results are sorted by announcement date,
	 * newest first. The total number of matching projects is read from the response.
	 */
	ProjectPage listProjects(int offset, int limit, String keyword)
			throws IOException, InterruptedException {
		String url = BASE_URL + "/project"
			+ "?offset=" + offset
			+ "&num=" + limit
			+ "&sortKey=announcementDate&sortDir=desc"
			+ "&searchKey=&searchMode="
			+ "&keyword=" + encodeParam(keyword);
		String body = this.get(url);
		JsonNode root = MAPPER.readTree(body);
		if(!root.isArray()) {
			return new ProjectPage(Collections.emptyList(), 0);
		}
		JpostProject[] arr = MAPPER.treeToValue(root, JpostProject[].class);
		int total = arr.length > 0 ? arr[0].getTotal() : 0;
		return new ProjectPage(List.of(arr), total);
	}

	List<JpostFile> listFiles(String projectId, int offset, int limit)
			throws IOException, InterruptedException {
		String url = BASE_URL + "/file"
			+ "?offset=" + offset
			+ "&num=" + limit
			+ "&target=server"
			+ "&projectId=" + encodeParam(projectId)
			+ "&sortKey=id&sortDir="
			+ "&searchKey=&searchMode=&keyword=&fileType="
			+ "&projectKey=&projectPin=&projectType=";
		String body = this.get(url);
		JsonNode root = MAPPER.readTree(body);
		JsonNode list = root.path("list");
		if(!list.isArray()) {
			return Collections.emptyList();
		}
		JpostFile[] arr = MAPPER.treeToValue(list, JpostFile[].class);
		return List.of(arr);
	}

	/**
	 * Fetches all files of the project, paginated. Stops when {@code total} is
	 * reached (if &gt; 0) or when the API returns an empty/short page.
	 */
	List<JpostFile> listAllFiles(String projectId, int total, IntConsumer progress)
			throws IOException, InterruptedException {
		final int pageSize = 100;
		List<JpostFile> all = new ArrayList<JpostFile>(Math.max(total, pageSize));
		int offset = 0;
		while(true) {
			List<JpostFile> page = this.listFiles(projectId, offset, pageSize);
			if(page.isEmpty()) {
				break;
			}
			all.addAll(page);
			if(progress != null) {
				progress.accept(all.size());
			}
			if(total > 0 && all.size() >= total) {
				break;
			}
			if(page.size() < pageSize) {
				break;
			}
			offset += page.size();
		}
		return all;
	}

	/** Builds the public download URL for a file. */
	static String downloadUrl(JpostFile file) {
		if(file.isArchived()) {
			return STORAGE_URL + "/" + stripRevision(file.getLocation())
				+ "/" + encodePath(file.getFileName());
		}
		return DATA_URL + "/" + file.getLocation() + "/" + encodePath(file.getFileName());
	}

	/**
	 * Downloads a project file into {@code dest}. Reports total bytes received via
	 * {@code progress}. Streams to disk to avoid loading large files in memory.
	 */
	void download(JpostFile file, Path dest, LongConsumer progress)
			throws IOException, InterruptedException {
		String url = downloadUrl(file);
		HttpRequest req = HttpRequest.newBuilder(URI.create(url))
				.header("Referer", SITE + "/")
				.timeout(Duration.ofMinutes(60))
				.GET()
				.build();
		HttpResponse<InputStream> resp = this.http.send(req, HttpResponse.BodyHandlers.ofInputStream());
		if(resp.statusCode() / 100 != 2) {
			resp.body().close();
			throw new IOException("HTTP " + resp.statusCode() + " for " + url);
		}
		Files.createDirectories(dest.getParent());
		Path tmp = Files.createTempFile(dest.getParent(), "dl-", ".part");
		try(InputStream in = resp.body();
			OutputStream out = Files.newOutputStream(tmp)) {
			byte[] buf = new byte[1 << 16];
			long total = 0L;
			int n;
			while((n = in.read(buf)) > 0) {
				out.write(buf, 0, n);
				total += n;
				if(progress != null) {
					progress.accept(total);
				}
			}
		}
		Files.move(tmp, dest, StandardCopyOption.REPLACE_EXISTING);
	}

	/** Removes the trailing {@code .<revision>} from a location (JPST000001.0 -&gt; JPST000001). */
	private static String stripRevision(String location) {
		return location == null ? "" : location.replaceFirst("\\.\\d+", "");
	}

	private static String encodePath(String name) {
		return URLEncoder.encode(name, StandardCharsets.UTF_8).replace("+", "%20");
	}

	private static String encodeParam(String value) {
		return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
	}

	private String get(String url) throws IOException, InterruptedException {
		HttpRequest req = HttpRequest.newBuilder(URI.create(url))
				.header("Accept", "application/json")
				.header("Referer", SITE + "/")
				.header("X-Requested-With", "XMLHttpRequest")
				.timeout(Duration.ofSeconds(60))
				.GET()
				.build();
		HttpResponse<String> resp = this.http.send(req, HttpResponse.BodyHandlers.ofString());
		if(resp.statusCode() / 100 != 2) {
			throw new IOException("HTTP " + resp.statusCode() + " for " + url);
		}
		return resp.body();
	}
}
