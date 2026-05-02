package ninja.mspp.operation.mb_post;

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

import ninja.mspp.operation.mb_post.model.MbPostFile;
import ninja.mspp.operation.mb_post.model.MbPostFileDetail;
import ninja.mspp.operation.mb_post.model.MbPostProject;

class MbPostClient {
	private static final String BASE_URL = "https://repository.massbank.jp/api";
	private static final String DATA_URL = "https://repository.massbank.jp/data";
	private static final ObjectMapper MAPPER = new ObjectMapper();

	private final HttpClient http;

	MbPostClient() {
		this.http = HttpClient.newBuilder()
				.connectTimeout(Duration.ofSeconds(15))
				.build();
	}

	int getOpenedProjectCount() throws IOException, InterruptedException {
		String body = this.get(BASE_URL + "/statistics");
		JsonNode root = MAPPER.readTree(body);
		return root.path("project").path("opened").asInt(0);
	}

	List<MbPostProject> listProjects(int offset, int limit) throws IOException, InterruptedException {
		String body = this.get(BASE_URL + "/projects?limit=" + limit + "&offset=" + offset);
		JsonNode root = MAPPER.readTree(body);
		JsonNode list = root.path("list");
		if(!list.isArray()) {
			return Collections.emptyList();
		}
		MbPostProject[] arr = MAPPER.treeToValue(list, MbPostProject[].class);
		return List.of(arr);
	}

	/**
	 * Fetches all opened projects, paginated. {@code progress} is called with the
	 * cumulative number of projects fetched so far (or -1 once when total is unknown).
	 */
	List<MbPostProject> listAllProjects(IntConsumer progress) throws IOException, InterruptedException {
		final int pageSize = 100;
		int total = this.getOpenedProjectCount();
		List<MbPostProject> all = new ArrayList<MbPostProject>(Math.max(total, pageSize));
		int offset = 0;
		while(true) {
			List<MbPostProject> page = this.listProjects(offset, pageSize);
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

	MbPostProject getProject(String projectId) throws IOException, InterruptedException {
		String body = this.get(BASE_URL + "/projects/" + projectId);
		return MAPPER.readValue(body, MbPostProject.class);
	}

	MbPostFileDetail getFileDetail(String location, String fileId)
			throws IOException, InterruptedException {
		String body = this.get(BASE_URL + "/projects/" + location + "/files/" + fileId);
		return MAPPER.readValue(body, MbPostFileDetail.class);
	}

	List<MbPostFile> listFiles(String location, int offset, int limit)
			throws IOException, InterruptedException {
		String body = this.get(BASE_URL + "/projects/" + location
			+ "/files?limit=" + limit + "&offset=" + offset);
		JsonNode root = MAPPER.readTree(body);
		JsonNode list = root.path("list");
		if(!list.isArray()) {
			return Collections.emptyList();
		}
		MbPostFile[] arr = MAPPER.treeToValue(list, MbPostFile[].class);
		return List.of(arr);
	}

	/**
	 * Fetches all files of the project, paginated. Stops when {@code total} is
	 * reached (if &gt; 0) or when the API returns an empty/short page.
	 */
	List<MbPostFile> listAllFiles(String location, int total, IntConsumer progress)
			throws IOException, InterruptedException {
		final int pageSize = 100;
		List<MbPostFile> all = new ArrayList<MbPostFile>(Math.max(total, pageSize));
		int offset = 0;
		while(true) {
			List<MbPostFile> page = this.listFiles(location, offset, pageSize);
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

	/**
	 * Downloads a project file into {@code dest}. Reports total bytes received via
	 * {@code progress}. Streams to disk to avoid loading large files in memory.
	 */
	void download(String location, String filename, Path dest, LongConsumer progress)
			throws IOException, InterruptedException {
		String url = DATA_URL + "/" + location + "/" + encodePath(filename);
		HttpRequest req = HttpRequest.newBuilder(URI.create(url))
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

	private static String encodePath(String name) {
		return URLEncoder.encode(name, StandardCharsets.UTF_8).replace("+", "%20");
	}

	private String get(String url) throws IOException, InterruptedException {
		HttpRequest req = HttpRequest.newBuilder(URI.create(url))
				.header("Accept", "application/json")
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
