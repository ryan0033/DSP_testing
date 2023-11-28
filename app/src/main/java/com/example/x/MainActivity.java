package com.example.x; // 注意：這裡應該是你的應用程式包名稱

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.io.android.AudioDispatcherFactory;
import be.tarsos.dsp.mfcc.MFCC;

public class MainActivity extends AppCompatActivity {

    private static final int READ_REQUEST_CODE = 42;
    private TextView mfccTextView;

    private String getPathFromUri(Uri uri) {
        String[] projection = {MediaStore.Audio.Media.DATA};
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);

        if (cursor != null) {
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
            cursor.moveToFirst();
            String filePath = cursor.getString(column_index);
            cursor.close();
            return filePath;
        }

        return null;
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        result = cursor.getString(nameIndex);
                    } else {
                        // 若 DISPLAY_NAME 欄位不存在，嘗試獲取其他相關欄位
                        // 例如 MediaStore.Audio.Media.TITLE 或 MediaStore.Audio.Media._ID
                        // 如果有其他適合的欄位，請替換它
                        int titleIndex = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
                        if (titleIndex != -1) {
                            result = cursor.getString(titleIndex);
                        } else {
                            int idIndex = cursor.getColumnIndex(MediaStore.Audio.Media._ID);
                            if (idIndex != -1) {
                                result = cursor.getString(idIndex);
                            }
                        }
                    }
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        if (result == null) {
            // 如果仍然找不到適合的名稱，使用檔案路徑的最後一部分作為名稱
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 初始化 TextView
        mfccTextView = findViewById(R.id.mfccTextView);

        // 設置按鈕點擊事件
        Button loadFileButton = findViewById(R.id.loadFileButton);
        loadFileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 在按鈕點擊事件中處理 MFCC 計算邏輯
                openFileChooser();
            }
        });
    }

    private void openFileChooser() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("audio/*"); // 接受所有音訊格式

        someActivityResultLauncher.launch(intent);
    }

    ActivityResultLauncher<Intent> someActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    // 獲取選擇的檔案路徑
                    String filePath = getPathFromUri(result.getData().getData());

                    if (filePath != null) {
                        // 獲取選擇的檔案名稱
                        String fileName = getFileName(result.getData().getData());

                        // 計算 MFCC 並更新 TextView
                        float[] mfccValues = calculateMFCC(filePath);
                        updateMFCCTextView(mfccValues, fileName);

                        // 在這裡添加上傳成功的處理代碼
                        showUploadSuccessMessage();
                    }
                } else {
                    // 處理取消選擇或其他情況
                    Toast.makeText(this, "選擇取消或發生錯誤", Toast.LENGTH_SHORT).show();
                }
            });

    private float[] calculateMFCC(String filePath) {
        try {
            int sampleRate = 44100;
            int bufferSize = 1024;
            int bufferOverlap = 512;

            AudioDispatcher dispatcher = AudioDispatcherFactory.fromPipe(filePath, sampleRate, bufferSize, bufferOverlap);
            MFCC mfcc = new MFCC(bufferSize, sampleRate, 13, 40, 300, 3000);

            dispatcher.addAudioProcessor(mfcc);
            dispatcher.run();

            return mfcc.getMFCC();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new float[0];
    }

    private void updateMFCCTextView(float[] mfccValues, String fileName) {
        StringBuilder sb = new StringBuilder("MFCC Values for file: " + fileName + "\n");
        for (float value : mfccValues) {
            sb.append(value).append("\n");
        }
        mfccTextView.setText(sb.toString());
        // 在這裡添加上傳成功的處理代碼
        showUploadSuccessMessage();
    }

    private void showUploadSuccessMessage() {
        // 顯示一個 Toast 消息，或者你可以使用其他方式通知用戶上傳成功
        Toast.makeText(getApplicationContext(), "上傳成功", Toast.LENGTH_SHORT).show();
    }
}
