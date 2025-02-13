package ninja.mspp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import ninja.mspp.core.annotation.method.Service;
import ninja.mspp.core.model.listener.ListenerMethod;
import ninja.mspp.core.view.ViewInfo;
import ninja.mspp.view.MainFrame;


public class App extends Application {
	private ServerSocket serverSocket;
	private final ExecutorService executorService = Executors.newCachedThreadPool();
	
	@Override
	public void start(Stage primaryStage) throws Exception {		
		MsppManager manager = MsppManager.getInstance();
		manager.setMainStage(primaryStage);
		
		ViewInfo<MainFrame> viewInfo = manager.createWindow(MainFrame.class, "MainFrame.fxml");
		Parent root = viewInfo.getWindow();
		MainFrame mainFrame = viewInfo.getController();
		manager.setMainFrame(mainFrame);
		
		Scene scene = new Scene(root);
		primaryStage.setScene(scene);
		
		String[] icons = {
			"MS_icon_16.png", "MS_icon_24.png", "MS_icon_32.png", "MS_icon_48.png", "MS_icon_50.png", "MS_icon_128.png",
			"MS_icon_150.png", "MS_icon_256.png"
		};
		
		for (String icon : icons) {
			Image iconImage = new Image(getClass().getResourceAsStream("/ninja/mspp/images/icon/" + icon));
			primaryStage.getIcons().add(iconImage);
		}
		
		ResourceBundle config = manager.getConfig();
		primaryStage.setTitle(config.getString("app.name"));
		
		int width = Integer.parseInt(config.getString("app.width"));
		int height = Integer.parseInt(config.getString("app.height"));
		primaryStage.setWidth(width);
		primaryStage.setHeight(height);
		
		primaryStage.setOnCloseRequest(
			event -> {
				manager.closeSession();
			}
		);

		primaryStage.show();
		
		int port = Integer.parseInt(manager.getConfig().getString("app.service.port"));
		this.startService(port);
	}


	private void startService(int port) {
        Thread serverThread = new Thread(
        	() -> {
        		try {
        			this.serverSocket = new ServerSocket(port);
        			System.out.println("Server started on port " + port);
                

        			while (!Thread.currentThread().isInterrupted()) {
        				Socket clientSocket = this.serverSocket.accept();
        				this.handleClient(clientSocket);
        			}
        		}
        		catch (IOException e) {
        			System.out.println("Server error: " + e.getMessage());
        		}
        	}
        );
        serverThread.setDaemon(true);
        serverThread.start();		
	}


	private void handleClient(Socket socket) {
		executorService.submit(
			() -> {
				try {
					System.out.println("Client connected: " + socket.getInetAddress());
	                
	                BufferedReader in = new BufferedReader(
	                    new InputStreamReader(socket.getInputStream())
	                );
	                
	                String line = in.readLine();
	                if (line != null) {
	                    System.out.println("Request: " + line);
	                    String[] requestParts = line.split(" ");
	                    
	                    if (requestParts.length >= 3) {
	                        String method = requestParts[0];
	                        
	                        Map<String, String> headers = new HashMap<>();
	                        String headerLine;
	                        int contentLength = 0;
	                        
	                        while ((headerLine = in.readLine()) != null && !headerLine.isEmpty()) {
	                            String[] headerParts = headerLine.split(": ", 2);
	                            if (headerParts.length == 2) {
	                                headers.put(headerParts[0], headerParts[1]);
	                                if (headerParts[0].equalsIgnoreCase("Content-Length")) {
	                                    contentLength = Integer.parseInt(headerParts[1]);
	                                }
	                            }
	                        }
	                        
	                        String jsonBody = "";
	                        String response = "";
	                        if (method.equals("POST") && contentLength > 0) {
	                            char[] bodyChars = new char[contentLength];
	                            in.read(bodyChars, 0, contentLength);
	                            jsonBody = new String(bodyChars);
	                            System.out.println("Received JSON: " + jsonBody);
	                            
	                            ObjectMapper mapper = new ObjectMapper();
	                            TypeReference<Map<String, String>> typeRef 
	                                = new TypeReference<Map<String, String>>() {};        
	                            Map<String, String> map = mapper.readValue(jsonBody, typeRef);
	                            
	                            response = this.processJsonRequest(map);
	                        }
	                        
	                        OutputStream out = socket.getOutputStream();
	                        
	                        String httpResponse = "HTTP/1.1 200 OK\r\n" +
	                                              "Content-Type: application/json\r\n" +
	                                              "Content-Length: " + response.length() + "\r\n" +
	                                              "Connection: close\r\n" +
	                                              "\r\n" +
	                                              response;

	                        out.write(httpResponse.getBytes("UTF-8"));
	                        out.flush();
	                    }
	                }
	                socket.close();
	            }
				catch (IOException e) {
	                System.out.println("Client error: " + e.getMessage());
	            }
	        }
		);
	}

	private String processJsonRequest(Map<String, String> map) throws JsonProcessingException {
		String method = map.get("method");
		MsppManager manager = MsppManager.getInstance();
		List<ListenerMethod<Service>> list = manager.getMethods(Service.class);

		Object object = null;
		for(ListenerMethod<Service> element : list) {
			if(element.getAnnotation().value().equals(method)) {
				object = element.invoke(map);
			}
		}
		
		String response = "";
		if(object != null) {
			ObjectMapper mapper = new ObjectMapper();
			response = mapper.writeValueAsString(object);
		}
		
		return response;
	}

	
	public static void main(String[] args) {
		launch(args);
	}
}
