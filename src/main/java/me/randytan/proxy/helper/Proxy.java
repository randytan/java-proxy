package me.randytan.proxy.helper;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Scanner;

/**
 * The Proxy creates a Server Socket which will wait for connections on the specified port.
 * Once a connection arrives and a socket is accepted, the Proxy creates a RequestHandler object
 * on a new thread and passes the socket to it to be handled.
 * This allows the Proxy to continue accept further connections while others are being handled.
 *
 * The Proxy class is also responsible for providing the dynamic management of the proxy through the console
 * and is run on a separate thread in order to not interrupt the acceptance of socket connections.
 * This allows the administrator to dynamically block web sites in real time.

 *
 */
public class Proxy implements Runnable{

    static final Logger logger = LogManager.getLogger(Proxy.class.getName());

    private ServerSocket serverSocket;
    private volatile boolean running = true;

    /**
     * ArrayList of threads that are currently running and servicing requests.
     * This list is required in order to join all threads on closing of server
     */
     ArrayList<Thread> servicingThreads;


    /**
     * Create the Proxy Server
     * @param port Port number to run proxy server from.
     */
    public Proxy(int port) {

        // Create array list to hold servicing threads
        servicingThreads = new ArrayList<>();

        // Start dynamic manager on a separate thread.
        new Thread(this).start();    // Starts overridden run() method at bottom

        try {
            // Create the Server Socket for the Proxy
            serverSocket = new ServerSocket(port);
            logger.info("Waiting for client on port " + serverSocket.getLocalPort() + "..");
            running = true;
        } catch (SocketException se) {
            logger.error("Socket Exception when connecting to client: " + se.getMessage());
        } catch (SocketTimeoutException ste) {
            logger.error("Timeout occurred while connecting to client. Full error message: " + ste.getMessage());
        } catch (IOException io) {
            logger.error("IO exception when connecting to client - " + io.getLocalizedMessage());
        }
    }


    /**
     * Listens to port and accepts new socket connections.
     * Creates a new thread to handle the request and passes it the socket connection and continues listening.
     */
    public void listen(){

        while(running){
            try {
                // serverSocket.accept() Blocks until a connection is made
                Socket socket = serverSocket.accept();

                // Create new Thread and pass it Runnable RequestHandler
                Thread thread = new Thread(new RequestHandler(socket));
                servicingThreads.add(thread);

                thread.start();
            } catch (SocketException e) {
                // Socket exception is triggered by management system to shut down the proxy
                logger.warn("Server closed");
            } catch (IOException e) {
                logger.error(e.getMessage());
            }
        }
    }


    /**
     * Saves the blocked and cached sites to a file so they can be re loaded at a later time.
     * Also joins all of the RequestHandler threads currently servicing requests.
     */
    private void closeServer(){
        logger.info("\nClosing Server..");
        running = false;

        try{
            // Close all servicing threads
            for(Thread thread : servicingThreads){
                if(thread.isAlive()){
                    logger.info("Waiting on "+  thread.getId()+" to close..");
                    thread.join();
                    logger.info(" closed");
                }
            }
        } catch (InterruptedException e) {
            logger.error(e.getMessage());
            Thread.currentThread().interrupt();
        }

        // Close Server Socket
        try{
            logger.info("Terminating Connection");
            serverSocket.close();
        } catch (Exception e) {
            logger.error("Exception closing proxy's server socket: " + e.getMessage());
        }

    }

    /**
     * Creates a management interface which can dynamically update the proxy configurations
     *  	close	: Closes the proxy server
     * To force close/exit the application press Ctrl^C.
     */
    public void run() {
        Scanner scanner = new Scanner(System.in);

        String command;
        while(running){
            logger.info("Enter \"close\" to close server.");
            command = scanner.nextLine();

            if(command.equals("close")){
                running = false;
                closeServer();
            }
        }
        scanner.close();
    }

}