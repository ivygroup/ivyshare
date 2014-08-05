package com.ivyshare.httpserver;

import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.apache.http.ConnectionClosedException;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpServerConnection;
import org.apache.http.HttpStatus;
import org.apache.http.MethodNotSupportedException;
import org.apache.http.entity.ContentProducer;
import org.apache.http.entity.EntityTemplate;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.impl.DefaultHttpServerConnection;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.BasicHttpProcessor;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.protocol.HttpRequestHandlerRegistry;
import org.apache.http.protocol.HttpService;
import org.apache.http.protocol.ResponseConnControl;
import org.apache.http.protocol.ResponseContent;
import org.apache.http.protocol.ResponseDate;
import org.apache.http.protocol.ResponseServer;

import android.util.Log;

import com.ivy.ivyengine.control.LocalSetting;
import com.ivyshare.util.CommonUtils;

public class HttpServerManager implements HttpRequestHandler{
	private static final String TAG = HttpServerManager.class.getSimpleName();

    private Thread mThread = null;

    private List<String> mListFiles;
    private String mShareUrl = "";
    private int mPortNumber = 8080;

    private LocalSetting mLocalSetting;

    public static Set<Integer> mSetPortNumbers = new HashSet<Integer>();

    private ServerSocket mServerSocket = null;

    public int startShare(List<String> list, int port) {
    	mLocalSetting = LocalSetting.getInstance();
    	if (mLocalSetting.getMySelf().mIP == null) {
    		return -1;
    	}

    	if (mSetPortNumbers.contains(port)) {
    		return -1;
    	}
    	
    	if (mThread != null) {
    		mThread.interrupt();
    	}

    	mShareUrl = new StringBuilder("http://")
    			.append(mLocalSetting.getMySelf().mIP.getHostAddress())
        		.append(':').append(mPortNumber).append('/').toString();

    	try {
            mThread = new SocketListenThread(port);
            mThread.setDaemon(false);
            mThread.start();
    	} catch (IOException e) {
    		return -1;
    	}

    	mListFiles = list;
    	mPortNumber = port;
    	mSetPortNumbers.add(mPortNumber);

        Log.e(TAG,"start thread OK");
    	return 0;
    }

    public String getShareURL() {
    	return mShareUrl;
    }

    public int stopShare() {
    	mSetPortNumbers.remove(mPortNumber);

        if( mThread == null ) {
            return 0;
        }

        try {
            mThread.interrupt();
            mServerSocket.close();
            mThread = null;
        } catch (IOException e) {
        	
        }

        Log.e(TAG,"stop thread ok");
        return 0;
    }

    private void handleRootRequest(HttpResponse response) {
		if (mListFiles.size() == 1) {
			setMoveTemporarilyResponse(mListFiles.get(0), response);
		} else {
			setFileListResponse(response);
		}
    }

    private void setMoveTemporarilyResponse(String path, HttpResponse response) {

        response.setStatusCode(HttpStatus.SC_MOVED_TEMPORARILY);

        String name="";
        try {
        	name = URLEncoder.encode(CommonUtils.getFileNameByPath(path),"UTF-8");
        } catch (UnsupportedEncodingException e) {
        }

        response.addHeader("Location", mShareUrl+name);
        EntityTemplate body = new EntityTemplate(new ContentProducer() {
            public void writeTo(final OutputStream outstream) throws IOException {
                OutputStreamWriter writer = new OutputStreamWriter(outstream, "UTF-8"); 
                writer.write("<html><body><h1>");
                writer.write("HTTP/1.1 302 Moved TEMPORARILY");
                writer.write("</h1></body></html>");
                writer.flush();
            }
        });
        body.setContentType("text/html; charset=UTF-8");
        response.setEntity(body);
    }

