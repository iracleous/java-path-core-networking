package gr.codehub.core.showcase.networking;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ThreadLocalRandom;

public class TCPServer {
	private static final Logger logger = LoggerFactory.getLogger(TCPServer.class);
	private boolean found;
	private boolean shutdown;
	private int randomNumber;
	private int range;
	private int port;
	private int numberOfTries;

	public TCPServer(int port) {
		this.port = port;
		found = true;
		randomNumber = 0;
		numberOfTries = 0;
		range = 100;
		shutdown = false;
	}

	public static void main(String[] args) {
		TCPServer tcpServer = new TCPServer(8080);
		tcpServer.startServer();
	}

	public void startServer() {
		//create server socket
		//port number must be between 0 and 65535
		//if port number is 0 it auto allocates a port
		try (ServerSocket serverSocket = new ServerSocket(port)) {
			while (!shutdown) {
				//wait for incoming connections
				logger.info("Server with ip {} waiting for new connections at port {}.", Util.getCorrectIp(),
						serverSocket.getLocalPort());
				manageClientConnection(serverSocket);
			}
		} catch (IOException e) {
			logger.error("Unknown input output exception.", e);
		}
	}

	public String guessNumberResponseMessage(String guess) {
		int number;
		String response;
		try {
			number = Integer.parseInt(guess);
		} catch (NumberFormatException e) {
			return "\"" + guess + "\" is not a number.";
		}
		numberOfTries++;
		if (number == randomNumber) {
			found = true;
			return "You won!Get some virtual fireworks.Let's play again!";
		} else if (Math.abs(number - randomNumber) < range / 12) response = "Burning.";
		else if (Math.abs(number - randomNumber) < range / 10) response = "Hotter.";
		else if (Math.abs(number - randomNumber) < range / 8) response = "Hot.";
		else if (Math.abs(number - randomNumber) < range / 6) response = "Warm.";
		else if (Math.abs(number - randomNumber) < range / 4) response = "Cold.";
		else response = "Freezing cold.";

		return response.concat("Number of tries: " + numberOfTries);
	}

	public void generateRandomNumber() {
		randomNumber = ThreadLocalRandom.current().nextInt(1, range);
		found = false;
		numberOfTries = 0;
		logger.info("Lucky number: {}", randomNumber);
	}

	public void manageClientConnection(ServerSocket serverSocket) {
		try (Socket clientConnectionSocket = serverSocket.accept(); BufferedReader clientInput = new BufferedReader(
				new InputStreamReader(clientConnectionSocket.getInputStream()));
			 PrintStream clientOutput = new PrintStream(clientConnectionSocket.getOutputStream());) {
			//accept incoming connections
			logger.info("Client connection accepted with ip: {}", clientConnectionSocket.getInetAddress());
			generateRandomNumber();
			//wait for client messages
			//until message is exit
			while (true) {
				if (found) generateRandomNumber();
				//read client message
				String clientMessage = clientInput.readLine();
				logger.info("Message Received: {}.", clientMessage);
				//client wants to stop the connection
				if ("exit".equals(clientMessage)) {
					clientOutput.println("Good bye!");
					logger.info("Closing connection with {}.", clientConnectionSocket.getInetAddress());
					break;
				} else if ("shutdown".equals(clientMessage)) {
					clientOutput.println("Server shutting down.Good bye!");
					logger.info("Server shutdown from {}.", clientConnectionSocket.getInetAddress());
					shutdown = true;
					break;
				}
				//get game logic message
				String serverResponseMessage = guessNumberResponseMessage(clientMessage);
				//send message back to client
				clientOutput.println(serverResponseMessage);
			}
		} catch (IOException e) {
			logger.error("Client connection exception.", e);
		}
	}
}
