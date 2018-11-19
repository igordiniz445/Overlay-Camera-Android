package com.example.technolog_igor.camerasurfaceview;


import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainMenu extends AppCompatActivity implements View.OnClickListener, Camera.ShutterCallback, Camera.PictureCallback {

    private CameraController cameraController;
    private boolean isCameraActive;
    private ImageButton botaoCamera;
    private ImageButton botaoConfirmar;
    private ImageButton botaoFlash;
    private ImageButton botaoSwitchCamera;
    private Bitmap fotoTirada;
    private boolean flashMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);

        isCameraActive = true;
        flashMode = false;
        cameraController = new CameraController(this, R.id.cameraSurfaceView);

        botaoCamera = findViewById(R.id.btn_tirarFoto);
        botaoConfirmar = findViewById(R.id.btn_confirmarFoto);
        botaoFlash = findViewById(R.id.flashButton);
        botaoSwitchCamera = findViewById(R.id.swtichCamera);
        botaoSwitchCamera.setOnClickListener(this);
        botaoCamera.setOnClickListener(this);
        botaoConfirmar.setOnClickListener(this);
        botaoFlash.setOnClickListener(this);

        if (!getApplication().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
            botaoFlash.setVisibility(View.GONE);
        }
    }

    @Override
    public void onPictureTaken(byte[] bytes, Camera camera) {
        Bitmap foto = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        fotoTirada = foto;
        cameraController.pararVisualizacao();
        botaoSwitchCamera.setVisibility(View.GONE);
    }

    @Override
    public void onShutter() {
        //capturar novamente
        botaoCamera.setImageResource(R.drawable.ic_cameranaook);
        botaoConfirmar.setVisibility(View.VISIBLE);
        isCameraActive = false;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_tirarFoto:
                if (isCameraActive) {
                    cameraController.tirarFoto(this, null, this);
                } else {
                    isCameraActive = true;
                    //tirar foto
                    botaoCamera.setImageResource(R.drawable.ic_cameravermelha2);
                    botaoConfirmar.setVisibility(View.GONE);
                    botaoSwitchCamera.setVisibility(View.VISIBLE);
                    cameraController.iniciarVisualizacao();
                }
                break;
            case R.id.btn_confirmarFoto:
                saveImage(fotoTirada, "photo");
                isCameraActive = true;
                //tirar foto
                botaoCamera.setImageResource(R.drawable.ic_cameravermelha2);
                botaoConfirmar.setVisibility(View.GONE);
                botaoSwitchCamera.setVisibility(View.VISIBLE);
                cameraController.iniciarVisualizacao();
                //finish();
                break;
            case R.id.flashButton:
                if (isCameraActive) {
                    flashMode = cameraController.botaoFlash(flashMode);
                }
                break;
            case R.id.swtichCamera:
                cameraController.switchCamera();

        }
    }

    private void saveImage(Bitmap finalBitmap, String image_name) {
        try {
            File albumDir = getAlbumStorageDir();
            OutputStream imageOut = null;
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String imageFileName = "DARWIN_" + timeStamp;
            File file = new File(albumDir, imageFileName + ".jpg");
            imageOut = new FileOutputStream(file);
            //Bitmap -> JPEG with 85% compression rate
            Matrix matrix = new Matrix();
            if(CameraController.CAMERA_SELECIONADA == 1){
                matrix.postRotate(-90);
            }else{
                matrix.postRotate(90);
            }

            Bitmap scaledBitmap = Bitmap.createScaledBitmap(finalBitmap, finalBitmap.getWidth(), finalBitmap.getHeight(), true);

            Bitmap rotatedBitmap = Bitmap.createBitmap(scaledBitmap, 0, 0, scaledBitmap.getWidth(), scaledBitmap.getHeight(), matrix, true);
            rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 85, imageOut);
            imageOut.flush();
            imageOut.close();
            //update gallery so you can view the image in gallery
            updateGallery("myAlbum", albumDir, file, imageFileName);
            Toast.makeText(MainMenu.this,"Imagem Salva com sucesso!",Toast.LENGTH_LONG).show();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(MainMenu.this,"Falha ao salvar imagem!",Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(MainMenu.this,"Falha ao salvar imagem!",Toast.LENGTH_LONG).show();
        }
    }
    public File getAlbumStorageDir() {
        // Get the directory for the user's public pictures directory.
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "myAlbum");
        file.mkdirs();
        return file;
    }
    private void updateGallery(String albumName, File albumDir, File file, String imageFileName) {
        //metadata of new image
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, imageFileName);
        values.put(MediaStore.Images.Media.DESCRIPTION, albumName);
        values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());
        values.put(MediaStore.Images.ImageColumns.BUCKET_ID, file.toString().toLowerCase(Locale.US).hashCode());
        values.put(MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME, file.getName().toLowerCase(Locale.US));
        values.put("_data", file.getAbsolutePath());

        ContentResolver cr = getContentResolver();
        cr.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        File f = new File(albumDir.getAbsolutePath());
        Uri contentUri = Uri.fromFile(f);
        //notify gallery for new image
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, contentUri);
        getApplicationContext().sendBroadcast(mediaScanIntent);
    }


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        checkOrientation(newConfig);
    }

    private void checkOrientation(Configuration newConfig) {
        // Checks the orientation of the screen
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }
}

