/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.tkausl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Tobias
 */
public class RconConnection {
    private int requestId = 0;
    Socket socket;
    InputStream is;
    OutputStream os;
    boolean loggedIn = false;
    public RconConnection(String host, int port) throws IOException{
        socket = new Socket(host, port);
        is = socket.getInputStream();
        os = socket.getOutputStream();
    }
    
    public boolean login(String password){
        if(loggedIn) return true;
        Packet p = new Packet();
        p.requestId = ++requestId;
        p.type = 3;
        p.payload = password;
        send(p);
        Packet ret = read();
        if(ret.requestId == p.requestId) loggedIn = true;
        return loggedIn;
    }
    
    public String command(String cmd){
        if(!loggedIn) return null;
        Packet p = new Packet();
        p.type = 2;
        p.requestId = ++requestId;
        p.payload = cmd;
        send(p);
        Packet ret = read();
        return ret.payload;
    }
    
    
    private void send(Packet p){
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        int len = p.payload.length() + 2 + 4 + 4;
        int id = p.requestId;
        int type = p.type;
        os.write((len) & 0xFF);
        os.write((len >> 8) & 0xFF);
        os.write((len >> 16) & 0xFF);
        os.write((len >> 24) & 0xFF);
        os.write((id) & 0xFF);
        os.write((id >> 8) & 0xFF);
        os.write((id >> 16) & 0xFF);
        os.write((id >> 24) & 0xFF);
        os.write((type) & 0xFF);
        os.write((type >> 8) & 0xFF);
        os.write((type >> 16) & 0xFF);
        os.write((type >> 24) & 0xFF);
        try {
            os.write(p.payload.getBytes("UTF-8"));
        } catch (IOException ex) {
            Logger.getLogger(RconConnection.class.getName()).log(Level.SEVERE, null, ex);
        }
        os.write(0);
        os.write(0);
        try {
            this.os.write(os.toByteArray());
            this.os.flush();
        } catch (IOException ex) {}
    }
    
    private Packet read(){
        try {
            Packet p = new Packet();
            byte[] lb = new byte[4];
            int r = is.read(lb);
            int len = (lb[0] & 0xFF) + ((lb[1] &0xFF) << 8) + ((lb[2] &0xFF) << 16) + ((lb[3] &0xFF) << 24);
            byte[] data = new byte[len];
            is.read(data);
            p.requestId = (data[0] & 0xFF) + ((data[1] &0xFF) << 8) + ((data[2] &0xFF) << 16) + ((data[3] &0xFF) << 24);
            p.type = (data[4] & 0xFF) + ((data[5] &0xFF) << 8) + ((data[6] &0xFF) << 16) + ((data[7] &0xFF) << 24);
            int payLen = len - 10;
            p.payload = new String(Arrays.copyOfRange(data, 8, 8+payLen), "UTF-8");
            return p;

        } catch (IOException ex) {}
        return null;
    }
    
    public static class Packet {
        public int requestId;
        public int type;
        public String payload;
    }
}
