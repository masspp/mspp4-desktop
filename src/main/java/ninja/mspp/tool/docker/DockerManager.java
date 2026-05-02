package ninja.mspp.tool.docker;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse.Mount;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.github.dockerjava.core.command.PullImageResultCallback;
import com.github.dockerjava.transport.DockerHttpClient;
import com.github.dockerjava.zerodep.ZerodepDockerHttpClient;

import ninja.mspp.MsppManager;
import ninja.mspp.core.util.FileUtil;

public class DockerManager {
	private static DockerManager instance;

	private File folder;
	private DockerClient client;
	private final Set<String> startedContainers = new LinkedHashSet<String>();

	private DockerManager() {
		this.folder = null;
		this.client = null;
	}

	public static DockerManager getInstance() {
		if(instance == null) {
			instance = new DockerManager();
			Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
				@Override
				public void run() {
					instance.shutdownContainers();
				}
			}, "docker-shutdown"));
		}
		return instance;
	}

	/**
	 * Stops every container this manager started in the current session.
	 * Errors are logged but never thrown — this runs in a shutdown hook.
	 */
	public synchronized void shutdownContainers() {
		if(this.client == null || this.startedContainers.isEmpty()) {
			return;
		}
		for(String name : this.startedContainers) {
			try {
				this.client.stopContainerCmd(name).withTimeout(Integer.valueOf(5)).exec();
			}
			catch(Exception e) {
				System.err.println("Failed to stop container " + name + ": " + e.getMessage());
			}
		}
		this.startedContainers.clear();
		try {
			this.client.close();
		}
		catch(Exception e) {
			// ignore
		}
	}

	public File getDockerFolder() {
		if(this.folder == null) {
			MsppManager manager = MsppManager.getInstance();
			File parent = manager.getConfigFolder();
			File dockerFolder = new File(parent, "docker");
			if(dockerFolder.exists()) {
				FileUtil.delete(dockerFolder);
			}
			dockerFolder.mkdirs();
			this.folder = dockerFolder;
		}
		return this.folder;
	}

	/**
	 * @return true if the Docker daemon responds to a ping.
	 */
	public boolean isDockerAvailable() {
		try {
			this.getClient().pingCmd().exec();
			return true;
		}
		catch(Exception e) {
			System.err.println("Docker ping failed: " + e.getClass().getName() + ": " + e.getMessage());
			return false;
		}
	}

	private synchronized DockerClient getClient() {
		if(this.client == null) {
			DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
			DockerHttpClient http = new ZerodepDockerHttpClient.Builder()
					.dockerHost(config.getDockerHost())
					.sslConfig(config.getSSLConfig())
					.build();
			this.client = DockerClientImpl.getInstance(config, http);
		}
		return this.client;
	}

	/**
	 * Ensures a running container with the given name exists for the image.
	 * If the container does not exist, the image is pulled (if necessary) and
	 * a new long-running container is created and started so commands can be
	 * exec'd into it.
	 *
	 * @param image image reference (e.g. "ubuntu:22.04")
	 * @param containerName container name to manage
	 * @return container id
	 */
	public String ensureContainer(String image, String containerName) throws InterruptedException {
		DockerClient docker = this.getClient();

		String existingId = this.findContainerId(docker, containerName);
		if(existingId != null) {
			InspectContainerResponse info = docker.inspectContainerCmd(existingId).exec();
			Boolean running = info.getState().getRunning();
			if(running == null || !running.booleanValue()) {
				docker.startContainerCmd(existingId).exec();
			}
			this.startedContainers.add(containerName);
			return existingId;
		}

		this.pullImageIfNeeded(docker, image);

		CreateContainerResponse created = docker.createContainerCmd(image)
				.withName(containerName)
				.withTty(Boolean.TRUE)
				.withCmd("tail", "-f", "/dev/null")
				.exec();
		docker.startContainerCmd(created.getId()).exec();
		this.startedContainers.add(containerName);
		return created.getId();
	}

	/**
	 * Like {@link #ensureContainer(String, String)} but binds a host directory
	 * into the container so files placed on the host are visible inside the
	 * container without docker cp roundtrips.
	 *
	 * If a container with the given name already exists but lacks the matching
	 * mount, it is removed and recreated.
	 */
	public String ensureContainerWithBind(
			String image, String containerName, File hostDir, String containerPath
	) throws InterruptedException {
		DockerClient docker = this.getClient();
		hostDir.mkdirs();

		String existingId = this.findContainerId(docker, containerName);
		if(existingId != null) {
			InspectContainerResponse info = docker.inspectContainerCmd(existingId).exec();
			if(this.hasMatchingMount(info, hostDir, containerPath)) {
				Boolean running = info.getState().getRunning();
				if(running == null || !running.booleanValue()) {
					docker.startContainerCmd(existingId).exec();
				}
				this.startedContainers.add(containerName);
				return existingId;
			}
			try {
				docker.stopContainerCmd(existingId).exec();
			}
			catch(Exception e) {
				// container may already be stopped
			}
			docker.removeContainerCmd(existingId).withForce(Boolean.TRUE).exec();
		}

		this.pullImageIfNeeded(docker, image);

		HostConfig hostConfig = HostConfig.newHostConfig()
				.withBinds(new Bind(hostDir.getAbsolutePath(), new Volume(containerPath)));

		CreateContainerResponse created = docker.createContainerCmd(image)
				.withName(containerName)
				.withTty(Boolean.TRUE)
				.withHostConfig(hostConfig)
				.withCmd("tail", "-f", "/dev/null")
				.exec();
		docker.startContainerCmd(created.getId()).exec();
		this.startedContainers.add(containerName);
		return created.getId();
	}

	private boolean hasMatchingMount(InspectContainerResponse info, File hostDir, String containerPath) {
		List<Mount> mounts = info.getMounts();
		if(mounts == null) {
			return false;
		}
		String hostNorm = hostDir.getAbsolutePath();
		for(Mount m : mounts) {
			Volume dest = m.getDestination();
			String destPath = dest == null ? null : dest.getPath();
			String src = m.getSource();
			if(containerPath.equals(destPath) && src != null && src.equalsIgnoreCase(hostNorm)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Result of an exec call: combined output and exit code.
	 */
	public static class ExecResult {
		public final String output;
		public final long exitCode;
		public ExecResult(String output, long exitCode) {
			this.output = output;
			this.exitCode = exitCode;
		}
	}

	/**
	 * Executes a command inside the container. Throws IOException if exit code is non-zero.
	 */
	public String execCommand(String containerName, String... command) throws InterruptedException, IOException {
		ExecResult result = this.execCommandRaw(containerName, command);
		if(result.exitCode != 0L) {
			throw new IOException(
				"Command failed (exit " + result.exitCode + "): " + String.join(" ", command)
				+ "\n" + result.output
			);
		}
		return result.output;
	}

	/**
	 * Executes a command inside the container and returns the result regardless of exit code.
	 */
	public ExecResult execCommandRaw(String containerName, String... command) throws InterruptedException, IOException {
		return this.execCommandStreaming(containerName, null, command);
	}

	/**
	 * Like {@link #execCommandRaw(String, String...)} but delivers each output line
	 * (split on '\n' or '\r' from either stdout or stderr) to the given listener
	 * as it arrives. Pass {@code null} for {@code lineListener} to disable streaming.
	 */
	public ExecResult execCommandStreaming(
			String containerName, Consumer<String> lineListener, String... command
	) throws InterruptedException, IOException {
		DockerClient docker = this.getClient();
		String containerId = this.findContainerId(docker, containerName);
		if(containerId == null) {
			throw new IllegalStateException("Container not found: " + containerName);
		}

		ExecCreateCmdResponse exec = docker.execCreateCmd(containerId)
				.withAttachStdout(Boolean.TRUE)
				.withAttachStderr(Boolean.TRUE)
				.withCmd(command)
				.exec();

		try(ByteArrayOutputStream stdout = new ByteArrayOutputStream();
			ByteArrayOutputStream stderr = new ByteArrayOutputStream()) {
			final StringBuilder buf = new StringBuilder();
			docker.execStartCmd(exec.getId())
					.exec(new ExecStartResultCallback(stdout, stderr) {
						@Override
						public void onNext(Frame frame) {
							super.onNext(frame);
							if(lineListener != null) {
								feedLines(frame.getPayload(), buf, lineListener);
							}
						}
					})
					.awaitCompletion();
			if(lineListener != null && buf.length() > 0) {
				lineListener.accept(buf.toString());
				buf.setLength(0);
			}
			String output = stdout.toString() + stderr.toString();
			Long exitCode = docker.inspectExecCmd(exec.getId()).exec().getExitCodeLong();
			long code = exitCode == null ? 0L : exitCode.longValue();
			return new ExecResult(output, code);
		}
	}

	private static void feedLines(byte[] data, StringBuilder buf, Consumer<String> sink) {
		if(data == null) {
			return;
		}
		for(int i = 0; i < data.length; i++) {
			byte b = data[i];
			if(b == (byte) '\n' || b == (byte) '\r') {
				if(buf.length() > 0) {
					sink.accept(buf.toString());
					buf.setLength(0);
				}
			}
			else {
				buf.append((char) (b & 0xFF));
			}
		}
	}

	/**
	 * Copies a host file or directory into the container at the given directory path.
	 *
	 * @param containerName container name
	 * @param hostPath host file or directory to send
	 * @param containerDir destination directory inside the container (must already exist)
	 */
	public void copyToContainer(String containerName, File hostPath, String containerDir) {
		DockerClient docker = this.getClient();
		String containerId = this.requireContainerId(docker, containerName);
		docker.copyArchiveToContainerCmd(containerId)
				.withHostResource(hostPath.getAbsolutePath())
				.withRemotePath(containerDir)
				.exec();
	}

	/**
	 * Copies a file or directory out of the container to the given host path.
	 * If the container resource is a single file, hostPath becomes that file.
	 * If it is a directory, the tree is reconstructed under hostPath.
	 *
	 * @param containerName container name
	 * @param containerPath path inside the container (file or directory)
	 * @param hostPath destination on host
	 */
	public void copyFromContainer(String containerName, String containerPath, File hostPath) throws IOException {
		DockerClient docker = this.getClient();
		String containerId = this.requireContainerId(docker, containerName);

		try(InputStream tar = docker.copyArchiveFromContainerCmd(containerId, containerPath).exec();
			TarArchiveInputStream tin = new TarArchiveInputStream(tar)) {
			TarArchiveEntry entry = tin.getNextTarEntry();
			if(entry == null) {
				throw new IOException("Empty archive from container: " + containerPath);
			}

			boolean singleFile = !entry.isDirectory() && tin.getNextTarEntry() == null;
			if(singleFile) {
				File parent = hostPath.getParentFile();
				if(parent != null) {
					parent.mkdirs();
				}
				try(OutputStream out = new FileOutputStream(hostPath)) {
					this.copyEntry(tin, out, entry);
				}
				return;
			}

			// Multi-entry: re-open and extract under hostPath as a tree.
			hostPath.mkdirs();
			try(InputStream tar2 = docker.copyArchiveFromContainerCmd(containerId, containerPath).exec();
				TarArchiveInputStream tin2 = new TarArchiveInputStream(tar2)) {
				TarArchiveEntry e;
				while((e = tin2.getNextTarEntry()) != null) {
					File target = new File(hostPath, e.getName());
					if(e.isDirectory()) {
						target.mkdirs();
					}
					else {
						File p = target.getParentFile();
						if(p != null) {
							p.mkdirs();
						}
						try(OutputStream out = new FileOutputStream(target)) {
							byte[] buf = new byte[8192];
							int n;
							while((n = tin2.read(buf)) > 0) {
								out.write(buf, 0, n);
							}
						}
					}
				}
			}
		}
	}

	private void copyEntry(TarArchiveInputStream tin, OutputStream out, TarArchiveEntry entry) throws IOException {
		long remaining = entry.getSize();
		byte[] buf = new byte[8192];
		while(remaining > 0) {
			int toRead = (int) Math.min(buf.length, remaining);
			int n = tin.read(buf, 0, toRead);
			if(n < 0) {
				break;
			}
			out.write(buf, 0, n);
			remaining -= n;
		}
	}

	private String requireContainerId(DockerClient docker, String name) {
		String id = this.findContainerId(docker, name);
		if(id == null) {
			throw new IllegalStateException("Container not found: " + name);
		}
		return id;
	}

	private String findContainerId(DockerClient docker, String name) {
		List<Container> containers = docker.listContainersCmd()
				.withShowAll(Boolean.TRUE)
				.withNameFilter(java.util.Collections.singletonList(name))
				.exec();
		String slash = "/" + name;
		for(Container c : containers) {
			String[] names = c.getNames();
			if(names == null) {
				continue;
			}
			for(String n : names) {
				if(name.equals(n) || slash.equals(n)) {
					return c.getId();
				}
			}
		}
		return null;
	}

	private void pullImageIfNeeded(DockerClient docker, String image) throws InterruptedException {
		try {
			docker.inspectImageCmd(image).exec();
			return;
		}
		catch(NotFoundException e) {
			// fall through to pull
		}
		docker.pullImageCmd(image)
				.exec(new PullImageResultCallback())
				.awaitCompletion();
	}
}
