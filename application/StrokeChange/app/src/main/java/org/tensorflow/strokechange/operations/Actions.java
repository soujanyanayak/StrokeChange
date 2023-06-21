package org.tensorflow.strokechange.operations;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Environment;
import android.util.Log;
import android.widget.ImageView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class Actions {

    public void saveToGallery(Bitmap bitmap, String fileName){
        Log.d("saveToGallery", bitmap.toString());

        FileOutputStream outputStream = null;
//        File file = Environment.getExternalStorageDirectory();
//        File dir = new File(file.getAbsolutePath() + "/StrokeImages");
//        dir.mkdirs();


//        String filename = String.format("%s.pn"fileName);
        File outfile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),fileName);

        try{
            outputStream = new FileOutputStream(outfile);
            bitmap.compress(Bitmap.CompressFormat.PNG,0,outputStream);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }


        try{
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
