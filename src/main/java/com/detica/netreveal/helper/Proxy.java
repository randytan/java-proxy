package com.detica.netreveal.helper;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
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
 * The Proxy server is also responsible for maintaining cached copies of the any websites that are requested by
 * clients and this includes the HTML markup, images, css and js files associated with each webpage.
 *
 * Upon closing the proxy server, the HashMaps which hold cached items and blocked sites are serialized and
 * written to a file and are loaded back in when the proxy is started once more, meaning that cached and blocked
 * sites are maintained.
 *
 */
public class Proxy implements Runnable{

    final static Logger logger = LogManager.getLogger(Proxy.class.getName());

    private ServerSocket serverSocket;
    private volatile boolean running = true;


    /**
     * Data structure for constant order lookup of cache items.
     * Key: URL of page/image requested.
     * Value: File in storage associated with this key.
     */
    static HashMap<String, File> cache;

    /**
     * Data structure for constant order lookup of blocked sites.
     * Key: URL of page/image requested.
     * Value: URL of page/image requested.
     */
    static HashMap<String, String> blockedSites;

    /**
     * ArrayList of threads that are currently running and servicing requests.
     * This list is required in order to join all threads on closing of server
     */
    static ArrayList<Thread> servicingThreads;


    /**
     * Create the Proxy Server
     * @param port Port number to run proxy server from.
     */
    public Proxy(int port) {

        // Unset proxy setting from system variables.
        Utilities.unsetProxy();

        // Load in hash map containing previously cached sites and blocked Sites
        cache = new HashMap<String, File>();
        blockedSites = new HashMap<String, String>();

        // Create array list to hold servicing threads
        servicingThreads = new ArrayList<Thread>();

        // Start dynamic manager on a separate thread.
        new Thread(this).start();    // Starts overriden run() method at bottom

        try {
            // Create the Server Socket for the Proxy
            serverSocket = new ServerSocket(port);
            logger.info("Waiting for client on port " + serverSocket.getLocalPort() + "..");
            running = true;
        } catch (SocketException se) {
            logger.error("Socket Exception when connecting to client");
            se.printStackTrace();
        } catch (SocketTimeoutException ste) {
            logger.error("Timeout occurred while connecting to client");
        } catch (IOException io) {
            logger.error("IO exception when connecting to client");
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
                logger.error("Server closed");
            } catch (IOException e) {
                e.printStackTrace();
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
     * 		blocked : Lists currently blocked sites
     *  	cached	: Lists currently cached sites
     *  	close	: Closes the proxy server
     *  	*		: Adds * to the list of blocked sites
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