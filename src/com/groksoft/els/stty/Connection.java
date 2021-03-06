package com.groksoft.els.stty;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.*;

/**
 * Handle individual client connections.
 * 
 * The Connection class is a subclass of Thread. It handles individual
 * connections between a Service and a client user. Each connection has a
 * separate thread. Each Service can have multiple connection requests pending
 * at once.
 */
public class Connection extends Thread
{
	protected static Logger logger = LogManager.getLogger("applog");//

	/** The service for the connection */
	protected DaemonBase service;
	/** The socket for the connection */
	protected Socket socket;

	/**
	 * Constructor.
	 * 
	 * Connection objects are created by Listener threads as part of the
	 * server's thread group. The superclass constructor is called to create a
	 * new thread to handle the connection request.
	 * 
	 * @param aSocket Socket for connection
	 * @param aService Service for connection
	 */
	public Connection (Socket aSocket, DaemonBase aService)
	{
		super("Daemon.Connection:" + aSocket.getInetAddress().getHostAddress() + ":" + aSocket.getPort());
		this.socket = aSocket;
		this.service = aService;
	} // constructor
	
	/**
	 * Get the associated Daemon instance
	 */
	public DaemonBase getConsole ()
	{
		return service;
	}
	
	/**
	 * Get the associated Socket instance
	 */
	public Socket getSocket ()
	{
		return socket;
	}

	/**
	 * Run the service for this connection.
	 * 
	 * Creates input and output streams for the connection and calls the
	 * interface method for the Service. Calls endConnection() when
	 * the method returns for any reason.
	 * 
	 */
	public void run ()
	{
		try
		{
			service.process(socket);
		}
		catch (IOException e)
		{
			logger.info(e);
		}
		finally
		{
			// notify the ConnectionManager that this connection has closed
			ServeStty cm = ServeStty.getInstance();
			if (cm != null)
			{
				cm.endConnection();
			}
		}
	}
} // Connection

