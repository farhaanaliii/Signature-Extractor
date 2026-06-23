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
	private String getSignature(String pkg){
		try{
			PackageManager pm = getPackageManager();
			PackageInfo pi = pm.getPackageInfo(pkg, PackageManager.GET_SIGNATURES);
			Signature[] sigs = pi.signatures;
			Signature sig = sigs[0];
			return sig.toCharsString();
		}catch(PackageManager.NameNotFoundException nnfe){
			return nnfe.toString();
		}
	}
}
