package com.github.farhaanaliii.signext;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private TextInputEditText packageNameInput;
    private MaterialButton getBtn;
    private MaterialButton copyBtn;
    private TextView sigText;

    private TabLayout tabLayout;
    private View installedContainer;
    private View classicContainer;

    private RecyclerView appsRecyclerView;
    private ProgressBar progressBar;
    private TextView emptyTextView;
    private TextInputEditText searchEditText;
    private ChipGroup filterChipGroup;

    private AppAdapter appAdapter;
    private List<AppInfo> masterAppList = new ArrayList<>();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private SharedPreferences favoritePrefs;
    private Set<String> favoritePackages = new HashSet<>();

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
            performCopyAction(copyBtn, content);
        });

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    installedContainer.setVisibility(View.VISIBLE);
                    classicContainer.setVisibility(View.GONE);
                } else {
                    installedContainer.setVisibility(View.GONE);
                    classicContainer.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        favoritePrefs = getSharedPreferences("favorites_prefs", Context.MODE_PRIVATE);
        favoritePackages = new HashSet<>(favoritePrefs.getStringSet("favorite_packages", new HashSet<>()));

        appsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        appAdapter = new AppAdapter(
            this::showDetailsBottomSheet,
            this::toggleFavorite
        );
        appsRecyclerView.setAdapter(appAdapter);

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterAndDisplayApps();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        loadInstalledApps();
    }

    private void init() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        packageNameInput = findViewById(R.id.packageName);
        getBtn = findViewById(R.id.getBtn);
        copyBtn = findViewById(R.id.copyBtn);
        sigText = findViewById(R.id.sigText);

        tabLayout = findViewById(R.id.tabLayout);
        installedContainer = findViewById(R.id.installedContainer);
        classicContainer = findViewById(R.id.classicContainer);

        appsRecyclerView = findViewById(R.id.appsRecyclerView);
        progressBar = findViewById(R.id.progressBar);
        emptyTextView = findViewById(R.id.emptyTextView);
        searchEditText = findViewById(R.id.searchEditText);
        filterChipGroup = findViewById(R.id.filterChipGroup);

        findViewById(R.id.chipAll).setOnClickListener(v -> filterAndDisplayApps());
        findViewById(R.id.chipUser).setOnClickListener(v -> filterAndDisplayApps());
        findViewById(R.id.chipSystem).setOnClickListener(v -> filterAndDisplayApps());
        findViewById(R.id.chipStarred).setOnClickListener(v -> filterAndDisplayApps());
    }

    private void loadInstalledApps() {
        progressBar.setVisibility(View.VISIBLE);
        appsRecyclerView.setVisibility(View.GONE);
        emptyTextView.setVisibility(View.GONE);

        executorService.execute(() -> {
            List<AppInfo> apps = new ArrayList<>();
            PackageManager pm = getPackageManager();
            List<PackageInfo> packages = pm.getInstalledPackages(0);

            for (PackageInfo packageInfo : packages) {
                ApplicationInfo appInfo = packageInfo.applicationInfo;
                if (appInfo == null) continue;

                String label = appInfo.loadLabel(pm).toString();
                String packageName = packageInfo.packageName;
                Drawable icon = appInfo.loadIcon(pm);
                boolean isSystem = (appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
                boolean isFavorite = favoritePackages.contains(packageName);

                apps.add(new AppInfo(label, packageName, icon, isSystem, isFavorite));
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                apps.sort((a, b) -> a.getLabel().compareToIgnoreCase(b.getLabel()));
            } else {
                java.util.Collections.sort(apps, (a, b) -> a.getLabel().compareToIgnoreCase(b.getLabel()));
            }

            runOnUiThread(() -> {
                masterAppList = apps;
                progressBar.setVisibility(View.GONE);
                appsRecyclerView.setVisibility(View.VISIBLE);
                filterAndDisplayApps();
            });
        });
    }

    private void filterAndDisplayApps() {
        int checkedId = filterChipGroup.getCheckedChipId();
        String query = searchEditText.getText() != null ? searchEditText.getText().toString().trim().toLowerCase() : "";
        List<AppInfo> filtered = new ArrayList<>();

        for (AppInfo app : masterAppList) {
            app.setFavorite(favoritePackages.contains(app.getPackageName()));

            boolean matchesChip = false;
            if (checkedId == R.id.chipAll) {
                matchesChip = true;
            } else if (checkedId == R.id.chipUser) {
                matchesChip = !app.isSystem();
            } else if (checkedId == R.id.chipSystem) {
                matchesChip = app.isSystem();
            } else if (checkedId == R.id.chipStarred) {
                matchesChip = app.isFavorite();
            }

            if (matchesChip) {
                if (query.isEmpty() || 
                    app.getLabel().toLowerCase().contains(query) || 
                    app.getPackageName().toLowerCase().contains(query)) {
                    filtered.add(app);
                }
            }
        }

        appAdapter.updateList(filtered);

        if (filtered.isEmpty()) {
            emptyTextView.setVisibility(View.VISIBLE);
            if (checkedId == R.id.chipStarred && query.isEmpty()) {
                emptyTextView.setText("No starred apps. Tap the star icon next to an app to add it here.");
            } else {
                emptyTextView.setText("No applications found");
            }
        } else {
            emptyTextView.setVisibility(View.GONE);
        }
    }

    private void toggleFavorite(AppInfo app) {
        boolean newFavoriteState = !app.isFavorite();
        app.setFavorite(newFavoriteState);
        if (newFavoriteState) {
            favoritePackages.add(app.getPackageName());
        } else {
            favoritePackages.remove(app.getPackageName());
        }
        favoritePrefs.edit().putStringSet("favorite_packages", favoritePackages).apply();
        filterAndDisplayApps();
    }

    private void showDetailsBottomSheet(AppInfo app) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_app_details, null);
        dialog.setContentView(dialogView);

        ImageView dialogAppIcon = dialogView.findViewById(R.id.dialogAppIcon);
        TextView dialogAppName = dialogView.findViewById(R.id.dialogAppName);
        TextView dialogAppPackage = dialogView.findViewById(R.id.dialogAppPackage);
        TextView dialogSigText = dialogView.findViewById(R.id.dialogSigText);
        MaterialButton dialogCopyBtn = dialogView.findViewById(R.id.dialogCopyBtn);

        dialogAppIcon.setImageDrawable(app.getIcon());
        dialogAppName.setText(app.getLabel());
        dialogAppPackage.setText(app.getPackageName());

        String signature = getSignature(app.getPackageName());
        dialogSigText.setText(signature);

        dialogCopyBtn.setOnClickListener(v -> {
            performCopyAction(dialogCopyBtn, signature);
        });

        dialog.show();
    }

    private void performCopyAction(MaterialButton button, String content) {
        if (content.isEmpty() || content.startsWith("Please") || content.startsWith("Package not found") || content.startsWith("No signature") || content.startsWith("Error")) {
            Toast.makeText(this, "Nothing to copy", Toast.LENGTH_SHORT).show();
            return;
        }

        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Signature", content);
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip);
        }

        button.setEnabled(false);
        button.setText("Copied!");
        button.setIconResource(R.drawable.ic_check);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            button.setEnabled(true);
            button.setText("Copy Signature");
            button.setIconResource(R.drawable.ic_copy);
        }, 1500);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}
