package ninja.mspp.io.mzml.convert;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ninja.mspp.tool.docker.DockerManager;
import ninja.mspp.tool.docker.DockerManager.ExecResult;

/**
 * Converts vendor MS files (Thermo .raw, Sciex .wiff) to mzML by running
 * ProteoWizard msconvert inside a Docker container that bind-mounts a host
 * working directory, so input/output files are accessed without docker cp.
 */
public class VendorConverter {
	private static final String IMAGE = "chambm/pwiz-skyline-i-agree-to-the-vendor-licenses:latest";
	private static final String CONTAINER = "mspp-pwiz";
	private static final String CONTAINER_WORK = "/data";

	private static final Pattern SPECTRA_PATTERN =
			Pattern.compile("writing spectra:\\s*(\\d+)/(\\d+)");

	private Consumer<String> statusListener;
	private DoubleConsumer progressListener;

	public static boolean isVendorFile(File file) {
		String name = file.getName().toLowerCase(Locale.ROOT);
		return name.endsWith(".raw") || name.endsWith(".wiff") || name.endsWith(".lcd");
	}

	public void setStatusListener(Consumer<String> listener) {
		this.statusListener = listener;
	}

	/**
	 * Sets a listener that receives msconvert progress as a fraction in [0, 1].
	 * Negative values indicate the phase has no determinate progress yet.
	 */
	public void setProgressListener(DoubleConsumer listener) {
		this.progressListener = listener;
	}

	private void status(String message) {
		if(this.statusListener != null) {
			this.statusListener.accept(message);
		}
	}

	private void progress(double fraction) {
		if(this.progressListener != null) {
			this.progressListener.accept(fraction);
		}
	}

	/**
	 * Converts the input vendor file to mzML and returns the produced mzML file
	 * (located inside the docker working folder, accessible directly on host).
	 */
	public File convertToMzml(File input) throws IOException, InterruptedException {
		if(!input.exists()) {
			throw new FileNotFoundException(input.getAbsolutePath());
		}

		DockerManager dm = DockerManager.getInstance();
		File workRoot = new File(dm.getDockerFolder(), "convert");
		workRoot.mkdirs();

		this.status("Preparing converter container...");
		dm.ensureContainerWithBind(IMAGE, CONTAINER, workRoot, CONTAINER_WORK);

		String jobId = UUID.randomUUID().toString();
		File hostJob = new File(workRoot, jobId);
		File hostIn = new File(hostJob, "in");
		File hostOut = new File(hostJob, "out");
		hostIn.mkdirs();
		hostOut.mkdirs();

		List<File> collected = this.collectInputFiles(input);
		StringBuilder copiedLog = new StringBuilder();
		this.status("Staging " + collected.size() + " input file(s)...");
		for(File companion : collected) {
			Files.copy(
				companion.toPath(),
				new File(hostIn, companion.getName()).toPath(),
				StandardCopyOption.REPLACE_EXISTING
			);
			copiedLog.append("  ").append(companion.getName())
				.append(" (").append(companion.length()).append(" bytes)\n");
		}

		String containerJob = CONTAINER_WORK + "/" + jobId;
		String containerInDir = containerJob + "/in";
		String containerOutDir = containerJob + "/out";

		this.status("Running msconvert (this may take a while)...");
		this.progress(-1.0);
		String containerInput = containerInDir + "/" + input.getName();
		ExecResult msconvert = dm.execCommandStreaming(
			CONTAINER,
			line -> this.handleMsconvertLine(line),
			"wine", "msconvert", "-v", containerInput,
			"--mzML", "--ignoreUnknownInstrumentError", "-o", containerOutDir
		);

		this.status("Locating output file...");
		File hostMzml = this.findHostMzml(hostOut);
		if(hostMzml == null || hostMzml.length() == 0L) {
			String inListing = this.listing(hostIn);
			String outListing = this.listing(hostOut);
			throw new IOException(
				"msconvert produced an empty or missing mzML for " + input.getName()
				+ " (exit " + msconvert.exitCode + ")"
				+ "\n--- staged input files ---\n" + copiedLog
				+ "--- host input dir (" + hostIn + ") ---\n" + inListing
				+ "--- host output dir (" + hostOut + ") ---\n" + outListing
				+ "\n--- msconvert output ---\n" + msconvert.output
			);
		}
		return hostMzml;
	}

	private void handleMsconvertLine(String line) {
		Matcher m = SPECTRA_PATTERN.matcher(line);
		if(m.find()) {
			double done = Double.parseDouble(m.group(1));
			double total = Double.parseDouble(m.group(2));
			if(total > 0.0) {
				this.progress(done / total);
				this.status("Converting: " + (int) done + "/" + (int) total + " spectra");
			}
		}
	}

	private File findHostMzml(File hostOut) {
		File[] files = hostOut.listFiles();
		if(files == null) {
			return null;
		}
		for(File f : files) {
			if(f.getName().toLowerCase(Locale.ROOT).endsWith(".mzml")) {
				return f;
			}
		}
		return null;
	}

	private String listing(File dir) {
		File[] files = dir.listFiles();
		if(files == null) {
			return "  (no such directory)\n";
		}
		StringBuilder sb = new StringBuilder();
		for(File f : files) {
			sb.append("  ").append(f.getName()).append(" (").append(f.length()).append(" bytes)\n");
		}
		return sb.toString();
	}

	private List<File> collectInputFiles(File input) {
		List<File> result = new ArrayList<File>();
		String lower = input.getName().toLowerCase(Locale.ROOT);
		if(!lower.endsWith(".wiff")) {
			result.add(input);
			return result;
		}

		String base = this.baseName(input.getName());
		File parent = input.getParentFile();
		File[] siblings = parent == null ? null : parent.listFiles();
		if(siblings == null) {
			result.add(input);
			return result;
		}
		String prefix = (base + ".wiff").toLowerCase(Locale.ROOT);
		for(File f : siblings) {
			if(f.getName().toLowerCase(Locale.ROOT).startsWith(prefix)) {
				result.add(f);
			}
		}
		if(result.isEmpty()) {
			result.add(input);
		}
		return result;
	}

	private String baseName(String filename) {
		int dot = filename.lastIndexOf('.');
		return dot < 0 ? filename : filename.substring(0, dot);
	}
}
