package ninja.mspp.view.panel;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import javafx.concurrent.Task;

/**
 * Reads data such as data points and peaks outside the JavaFX application thread.
 * The jobs run one at a time on a single thread, so that objects which cache their
 * results (for example XIC chromatograms) are not read by two threads at once.
 */
public class BackgroundLoader {
	private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(
		runnable -> {
			Thread thread = new Thread(runnable, "data-loader");
			thread.setDaemon(true);
			return thread;
		}
	);

	/**
	 * Runs the job in the background. The callbacks are called on the JavaFX application thread.
	 */
	public static <T> void load(Callable<T> job, Consumer<T> onLoaded, Consumer<Throwable> onFailed) {
		Task<T> task = new Task<T>() {
			@Override
			protected T call() throws Exception {
				return job.call();
			}
		};
		task.setOnSucceeded(event -> onLoaded.accept(task.getValue()));
		task.setOnFailed(event -> onFailed.accept(task.getException()));
		EXECUTOR.execute(task);
	}
}
