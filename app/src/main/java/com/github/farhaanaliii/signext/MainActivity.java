package com.github.farhaanaliii.signext;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.widget.EditText;
import android.widget.Button;
import android.widget.TextView;
import android.view.View;
import android.content.pm.PackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.Signature;

public class MainActivity extends AppCompatActivity {
    EditText packageName;
	Button getBtn;
	TextView sigText;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
		
		Toolbar toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
        
		init();
		
		getBtn.setOnClickListener(v -> {
            String pkgN = packageName.getText().toString();
            String signature = getSignature(pkgN);
            sigText.setText(signature);
        });
    }
    private void init(){
		packageName = findViewById(R.id.packageName);
		getBtn = findViewById(R.id.getBtn);
		sigText = findViewById(R.id.sigText);
	}
	private String getSignature(String pkg) {
		try {
			PackageManager pm = getPackageManager();
			if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
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
