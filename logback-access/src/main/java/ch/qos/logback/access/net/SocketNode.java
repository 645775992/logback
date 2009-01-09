/**
 * Logback: the generic, reliable, fast and flexible logging framework.
 * 
 * Copyright (C) 1999-2006, QOS.ch
 * 
 * This library is free software, you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation.
 */

package ch.qos.logback.access.net;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;

import ch.qos.logback.access.spi.AccessContext;
import ch.qos.logback.access.spi.AccessEvent;
import ch.qos.logback.core.spi.FilterReply;

// Contributors: Moses Hohman <mmhohman@rainbow.uchicago.edu>

/**
 * Read {@link AccessEvent} objects sent from a remote client using Sockets
 * (TCP). These logging events are logged according to local policy, as if they
 * were generated locally.
 * 
 * <p>
 * For example, the socket node might decide to log events to a local file and
 * also resent them to a second socket node.
 * 
 * @author Ceki G&uuml;lc&uuml;
 * @author S&eacute;bastien Pennec
 * 
 * @since 0.8.4
 */
public class SocketNode implements Runnable {

  Socket socket;
  AccessContext context;
  ObjectInputStream ois;

  public SocketNode(Socket socket, AccessContext context) {
    this.socket = socket;
    this.context = context;
    try {
      ois = new ObjectInputStream(new BufferedInputStream(socket
          .getInputStream()));
    } catch (Exception e) {
      System.out.println("Could not open ObjectInputStream to " + socket + e);
    }
  }

  public void run() {
    AccessEvent event;

    try {
      while (true) {
        // read an event from the wire
        event = (AccessEvent) ois.readObject();
        //check that the event should be logged
        if (context.getFilterChainDecision(event) == FilterReply.DENY) {
          break;
        }
        //send it to the appenders
        context.callAppenders(event); 
      }
    } catch (java.io.EOFException e) {
      System.out.println("Caught java.io.EOFException closing connection.");
    } catch (java.net.SocketException e) {
      System.out.println("Caught java.net.SocketException closing connection.");
    } catch (IOException e) {
      System.out.println("Caught java.io.IOException: " + e);
      System.out.println("Closing connection.");
    } catch (Exception e) {
      System.out.println("Unexpected exception. Closing connection." + e);
    }

    try {
      ois.close();
    } catch (Exception e) {
      System.out.println("Could not close connection." + e);
    }
  }
}