    private void setFileListResponse(HttpResponse response) {
        EntityTemplate body = new EntityTemplate(new ContentProducer() {
            public void writeTo(final OutputStream outstream) throws IOException {
                OutputStreamWriter writer = new OutputStreamWriter(outstream, "UTF-8"); 
                writer.write("<html><head><title>Ivy File Share</title></head>");
                writer.write("<body><p>Files list:</p>");
            	int nSize = mListFiles.size();
                for (int i=0; i<nSize; i++) {
                	String name = "";
                	String displayName = "";
                    try {
                    	displayName = CommonUtils.getFileNameByPath(mListFiles.get(i));
                    	name = URLEncoder.encode(displayName, "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                    }
                	String href = name;
                	writer.write("<a href=\"" + href + "\">" + displayName + "</a><br />");
                }
                writer.write("</body></html>");
                writer.flush();
            }
        });
        body.setContentType("text/html; charset=UTF-8");
        response.setStatusCode(HttpStatus.SC_OK);
        response.setEntity(body);
    }

    private void setFileResponse(String path, HttpResponse response) {

        final File file = new File(path);
        if (!file.exists()) {
            response.setStatusCode(HttpStatus.SC_NOT_FOUND);
            EntityTemplate body = new EntityTemplate(new ContentProducer() {
                
                public void writeTo(final OutputStream outstream) throws IOException {
                    OutputStreamWriter writer = new OutputStreamWriter(outstream, "UTF-8"); 
                    writer.write("<html><body><h1>");
                    writer.write("File ");
                    writer.write(file.getPath());
                    writer.write(" not found");
                    writer.write("</h1></body></html>");
                    writer.flush();
                }
                
            });
            body.setContentType("text/html; charset=UTF-8");
            response.setEntity(body);
            Log.e(TAG,"File " + file.getPath() + " not found");

        } else if (!file.canRead() || file.isDirectory()) {
            response.setStatusCode(HttpStatus.SC_FORBIDDEN);
            EntityTemplate body = new EntityTemplate(new ContentProducer() {
                
                public void writeTo(final OutputStream outstream) throws IOException {
                    OutputStreamWriter writer = new OutputStreamWriter(outstream, "UTF-8"); 
                    writer.write("<html><body><h1>");
                    writer.write("Access denied");
                    writer.write("</h1></body></html>");
                    writer.flush();
                }
                
            });
            body.setContentType("text/html; charset=UTF-8");
            response.setEntity(body);
            Log.e(TAG,"Cannot read file " + file.getPath());
            
        } else {
            response.setStatusCode(HttpStatus.SC_OK);
            FileEntity body = new FileEntity(file, "application/octet-stream");
            response.setEntity(body);
            Log.d(TAG,"Serving file " + file.getPath());
        }
    }

	@Override
	public void handle(HttpRequest request, HttpResponse response, HttpContext context)
			throws HttpException, IOException {

        String method = request.getRequestLine().getMethod().toUpperCase(Locale.ENGLISH);
        if (!method.equals("GET") && !method.equals("HEAD") && !method.equals("POST")) {
            throw new MethodNotSupportedException(method + " method not supported"); 
        }
        String target = URLDecoder.decode(request.getRequestLine().getUri());
        Log.d(TAG, "method " + method + " uri " + target);

        if (method.equals("GET")) {
        	// means get content
        	if (target == null || target.equals("/")) {
        		handleRootRequest(response);
        	// get content of one link
        	} else {
        		int nSize = mListFiles.size();
        		for (int i=0; i<nSize; i++) {
                	String name = "";
                    name = CommonUtils.getFileNameByPath(mListFiles.get(i));
                    if (name.equals(target.substring(1))) {
                		setFileResponse(mListFiles.get(i), response);
                		break;
                    }
        		}
        	}
        }
	}

    private class SocketListenThread extends Thread {
        private HttpService mHttpService = null;
        private HttpParams mParams  = null; 

        public SocketListenThread(int port) throws IOException {
        	mServerSocket = new ServerSocket(port);
        	mParams = new BasicHttpParams()
                .setIntParameter(CoreConnectionPNames.SO_TIMEOUT, 5000)
                .setIntParameter(CoreConnectionPNames.SOCKET_BUFFER_SIZE, 8 * 1024)
                .setBooleanParameter(CoreConnectionPNames.STALE_CONNECTION_CHECK, false)
                .setBooleanParameter(CoreConnectionPNames.TCP_NODELAY, true)
                .setParameter(CoreProtocolPNames.ORIGIN_SERVER, "HttpComponents/1.1");

            // Set up the HTTP protocol processor
            BasicHttpProcessor httpproc = new BasicHttpProcessor();
            httpproc.addInterceptor(new ResponseDate());
            httpproc.addInterceptor(new ResponseServer());
            httpproc.addInterceptor(new ResponseContent());
            httpproc.addInterceptor(new ResponseConnControl());
            
            // Set up request handlers
            HttpRequestHandlerRegistry reqistry = new HttpRequestHandlerRegistry();
            reqistry.register("*", HttpServerManager.this);

            // Set up the HTTP service
            mHttpService = new HttpService(
                    httpproc, 
                    new DefaultConnectionReuseStrategy(), 
                    new DefaultHttpResponseFactory());
            mHttpService.setParams(mParams);
            mHttpService.setHandlerResolver(reqistry);
        }

        public void run() {
            Log.d(TAG, "Listening on port " + mServerSocket.getLocalPort());
            while (!Thread.interrupted()) {
                try {
                    // Set up HTTP connection
                    Socket socket = mServerSocket.accept();
                    Log.d(TAG,"Incoming connection from " + socket.getInetAddress());

                    DefaultHttpServerConnection conn = new DefaultHttpServerConnection();
                    conn.bind(socket, mParams);

                    // Start worker thread
                    Thread t = new WorkerThread(mHttpService, conn);
                    t.setDaemon(true);
                    t.start();
                } catch (InterruptedIOException ex) {
                    break;
                } catch (IOException e) {
                    Log.e(TAG,"I/O error initialising connection thread: " 
                            + e.getMessage());
                    break;
                }
            }
        }
    }

    private class WorkerThread extends Thread {

        private final HttpService mHttpService;
        private final HttpServerConnection mConnection;
        
        public WorkerThread(
                final HttpService httpservice, 
                final HttpServerConnection conn) {
            super();
            this.mHttpService = httpservice;
            this.mConnection = conn;
        }
        public void run() {
            Log.e(TAG,"New connection thread");
            HttpContext context = new BasicHttpContext(null);
            try {
                while (!Thread.interrupted() && mConnection.isOpen()) {
                	mHttpService.handleRequest(mConnection, context);
                }
            } catch (ConnectionClosedException ex) {
                Log.e(TAG,"Client closed connection");
            } catch (IOException ex) {
                Log.e(TAG,"I/O error: " + ex.getMessage());
            } catch (HttpException ex) {
                Log.e(TAG,"Unrecoverable HTTP protocol violation: " + ex.getMessage());
            } finally {
                try {
                	mConnection.shutdown();
                } catch (IOException ignore) {
                }
            }
        }

    }
}