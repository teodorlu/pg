package com.github.igrishaev;

import clojure.lang.Keyword;
import clojure.lang.RT;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Connection implements Closeable {

    private static Keyword KW_PORT = Keyword.intern("port");
    private static Keyword KW_HOST = Keyword.intern("host");
    private static Keyword KW_USER = Keyword.intern("user");
    private static Keyword KW_DB = Keyword.intern("database");
    private static Keyword KW_PASS = Keyword.intern("password");
    private static Keyword KW_PG_PARAMS = Keyword.intern("pg-params");
    private static Keyword KW_PROTO_VER = Keyword.intern("protocol-version");

    public final String id;
    public final long createdAt;

    private Boolean isSSL = false;
    private int pid;
    private int secretKey;
    private Keyword txStatus;
    private Map<Keyword, Object> config;
    private Socket socket;
    private InputStream inStream;
    private OutputStream outStream;
    private Map<String, String> params;

    // private Map<String, Object> state;
    // private Map<String, Object> opt;

    // public void sendMessage(Message message) {
    //     bytes byte[] = message.encode();
    //     out_stream.write(bytes);
    // }

    public void close () {
        closeSocket();
    }

    public Boolean getSSL () {
        return isSSL;
    }

    public Map getConfig () {
        return config;
    }

    public Keyword getTxStatus () {
        return txStatus;
    }

    public void setTxStatus (Keyword status) {
        txStatus = status;
    }

    public void setPrivateKey (int key) {
        secretKey = key;
    }

    public int getPrivateKey () {
        return secretKey;
    }

    public OutputStream getOutputStream () {
        return outStream;
    }

    public InputStream getInputStream () {
        return inStream;
    }

    public Integer nextID () {
        return RT.nextID();
    }

    public Connection(Map<Keyword, Object> cljConfig) {

        config = cljConfig;
        params = new HashMap();

        id = String.format("pg%d", nextID());
        createdAt = System.currentTimeMillis();

        connect();
    }

    public int getPid () {
        return pid;
    }

    public void setPid (int pid) {
        this.pid = pid;
    }

    public Boolean isClosed () {
        return socket.isClosed();
    }

    private void closeSocket () {
        try {
            socket.close();
        }
        catch (IOException e) {
            throw new PGError(e, "could not close the socket");
        }
    }

    public String getParam (String param) {
        return params.get(param);
    }

    public void setParam (String param, String value) {
        params.put(param, value);
    }

    public Integer getPort () {
        Long port = (Long) config.get(KW_PORT);
        return port.intValue();
    }

    public String getHost () {
        return (String) config.get(KW_HOST);
    }

    public String getUser () {
        return (String) config.get(KW_USER);
    }

    private Integer getProtocolVersion () {
        Long proto = (Long) config.get(KW_PROTO_VER);
        return proto.intValue();
    }

    private Map<String, String> getPgParams () {
        return (Map<String, String>) config.get(KW_PG_PARAMS);
    }

    public String getPassword () {
        return (String) config.get(KW_PASS);
    }

    public String getDatabase () {
        return (String) config.get(KW_DB);
    }

    public String toString () {
        return String.format(
            "<PG connection %s@%s:%s/%s>",
            getUser(),
            getHost(),
            getPort(),
            getDatabase()
        );
    }

    private void connect () {

        Integer port = getPort();
        String host = getHost();

        try {
            socket = new Socket(host, port, true);
        }
        catch (IOException e) {
            throw new PGError(e, "Cannot connect to a socket");
        }

        try {
            inStream = socket.getInputStream();
        }
        catch (IOException e) {
            throw new PGError(e, "Cannot get an input stream");
        }

        try {
            outStream = socket.getOutputStream();
        }
        catch (IOException e) {
            throw new PGError(e, "Cannot get an output stream");
        }

    }

    public void sendMessage (AMessage message) {
        ByteBuffer buf = message.encode("UTF-8"); // TODO
        try {
            outStream.write(buf.array());
        }
        catch (IOException e) {
            throw new PGError(e, "could not write bb to the out stream");
        }
    }

    public String generateStatement () {
        return String.format("statement%d", nextID());
    }

    public String generatePortal () {
        return String.format("portal%d", nextID());
    }

    public void sendParse (String statement, String query, List<Long> oids) {
        sendMessage(new Parse(statement, query, oids));
    }

    public void sendStartupMessage () {
        StartupMessage msg =
            new StartupMessage(getProtocolVersion(),
                               getUser(),
                               getDatabase(),
                               getPgParams());
        sendMessage(msg);
    }

    public void sendExecute (String portal, Long rowCount) {
        sendMessage(new Execute(portal, rowCount));
    }

    public void sendCopyData (byte[] buf) {
        sendMessage(new CopyData(buf));
    }

    public void sendQuery (String query) {
        sendMessage(new Query(query));
    }

    public void sendPassword (String password) {
        sendMessage(new PasswordMessage(password));
    }

    public void sendSync () {
        sendMessage(new Sync());
    }

    public void sendFlush () {
        sendMessage(new Flush());
    }


    // public void sendParse (String query, Map oids) {
    //     String name = "aaa";
    //     Parse msg = new Parse(name, query, oids);
    //     ByteBuffer buf = msg.encode();
    //     socket.write(buf);
    // }

    // public void sendPassword (String password) {
    //     PasswordMessage msg = new PasswordMessage(password);
    //     ByteBuffer buf = msg.encode();
    //     socket.write(buf);
    // }

    // public void sendFlush () {
    //     Flush msg = new Flush();
    //     ByteBuffer buf  = msg.encode();
    //     socket.write(buf);
    // }

    // public void sendQuery (String sql) {
    //     Query msg = new Query(sql);
    //     ByteBuffer buf  = msg.encode();
    //     socket.write(buf);
    // }

    // public Object readMessage () {

    //     ByteBuffer bbHeader = ByteBuffer.allocate(5);
    //     socket.read(bbHeader);

    //     byte tag = bbHeader.get();
    //     int len = bbHeader.getInt();

    //     ByteBuffer bbBody = ByteBuffer.allocate(len - 4);
    //     socket.read(bbBody);

    //     switch (tag) {

    //     case 1:
    //         return new DataRow(bbBody);

    //     case 2:
    //         return new FooBar(bbBody);

    //     default:
    //         throw new PGError("AAAAAA");
    //     }

    // }

    // public Result query (String sql) {
    //     sendQuery(sql);
    //     return interact();
    // }

    // public handleDataRow(Result result, DataRow msg) {

    // }



    // public Result interact(String phase) {
    //     Object msg;
    //     Result = new Result();

    //     while (true) {
    //         msg = readMessage();
    //         switch (msg) {

    //         case DataRow msg ->
    //             handleDataRow(result, msg);

    //         case RowDescription msg ->
    //             handleRowDescription(result, msg);

    //         }

    //     }

    // }




}
