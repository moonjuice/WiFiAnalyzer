/*
 * WiFiAnalyzer
 * Copyright (C) 2019  VREM Software Development <VREMSoftwareDevelopment@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.vrem.wifianalyzer.wifi.scanner;

import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.os.Handler;
import android.text.format.DateFormat;
import android.util.Log;

import com.vrem.wifianalyzer.settings.Settings;
import com.vrem.wifianalyzer.wifi.model.WiFiData;
import com.vrem.wifianalyzer.wifi.model.WiFiDetail;

import org.apache.commons.collections4.Closure;
import org.apache.commons.collections4.IterableUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;

class Scanner implements ScannerService {
    private final List<UpdateNotifier> updateNotifiers;
    private final Settings settings;
    private Transformer transformer;
    private WiFiData wiFiData;
    private WiFiManagerWrapper wiFiManagerWrapper;
    private Cache cache;
    private PeriodicScan periodicScan;
    private JSONArray mResult = new JSONArray();

    Scanner(@NonNull WiFiManagerWrapper wiFiManagerWrapper, @NonNull Handler handler, @NonNull Settings settings) {
        this.updateNotifiers = new ArrayList<>();
        this.wiFiManagerWrapper = wiFiManagerWrapper;
        this.settings = settings;
        this.wiFiData = WiFiData.EMPTY;
        this.setTransformer(new Transformer());
        this.setCache(new Cache());
        this.periodicScan = new PeriodicScan(this, handler, settings);
    }

    @Override
    public void update() {
        wiFiManagerWrapper.enableWiFi();
        scanResults();
        wiFiData = transformer.transformToWiFiData(cache.getScanResults(), cache.getWifiInfo());
        IterableUtils.forEach(updateNotifiers, new UpdateClosure());
        try {
            Log.i("moon", "number:" + wiFiData.getWiFiDetails().size() + "-------------------------");
            for (WiFiDetail detail : wiFiData.getWiFiDetails()) {
                JSONObject object = new JSONObject();
                Log.i("moon", "SSID:" + detail.getSSID());
                    object.put("SSID", detail.getSSID());
                object.put("level", detail.getWiFiSignal().getLevel());
                object.put("distance", detail.getWiFiSignal().getDistance());
                object.put("actualTimestamp", getDate(detail.getTimeStamp() /1000));
                mResult.put(object);
            }
            Log.i("moon", "----------------------------------");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    @NonNull
    public WiFiData getWiFiData() {
        return wiFiData;
    }

    @Override
    public void register(@NonNull UpdateNotifier updateNotifier) {
        updateNotifiers.add(updateNotifier);
    }

    @Override
    public void unregister(@NonNull UpdateNotifier updateNotifier) {
        updateNotifiers.remove(updateNotifier);
    }

    @Override
    public void pause() {
        periodicScan.stop();
    }

    @Override
    public boolean isRunning() {
        return periodicScan.isRunning();
    }

    @Override
    public void resume() {
        mResult = new JSONArray();
        periodicScan.start();
    }

    @Override
    public void stop() {
        if (settings.isWiFiOffOnExit()) {
            wiFiManagerWrapper.disableWiFi();
        }
    }

    @Override
    public JSONArray getWiFiDataAsJson() {
        return mResult;
    }

    @NonNull
    PeriodicScan getPeriodicScan() {
        return periodicScan;
    }

    void setPeriodicScan(@NonNull PeriodicScan periodicScan) {
        this.periodicScan = periodicScan;
    }

    void setCache(@NonNull Cache cache) {
        this.cache = cache;
    }

    void setTransformer(@NonNull Transformer transformer) {
        this.transformer = transformer;
    }

    @NonNull
    List<UpdateNotifier> getUpdateNotifiers() {
        return updateNotifiers;
    }

    private void scanResults() {
        try {
            if (wiFiManagerWrapper.startScan()) {
                List<ScanResult> scanResults = wiFiManagerWrapper.scanResults();
                WifiInfo wifiInfo = wiFiManagerWrapper.wiFiInfo();
                cache.add(scanResults, wifiInfo);
            }
        } catch (Exception e) {
            // critical error: do not die
        }
    }

    private class UpdateClosure implements Closure<UpdateNotifier> {
        @Override
        public void execute(UpdateNotifier updateNotifier) {
            updateNotifier.update(wiFiData);
        }
    }

    private String getDate(long time) {
        Calendar cal = Calendar.getInstance(Locale.TAIWAN);
        cal.setTimeInMillis(time * 1000);
        String date = DateFormat.format("yyyy-MM-dd HH:mm:ss", cal).toString();
        return date;
    }
}
