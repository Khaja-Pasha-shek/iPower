package com.example.pipower;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.StatFs;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.Task;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import me.tankery.lib.circularseekbar.CircularSeekBar;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class MainActivity extends AppCompatActivity {

    private boolean discoveryActive = false;
    Handler nsdHandler = new Handler();
    static ArrayList<NsdServiceInfo> serviceInfos = new ArrayList<>();
    private NsdManager nsdManager;
    int delay = 10000;
    private NsdManager.DiscoveryListener discoveryListener;
    public WifiManager wifiManager;
    public WifiInfo wifiInfo;

    boolean wifiState = false;
    private NetworkState networkState = new NetworkState();

    private LocationService locationService = new LocationService();

    private boolean connected = false;
    private static final int location_settings = 0x1;
    public static String ssid, ipAddress = "";
    private TextView textView, conn_status, ip, cpuText, ramText, tempText;
    private ImageView imageView, bluetoothView, uartView, wifiView, batteryView;
    private LinearLayout layout;
    public OkHttpClient client;
    private final AtomicBoolean ipFound = new AtomicBoolean(false);
    EchoWebSocketListener listener;
    WebSocket ws;
    Button closeButton;
    ShapeableImageView powerButton;
    private LineChart cpuChart, ramChart, tempChart;
    private LineDataSet cpudataSet, ramdataSet, tempdataSet;
    private LineData cpulineData, ramLineData, tempLineData;
    private int timeIndex = 0;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable runnable;
    CircularSeekBar cpuSeekBar, ramSeekBar, tempSeekBar;
    private PieChart diskPieChart;
    String TAG = "NSD";

    Gson gson = new Gson();
    String _Ack = " ", _wifi = " ", _ssid = " ", _uart = " ", _bluetooth = " ";
    int _cpu = 0, _ram = 0, _temp = 0, _battery = 0, _charging = 0;
    long _diskFree = 0, _diskUsed = 0;
    boolean power = false;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        layout = findViewById(R.id.layout1);

        nsdManager = (NsdManager) getSystemService(Context.NSD_SERVICE);

        cpuChart = findViewById(R.id.cpuChart);
        ramChart = findViewById(R.id.ramChart);
        tempChart = findViewById(R.id.tempChart);
        diskPieChart = findViewById(R.id.diskPieChart);

        cpuSeekBar = findViewById(R.id.cpuSeekBar);
        ramSeekBar = findViewById(R.id.ramSeekBar);
        tempSeekBar = findViewById(R.id.tempSeekBar);

        cpuText = findViewById(R.id.cpu_text);
        ramText = findViewById(R.id.ram_text);
        tempText = findViewById(R.id.temp_textview);

        textView = findViewById(R.id.wifi);
        imageView = findViewById(R.id.imageView);
        wifiView = findViewById(R.id.wifiView);
        bluetoothView = findViewById(R.id.bluetoothView);
        uartView = findViewById(R.id.uartView);
        batteryView = findViewById(R.id.batteryView);

        conn_status = findViewById(R.id.conn_status);
        ip = findViewById(R.id.ip);
        closeButton = findViewById(R.id.close);

        cpuSeekBar.setOnTouchListener((v, event) -> true); // consume all touch events
        ramSeekBar.setOnTouchListener((v, event) -> true);
        tempSeekBar.setOnTouchListener((v, event) -> true);

        startCpuUpdate();
        showDiskUsage();

        setupChart();
        setRamChart();
        setTempChart();
        startUpdating();

        //Websocket Call
        client = new OkHttpClient.Builder()
                .readTimeout(60000, TimeUnit.MILLISECONDS) // Increased to 10 seconds
                .connectTimeout(60000, TimeUnit.MILLISECONDS) // Increased to 10 seconds
                .pingInterval(3, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();
        closeButton.setOnClickListener(V -> listener.closeConnection(ws));

        ShapeableImageView roundedImageButton = findViewById(R.id.manageWifiImageButton);
        powerButton = findViewById(R.id.powerImageButton);
        powerButton.setOnClickListener(v -> {
            if (power) {
                powerButton.setImageResource(R.drawable.power_on);
                power = !power;

                if (ws != null) {
                    CommandMessage cmd = new CommandMessage(1, "on");
                    String json = gson.toJson(cmd);
                    boolean sent = ws.send(json); // returns true if send is successful
                    Log.d("WebSocket", "Sent: " + json + " Status: " + sent);
                }
            } else {
                powerButton.setImageResource(R.drawable.power_off);
                if (ws != null) {
                    CommandMessage cmd = new CommandMessage(1, "off");
                    String json = gson.toJson(cmd);
                    boolean sent = ws.send(json); // returns true if send is successful
                    Log.d("WebSocket", "Sent: " + json + " Status: " + sent);
                }
                power = !power;
            }

        });

        roundedImageButton.setOnClickListener(v -> {
            // Handle the click event here
            Toast.makeText(this, "Hi", Toast.LENGTH_SHORT).show();
        });
    }

    private void showDiskUsage() {
//        long[] usage = getDiskUsage(); // [used, free]
        int totalDisk = 100;
        float used = _diskUsed;
        float free = totalDisk - used;

        List<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry(used, "Used"));
        entries.add(new PieEntry(free, "Free"));

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setValueTextSize(14f);

        PieData data = new PieData(dataSet);

        diskPieChart.setData(data);
        diskPieChart.setUsePercentValues(true);
        diskPieChart.setDrawHoleEnabled(true);
        diskPieChart.setHoleRadius(50f);
        diskPieChart.setTransparentCircleRadius(55f);
        diskPieChart.setHoleColor(getResources().getColor(android.R.color.transparent));
        diskPieChart.setCenterText("Disk Usage");
        diskPieChart.setCenterTextSize(18f);
        diskPieChart.getDescription().setEnabled(false);
        diskPieChart.getLegend().setEnabled(true);

        diskPieChart.invalidate(); // refresh
    }


    private void startCpuUpdate() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                float simulatedCpu = getFakeCpuUsage();  // Replace with actual CPU logic if needed

                addCpuValue(_cpu);
                addRamValue(_ram);
                addTempValue(_temp);

                //setCpu Usage
                cpuSeekBar.setProgress(_cpu);
                cpuText.setText(_cpu + "%");

                //setRamUsage
                ramSeekBar.setProgress(_ram);
                ramText.setText(_ram + "%");

                //setTempUsage
                tempSeekBar.setProgress(_temp);
                tempText.setText(_temp + " C");
                showDiskUsage();
                handler.postDelayed(this, 1000); // repeat every second

            }
        }, 1000);
    }

    private void setupChart() {
        ArrayList<Entry> initialEntries = new ArrayList<>();
        cpudataSet = new LineDataSet(initialEntries, "CPU Usage");
        cpudataSet.setColor(Color.parseColor("#FF5722"));
        cpudataSet.setDrawFilled(true);
        cpudataSet.setDrawCircles(false);
        cpudataSet.setLineWidth(2f);
        cpudataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        Drawable drawable = ContextCompat.getDrawable(this, R.drawable.cpu_gradient);
        cpudataSet.setFillDrawable(drawable);

        cpulineData = new LineData(cpudataSet);
        cpulineData.setDrawValues(false);

        cpuChart.setData(cpulineData);
        cpuChart.getDescription().setEnabled(false);
        cpuChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        cpuChart.getXAxis().setDrawGridLines(false);
        cpuChart.getAxisLeft().setDrawGridLines(false);
        cpuChart.getAxisRight().setEnabled(false);
        cpuChart.getAxisLeft().setAxisMinimum(0f);
        cpuChart.getAxisLeft().setAxisMaximum(100f);
        cpuChart.setTouchEnabled(false);
        cpuChart.setDragEnabled(true);
        cpuChart.setScaleEnabled(false);
        cpuChart.setViewPortOffsets(60, 20, 20, 80);
    }

    private void setRamChart() {
        ArrayList<Entry> initialEntries = new ArrayList<>();
        ramdataSet = new LineDataSet(initialEntries, "RAM Usage");
        ramdataSet.setColor(Color.parseColor("#2196F3"));
        ramdataSet.setDrawFilled(true);
        ramdataSet.setDrawCircles(false);
        ramdataSet.setLineWidth(2f);
        ramdataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        Drawable drawable = ContextCompat.getDrawable(this, R.drawable.ram_gradient);
        ramdataSet.setFillDrawable(drawable);

        ramLineData = new LineData(ramdataSet);
        ramLineData.setDrawValues(false);

        ramChart.setData(ramLineData);
        ramChart.getDescription().setEnabled(false);
        ramChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        ramChart.getXAxis().setDrawGridLines(false);
        ramChart.getAxisLeft().setDrawGridLines(false);
        ramChart.getAxisRight().setEnabled(false);
        ramChart.getAxisLeft().setAxisMinimum(0f);
        ramChart.getAxisLeft().setAxisMaximum(100f);
        ramChart.setTouchEnabled(false);
        ramChart.setDragEnabled(true);
        ramChart.setScaleEnabled(false);
        ramChart.setViewPortOffsets(60, 20, 20, 80);
    }

    private void setTempChart() {
        ArrayList<Entry> initialEntries = new ArrayList<>();
        tempdataSet = new LineDataSet(initialEntries, "Temperature Index");
        tempdataSet.setColor(Color.parseColor("#FF9800"));
        tempdataSet.setDrawFilled(true);
        tempdataSet.setDrawCircles(false);
        tempdataSet.setLineWidth(2f);

        tempdataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        Drawable drawable = ContextCompat.getDrawable(this, R.drawable.temp_gradient);

        tempdataSet.setFillDrawable(drawable);
        tempLineData = new LineData(tempdataSet);
        tempLineData.setDrawValues(false);

        tempChart.setData(tempLineData);
        tempChart.getDescription().setEnabled(false);
        tempChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        tempChart.getXAxis().setDrawGridLines(false);
        tempChart.getAxisLeft().setDrawGridLines(false);
        tempChart.getAxisRight().setEnabled(false);
        tempChart.getAxisLeft().setAxisMinimum(0f);
        tempChart.getAxisLeft().setAxisMaximum(100f);
        tempChart.setTouchEnabled(false);
        tempChart.setDragEnabled(true);
        tempChart.setScaleEnabled(false);
        tempChart.setViewPortOffsets(60, 20, 20, 80);
    }

    private void startUpdating() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!connected) {
                    bluetoothView.setImageResource(R.drawable.bluetooth_disconnected);
                    uartView.setImageResource(R.drawable.cable_disconnected);
                    wifiView.setImageResource(R.drawable.no_wifi);
                    batteryView.setImageResource(R.drawable.battery_empty);
                    powerButton.setImageResource(R.drawable.power_off);
                } else {
                    if (Objects.equals(_Ack, "ok")) {
                        runOnUiThread(() -> {
                            if (power) {
                                powerButton.setImageResource(R.drawable.power_on);
                            } else {
                                powerButton.setImageResource(R.drawable.power_off);
                            }
                            if (Objects.equals(_bluetooth, "active")) {
                                bluetoothView.setImageResource(R.drawable.bluetooth_connected);
                            } else {
                                bluetoothView.setImageResource(R.drawable.bluetooth_disconnected);
                            }
                            if (Objects.equals(_wifi, "active")) {
                                wifiView.setImageResource(R.drawable.wifi);
                            } else {
                                wifiView.setImageResource(R.drawable.no_wifi);
                            }
                            if (Objects.equals(_uart, "active")) {
                                uartView.setImageResource(R.drawable.cable_connected);
                            } else {
                                uartView.setImageResource(R.drawable.cable_disconnected);
                            }

                            if (_charging == 0) {
                                if (_battery > 0 && _battery < 30) {
                                    batteryView.setImageResource(R.drawable.battery_25);
                                } else if (_battery > 30 && _battery < 60) {
                                    batteryView.setImageResource(R.drawable.battery_50);
                                } else if (_battery > 60 && _battery < 90) {
                                    batteryView.setImageResource(R.drawable.battery_75);
                                } else if (_battery > 90) {
                                    batteryView.setImageResource(R.drawable.battery_full);
                                }
                            }
                            if (_charging == 1) {
                                batteryView.setImageResource(R.drawable.battery_charging);
                            }
                        });
                    }
                }
                handler.postDelayed(this, 1000); // repeat every 1 second
            }

        }, 1000);
    }

    private void addCpuValue(float cpuUsage) {
        cpulineData.addEntry(new Entry(timeIndex++, cpuUsage), 0);

        // Remove old entries to keep the graph moving
        // show latest 30 values
        int MAX_VISIBLE_POINTS = 30;
        if (cpudataSet.getEntryCount() > MAX_VISIBLE_POINTS) {
            cpudataSet.removeFirst();
            for (Entry e : cpudataSet.getValues()) {
                e.setX(e.getX() - 1); // shift X-axis values left
            }
        }

        cpulineData.notifyDataChanged();
        cpuChart.notifyDataSetChanged();
        cpuChart.setVisibleXRangeMaximum(MAX_VISIBLE_POINTS);
        cpuChart.moveViewToX(timeIndex);
    }

    private void addRamValue(float ramUsage) {
        ramdataSet.addEntry(new Entry(timeIndex++, ramUsage));

        int MAX_VISIBLE_POINTS = 30;
        if (ramdataSet.getEntryCount() > MAX_VISIBLE_POINTS) {
            ramdataSet.removeFirst();
            for (Entry e : ramdataSet.getValues()) {
                e.setX(e.getX() - 1); // shift X-axis values left
            }
        }

        ramLineData.notifyDataChanged();
        ramChart.notifyDataSetChanged();
        ramChart.setVisibleXRangeMaximum(MAX_VISIBLE_POINTS);
        ramChart.moveViewToX(timeIndex);
    }

    private void addTempValue(float Usage) {
        tempLineData.addEntry(new Entry(timeIndex++, Usage), 0);

        // Remove old entries to keep the graph moving
        // show latest 30 values
        int MAX_VISIBLE_POINTS = 30;
        if (tempdataSet.getEntryCount() > MAX_VISIBLE_POINTS) {
            tempdataSet.removeFirst();
            for (Entry e : tempdataSet.getValues()) {
                e.setX(e.getX() - 1); // shift X-axis values left
            }
        }

        tempLineData.notifyDataChanged();
        tempChart.notifyDataSetChanged();
        tempChart.setVisibleXRangeMaximum(MAX_VISIBLE_POINTS);
        tempChart.moveViewToX(timeIndex);
    }

    private float getFakeCpuUsage() {
        return new Random().nextInt(100); // Simulate 0-100% CPU usage
    }

    private void deviceActivity(boolean state) {
        runOnUiThread(() -> {
            if (state) {
                imageView.setImageResource(R.drawable.online);
                conn_status.setText("online");
            } else {
                imageView.setImageResource(R.drawable.offline);
                conn_status.setText("offline");
            }
            if (ipFound.get()) {
                ip.setText(ipAddress);
            } else {
                ipFound.set(false);
                ip.setText("Searching...");
            }
        });
    }

    private void start() {
        String url = "ws:/" + ipAddress + ":81";
        Log.i("URL", url);
        Request request = new Request.Builder().url(url).build();
        listener = new EchoWebSocketListener();
        ws = client.newWebSocket(request, listener);
    }

    private final class EchoWebSocketListener extends WebSocketListener {
        private static final int NORMAL_CLOSURE_STATUS = 1000;

        public void closeConnection(WebSocket webSocket) {
            connected = false;
            deviceActivity(false);
            webSocket.close(NORMAL_CLOSURE_STATUS, "Goodbye !");
        }

        @Override
        public void onOpen(@NonNull WebSocket webSocket, @NonNull Response response) {
            connected = true;
            deviceActivity(true);
            Log.i("WebSocket", "Connection opened"); // Log the connection
        }

        public void submit(WebSocket webSocket, int value) {
            webSocket.send(String.valueOf(value));
        }

        @Override
        public void onMessage(@NonNull WebSocket webSocket, String text) {
            Log.i("Received:", text);
            if(!text.isEmpty()){
                Gson gson = new Gson();
                SystemResponse status = gson.fromJson(text, SystemResponse.class);


                _Ack = "ok";
                _wifi = status.data.wifi;
                _battery = status.data.battery;
                _charging = status.data.charging;
                _bluetooth = status.data.bluetooth;
                _uart = status.data.uart;
                _ssid = status.data.ssid;
                _cpu = status.data.cpu;
                _ram = status.data.ram;
                _temp = status.data.temp;
                _diskUsed = status.data.storage;

            }

            /*if (!text.isEmpty()) {
                JsonResponse jsonResponse = gson.fromJson(text, JsonResponse.class);

                if (jsonResponse != null) {
                    if (Objects.equals(jsonResponse.getPower(), "on")) {
                        power = true;
                        if (jsonResponse.getData() != null) {
                            JsonResponse.Data data = jsonResponse.getData();
                            Log.i("Data: ", data.toString());

                            _Ack = jsonResponse.getAck();
                            _wifi = data.getWifi();
                            _battery = data.getBattery();
                            _charging = data.getCharging();
                            _bluetooth = data.getBluetooth();
                            _uart = data.getUart();
                            _ssid = data.getSsid();
                            _cpu = data.getCpu();
                            _ram = data.getRam();
                            _temp = data.getTemp();
                            _diskUsed = data.getDiskUsed();
                            _diskFree = data.getDiskFree();
                        }

                    } else {
                        power = false;
                        _wifi = "inactive";
                        _battery = 0;
                        _charging = 0;
                        _bluetooth = "inactive";
                        _uart = "inactive";
                        _ssid = " ";
                        _cpu = 0;
                        _ram = 0;
                        _temp = 0;
                        _diskUsed = 0;
                        _diskFree = 0;
                    }

                } else {
                    Log.e("ERROR", "Failed to parse JSON");
                }

            }*/
        }

        @Override
        public void onMessage(WebSocket webSocket, ByteString bytes) {
//            Log.i("Receiving", "Receiving bytes : " + bytes.hex());
        }

        @Override
        public void onClosing(WebSocket webSocket, int code, @NonNull String reason) {
            webSocket.close(NORMAL_CLOSURE_STATUS, null);
            connected = false;
            deviceActivity(false);
            Log.w("Closing: ", reason);
        }

        @Override
        public void onFailure(@NonNull WebSocket webSocket, Throwable t, Response response) {
            connected = false;
            deviceActivity(connected);
            Log.e("WS: onFailure:", Objects.requireNonNull(Objects.requireNonNull(Objects.requireNonNull(Objects.requireNonNull(t.getMessage())))));
        }
    }

    void startScan() {

        discoveryListener = new NsdManager.DiscoveryListener() {

            @Override
            public void onStartDiscoveryFailed(String s, int i) {
                stopScan();
//                Log.d("discoveryListener", "onStart discovery failed");
            }

            @Override
            public void onStopDiscoveryFailed(String s, int i) {
                nsdManager.stopServiceDiscovery(this);
            }

            @Override
            public void onDiscoveryStarted(String s) {
//                Log.d("discoveryListener", "Service discovery started");
            }

            @Override
            public void onDiscoveryStopped(String s) {
//                Log.d("discoveryListener", "Service discovery stopped");
            }

            @Override
            public void onServiceFound(NsdServiceInfo nsdServiceInfo) {

                nsdManager.resolveService(nsdServiceInfo, new NsdManager.ResolveListener() {
                    @Override
                    public void onServiceResolved(NsdServiceInfo nsdServiceInfo) {
                        Log.i("service ", "Name = " + nsdServiceInfo.getServiceName());
                        Log.i("service ", "host = " + nsdServiceInfo.getHost());
                        Log.i("service ", "port = " + nsdServiceInfo.getPort());

                        if (!connected) {
                            Log.d(TAG, "IpStatus:" + ipFound.get());
                            if (Objects.equals(nsdServiceInfo.getServiceName(), "ipower")) {
                                if (serviceInfos.isEmpty()) {
                                    serviceInfos.add(nsdServiceInfo);
                                }

                                Log.d(TAG, "Size=" + serviceInfos.size());
                                ipFound.set(true);
                                ipAddress = String.valueOf(nsdServiceInfo.getHost());
                                start();
                            }
                        }

                        for (NsdServiceInfo service : serviceInfos) {
                            Log.d(TAG, "onServiceResolved: " + service.getServiceName());
                            Log.d(TAG, "onServiceResolved: " + service.getServiceType());
                            Log.d(TAG, "onServiceResolved: " + service.getHost());
                            Log.d(TAG, "onServiceResolved: " + service.getPort());
                        }

                        Log.i("service ", "Name = " + nsdServiceInfo.getServiceName());
                        Log.i("service ", "host = " + nsdServiceInfo.getHost());
                        Log.i("service ", "port = " + nsdServiceInfo.getPort());


                    }

                    @Override
                    public void onResolveFailed(NsdServiceInfo nsdServiceInfo, int i) {

                    }
                });
            }

            @Override
            public void onServiceLost(NsdServiceInfo nsdServiceInfo) {
                Log.d("Service Lost", nsdServiceInfo.getServiceName());
                ipFound.set(false);
                serviceInfos.clear();
                stopScan();
            }
        };

        nsdManager.discoverServices("_http._tcp", NsdManager.PROTOCOL_DNS_SD, discoveryListener);
        discoveryActive = true;
    }


    void stopScan() {
        if (discoveryActive) {
            nsdManager.stopServiceDiscovery(discoveryListener);
            discoveryActive = false;
        }
    }


    public class LocationService extends BroadcastReceiver {
        LocationManager locationManager;
        LocationRequest locationRequest;

        boolean gps = false;

        @Override
        public void onReceive(Context context, Intent intent) {
            locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            gps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

            LocationSettingsRequest.Builder locBuilder = new LocationSettingsRequest.Builder();
            if (gps) {
                Log.i("Location:", "enabled");
            }

            if (!gps) {
                Log.i("Location:", "Not enabled");
                //create location Request
                locationRequest = LocationRequest.create();
                locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                locationRequest.setInterval(10000);
                locationRequest.setFastestInterval(10000 / 2);

                locBuilder.addLocationRequest(locationRequest);
                locBuilder.setAlwaysShow(true);
                SettingsClient client = LocationServices.getSettingsClient(context);

                Task<LocationSettingsResponse> result = client.checkLocationSettings(locBuilder.build());
                result.addOnSuccessListener((Activity) context, locationSettingsResponse -> Toast.makeText(context, "GPS on", Toast.LENGTH_SHORT).show());

                result.addOnFailureListener((Activity) context, e -> {
                    Toast.makeText(context, "GPS off", Toast.LENGTH_SHORT).show();
                    if (e instanceof ResolvableApiException) {
                        ResolvableApiException resolvableApiException = (ResolvableApiException) e;
                        try {
                            resolvableApiException.startResolutionForResult((Activity) context, location_settings);
                        } catch (IntentSender.SendIntentException ex) {
                            ex.printStackTrace();
                        }
                    }
                });
            }
        }
    }

    public class NetworkState extends BroadcastReceiver {
        ConnectivityManager connMgr;
        NetworkInfo networkInfo;

        @SuppressLint("SetTextI18n")
        @Override
        public void onReceive(Context context, Intent intent) {
            connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            networkInfo = connMgr.getActiveNetworkInfo();

            if (networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
//                Log.i("Receiver", "WiFi");

                //Register WIFI manager
                wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
                wifiInfo = wifiManager.getConnectionInfo();

                if (wifiInfo.getSupplicantState() == SupplicantState.COMPLETED) {
//                    ip = Formatter.formatIpAddress(wifiInfo.getIpAddress());
                    ssid = wifiInfo.getSSID();
                    wifiState = true;
//                    Log.i("Receiver", ssid);
                }

                if (layout.getVisibility() == View.VISIBLE) {
                    layout.setVisibility(View.INVISIBLE);
                }

            } else if (networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
                wifiState = false;
//                Log.i("Receiver", "Mobile");
                layout.setVisibility(View.VISIBLE);
                textView.setText("Not connected to Wifi");
            } else {
                wifiState = false;
//                Log.i("Receiver", "Not Connected to Any Network");
                layout.setVisibility(View.VISIBLE);
                textView.setText("Not connected");
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        }

        //Register WiFi/Mobile Connection State
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        IntentFilter locationFilter = new IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION);

        locationService = new LocationService();
        networkState = new NetworkState();

        this.registerReceiver(networkState, filter);
        this.registerReceiver(locationService, locationFilter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        nsdHandler.postDelayed(runnable = () -> {
            nsdHandler.postDelayed(runnable, delay);
            if (discoveryActive) {
                stopScan();
            }
            startScan();
        }, delay);
    }

    @Override
    protected void onPause() {
        super.onPause();
        nsdHandler.removeCallbacks(runnable);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (networkState != null) {
            this.unregisterReceiver(networkState);
        }
        if (locationService != null) {
            this.unregisterReceiver(locationService);
        }
        if (nsdHandler != null) {
            nsdHandler.removeCallbacks(runnable);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        stopScan();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (nsdHandler != null) {
            nsdHandler.removeCallbacks(runnable);
        }
    }
}