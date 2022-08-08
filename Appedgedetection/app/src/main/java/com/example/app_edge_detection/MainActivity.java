package com.example.app_edge_detection;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {

    /**
     * Camera
     */
    private Button bCamera;
    private static final int CAMERA_REQUEST = 1888;
    private static final int MY_CAMERA_PERMISSION_CODE = 100;
    private ImageView ivPreviewImage;
    int SELECT_PICTURE = 200; // constant to compare the activity result code

    private Bitmap photo_pre;
    private Bitmap photo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        OpenCVLoader.initDebug();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
        ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},1);

        /**
         * Choose image from gallery
         */
        Button bSelectImage = (Button) findViewById(R.id.btnSelectImg);
        ivPreviewImage = (ImageView) findViewById(R.id.img_photo);
        bCamera = (Button) findViewById(R.id.btnCamera);
        Button bReset = (Button) findViewById(R.id.btnReset);
        Button bSave = (Button) findViewById(R.id.btnSave);
        /**
         * filters
         */
        Button bRobert = (Button) findViewById(R.id.btnRobert);
        Button bPrewitt = (Button) findViewById(R.id.btnPrewitt);
        Button bSobel = (Button) findViewById(R.id.btnSobel);
        Button bLaplacian = (Button) findViewById(R.id.btnLaplacian);
        Button bCanny = (Button) findViewById(R.id.btnCanny);

        bSelectImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                imageChooser();
            }
        });

        bCamera.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onClick(View v) {
                if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.CAMERA}, MY_CAMERA_PERMISSION_CODE);
                }
                else {
                    Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivityForResult(cameraIntent, CAMERA_REQUEST);
                }
            }
        });

        /**
         * Action filters
         */

        bRobert.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Robert();
            }
        });
        bPrewitt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Prewitt();
            }
        });


        bLaplacian.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Laplacian();
            }
        });
        bSobel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Sobel();
            }
        });
        bCanny.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Canny();
            }
        });

        bReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Reset();
            }
        });

        bSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveToGallery();
            }
        });

    }

    void imageChooser() {
        // create an instance of the intent of the type image
        Intent i = new Intent();
        i.setType("image/*");
        i.setAction(Intent.ACTION_GET_CONTENT);
        // pass the constant to compare it with the returned requestCode
        startActivityForResult(Intent.createChooser(i, "Select Picture"), SELECT_PICTURE);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            // compare the resultCode with the SELECT_PICTURE constant
            if (requestCode == SELECT_PICTURE) {
                // Get the url of the image from data
                Uri selectedImageUri = data.getData();
                if (null != selectedImageUri) {
                    try {
                        InputStream inputStream = getContentResolver().openInputStream(selectedImageUri);
                        photo = BitmapFactory.decodeStream(inputStream);
                        photo_pre = photo;
                        // update the preview image in the layout
                        ivPreviewImage.setImageBitmap(photo);
//                        ivPreviewImage.setImageURI(selectedImageUri);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            } else if(requestCode == CAMERA_REQUEST){ // compare the resultCode with the CAMERA_REQUEST constant
                Bitmap photo = (Bitmap) data.getExtras().get("data");
                ivPreviewImage.setImageBitmap(photo);
            }
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == MY_CAMERA_PERMISSION_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "camera permission granted", Toast.LENGTH_LONG).show();
                Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(cameraIntent, CAMERA_REQUEST);
            }
            else {
                Toast.makeText(this, "camera permission denied", Toast.LENGTH_LONG).show();
            }
        }
    }

    public void saveToGallery(){
        BitmapDrawable bitmapDrawable = (BitmapDrawable) ivPreviewImage.getDrawable();
        Bitmap bitmap = bitmapDrawable.getBitmap();

        FileOutputStream outputStream = null;
        File file = Environment.getExternalStorageDirectory();
        File dir = new File(file.getAbsolutePath()+"/XLA");
        dir.mkdir();

        String filename = String.format("%d.png", System.currentTimeMillis());
        File outFile = new File(dir,filename);
        try {
            outputStream = new FileOutputStream(outFile);
        }catch (Exception e){
            e.printStackTrace();
        }
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
        try {
            outputStream.flush();
        }catch (Exception e){
            e.printStackTrace();
        }
        try {
            outputStream.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    public void Reset(){
        photo = photo_pre;
        ivPreviewImage.setImageBitmap(photo);
    }

    /**
     * Sobel function
     */
    public void Sobel(){
        Bitmap img = ((BitmapDrawable) ivPreviewImage.getDrawable()).getBitmap();
        Mat source = new Mat();
        Mat destination = new Mat();

        Utils.bitmapToMat(img, source);
        Imgproc.cvtColor(source, source, Imgproc.COLOR_RGBA2GRAY);
        Imgproc.Sobel(source, destination,-1, 1, 1);

        Bitmap resultBitmap = Bitmap.createBitmap(destination.width(), destination.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(destination, resultBitmap);
        ivPreviewImage.setImageBitmap(resultBitmap);
    }

    /**
     * Roberts Cross
     */
    public void Robert(){
        Bitmap img = ((BitmapDrawable) ivPreviewImage.getDrawable()).getBitmap();
        Mat source = new Mat ( img.getHeight(), img.getWidth(), CvType.CV_8UC3);
        Mat Convolvedx = new Mat(source.rows(),source.cols(),source.type());
        Mat Convolvedy = new Mat(source.rows(),source.cols(),source.type());
        Mat destination = new Mat(source.rows(),source.cols(),source.type());

        Mat kernelx = new Mat(3,3, CvType.CV_32F) {
            {
                put(0,0,0);
                put(0,1,0);
                put(0,2,0);

                put(1,0,0);
                put(1,1,1);
                put(1,2,0);

                put(2,0,0);
                put(2,1,0);
                put(2,2,-1);

            }
        };
        Mat kernely = new Mat(3,3, CvType.CV_32F) {
            {
                put(0,0,0);
                put(0,1,0);
                put(0,2,0);

                put(1,0,0);
                put(1,1,0);
                put(1,2,1);

                put(2,0,0);
                put(2,1,-1);
                put(2,2,0);

            }
        };

        Utils.bitmapToMat(img, source);
        Imgproc.cvtColor(source, source, Imgproc.COLOR_RGBA2GRAY);
        Imgproc.filter2D(source, Convolvedx, -1,kernelx);
        Imgproc.filter2D(source, Convolvedy, -1,kernely);
        Core.add(Convolvedx,Convolvedy,destination);


        Bitmap resultBitmap = Bitmap.createBitmap(destination.width(), destination.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(destination, resultBitmap);
        ivPreviewImage.setImageBitmap(resultBitmap);
    }

    /**
     * Prewitt
     */
    public void Prewitt() {
        Bitmap img = ((BitmapDrawable) ivPreviewImage.getDrawable()).getBitmap();
        Mat source = new Mat ( img.getHeight(), img.getWidth(), CvType.CV_8UC3);
        Mat Convolvedx = new Mat(source.rows(),source.cols(),source.type());
        Mat Convolvedy = new Mat(source.rows(),source.cols(),source.type());
        Mat destination = new Mat(source.rows(),source.cols(),source.type());

        Mat kernelx = new Mat(3,3, CvType.CV_32F) {
            {
                put(0,0,-1);
                put(0,1,0);
                put(0,2,1);

                put(1,0,-1);
                put(1,1,0);
                put(1,2,1);

                put(2,0,-1);
                put(2,1,0);
                put(2,2,1);

            }
        };
        Mat kernely = new Mat(3,3, CvType.CV_32F) {
            {
                put(0,0,-1);
                put(0,1,-1);
                put(0,2,-1);

                put(1,0,0);
                put(1,1,0);
                put(1,2,0);

                put(2,0,1);
                put(2,1,1);
                put(2,2,1);

            }
        };

        Utils.bitmapToMat(img, source);
        Imgproc.cvtColor(source, source, Imgproc.COLOR_RGBA2GRAY);
        Imgproc.filter2D(source, Convolvedx, -1,kernelx); //kết quả sẽ có cùng độ sâu với ảnh gốc
        Imgproc.filter2D(source, Convolvedy, -1,kernely);
        Core.add(Convolvedx,Convolvedy,destination);


        Bitmap resultBitmap = Bitmap.createBitmap(destination.width(), destination.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(destination, resultBitmap);
        ivPreviewImage.setImageBitmap(resultBitmap);
    }

    /**
     * Laplacian function
     */
    public void Laplacian(){
        Bitmap img = ((BitmapDrawable) ivPreviewImage.getDrawable()).getBitmap();
        Mat source = new Mat();
        Mat destination = new Mat();

        Utils.bitmapToMat(img, source);
        Imgproc.cvtColor(source, source, Imgproc.COLOR_RGBA2GRAY);        Imgproc.Laplacian(source, destination, source.depth());

        Bitmap resultBitmap = Bitmap.createBitmap(destination.width(), destination.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(destination, resultBitmap);
        ivPreviewImage.setImageBitmap(resultBitmap);
    }

    /**
     * Canny
     */
    public void Canny(){
        Bitmap img = ((BitmapDrawable) ivPreviewImage.getDrawable()).getBitmap();
        Bitmap myBitmap32 = img.copy(Bitmap.Config.ARGB_8888, true);
        Mat source = new Mat ( img.getHeight(), img.getWidth(), CvType.CV_8UC3);
        Mat gray = new Mat(source.size(), CvType.CV_8UC1);
        Mat edge = new Mat();
        Mat destination = new Mat();

        Utils.bitmapToMat(myBitmap32, source);
        Imgproc.cvtColor(source, gray, Imgproc.COLOR_RGB2GRAY);
        Imgproc.Canny(gray, edge, 80, 90);
        Imgproc.cvtColor(edge, destination, Imgproc.COLOR_GRAY2RGBA,4);

        Bitmap resultBitmap = Bitmap.createBitmap(destination.cols(), destination.rows(),Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(destination, resultBitmap);
        ivPreviewImage.setImageBitmap(resultBitmap);
    }

}