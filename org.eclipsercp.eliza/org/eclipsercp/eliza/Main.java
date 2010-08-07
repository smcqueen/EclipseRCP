/*******************************************************************************
 * Copyright (c) 2010 Jean-Michel Lemieux, Jeff McAffer and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Hyperbola is an RCP application developed for the book 
 *     Eclipse Rich Client Platform - 
 *         Designing, Coding, and Packaging Java Applications 
 * See http://eclipsercp.org
 * 
 * Contributors:
 *     Jean-Michel Lemieux and Jeff McAffer - initial implementation
 *******************************************************************************/
package org.eclipsercp.eliza;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;

/**
 * Simple xmpp chat echo bot. Accepts chat requests and returns the
 * source message as the chat response.
 */
public class Main {

	private static Logger logger = Logger.getLogger(Main.class.getName());
	private static String HOSTNAME = "localhost";
	
	public static void main(String[] args) {
		try {
			FileHandler fileHandler1 = new FileHandler("eliza.log");
			logger.addHandler(fileHandler1);
		} catch (IOException ioe) {
			logger.log(Level.SEVERE, "Unable to create file handler.", ioe);
		}

		XMPPConnection elizaConnection = null;

		while (true) {
			try {
				if (elizaConnection == null || !elizaConnection.isConnected()) {
					elizaConnection = new XMPPConnection(HOSTNAME);
					elizaConnection.login("eliza", "secret");
					logger.info("Eliza started");
					hookIncomingChatListener(elizaConnection);
				}
				Thread.sleep(10000);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private static void hookIncomingChatListener(final XMPPConnection connection) {
		if (connection != null) {
			PacketListener listener = new PacketListener() {
				public void processPacket(Packet packet) {
					final Message message = (Message) packet;
					if (message.getType() != Message.Type.CHAT)
						return;
					echo(connection, message);
				}
			};
			PacketFilter filter = new PacketTypeFilter(Message.class);
			connection.addPacketListener(listener, filter);
		}
	}

	private static void echo(XMPPConnection connection, Message in) {
		Message out = new Message(in.getFrom(), Message.Type.CHAT);
		out.setBody(in.getBody().toUpperCase());
		out.setThread(in.getThread());
		connection.sendPacket(out);
	}
}
