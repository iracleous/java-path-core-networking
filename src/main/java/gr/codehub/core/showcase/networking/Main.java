package gr.codehub.core.showcase.networking;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class Main {
	private static final Logger logger = LoggerFactory.getLogger(Main.class);

	public static void main(String[] args) {
		httpGetRequestOld();
		httpGetRequestSynchronous();
		httpGetRequestAsynchronous();
		httpGetRequestMultipleRequests();
		httpPostRequest();
		basicNetworkInformation();
		availablePorts();
	}

	public static void basicNetworkInformation() {
		String address = "";
		InetAddress local = null;
		try {
			address = InetAddress.getLocalHost().toString();
			local = InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
			logger.error("Unknown host exception.", e);
		}
		logger.info(address);
		logger.info(local.getHostAddress());
		logger.info("Correct ip: {}", Util.getCorrectIp());
		logger.info(local.getCanonicalHostName());
		logger.info(local.getHostName());
	}

	public static void availablePorts() {
		int portRange = 65535;
		for (int port = 1; port <= portRange; port += 1) {
			try {
				DatagramSocket ds = new DatagramSocket(port);
				ds.close();
			} catch (IOException e) {
				logger.error("Port {} is not available.", port, e);
			}
		}
	}

	public static void httpGetRequestOld() {
		URL url;
		HttpURLConnection httpURLConnection;
		int status = 0;

		try {
			url = new URL("https://www.codehub.gr");
			httpURLConnection = (HttpURLConnection) url.openConnection();
			status = httpURLConnection.getResponseCode();
		} catch (IOException e) {
			logger.error("Unknown input output exception.", e);
		}

		if (HttpURLConnection.HTTP_OK == status) {
			logger.info("Everything is OK!");
		} else if (HttpURLConnection.HTTP_NOT_FOUND == status) {
			logger.info("Page not found.");
		}
	}

	public static void httpGetRequestSynchronous() {
		HttpClient httpClient = HttpClient.newHttpClient();
		HttpRequest request = HttpRequest.newBuilder().uri(URI.create("https://www.codehub.gr")).build();
		HttpResponse<String> response = null;

		try {
			response = httpClient.send(request, BodyHandlers.ofString());
		} catch (IOException e) {
			logger.error("Unknown host exception.", e);
		} catch (InterruptedException e) {
			logger.error("Unknown interrupt exception.", e);
		}

		if (HttpURLConnection.HTTP_OK == response.statusCode()) {
			logger.info("Everything is ok!!!");
		} else if (HttpURLConnection.HTTP_NOT_FOUND == response.statusCode()) {
			logger.info("Page not found");
		}

		logger.info("{}", response.body());
	}

	public static void httpGetRequestAsynchronous() {
		HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
		HttpRequest request = HttpRequest.newBuilder().uri(URI.create("https://httpbin.org/get")).GET().build();
		HttpResponse<String> result = null;

		CompletableFuture<HttpResponse<String>> futureResponse = httpClient.sendAsync(request, BodyHandlers.ofString());
		while (!futureResponse.isDone()) {
			logger.info("Asynchronously waiting....");
		}

		try {
			result = futureResponse.get(10, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			logger.error("Unknown interrupt exception.", e);
		} catch (ExecutionException e) {
			logger.error("Retrieval of aborted task exception.", e);
		} catch (TimeoutException e) {
			logger.error("Response timed out exception.", e);
		}

		logger.info("{}", result.statusCode());
		logger.info("{}", result.uri());
		logger.info("{}", result.request());
		logger.info("{}", result.headers());
		logger.info("{}", result.body());
	}

	public static void httpGetRequestMultipleRequests() {
		//@formatter:off
		ExecutorService executorService = Executors.newFixedThreadPool(3);
		HttpClient httpClient = HttpClient.newBuilder()
				.executor(executorService)
				.connectTimeout(Duration.ofSeconds(10))
				.build();
		List<URI> targets = null;

		try {
			targets = Arrays.asList(
					new URI("https://httpbin.org/get?parameter=value"),
					new URI("https://httpbin.org/get?ping=pong"),
					new URI("https://httpbin.org/get?username=John&password=1234")
			);
		} catch (URISyntaxException e) {
			logger.error("Not valid URI exception.", e);
		}

		List<CompletableFuture<String>> results = targets.stream()
				.map(url -> httpClient.sendAsync(
						HttpRequest.newBuilder(url)
								.GET()
								.build(),
						BodyHandlers.ofString())
						.thenApply(response -> response.body()))
				.collect(Collectors.toList());
		//@formatter:on

		for (CompletableFuture<String> future : results) {
			try {
				logger.info("{}", future.get());
			} catch (InterruptedException e) {
				logger.error("Unknown interrupt exception.", e);
			} catch (ExecutionException e) {
				logger.error("Retrieval of aborted task exception.", e);
			}
		}
		executorService.shutdown();
	}

	public static void httpPostRequest() {
		//@formatter:off
		HttpClient httpClient = HttpClient.newBuilder()
				.connectTimeout(Duration.ofSeconds(10))
				.build();
		String json = new JSONObject()
				.put("name","Linus")
				.put("surname","Torvalds")
				.put("lineOfCodesWritten","unlimited")
				.put("projects", new JSONObject().put("kernel", "Linux").put("versionControl","Git"))
				.toString();

		HttpRequest request = HttpRequest.newBuilder()
				.POST(BodyPublishers.ofString(json))
				.uri(URI.create("https://httpbin.org/post"))
				.header("Content-Type", "application/json")
				.build();
		HttpResponse<String> response = null;
		//@formatter:on
		try {
			response = httpClient.send(request, BodyHandlers.ofString());
		} catch (IOException e) {
			logger.error("Unknown input output exception.", e);
		} catch (InterruptedException e) {
			logger.error("Unknown interrupt exception.", e);
		}
		logger.info("{}", response.body());
	}
}
