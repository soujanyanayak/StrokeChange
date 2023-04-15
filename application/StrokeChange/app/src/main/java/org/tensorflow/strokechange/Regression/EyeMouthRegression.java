package org.tensorflow.strokechange.Regression;

import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.util.Log;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class EyeMouthRegression {

    public MappedByteBuffer loadEyeModelFile(Activity activity) throws IOException {
        AssetFileDescriptor fileDescriptor=activity.getAssets().openFd("eye-regression.tflite");
        FileInputStream inputStream=new FileInputStream(fileDescriptor.getFileDescriptor());

        FileChannel fileChannel=inputStream.getChannel();
        long startOffset=fileDescriptor.getStartOffset();
        long declareLength=fileDescriptor.getDeclaredLength();
        Log.d("Regression", "Loaded Model");
        return fileChannel.map(FileChannel.MapMode.READ_ONLY,startOffset,declareLength);
    }
    public MappedByteBuffer loadMouthModelFile(Activity activity) throws IOException {
        AssetFileDescriptor fileDescriptor=activity.getAssets().openFd("mouth-regression.tflite");
        FileInputStream inputStream=new FileInputStream(fileDescriptor.getFileDescriptor());

        FileChannel fileChannel=inputStream.getChannel();
        long startOffset=fileDescriptor.getStartOffset();
        long declareLength=fileDescriptor.getDeclaredLength();
        Log.d("Regression", "Loaded Model");
        return fileChannel.map(FileChannel.MapMode.READ_ONLY,startOffset,declareLength);
    }
}

