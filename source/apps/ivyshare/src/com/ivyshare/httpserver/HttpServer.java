/*
d * $HeadURL$
 * $Revision$
 * $Date$
 *
 * ====================================================================
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */

package com.ivyshare.httpserver;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Locale;
import java.util.logging.Logger;

import org.apache.http.ConnectionClosedException;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
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
import org.apache.http.params.HttpParams;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.protocol.BasicHttpProcessor;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.protocol.HttpRequestHandlerRegistry;
import org.apache.http.protocol.HttpService;
import org.apache.http.protocol.ResponseConnControl;
import org.apache.http.protocol.ResponseContent;
import org.apache.http.protocol.ResponseDate;
import org.apache.http.protocol.ResponseServer;
import org.apache.http.util.EntityUtils;

import com.ivyshare.R.string;

import android.R.integer;
import android.text.StaticLayout;
import android.util.Log;

/**
 * Basic, yet fully functional and spec compliant, HTTP/1.1 file server.
 * <p>
 * Please note the purpose of this application is demonstrate the usage of HttpCore APIs.
 * It is NOT intended to demonstrate the most efficient way of building an HTTP file server. 
 * 
 *
 * @version $Revision$
 */
public class HttpServer {
    
    protected static final String TAG = "HttpServer";
    static private Thread mThread = null;
    
    static private ServerSocket serversocket = null;
    static private HttpParams params  = null; 
    static private HttpService httpService = null;
    
    static private String mUrl = null;
    static private String mName = null;

    public static void start(String args, String url,String name) throws Exception {
        if (args == null) {
            Log.e(TAG,"Please specify document root directory");
            return;
        }
        
        Log.e(TAG,"start path = "+args);
     
        if( mThread != null ){
            mThread.interrupt();
            Log.e(TAG,"interrupt the old thread");
        }
        
        mUrl = url;
        mName = name;
        
        mThread = new RequestListenerThread(8080, args);
        mThread.setDaemon(false);
        mThread.start();
        
        Log.e(TAG,"start thread ok");
    }
    
    public static void stop() throws Exception {
        
        if( mThread == null )
            return;
        
        mThread.interrupt();
        
        mUrl = null;
		mName = null;
        
        serversocket.close();
                
        serversocket = null;
        params  = null; 
        httpService = null;
        mThread = null;
        
        Log.e(TAG,"stop thread ok");
    }
    
    static class HttpFileHandler implements HttpRequestHandler  {
        
        private final String docRoot;
        
        public HttpFileHandler(final String docRoot) {
            super();
            this.docRoot = docRoot;
        }
        
