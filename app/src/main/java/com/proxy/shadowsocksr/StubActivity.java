package com.proxy.shadowsocksr;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class StubActivity extends Activity
{
    @Override protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
    }
}
