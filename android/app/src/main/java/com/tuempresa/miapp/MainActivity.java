package com.M3ssa10.miapp;

import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
  @Override
  public void onCreate(android.os.Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    registerPlugin(FolderTxtReaderPlugin.class);
  }
}
