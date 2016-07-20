package org.jboss.tools.vscode.java.handlers;

import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.jboss.tools.vscode.ipc.RequestHandler;
import org.jboss.tools.vscode.java.JavaLanguageServerPlugin;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Notification;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;

public class ClassfileContentHandler implements RequestHandler {
	public static final String REQ_CLASSFILECONTENTS = "java/ClassFileContents";

	@Override
	public boolean canHandle(String request) {
		return REQ_CLASSFILECONTENTS.equals(REQ_CLASSFILECONTENTS);
	}

	@Override
	public JSONRPC2Response process(JSONRPC2Request request) {
		String uriString = (String) request.getNamedParams().get("uri");
		JSONRPC2Response response = new JSONRPC2Response(request.getID());
		try {
			URI uri = new URI(uriString);
			if (uri.getAuthority().equals("contents")) {
				String handleId = uri.getQuery();
				IJavaElement element = JavaCore.create(handleId);
				IClassFile cf = (IClassFile) element.getAncestor(IJavaElement.CLASS_FILE);
				if (cf != null) {
					IBuffer buffer = cf.getBuffer();
					if (buffer != null) {
						response.setResult(buffer.getContents());
						JavaLanguageServerPlugin.logInfo("Completion request completed");
						return response;
					}
				}
			}
		} catch (URISyntaxException e) {
			JavaLanguageServerPlugin.logException("Problem reading URI " + uriString, e);
		} catch (JavaModelException e) {
			JavaLanguageServerPlugin.logException("Exception getting java element ", e);
		}
	
		return response;
	}

	@Override
	public void process(JSONRPC2Notification request) {
		// not implemented
	}

}
