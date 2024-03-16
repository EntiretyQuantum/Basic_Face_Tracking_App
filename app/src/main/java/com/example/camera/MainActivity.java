package com.example.camera;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.util.Objects;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {

    private PreviewView previewView;
    private TextView faceDetectedTextView;
    private TextView faceNotVisibleCountTextView;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private int faceNotVisibleCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        previewView = findViewById(R.id.previewView);
        faceDetectedTextView = findViewById(R.id.faceDetectedTextView);
        faceNotVisibleCountTextView = findViewById(R.id.faceNotVisibleCountTextView);

        cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @OptIn(markerClass = ExperimentalGetImage.class) private void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder().build();
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build();

        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), image -> {
            InputImage inputImage = InputImage.fromMediaImage(Objects.requireNonNull(image.getImage()), image.getImageInfo().getRotationDegrees());

            FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                    .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                    .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                    .build();

            FaceDetector detector = com.google.mlkit.vision.face.FaceDetection.getClient(options);

            detector.process(inputImage)
                    .addOnSuccessListener(faces -> {
                        runOnUiThread(() -> {
                            if (faces.size() > 0) {
                                faceDetectedTextView.setText("Face detected");
                                faceNotVisibleCount = 0;
                            } else {
                                faceDetectedTextView.setText("No face detected");
                                faceNotVisibleCount++;
                                if (faceNotVisibleCount == 100) {
                                    Toast.makeText(MainActivity.this, "Face not visible 100 times", Toast.LENGTH_SHORT).show();
                                }
                                if (faceNotVisibleCount == 150) {
                                    Intent intent = new Intent(MainActivity.this, AnotherActivity.class);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                }
                            }
                            faceNotVisibleCountTextView.setText("Face Not Visible Count: " + faceNotVisibleCount);
                        });
                    })
                    .addOnFailureListener(e -> {
                        runOnUiThread(() -> faceDetectedTextView.setText("Face detection failed"));
                    })
                    .addOnCompleteListener(task -> {
                        image.close();
                    });
        });

        cameraProvider.bindToLifecycle(this, cameraSelector, imageAnalysis, preview);
    }
}
