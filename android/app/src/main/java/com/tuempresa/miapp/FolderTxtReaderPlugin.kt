package com.tuempresa.miapp

import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResult
import androidx.documentfile.provider.DocumentFile
import com.getcapacitor.*
import com.getcapacitor.annotation.CapacitorPlugin
import com.getcapacitor.annotation.ActivityCallback
import java.io.BufferedReader
import java.io.InputStreamReader

@CapacitorPlugin(name = "FolderTxtReader")
class FolderTxtReaderPlugin : Plugin() {

  @PluginMethod
  fun pickFolderAndReadTxt(call: PluginCall) {
    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
      addFlags(Intent.FLAG_GRANT_PREFIX_URI_PERMISSION)
    }
    startActivityForResult(call, intent, "onFolderPicked")
  }

  @ActivityCallback
  private fun onFolderPicked(call: PluginCall, result: ActivityResult) {
    if (result.resultCode != android.app.Activity.RESULT_OK) {
      call.reject("Cancelado")
      return
    }

    val treeUri: Uri? = result.data?.data
    if (treeUri == null) {
      call.reject("No se recibió URI de carpeta")
      return
    }

    // Persistir permiso para poder leer luego (si el usuario vuelve a abrir la app)
    try {
      val flags = result.data?.flags ?: 0
      val takeFlags = flags and
        (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
      context.contentResolver.takePersistableUriPermission(treeUri, takeFlags)
    } catch (_: Exception) {
      // Si falla, igual intentamos leer en esta sesión
    }

    val root = DocumentFile.fromTreeUri(context, treeUri)
    if (root == null || !root.isDirectory) {
      call.reject("La URI no es un directorio")
      return
    }

    val out = JSArray()
    walk(root) { file ->
      val name = file.name ?: return@walk
      if (file.isFile && name.lowercase().endsWith(".txt")) {
        val text = readText(file.uri)
        val obj = JSObject()
        obj.put("name", name)
        obj.put("uri", file.uri.toString())
        obj.put("text", text)
        out.put(obj)
      }
    }

    val ret = JSObject()
    ret.put("folderUri", treeUri.toString())
    ret.put("files", out)
    call.resolve(ret)
  }

  private fun walk(dir: DocumentFile, onFile: (DocumentFile) -> Unit) {
    val children = dir.listFiles()
    for (c in children) {
      if (c.isDirectory) walk(c, onFile) else onFile(c)
    }
  }

  private fun readText(uri: Uri): String {
    return try {
      context.contentResolver.openInputStream(uri)?.use { input ->
        BufferedReader(InputStreamReader(input)).use { br ->
          br.readText()
        }
      } ?: ""
    } catch (e: Exception) {
      ""
    }
  }
}

