package com.slm.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.util.Log;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class SLMNetwork extends CordovaPlugin {

    private static final String TAG = "SLMNetwork";
    private ConnectivityManager.NetworkCallback networkCallback;
    private CallbackContext monitorCallbackContext;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        switch (action) {
            case "getConnectionInfo":
                getConnectionInfo(callbackContext);
                return true;
            case "startMonitoring":
                startMonitoring(callbackContext);
                return true;
            case "stopMonitoring":
                stopMonitoring(callbackContext);
                return true;
            default:
                return false;
        }
    }

    // ============================================
    // getConnectionInfo
    // ============================================

    private void getConnectionInfo(CallbackContext callbackContext) {
        cordova.getThreadPool().execute(() -> {
            try {
                JSONObject info = buildNetworkInfo();
                callbackContext.success(info);
            } catch (Exception e) {
                Log.e(TAG, "getConnectionInfo error: " + e.getMessage());
                callbackContext.error("Error obteniendo info de red: " + e.getMessage());
            }
        });
    }

    // ============================================
    // startMonitoring
    // ============================================

    private void startMonitoring(CallbackContext callbackContext) {
        // Detener monitor anterior si existe
        stopCurrentMonitor();

        monitorCallbackContext = callbackContext;

        try {
            Context context = cordova.getActivity().getApplicationContext();
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

            if (cm == null) {
                callbackContext.error("ConnectivityManager no disponible");
                return;
            }

            networkCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onCapabilitiesChanged(Network network, NetworkCapabilities capabilities) {
                    sendNetworkUpdate();
                }

                @Override
                public void onLost(Network network) {
                    sendNetworkUpdate();
                }

                @Override
                public void onAvailable(Network network) {
                    sendNetworkUpdate();
                }
            };

            NetworkRequest request = new NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build();

            cm.registerNetworkCallback(request, networkCallback);

            // Enviar estado inicial
            sendNetworkUpdate();

        } catch (Exception e) {
            Log.e(TAG, "startMonitoring error: " + e.getMessage());
            callbackContext.error("Error iniciando monitoreo: " + e.getMessage());
        }
    }

    // ============================================
    // stopMonitoring
    // ============================================

    private void stopMonitoring(CallbackContext callbackContext) {
        try {
            stopCurrentMonitor();

            JSONObject info = new JSONObject();
            info.put("stopped", true);
            callbackContext.success(info);
        } catch (Exception e) {
            Log.e(TAG, "stopMonitoring error: " + e.getMessage());
            callbackContext.error("Error deteniendo monitoreo: " + e.getMessage());
        }
    }

    // ============================================
    // Helpers
    // ============================================

    private void sendNetworkUpdate() {
        if (monitorCallbackContext == null) return;

        try {
            JSONObject info = buildNetworkInfo();
            PluginResult result = new PluginResult(PluginResult.Status.OK, info);
            result.setKeepCallback(true);
            monitorCallbackContext.sendPluginResult(result);
        } catch (Exception e) {
            Log.e(TAG, "sendNetworkUpdate error: " + e.getMessage());
        }
    }

    private JSONObject buildNetworkInfo() throws JSONException {
        Context context = cordova.getActivity().getApplicationContext();
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        String type = "none";
        boolean isConnected = false;
        boolean isExpensive = false;

        JSONObject details = new JSONObject();
        details.put("isConstrained", false);
        details.put("supportsDNS", false);
        details.put("supportsIPv4", false);
        details.put("supportsIPv6", false);

        if (cm != null) {
            Network activeNetwork = cm.getActiveNetwork();
            if (activeNetwork != null) {
                NetworkCapabilities caps = cm.getNetworkCapabilities(activeNetwork);
                if (caps != null) {
                    isConnected = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);

                    if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                        type = "wifi";
                    } else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                        type = "cellular";
                    } else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                        type = "ethernet";
                    } else {
                        type = "other";
                    }

                    isExpensive = !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED);

                    details.put("isConstrained", false);
                    details.put("supportsDNS", isConnected);
                    details.put("supportsIPv4", isConnected);
                    details.put("supportsIPv6", isConnected);
                }
            }
        }

        JSONObject info = new JSONObject();
        info.put("type", type);
        info.put("isConnected", isConnected);
        info.put("isExpensive", isExpensive);
        info.put("details", details);

        return info;
    }

    private void stopCurrentMonitor() {
        if (networkCallback != null) {
            try {
                Context context = cordova.getActivity().getApplicationContext();
                ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                if (cm != null) {
                    cm.unregisterNetworkCallback(networkCallback);
                }
            } catch (Exception e) {
                Log.e(TAG, "stopCurrentMonitor error: " + e.getMessage());
            }
            networkCallback = null;
        }
        monitorCallbackContext = null;
    }

    @Override
    public void onDestroy() {
        stopCurrentMonitor();
        super.onDestroy();
    }

    @Override
    public void onReset() {
        stopCurrentMonitor();
        super.onReset();
    }
}
