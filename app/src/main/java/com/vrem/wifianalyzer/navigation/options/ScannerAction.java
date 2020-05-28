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

package com.vrem.wifianalyzer.navigation.options;

import android.media.MediaScannerConnection;
import android.os.Environment;
import android.util.Log;

import com.vrem.wifianalyzer.MainContext;
import com.vrem.wifianalyzer.wifi.model.WiFiData;
import com.vrem.wifianalyzer.wifi.scanner.ScannerService;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

class ScannerAction implements Action {
    @Override
    public void execute() {
        ScannerService scannerService = MainContext.INSTANCE.getScannerService();
        if (scannerService.isRunning()) {
            scannerService.pause();
            saveData(scannerService.getWiFiDataAsJson());
        } else {
            scannerService.resume();
        }
    }

    private void saveData(JSONArray wiFiData) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        String currentDateandTime = sdf.format(new Date());
        File textFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), currentDateandTime + ".json");
        try {
            Log.d("moon", textFile.getAbsolutePath());
            FileOutputStream f = new FileOutputStream(textFile);
            PrintWriter pw = new PrintWriter(f);
            pw.print(wiFiData.toString());
            pw.flush();
            pw.close();
            f.close();
            MediaScannerConnection.scanFile(MainContext.INSTANCE.getContext(), new String[] { textFile.getAbsolutePath() }, null, null);
        } catch (FileNotFoundException e) {
            Log.i("moon", "******* File not found. Did you add a WRITE_EXTERNAL_STORAGE permission to the manifest?");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