        public void handle(
                final HttpRequest request, 
                final HttpResponse response,
                final HttpContext context) throws HttpException, IOException {

            String method = request.getRequestLine().getMethod().toUpperCase(Locale.ENGLISH);
            if (!method.equals("GET") && !method.equals("HEAD") && !method.equals("POST")) {
                throw new MethodNotSupportedException(method + " method not supported"); 
            }
            
            if(method.equals("POST")){
            
                if (request instanceof HttpEntityEnclosingRequest) {
                    
                    Header[] headers = request.getHeaders("Content-Type");
                    String boundary = headers[0].toString()
                            .substring(headers[0].toString().indexOf("boundary=")+9);
                    boundary = "\r\n\r\n"+"--"+boundary+"--";
                    
                    HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
                    long contentLenth = entity.getContentLength();
                     InputStream insputStream=entity.getContent();
                    
                    final int headerLenth = 300;
                    //read header
                    byte buffer[]=new byte[headerLenth];
                    insputStream.read(buffer,0,headerLenth);
                    
                    //get file name 
                    String header = new String(buffer);
                    final String fileName = "filename=\"";
                    String name = header.substring(header.indexOf(fileName)+fileName.length());
                    name = name.substring(0,name.indexOf("\""));
                    
                    FileOutputStream fos = new FileOutputStream("/sdcard/"+name);
                    //
                    //the data content after "\r\n\r\n"
                    //which is 4 bytes
                    //
                    int index = header.indexOf("\r\n\r\n")+4;
                    
                    //write the conten which contain in 300 byte
                    fos.write( buffer,index,headerLenth-index);
                    
                    //
                    //int the end of conteng there are 
                    //"\r\n\r\n--(boundary string)--\r\n"
                    //which size is boundary.length()
                    //
                    long lenth = contentLenth - headerLenth - boundary.length();
                    
                    int writeByte = 0;
                    int readLenth = 0;
                    
                    byte writeBuffer[]=new byte[1024];
                    while( (readLenth = insputStream.read(writeBuffer,0,1024)) > 0){
                        
                        if( (lenth - writeByte) < 1024){
                            fos.write(writeBuffer,0,(int)(lenth-writeByte));
                            writeByte += (int)(lenth-writeByte);
                        }
                        else{
                            fos.write(writeBuffer,0,readLenth);
                            writeByte += readLenth;
                        }
                        
                        if(writeByte >= lenth)
                            break;
                        
                    }
                    
                    fos.flush();
                    fos.close();
                }
                
                response.setStatusCode(HttpStatus.SC_OK);
                EntityTemplate body = new EntityTemplate(new ContentProducer() {
                    
                    public void writeTo(final OutputStream outstream) throws IOException {
                        OutputStreamWriter writer = new OutputStreamWriter(outstream, "UTF-8"); 
                        writer.write("<html><body><h1>");
                        writer.write("Update OK!");
                        writer.write("</h1></body></html>");
                        writer.flush();
                    }
                    
                });
                body.setContentType("text/html; charset=UTF-8");
                response.setEntity(body);
                
                return;
            }
            
            String target = request.getRequestLine().getUri();
            if (URLDecoder.decode(target) == null || URLDecoder.decode(target).equals("/")) {
                
                String sName = URLEncoder.encode(mName,"UTF-8");
                
                Log.e(TAG, "response Moved Permanently");
                response.setStatusCode(HttpStatus.SC_MOVED_TEMPORARILY);
                response.addHeader("Location", mUrl+sName);
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
                return;
            }
            
            if (URLDecoder.decode(target).equals("/up.html")) {
                
                response.setStatusCode(HttpStatus.SC_OK);
                EntityTemplate body = new EntityTemplate(new ContentProducer() {
                    
                    public void writeTo(final OutputStream outstream) throws IOException {
                        OutputStreamWriter writer = new OutputStreamWriter(outstream, "UTF-8");
                        writer.write("<html><body><h1>");
                        writer.write(FormHtml(mUrl));
                        writer.write("</h1></body></html>");
                        writer.flush();
                    }
                });
                body.setContentType("text/html; charset=UTF-8");
                response.setEntity(body);
                return;
            }
            
            final File file = new File(this.docRoot, URLDecoder.decode(target,"UTF-8"));
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
                Log.e(TAG,"Serving file " + file.getPath());
                
            }
        }
        
    }
    
    static class RequestListenerThread extends Thread {
        
        public RequestListenerThread(int port, final String docroot) throws IOException {
            serversocket = new ServerSocket(port);
            params = new BasicHttpParams();
                params
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
            reqistry.register("*", new HttpFileHandler(docroot));
            
            // Set up the HTTP service
            httpService = new HttpService(
                    httpproc, 
                    new DefaultConnectionReuseStrategy(), 
                    new DefaultHttpResponseFactory());
            httpService.setParams(params);
            httpService.setHandlerResolver(reqistry);
        }
        
        public void run() {
            Log.e(TAG,"Listening on port " + serversocket.getLocalPort());
            while (!Thread.interrupted()) {
                try {
                    // Set up HTTP connection
                    Socket socket = serversocket.accept();
                    DefaultHttpServerConnection conn = new DefaultHttpServerConnection();
                    Log.e(TAG,"Incoming connection from " + socket.getInetAddress());
                    conn.bind(socket, params);

                    // Start worker thread
                    Thread t = new WorkerThread(httpService, conn);
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
    
    static class WorkerThread extends Thread {

        private final HttpService httpservice;
        private final HttpServerConnection conn;
        
        public WorkerThread(
                final HttpService httpservice, 
                final HttpServerConnection conn) {
            super();
            this.httpservice = httpservice;
            this.conn = conn;
        }
        
        public void run() {
            Log.e(TAG,"New connection thread");
            HttpContext context = new BasicHttpContext(null);
            try {
                while (!Thread.interrupted() && this.conn.isOpen()) {
                    this.httpservice.handleRequest(this.conn, context);
                }
            } catch (ConnectionClosedException ex) {
                Log.e(TAG,"Client closed connection");
            } catch (IOException ex) {
                Log.e(TAG,"I/O error: " + ex.getMessage());
            } catch (HttpException ex) {
                Log.e(TAG,"Unrecoverable HTTP protocol violation: " + ex.getMessage());
            } finally {
                try {
                    this.conn.shutdown();
                } catch (IOException ignore) {}
            }
        }

    } 
    
    static private String FormHtml(String action)
    {
        String htmlString = "<form"+" name=UploadForm"+" enctype=multipart/form-data"+" method=post"
                            +" action="
                            +action+"up.html"
                            +">";
        
        htmlString += "<input"+" type=file"+" name=file"+" size=20"+" maxlength=20"+"> <br>";
        htmlString += "<input"+" type=submit"+" value=Submit>";
        htmlString += "</form>";
        return htmlString;
    }
    
}