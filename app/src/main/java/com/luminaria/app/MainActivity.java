package com.luminaria.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.media.AudioManager;
import android.speech.tts.TextToSpeech;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final int REQ_FILE = 1001;
    private static final String START_URL = "file:///android_asset/index.html";

    /* Makes "download" links (blob: / data:) save a real file through the Android bridge. */
    private static final String HOOK_JS =
        "(function(){if(window.__lumHook)return;window.__lumHook=1;"
      + "var c=HTMLAnchorElement.prototype.click;"
      + "HTMLAnchorElement.prototype.click=function(){"
      + "var a=this;"
      + "if(a.download&&/^(blob:|data:)/.test(a.href)){"
      + "var x=new XMLHttpRequest();x.open('GET',a.href,true);x.responseType='blob';"
      + "x.onload=function(){var b=x.response;var r=new FileReader();"
      + "r.onloadend=function(){AndroidBridge.saveBase64(String(r.result).split(',')[1]||'',b.type||'application/octet-stream',a.download);};"
      + "r.readAsDataURL(b);};"
      + "x.send();return;}"
      + "return c.apply(this,arguments);};})();";

    private WebView web;
    private ValueCallback<Uri[]> filePathCallback;

    private TextToSpeech tts;
    private boolean ttsReady = false;
    private String pendingText = null;
    private String pendingLang = null;

    @SuppressLint({"SetJavaScriptEnabled", "JavascriptInterface"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        tts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                String t = null;
                String l = null;
                synchronized (MainActivity.this) {
                    ttsReady = (status == TextToSpeech.SUCCESS);
                    if (ttsReady && pendingText != null) {
                        t = pendingText;
                        l = pendingLang;
                        pendingText = null;
                    }
                }
                if (t != null) {
                    speakNow(t, l);
                }
            }
        });

        web = new WebView(this);
        setContentView(web);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setMediaPlaybackRequiresUserGesture(false);

        web.addJavascriptInterface(new Bridge(), "AndroidBridge");
        web.addJavascriptInterface(new TtsBridge(), "LuminariaTTS");

        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                if (url.startsWith("file:///android_asset")) {
                    return false;
                }
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, request.getUrl()));
                } catch (Exception ignored) {
                }
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                view.evaluateJavascript(HOOK_JS, null);
            }
        });

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback,
                                             FileChooserParams params) {
                if (filePathCallback != null) {
                    filePathCallback.onReceiveValue(null);
                }
                filePathCallback = callback;
                try {
                    startActivityForResult(params.createIntent(), REQ_FILE);
                } catch (Exception e) {
                    filePathCallback = null;
                    return false;
                }
                return true;
            }
        });

        if (savedInstanceState != null) {
            web.restoreState(savedInstanceState);
        } else {
            web.loadUrl(START_URL);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        web.saveState(outState);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQ_FILE) {
            if (filePathCallback != null) {
                filePathCallback.onReceiveValue(
                    WebChromeClient.FileChooserParams.parseResult(resultCode, data));
                filePathCallback = null;
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onBackPressed() {
        if (web.canGoBack()) {
            web.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        web.onPause();
        if (tts != null) {
            tts.stop();
        }
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        web.onResume();
    }

    private static boolean langBad(int r) {
        return r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED;
    }

    private void speakNow(String text, String lang) {
        if (tts == null) {
            return;
        }
        boolean fil = "fil".equals(lang);
        int r = tts.setLanguage(fil ? new Locale("fil", "PH") : new Locale("en", "PH"));
        if (langBad(r) && fil) {
            r = tts.setLanguage(new Locale("tl", "PH"));
        }
        if (langBad(r)) {
            tts.setLanguage(Locale.US);
        }
        tts.setSpeechRate(0.9f);
        tts.setPitch(1.0f);
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "luminaria");
    }

    private void toast(final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    private String saveFile(byte[] data, String mime, String name) throws IOException {
        if (Build.VERSION.SDK_INT >= 29) {
            ContentValues v = new ContentValues();
            v.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
            v.put(MediaStore.MediaColumns.MIME_TYPE, mime);
            v.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
            Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v);
            if (uri == null) {
                throw new IOException("Could not create file");
            }
            OutputStream out = getContentResolver().openOutputStream(uri);
            if (out == null) {
                throw new IOException("Could not open file");
            }
            try {
                out.write(data);
            } finally {
                out.close();
            }
            return "Downloads folder";
        } else {
            File dir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
            if (dir == null) {
                dir = getFilesDir();
            }
            dir.mkdirs();
            FileOutputStream out = new FileOutputStream(new File(dir, name));
            try {
                out.write(data);
            } finally {
                out.close();
            }
            return "Android > data > com.luminaria.app > files > Download";
        }
    }

    /* The app looks for window.LuminariaTTS and uses it to read aloud. */
    private class TtsBridge {
        @JavascriptInterface
        public void speak(String text, String lang) {
            if (text == null || text.trim().isEmpty()) {
                return;
            }
            boolean ready;
            synchronized (MainActivity.this) {
                ready = ttsReady;
                if (!ready) {
                    pendingText = text;
                    pendingLang = lang;
                }
            }
            if (ready) {
                speakNow(text, lang);
            }
        }

        @JavascriptInterface
        public void stop() {
            if (tts != null) {
                tts.stop();
            }
        }
    }

    private class Bridge {
        @JavascriptInterface
        public void saveBase64(String base64, String mime, String name) {
            try {
                byte[] data = Base64.decode(base64, Base64.DEFAULT);
                String safe = (name == null || name.trim().isEmpty())
                        ? "luminaria-file"
                        : name.replaceAll("[\\\\/:*?\"<>|]", "_");
                String where = saveFile(data, mime, safe);
                toast("Saved " + safe + " in " + where);
            } catch (Exception e) {
                toast("Could not save the file.");
            }
        }
    }
}
