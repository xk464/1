package com.example.dailycheck.net;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.example.dailycheck.data.AppDatabase;
import com.example.dailycheck.data.entity.Task;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 局域网广播管理：通过 UDP 广播在 WiFi 局域网内分发/接收任务
 * 协议：JSON 文本，{ "type":"tasks", "date":"yyyy-MM-dd", "items":[...] }
 * 端口固定 45678
 */
public class UdpBroadcastManager {

    private static final String TAG = "UdpBroadcast";
    public static final int PORT = 45678;
    public static final String MAGIC = "DailyCheck/1";

    private final Context context;
    private final AtomicBoolean running = new AtomicBoolean(false);

    /** 接收到任务时的回调 */
    public interface Listener {
        void onReceived(String date, List<String> taskTitles);
    }

    public UdpBroadcastManager(Context context) {
        this.context = context.getApplicationContext();
    }

    /** 停止监听（在 Fragment 销毁时调用） */
    public void stop() {
        running.set(false);
    }

    /** 广播当天任务到局域网，返回发送条数 */
    public int broadcastToday() {
        String date = com.example.dailycheck.util.DateUtils.today();
        List<Task> tasks = AppDatabase.getInstance(context).taskDao().getTasksByDate(date);
        List<String> titles = new ArrayList<>();
        for (Task t : tasks) {
            titles.add(t.title);
        }
        return send(date, titles);
    }

    /** 发送指定任务列表 */
    public int send(String date, List<String> titles) {
        try {
            JSONObject root = new JSONObject();
            root.put("magic", MAGIC);
            root.put("type", "tasks");
            root.put("date", date);
            JSONArray arr = new JSONArray();
            for (String t : titles) arr.put(t);
            root.put("items", arr);

            byte[] data = root.toString().getBytes(StandardCharsets.UTF_8);
            DatagramSocket socket = new DatagramSocket();
            socket.setBroadcast(true);
            InetAddress addr = InetAddress.getByName("255.255.255.255");
            socket.send(new DatagramPacket(data, data.length, addr, PORT));
            socket.close();
            return titles.size();
        } catch (Exception e) {
            Log.e(TAG, "send failed", e);
            return 0;
        }
    }

    /**
     * 在后台线程开始监听局域网广播。需调用 acquireMulticastLock 已确保可接收广播。
     * 调用 stop() 可停止监听。
     */
    public void startListening(final Listener listener) {
        running.set(true);
        final WifiManager.MulticastLock lock = acquireMulticastLock();
        new Thread(() -> {
            try (DatagramSocket socket = new DatagramSocket(PORT)) {
                socket.setSoTimeout(2000);
                byte[] buf = new byte[8192];
                while (running.get()) {
                    try {
                        DatagramPacket packet = new DatagramPacket(buf, buf.length);
                        socket.receive(packet);
                        String text = new String(packet.getData(), 0, packet.getLength(),
                                StandardCharsets.UTF_8);
                        JSONObject root = new JSONObject(text);
                        if (!MAGIC.equals(root.optString("magic"))) continue;
                        if (!"tasks".equals(root.optString("type"))) continue;
                        String date = root.getString("date");
                        JSONArray items = root.getJSONArray("items");
                        List<String> titles = new ArrayList<>();
                        for (int i = 0; i < items.length(); i++) {
                            titles.add(items.getString(i));
                        }
                        if (listener != null) listener.onReceived(date, titles);
                    } catch (java.net.SocketTimeoutException ignore) {
                        // 超时后继续循环，检查 running
                    } catch (Exception e) {
                        Log.e(TAG, "recv loop err", e);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "listen init failed", e);
            } finally {
                if (lock != null) lock.release();
            }
        }, "udp-listen").start();
    }

    /** 申请 WiFi 多播锁，确保设备休眠时也能收到广播 */
    private WifiManager.MulticastLock acquireMulticastLock() {
        try {
            WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            if (wifi == null) return null;
            WifiManager.MulticastLock lock = wifi.createMulticastLock("daily_check_udp");
            lock.acquire();
            return lock;
        } catch (Exception e) {
            return null;
        }
    }
}
