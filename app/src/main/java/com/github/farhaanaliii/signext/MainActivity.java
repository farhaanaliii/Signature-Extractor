package com.github.farhaanaliii.signext;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class MainActivity extends AppCompatActivity {
    private TextInputEditText packageNameInput;
    private MaterialButton getBtn;
    private MaterialButton copyBtn;
    private TextView sigText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();

        getBtn.setOnClickListener(v -> {
            String pkgN = "";
            if (packageNameInput.getText() != null) {
                pkgN = packageNameInput.getText().toString().trim();
            }
            
            if (pkgN.isEmpty()) {
                sigText.setText("Please enter a package name");
                return;
            }
            
            String signature = getSignature(pkgN);
            sigText.setText(signature);
        });

        copyBtn.setOnClickListener(v -> {
            String content = sigText.getText().toString();
            if (content.isEmpty() || content.startsWith("Please") || content.startsWith("Package not found")) {
                Toast.makeText(this, "Nothing to copy", Toast.LENGTH_SHORT).show();
                return;
            }
            
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Signature", content);
            if (clipboard != null) {
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, "Signature copied to clipboard", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void init() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        packageNameInput = findViewById(R.id.packageName);
        getBtn = findViewById(R.id.getBtn);
        copyBtn = findViewById(R.id.copyBtn);
        sigText = findViewById(R.id.sigText);
    }

    private String getSignature(String pkg) {
        try {
            PackageManager pm = getPackageManager();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                PackageInfo pi = pm.getPackageInfo(pkg, PackageManager.GET_SIGNING_CERTIFICATES);
                if (pi.signingInfo != null) {
                    Signature[] sigs = pi.signingInfo.getApkContentsSigners();
                    if (sigs != null && sigs.length > 0) {
                        return sigs[0].toCharsString();
                    }
                }
            }

            // Fallback for older versions or if signingInfo is null
            PackageInfo pi = pm.getPackageInfo(pkg, PackageManager.GET_SIGNATURES);
            Signature[] sigs = pi.signatures;
            if (sigs != null && sigs.length > 0) {
                return sigs[0].toCharsString();
            }
            return "No signature found";
        } catch (PackageManager.NameNotFoundException nnfe) {
            return "Package not found: " + pkg;
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
}
