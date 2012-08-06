/*
    e7o.de CalDAV project
    Copyright (C) 2010 sven herzky

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package de.e7o.caldav.caldav;

import java.io.*;
import java.net.URL;

import org.apache.http.*;
import org.apache.http.client.*;
import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.*;
import org.apache.http.auth.*;

public class Connection {
	private CaldavServer base;
	private String extUri;
	private UsernamePasswordCredentials authInfo;
	public int lastCode = 0;
	public Header[] lastHeaders = null;
	
	protected Connection(CaldavServer base) {
		this.base = base;
		if (base.username != null) {
			this.authInfo = new UsernamePasswordCredentials(base.username, base.password);
		} else {
			this.authInfo = null;
		}
	}
	
	public InputStream request(String extUri, String requestMethod, String bodyData, String[]... additionalHeaders) throws Exception {
		int i;
		
		// Pre
		HttpClient httpclient = new DefaultHttpClient();
		HttpAny ha = new HttpAny(base.baseUri + extUri);
		ha.setMethod(requestMethod);
		
		// Headers
		for (i = 0; i < additionalHeaders.length; i++) {
			ha.addHeader(additionalHeaders[i][0], additionalHeaders[i][1]);
		}
		
		// Body
		ha.setEntity(new StringEntity(bodyData));
		
		// Authentication
		URL url = new URL(base.baseUri);
		UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(
			this.authInfo.getUserName(),
			this.authInfo.getPassword()
		);
		((AbstractHttpClient)httpclient).getCredentialsProvider().setCredentials(
			new AuthScope(url.getHost(), 80, null, "Basic"),
			credentials
		);
		
		// Send request
		HttpResponse httpresp = httpclient.execute(ha);
		
		// Remember state
		lastCode = httpresp.getStatusLine().getStatusCode();
		lastHeaders = httpresp.getAllHeaders();
		
		// Done - return InputStream
		HttpEntity httpent = httpresp.getEntity();
		return httpent.getContent();
	}
	
	public static String InputStreamToString(InputStream is) {
		String t;
		StringBuilder s = new StringBuilder();
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		try {
			while ((t = br.readLine()) != null) s.append(t + "\n");
			br.close();
			return s.toString();
		} catch (Exception e) {
			return null;
		}
	}
}