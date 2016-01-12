package com.example.remmy.photoz;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    private static ImageView imgView;
    private static Button buttonSbm;
    static final int REQUEST_IMAGE_CAPTURE = 1;

    private static final int SELECTED_PICTURE = 2;
    private String mCurrentPhotoPath;
    public String TAG = MainActivity.class.getSimpleName();

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
    };
    private int targetW;
    private int targetH;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imgView = (ImageView) findViewById(R.id.imageView);

        verifyStoragePermissions(this);
    }

    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }

    public void btnClick(View v) {
        if (v.getId() == R.id.buttonCamera) {
            dispatchTakePictureIntent();
        } else if (v.getId() == R.id.buttonChooseImage) {
            Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(i, SELECTED_PICTURE);
        } else if (v.getId() == R.id.buttonUpload) {
            Toast.makeText(this, "Coming soon", Toast.LENGTH_SHORT).show();
        }
        /**/
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case SELECTED_PICTURE:
                if (resultCode == RESULT_OK) {
                    Uri uri = data.getData();
                    String[] projection = {MediaStore.Images.Media.DATA};

                    Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
                    cursor.moveToFirst();

                    int columnIndex = cursor.getColumnIndex(projection[0]);
                    String filePath = cursor.getString(columnIndex);
                    cursor.close();
                    /*Log.d(TAG, "onActivityResult: filepath" + filePath);
                    Bitmap yourSelectedImage = BitmapFactory.decodeFile(filePath);
                    Drawable d = new BitmapDrawable(yourSelectedImage);
                    imgView.setBackground(d);
                    if(yourSelectedImage != null){
                       // putOverlay(yourSelectedImage);
                    }*/
                    setPic(filePath);

                    //Drawable d = new BitmapDrawable(yourSelectedImage);


                   /* Drawable foreground = new BitmapDrawable(BitmapFactory.decodeResource(getResources(), R.drawable.banner));
                    imgView.set(foreground);*/
                    //imgView.setBackground(d);
                }
                break;
            case REQUEST_IMAGE_CAPTURE:
                if (resultCode == RESULT_OK) {
                    setPic(mCurrentPhotoPath);
                }

        }
    }

    private void setPic(String photoPath) {
        // Get the dimensions of the View
        targetW = imgView.getWidth();
        targetH = imgView.getHeight();

        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(photoPath, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        // Determine how much to scale down the image
        int scaleFactor = Math.min(photoW / targetW, photoH / targetH);

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

        Bitmap bitmap = BitmapFactory.decodeFile(photoPath, bmOptions);
        putOverlay(bitmap);

    }

    public void putOverlay(Bitmap bitmap) {
        Bitmap bmOverlay = Bitmap.createBitmap(targetW, targetH, bitmap.getConfig());
        Bitmap banner = BitmapFactory.decodeResource(getResources(), R.drawable.banner);

        float marginLeft = (float) (targetW * 0.5 - banner.getWidth() * 0.5);
        float marginTop = (float) (targetH * 0.5 - banner.getHeight() * 0.5);

        Canvas canvas = new Canvas(bmOverlay);
        Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
        canvas.drawBitmap(bitmap, new Matrix(), null);
        canvas.drawBitmap(banner, marginLeft, marginTop, paint);

        ColorMatrix matrix = new ColorMatrix();
        matrix.setSaturation(0);

        ColorMatrixColorFilter filter = new ColorMatrixColorFilter(matrix);
        imgView.setImageBitmap(bmOverlay);
        imgView.setColorFilter(filter);
    }

    private void dispatchTakePictureIntent() {
        Log.d(TAG, "dispatchTakePictureIntent: ");
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
                Log.d(TAG, "dispatchTakePictureIntent: error" + ex.getMessage().toString());
                Toast.makeText(this, "Could not create image file", Toast.LENGTH_SHORT).show();
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                        Uri.fromFile(photoFile));
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }

        } else {
            Toast.makeText(this, "Activity not found", Toast.LENGTH_SHORT).show();
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        //mCurrentPhotoPath = "file:" + image.getAbsolutePath();
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }
}
