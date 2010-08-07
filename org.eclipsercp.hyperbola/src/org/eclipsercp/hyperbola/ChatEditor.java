/*******************************************************************************
 * Copyright (c) 2004, 2005 Jean-Michel Lemieux, Jeff McAffer and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Hyperbola is an RCP application developed for the book 
 *     Eclipse Rich Client Platform - 
 *         Designing, Coding, and Packaging Java Applications 
 *
 * Contributors:
 *     Jean-Michel Lemieux and Jeff McAffer - initial implementation
 *******************************************************************************/
package org.eclipsercp.hyperbola;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;
import org.eclipsercp.hyperbola.model.Session;
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;

/**
 * An editor that presents a chat with a specified participant.
 */
public class ChatEditor extends EditorPart {

	public static String ID = "org.eclipsercp.hyperbola.editors.chat";

	private Text transcript;

	private Text entry;

	private Chat chat;

	private MessageListener messageListener;

	public ChatEditor() {
	}

	public void doSave(IProgressMonitor monitor) {
		// Save not supported
	}

	public void doSaveAs() {
		// Save As not supported
	}

	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		if (!(input instanceof ChatEditorInput))
			throw new PartInitException(
					"ChatEditor.init expects a ChatEditorInput.  Actual input: "
							+ input);
		setSite(site);
		setInput(input);
		setPartName(getParticipant());
	}

	public boolean isDirty() {
		return false;
	}

	public boolean isSaveAsAllowed() {
		// Save As not supported
		return false;
	}

	public void createPartControl(Composite parent) {
		GridLayout layout = new GridLayout();
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		parent.setLayout(layout);

		transcript = new Text(parent, SWT.BORDER | SWT.MULTI | SWT.WRAP);
		transcript.setLayoutData(new GridData(GridData.FILL, GridData.FILL,
				true, true));
		transcript.setEditable(false);
		transcript.setBackground(transcript.getDisplay().getSystemColor(
				SWT.COLOR_INFO_BACKGROUND));
		transcript.setForeground(transcript.getDisplay().getSystemColor(
				SWT.COLOR_INFO_FOREGROUND));

		entry = new Text(parent, SWT.BORDER | SWT.WRAP);
		GridData gridData = new GridData(GridData.FILL, GridData.FILL, true,
				false);
		gridData.heightHint = entry.getLineHeight() * 2;
		entry.setLayoutData(gridData);
		entry.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent event) {
				if (event.character == SWT.CR) {
					sendMessage();
					event.doit = false;
				}
			}
		});
		messageListener = new MessageListener() {
			
			public void processMessage(Chat chat, Message message) {
				process(message);
			}
		};
		
		getChat().addMessageListener(messageListener);
	}

	public void setFocus() {
		if (entry != null && !entry.isDisposed())
			entry.setFocus();
	}

	private Session getSession() {
		return Session.getInstance();
	}

	private String getParticipant() {
		return ((ChatEditorInput) getEditorInput()).getParticipant();
	}

	private Chat getChat() {
		if (chat == null)
			chat = getSession().getChat(getParticipant(), true);
		return chat;
	}

	private String getUser() {
		return getSession().getConnection().getUser();
	}

	private void process(final Message message) {
		if (transcript.isDisposed())
			return;
		transcript.getDisplay().asyncExec(new Runnable() {
			public void run() {
				if (transcript.isDisposed())
					return;
				transcript.append(renderMessage(message.getFrom(), message
						.getBody()));
				transcript.append("\n");
				scrollToEnd();
			}
		});
	}

	private String renderMessage(String from, String body) {
		if (from == null)
			return body;
		int j = from.indexOf('@');
		if (j > 0)
			from = from.substring(0, j);
		return "<" + from + ">  " + body;
	}

	private void scrollToEnd() {
		int n = transcript.getCharCount();
		transcript.setSelection(n, n);
		transcript.showSelection();
	}

	/**
	 * Sends the text in the entry field to the contact(s). The sent text will
	 * appear in the transcript area.
	 */
	public void sendMessage() {
		String body = entry.getText();
		if (body.length() == 0)
			return;
		try {
			chat.sendMessage(body);
		} catch (XMPPException e) {
			e.printStackTrace();
		}

		transcript.append(renderMessage(getUser(), body));
		transcript.append("\n");
		scrollToEnd();
		entry.setText("");
	}

	public void dispose() {
		if (chat != null) {
			if (messageListener != null) {
				getChat().removeMessageListener(messageListener);
				messageListener = null;
			}
			getSession().terminateChat(chat);
			chat = null;
		}
	}

	/**
	 * Processes the first message, which triggered the opening of this chat
	 * editor.
	 * 
	 * @param message
	 *            the message
	 */
	public void processFirstMessage(Message message) {
		process(message);
	}
}
