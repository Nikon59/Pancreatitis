package com.pancreatitis.ui.helps;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.pancreatitis.R;

public class HelpsFragment extends Fragment {

    private static final String HELP_URL = "file:///android_asset/help.html";

    private WebView helpWebView;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View view = inflater.inflate(R.layout.fragment_helps, container, false);

        helpWebView = view.findViewById(R.id.helpWebView);

        setupWebView();
        helpWebView.loadUrl(HELP_URL);

        return view;
    }

    private void setupWebView() {
        WebSettings settings = helpWebView.getSettings();

        // JavaScript для статичной HTML-справки обычно не нужен
        settings.setJavaScriptEnabled(false);

        // Адаптация страницы под ширину экрана
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(false);

        // Отключаем зум, чтобы справка выглядела как обычный экран приложения
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);

        // Доступ к локальным файлам и контенту
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);

        helpWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedError(
                    WebView view,
                    WebResourceRequest request,
                    WebResourceError error
            ) {
                if (request.isForMainFrame()) {
                    if (getContext() != null) {
                        Toast.makeText(
                                getContext(),
                                "Не удалось загрузить страницу помощи",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        if (helpWebView != null) {
            helpWebView.stopLoading();

            ViewGroup parent = (ViewGroup) helpWebView.getParent();
            if (parent != null) {
                parent.removeView(helpWebView);
            }

            helpWebView.destroy();
            helpWebView = null;
        }

        super.onDestroyView();
    }
}