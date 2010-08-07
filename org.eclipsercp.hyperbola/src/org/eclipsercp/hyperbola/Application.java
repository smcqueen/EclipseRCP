/*******************************************************************************
 * Copyright (c) 2005 Jean-Michel Lemieux, Jeff McAffer and others.
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
 *     Jean-Michel Lemieux and Jeff McAffer - initial API and implementation
 *******************************************************************************/
package org.eclipsercp.hyperbola;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipsercp.hyperbola.model.ConnectionDetails;
import org.eclipsercp.hyperbola.model.Session;
import org.jivesoftware.smack.XMPPException;

/**
 * This class controls all aspects of the application's execution
 */
public class Application implements IApplication {

	public static final String PLUGIN_ID = "org.eclipsercp.hyperbola";

	private boolean login(final Session session) {
		boolean firstTry = true;
		SecureLoginDialog loginDialog = new SecureLoginDialog(null);
		while (Session.getInstance().getConnection() == null) {
			IPreferencesService service = Platform.getPreferencesService();
			boolean auto_login = service.getBoolean(Application.PLUGIN_ID,
					GeneralPreferencePage.AUTO_LOGIN, true, null);
			ConnectionDetails details = loginDialog.getConnectionDetails();
			if (!auto_login || details == null || !firstTry) {
				if (loginDialog.open() != Window.OK)
					return false;
				details = loginDialog.getConnectionDetails();
			}
			firstTry = false;
			session.setConnectionDetails(details);
			connectWithProgress(session);
		}
		return true;
	}

	private void connectWithProgress(final Session session) {
		ProgressMonitorDialog progress = new ProgressMonitorDialog(null);
		progress.setCancelable(true);
		try {
			progress.run(true, true, new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor)
				throws InvocationTargetException {
					try {
						session.connectAndLogin(monitor);
					} catch (XMPPException e) {
						e.printStackTrace();
					}
				}
			});
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public Object start(IApplicationContext context) throws Exception {
		Display display = PlatformUI.createDisplay();
		try {
			final Session session = Session.getInstance();
			context.applicationRunning();
			if (!login(session))
				return IApplication.EXIT_OK;

			int returnCode = PlatformUI.createAndRunWorkbench(display,
					new ApplicationWorkbenchAdvisor());
			if (returnCode == PlatformUI.RETURN_RESTART) {
				return IApplication.EXIT_RESTART;
			}
			return IApplication.EXIT_OK;
		} finally {
			display.dispose();
		}
	}

	public void stop() {
		
	}
}
