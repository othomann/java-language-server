/*******************************************************************************
 * Copyright (c) 2016 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.jboss.tools.vscode.java.internal.handlers;

import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.DeleteEdit;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;
import org.jboss.tools.langs.DidChangeTextDocumentParams;
import org.jboss.tools.langs.DidCloseTextDocumentParams;
import org.jboss.tools.langs.DidOpenTextDocumentParams;
import org.jboss.tools.langs.DidSaveTextDocumentParams;
import org.jboss.tools.langs.Range;
import org.jboss.tools.langs.TextDocumentContentChangeEvent;
import org.jboss.tools.langs.base.LSPMethods;
import org.jboss.tools.vscode.internal.ipc.NotificationHandler;
import org.jboss.tools.vscode.java.internal.JDTUtils;
import org.jboss.tools.vscode.java.internal.JavaLanguageServerPlugin;

public class DocumentLifeCycleHandler {

	public class ClosedHandler implements NotificationHandler<DidCloseTextDocumentParams, Object>{
		@Override
		public boolean canHandle(String request) {
			return LSPMethods.DOCUMENT_CLOSED.getMethod().equals(request);
		}

		@Override
		public Object handle(DidCloseTextDocumentParams params) {
			try {
				ResourcesPlugin.getWorkspace().run(new IWorkspaceRunnable() {
					@Override
					public void run(IProgressMonitor monitor) throws CoreException {
						handleClosed(params, monitor);
					}
				}, new NullProgressMonitor());
			} catch (CoreException e) {
				JavaLanguageServerPlugin.logException("Handle document close ", e);
			}
			return null;
		}

	}

	public class OpenHandler implements NotificationHandler<DidOpenTextDocumentParams, Object>{

		@Override
		public boolean canHandle(String request) {
			return LSPMethods.DOCUMENT_OPENED.getMethod().equals(request);
		}

		@Override
		public Object handle(DidOpenTextDocumentParams params) {
			try {
				ResourcesPlugin.getWorkspace().run(new IWorkspaceRunnable() {
					@Override
					public void run(IProgressMonitor monitor) throws CoreException {
						handleOpen(params, monitor);
					}
				}, new NullProgressMonitor());
			} catch (CoreException e) {
				JavaLanguageServerPlugin.logException("Handle document open ", e);
			}
			return null;
		}
	}

	public class ChangeHandler implements NotificationHandler<DidChangeTextDocumentParams, Object>{

		@Override
		public boolean canHandle(String request) {
			return LSPMethods.DOCUMENT_CHANGED.getMethod().equals(request);
		}

		@Override
		public Object handle(DidChangeTextDocumentParams params) {
			try {
				ResourcesPlugin.getWorkspace().run(new IWorkspaceRunnable() {
					@Override
					public void run(IProgressMonitor monitor) throws CoreException {
						handleChanged(params, monitor);
					}
				}, new NullProgressMonitor());
			} catch (CoreException e) {
				JavaLanguageServerPlugin.logException("Handle document open ", e);
			}
			return null;
		}
	}

	public class SaveHandler implements NotificationHandler<DidSaveTextDocumentParams, Object>{

		@Override
		public boolean canHandle(String request) {
			return LSPMethods.DOCUMENT_SAVED.getMethod().equals(request);
		}

		@Override
		public Object handle(DidSaveTextDocumentParams params) {
			try {
				ResourcesPlugin.getWorkspace().run(new IWorkspaceRunnable() {
					@Override
					public void run(IProgressMonitor monitor) throws CoreException {
						handleSaved(params, monitor);
					}
				}, new NullProgressMonitor());
			} catch (CoreException e) {
				JavaLanguageServerPlugin.logException("Handle document open ", e);
			}
			return null;
		}
	}

	public DocumentLifeCycleHandler() {
	}

	private void handleOpen(DidOpenTextDocumentParams params, IProgressMonitor monitor) {
		ICompilationUnit unit = JDTUtils.createCompilationUnit(params.getTextDocument().getUri());
		if (unit == null || unit.getResource() == null) {
			return;
		}
		try {
			// The open event can happen before the workspace element added event when a new file is added.
			// checks if the underlying resource exists and refreshes to sync the newly created file.
			if(!unit.getResource().isAccessible()){
				try {
					unit.getResource().refreshLocal(IResource.DEPTH_ONE, monitor);
				} catch (CoreException e) {
					// ignored
				}
			}

			IBuffer buffer = unit.getBuffer();
			if(buffer != null) {
				buffer.setContents(params.getTextDocument().getText());
			}

			// TODO: wire up cancellation.
			unit.reconcile(ICompilationUnit.NO_AST, true, unit.getOwner(), monitor);
		} catch (JavaModelException e) {
			JavaLanguageServerPlugin.logException("Document open: ",e);
		}
	}

	private void handleChanged(DidChangeTextDocumentParams params, IProgressMonitor monitor) {
		ICompilationUnit unit = JDTUtils.resolveCompilationUnit(params.getTextDocument().getUri());

		if (unit == null) {
			return;
		}

		try {
			IDocument document = JsonRpcHelpers.toDocument(unit.getBuffer());
			MultiTextEdit root = new MultiTextEdit();
			List<TextDocumentContentChangeEvent> contentChanges = params.getContentChanges();
			for (TextDocumentContentChangeEvent changeEvent : contentChanges) {

				Range range = changeEvent.getRange();

				int startOffset = document.getLineOffset(range.getStart().getLine().intValue()) + range.getStart().getCharacter().intValue();
				int length = changeEvent.getRangeLength().intValue();

				TextEdit edit = null;
				if (length == 0) {
					edit = new InsertEdit(startOffset, changeEvent.getText());
				} else if (changeEvent.getText().isEmpty()){
					edit = new DeleteEdit(startOffset, length);
				} else {
					edit = new ReplaceEdit(startOffset, length, changeEvent.getText());
				}
				root.addChild(edit);
			}

			if (root.hasChildren()) {
				root.apply(document);
				unit.reconcile(ICompilationUnit.NO_AST, false, null, monitor);
			}
		} catch (JavaModelException e) {
			JavaLanguageServerPlugin.logException("Document changed: ",e);
		} catch (org.eclipse.jface.text.BadLocationException e) {
			JavaLanguageServerPlugin.logException("Document changed: ",e);
		}
	}

	private void handleClosed(DidCloseTextDocumentParams params, IProgressMonitor monitor) {
		JavaLanguageServerPlugin.logInfo("DocumentLifeCycleHandler.handleClosed");
		String uri = params.getTextDocument().getUri();
		JDTUtils.discard(uri);
	}

	private void handleSaved(DidSaveTextDocumentParams params, IProgressMonitor monitor) {
		JavaLanguageServerPlugin.logInfo("DocumentLifeCycleHandler.handleSaved");
		ICompilationUnit unit = JDTUtils.resolveCompilationUnit(params.getTextDocument().getUri());
		try {
			unit.save(monitor, true);
		} catch (JavaModelException e) {
			JavaLanguageServerPlugin.logException("Document saved: ",e);
		}
	}
}
