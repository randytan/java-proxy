package me.randytan.proxy.helper;

import com.google.common.collect.ObjectArrays;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URL;

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
     * Thread that is used to transmit data read from client to server when using HTTPS
     * Reference to this is required so it can be closed once completed.
     */
    private Thread httpsClientToServer;

    /**
    *  Configuration for domain and IP address name pattern that determined as local
    * */
    private static String localDomainPattern = "*.local.randytan.me;*.dev;*.network";
    private static String localIPAddrPattern = "10.*;172.*";

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
     * Reads and examines the requestString and calls the appropriate method based
     * on the request type.
     */
    public void run() {

        Utilities.unsetProxy();

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

        String[] localDomainArray = localDomainPattern.split(";");
        String[] localDomainIPAddr = localIPAddrPattern.split(";");

        String[] localAddresses = ObjectArrays.concat(localDomainArray, localDomainIPAddr, String.class);

        for (String localAddr : localAddresses) {
            // check if the addresses is a local domain and IP address.
            if(urlString.contains(localAddr)){
                // Check request type and send using no proxy configuration
                if(request.equals("CONNECT")){
                    logger.info("HTTPS Request for : " + urlString + "\n");
                    handleHTTPSRequest(urlString, true);
                } else {
                    handleHTTPRequest(urlString, true);
                }

            } else {
                // Check request type and send using proxy configuration
                if(request.equals("CONNECT")){
                    logger.info("HTTPS Request for : " + urlString + "\n");
                    handleHTTPSRequest(urlString, false);
                } else {
                    logger.info("HTTP GET for : " + urlString + "\n");
                    handleHTTPRequest(urlString, false);
                }
            }
        }

    }


    /**
     * Sends the contents of the file specified by the urlString to the client
     * @param urlString URL of the file requested
     */
    private void handleHTTPRequest(String urlString, boolean localDomain){
        try {
            URL remoteURL = new URL(urlString);
            if(localDomain){
                Utilities.unsetProxy();
            } else {
                Utilities.setProxy();
            }

            try {
                // Create a connection to remote server
                HttpURLConnection proxyToServerCon = (HttpURLConnection)remoteURL.openConnection();
                proxyToServerCon.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                proxyToServerCon.setRequestProperty("Content-Language", "en-US");
                proxyToServerCon.setUseCaches(false);
                proxyToServerCon.setDoOutput(true);

                // Create Buffered Reader from proxy remote Server
                BufferedReader proxyToServerBR = new BufferedReader(new InputStreamReader(proxyToServerCon.getInputStream()));

                // Send success code to client
                String line = "HTTP/1.0 200 OK\n" +
                        "Proxy-agent: ProxyServer/1.0\n" +
                        "\r\n";
                proxyToClientBw.write(line);

                // Read from input stream between proxy and remote server
                while((line = proxyToServerBR.readLine()) != null){
                    // Send on data to client
                    proxyToClientBw.write(line);
                }

                // Ensure all data is sent by this point
                proxyToClientBw.flush();

                // Close Down Resources
                if(proxyToServerBR != null){
                    proxyToServerBR.close();
                }

                if(proxyToClientBw != null){
                    proxyToClientBw.close();
                }

            } catch (IOException ioe){
                if (ioe.getMessage() == "Stream closed"){
                    logger.info(ioe.getMessage());
                } else {
                    logger.error(ioe.getMessage());
                }

            }

        } catch (MalformedURLException mfue){
            logger.error(mfue.getMessage());
        }

    }


    /**
     * Handles HTTPS requests between client and remote server
     * @param urlString desired file to be transmitted over https
     */
    private void handleHTTPSRequest(String urlString, boolean localDomain){

        // Extract the URL and port of remote
        String url = urlString.substring(7);
        String[] pieces = url.split(":");
        url = pieces[0];
        int port  = Integer.parseInt(pieces[1]);

        if(localDomain){
            Utilities.unsetProxy();
        } else {
            Utilities.setProxy();
        }

        try{
            // Only first line of HTTPS request has been read at this point (CONNECT *)
            // Read (and throw away) the rest of the initial data on the stream
            for(int i=0;i<5;i++){
                proxyToClientBr.readLine();
            }

            int fileExtensionIndex = urlString.lastIndexOf(".");
            String fileExtension = urlString.substring(fileExtensionIndex, urlString.length());

            // Get actual IP associated with this URL through DNS
            InetAddress address = InetAddress.getByName(url);

            // Open a socket to the remote server
            Socket proxyToServerSocket = new Socket(address, port);
            proxyToServerSocket.setSoTimeout(5000);

            BufferedImage image = null;
            if((fileExtension.contains(".png")) || fileExtension.contains(".jpg") || fileExtension.contains(".jpeg") || fileExtension.contains(".gif")) {
                URL remoteURL = new URL(url);
                image = ImageIO.read(remoteURL);
            }

            // Send Connection established to the client
            String line = "HTTP/1.0 200 Connection established\r\n" +
                    "Proxy-Agent: ProxyServer/1.0\r\n" +
                    "\r\n";
            proxyToClientBw.write(line);
            proxyToClientBw.flush();

            if (image != null){
                ImageIO.write(image, fileExtension.substring(1), clientSocket.getOutputStream());
            }

            // Client and Remote will both start sending data to proxy at this point
            // Proxy needs to asynchronously read data from each party and send it to the other party

            // Create a Buffered Writer between proxy and remote
            BufferedWriter proxyToServerBW = new BufferedWriter(new OutputStreamWriter(proxyToServerSocket.getOutputStream()));

            // Create Buffered Reader from proxy and remote
            BufferedReader proxyToServerBR = new BufferedReader(new InputStreamReader(proxyToServerSocket.getInputStream()));

            // Create a new thread to listen to client and transmit to server
            ClientToServerHttpsTransmit clientToServerHttps = new ClientToServerHttpsTransmit(clientSocket.getInputStream(), proxyToServerSocket.getOutputStream());

            httpsClientToServer = new Thread(clientToServerHttps);
            httpsClientToServer.start();

            // Listen to remote server and relay to client
            try {
                byte[] buffer = new byte[4096];
                int read;
                do {
                    read = proxyToServerSocket.getInputStream().read(buffer);
                    if (read > 0) {
                        clientSocket.getOutputStream().write(buffer, 0, read);
                        if (proxyToServerSocket.getInputStream().available() < 1) {
                            clientSocket.getOutputStream().flush();
                        }
                    }
                } while (read >= 0);
            }
            catch (SocketTimeoutException e) {

            }
            catch (IOException e) {
                logger.error(e.getMessage());
            }

            // Close Down Resources
            if(proxyToServerSocket != null){
                proxyToServerSocket.close();
            }

            if(proxyToServerBR != null){
                proxyToServerBR.close();
            }

            if(proxyToServerBW != null){
                proxyToServerBW.close();
            }

            if(proxyToClientBw != null){
                proxyToClientBw.close();
            }


        } catch (SocketTimeoutException e) {
            String line = "HTTP/1.0 504 Timeout Occured after 10s\n" +
                    "User-Agent: ProxyServer/1.0\n" +
                    "\r\n";
            try{
                proxyToClientBw.write(line);
                proxyToClientBw.flush();
            } catch (IOException ioe) {
                logger.error(ioe.getMessage());
            }
        }
        catch (Exception e){
            logger.error("Error on HTTPS Connection : " + urlString );
        }
    }

    /**
     * Listen to data from client and transmits it to server.
     * This is done on a separate thread as must be done
     * asynchronously to reading data from server and transmitting
     * that data to the client.
     */
    class ClientToServerHttpsTransmit implements Runnable{

        InputStream proxyToClientIS;
        OutputStream proxyToServerOS;

        /**
         * Creates Object to Listen to Client and Transmit that data to the server
         * @param proxyToClientIS Stream that proxy uses to receive data from client
         * @param proxyToServerOS Stream that proxy uses to transmit data to remote server
         */
        public ClientToServerHttpsTransmit(InputStream proxyToClientIS, OutputStream proxyToServerOS) {
            this.proxyToClientIS = proxyToClientIS;
            this.proxyToServerOS = proxyToServerOS;
        }

        public void run(){
            try {
                // Read byte by byte from client and send directly to server
                byte[] buffer = new byte[4096];
                int read;
                do {
                    read = proxyToClientIS.read(buffer);
                    if (read > 0) {
                        proxyToServerOS.write(buffer, 0, read);
                        if (proxyToClientIS.available() < 1) {
                            proxyToServerOS.flush();
                        }
                    }
                } while (read >= 0);
            }
            catch (SocketTimeoutException ste) {
                // TODO: handle exception
            }
            catch (IOException e) {
                logger.error("IO Error - Proxy to client HTTPS: " + e.getMessage());
            }
        }
    }


}




