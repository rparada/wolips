/*
 * ====================================================================
 * 
 * The ObjectStyle Group Software License, Version 1.0
 * 
 * Copyright (c) 2002 - 2004 The ObjectStyle Group and individual authors of the
 * software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any, must
 * include the following acknowlegement: "This product includes software
 * developed by the ObjectStyle Group (http://objectstyle.org/)." Alternately,
 * this acknowlegement may appear in the software itself, if and wherever such
 * third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse or
 * promote products derived from this software without prior written permission.
 * For written permission, please contact andrus@objectstyle.org.
 * 
 * 5. Products derived from this software may not be called "ObjectStyle" nor
 * may "ObjectStyle" appear in their names without prior written permission of
 * the ObjectStyle Group.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * OBJECTSTYLE GROUP OR ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many individuals on
 * behalf of the ObjectStyle Group. For more information on the ObjectStyle
 * Group, please see <http://objectstyle.org/>.
 *  
 */
package org.objectstyle.wolips.projectbuild.builder;
import java.util.Map;

import org.eclipse.ant.core.AntRunner;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Display;
import org.objectstyle.wolips.ant.launching.LaunchAntInExternalVM;
import org.objectstyle.wolips.datasets.adaptable.Project;
import org.objectstyle.wolips.preferences.Preferences;
import org.objectstyle.wolips.projectbuild.ProjectBuildPlugin;
import org.objectstyle.wolips.workbenchutilities.WorkbenchUtilitiesPlugin;
/**
 * @author uli
 */
public class WOAntBuilder extends AbstractIncrementalProjectBuilder {
	private static final int TOTAL_WORK_UNITS = 1;
	/**
	 * Constructor for WOBuilder.
	 */
	public WOAntBuilder() {
		super();
	}
	/**
	 * Runs the build with the ant runner.
	 * 
	 * @see org.eclipse.core.internal.events.InternalBuilder#build(int, java.
	 *      util. Map, org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected IProject[] build(int kind, Map args, IProgressMonitor monitor)
			throws CoreException {
		if (AntRunner.isBuildRunning() || this.getProject() == null) {
			monitor.done();
			return null;
		}
		monitor.beginTask(AntBuildMessages.getString("Build.Monitor.Title"),
				WOAntBuilder.TOTAL_WORK_UNITS);
		if (!Preferences.getPREF_RUN_WOBUILDER_ON_BUILD()
				|| getProject() == null || !getProject().exists()) {
			monitor.done();
			return null;
		}
		String aBuildFile = null;
		try {
			if (!projectNeedsAnUpdate()
					&& kind != IncrementalProjectBuilder.FULL_BUILD) {
				monitor.done();
				return null;
			}
			aBuildFile = this.buildFile();
			if (checkIfBuildfileExist(aBuildFile)) {
				getProject().getFile(aBuildFile).deleteMarkers(
						ProjectBuildPlugin.MARKER_TASK_GENERIC, false,
						IResource.DEPTH_ONE);
				this.execute(monitor, aBuildFile);
			}
		} catch (Exception e) {
			this.handleException(e);
		}
		aBuildFile = null;
		/*
		 * monitor.beginTask( BuildMessages.getString("Build.Refresh.Title"),
		 * WOAntBuilder.TOTAL_WORK_UNITS);
		 */
		//this.forgetLastBuiltState();
		//getProject().refreshLocal(IProject.DEPTH_INFINITE, monitor);
		monitor.done();
		return null;
	}
	/**
	 * Method execute.
	 * 
	 * @param monitor
	 * @param aBuildFile
	 * @throws Exception
	 */
	private void execute(IProgressMonitor monitor, String aBuildFile)
			throws Exception {
		//RunAnt runAnt = new RunAnt();
		//if (projectNeedsClean())
		//TODO:handle clean
		Project project = (Project)this.getProject().getAdapter(Project.class);
		project.setUpPatternsetFiles();
		this.launchAntInExternalVM(getProject().getFile(aBuildFile), monitor);
	}
	/**
	 * Method handleException.
	 * 
	 * @param anException
	 */
	private void handleException(Exception anException) {
		IMarker aMarker = null;
		try {
			if (anException == null) {
				throw new NullPointerException(
						"WOBuilder.handleException called without an exception.");
			}
			aMarker = this.getBuildfileMarker();
			aMarker.setAttribute(IMarker.MESSAGE, "WOLips: "
					+ anException.getMessage());
		} catch (Exception e) {
			ProjectBuildPlugin.getDefault().getPluginLogger().log(e);
		} finally {
			aMarker = null;
		}
	}
	private IMarker getBuildfileMarker() {
		IMarker aMarker = null;
		try {
			aMarker = getProject().getFile(this.buildFile()).createMarker(
					ProjectBuildPlugin.MARKER_TASK_GENERIC);
			aMarker.setAttribute(IMarker.PRIORITY, IMarker.PRIORITY_HIGH);
		} catch (CoreException e) {
			ProjectBuildPlugin.getDefault().getPluginLogger().log(e);
		}
		return aMarker;
	}
	/**
	 * Checks if the build file exists.
	 * 
	 * @param aBuildFile
	 * @return boolean
	 */
	private boolean checkIfBuildfileExist(String aBuildFile) {
		try {
			if (getProject().getFile(aBuildFile).exists())
				return true;
		} catch (Exception anException) {
			ProjectBuildPlugin.getDefault().getPluginLogger().log(anException);
		}
		IMarker aMarker = null;
		try {
			aMarker = getProject().createMarker(
					ProjectBuildPlugin.MARKER_TASK_GENERIC);
			aMarker.setAttribute(IMarker.MESSAGE, "WOLips: Can not find: "
					+ this.buildFile());
			aMarker.setAttribute(IMarker.PRIORITY, IMarker.PRIORITY_HIGH);
		} catch (Exception anException) {
			ProjectBuildPlugin.getDefault().getPluginLogger().log(anException);
		} finally {
			aMarker = null;
		}
		return false;
	}
	/**
	 * @return String
	 */
	public String buildFile() {
		return "build.xml";
	}
	/**
	 * @return String
	 */
	public String defaultTarget() {
		return null;
	}
	/**
	 * @return String
	 */
	public String cleanTarget() {
		return "clean";
	}
	/**
	 * Method inExternalVM.
	 * 
	 * @param buildFile
	 * @param monitor
	 */
	private void launchAntInExternalVM(IFile buildFile, IProgressMonitor monitor) {
		try {
			LaunchAntInExternalVM.launchAntInExternalVM(buildFile, monitor, Preferences.getPREF_CAPTURE_ANT_OUTPUT(), null);
		} catch (CoreException e) {
			WorkbenchUtilitiesPlugin.handleException(Display.getCurrent()
					.getActiveShell(), e, AntBuildMessages
					.getString("Build.Exception"));
			return;
		}
	}
}