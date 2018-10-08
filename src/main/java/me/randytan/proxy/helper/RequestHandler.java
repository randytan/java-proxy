package me.randytan.proxy.helper;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

public class RequestHandler implements Runnable {

    static final  Logger logger = LogManager.getLogger(RequestHandler.class);

    /**
     * Socket connected to client passed by Proxy server
     */
    Socket clientSocket;

    /**
     * Read data client sends to proxy
     */
    BufferedReader proxyToClientBr;

    /**
     * Send data from proxy to client
     */
    BufferedWriter proxyToClientBw;

    /**
     * Creates a RequestHandler object capable of servicing HTTP(S) GET requests
     * @param clientSocket socket connected to the client
     */
    public RequestHandler(Socket clientSocket){
        this.clientSocket = clientSocket;
        try{
            this.clientSocket.setSoTimeout(2000);
            proxyToClientBr = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            proxyToClientBw = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));

        }
        catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    /**
     * Creates a HttpClientBuilder to create the custom route planner and setup the proxy configuration.
     * */
    public HttpClientBuilder getClientBuilder() {
        HttpClientBuilder clientBuilder = HttpClientBuilder.create();
        CustomProxyRoutePlanner routePlanner = new CustomProxyRoutePlanner(null);
        clientBuilder.setRoutePlanner(routePlanner);
        return clientBuilder;
    }

    /**
     * Reads and examines the requestString and calls the appropriate method based
     * on the request type.
     */
    public void run() {

        // Get Request from client
        String requestString;
        try{
            requestString = proxyToClientBr.readLine();
        } catch (IOException e) {
            logger.error("Error reading request from client: " + e.getMessage());
            return;
        }

        // Parse out URL
        logger.info("Request Received: " + requestString);

        // Get the Request type
        String request = requestString.substring(0,requestString.indexOf(' '));

        // Remove request type and space
        String urlString = requestString.substring(requestString.indexOf(' ')+1);

        // Remove everything past next space
        urlString = urlString.substring(0, urlString.indexOf(' '));

        // Prepend http:// if necessary to create correct URL
        if(!urlString.substring(0,4).equals("http")){
            String http = "http://";
            urlString = http + urlString;
        }

        String url;
        HttpUriRequest target;
        if(request.equals("CONNECT")){
            logger.info("HTTPS Request for : " + urlString + "\n");
            // Extract the URL and port of remote
            url = urlString.substring(7);
            String[] pieces = url.split(":");
            url = pieces[0];
        } else {
            url = urlString;
        }

        target = new HttpGet(url);
        try (CloseableHttpClient httpclient = getClientBuilder().build()) {
            CloseableHttpResponse response = httpclient.execute(target);
            try {
                logger.debug(response.getStatusLine());
                proxyToClientBw.write(EntityUtils.toString(response.getEntity()));
                proxyToClientBw.flush();

            } finally {
                response.close();
                if(proxyToClientBr != null){
                    proxyToClientBr.close();
                }
            }
        } catch (IOException ioe){
            ioe.getMessage();
        }

    }


}




