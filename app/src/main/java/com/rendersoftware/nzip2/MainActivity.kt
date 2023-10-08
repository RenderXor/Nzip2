package com.rendersoftware.nzip2


import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.rendersoftware.nzip2.databinding.ActivityMainBinding
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.exception.ZipException
import java.io.File
import java.io.FileOutputStream


class MainActivity : Activity() {

    private val TAG = "ZipManager"

    private val REQUEST_CODE_READ_EXTERNAL_STORAGE = 100
    private val REQUEST_CODE_SELECT_FILE = 101

    private lateinit var listViewFiles: ListView

    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        binding.button.setOnClickListener {
            onSelectFile()
        }

        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
                REQUEST_CODE_READ_EXTERNAL_STORAGE
            )
        } else {
            initViews()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_READ_EXTERNAL_STORAGE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            initViews()
        } else {
            Toast.makeText(
                this,
                "No se ha concedido el permiso para acceder al almacenamiento externo",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun initViews() {
        listViewFiles = binding.listview
    }

    private fun onSelectFile() {
        val intent = Intent()
        intent.action = Intent.ACTION_OPEN_DOCUMENT
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "application/zip"

        startActivityForResult(intent, REQUEST_CODE_SELECT_FILE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_SELECT_FILE && resultCode == Activity.RESULT_OK) {
            Log.i(TAG, "onActivityResult")
            try {
                val uri = data?.data
                if (uri != null) {
                    val contentResolver = applicationContext.contentResolver
                    val inputStream = contentResolver.openInputStream(uri)
                    if (inputStream != null) {
                        val tempFile = File.createTempFile("temp_zip", ".zip")
                        val tempFilePath = tempFile.absolutePath
                        //Copia el contenido del inputStream a un archivo temporal
                        FileOutputStream(tempFilePath).use { output -> inputStream.copyTo(output) }
                        //Verifica si el archivo zip esta protegido con cotraseña
                        val isPasswordProtected = isZipFilePasswordProtected(tempFilePath)
                        if (isPasswordProtected) {
                            Log.i(TAG, "onActivityResult el archvo esta protegido con contraseña")

                            try {
                                val password = "71021550B"
                                val zipFile = ZipFile(tempFilePath)
                                if (zipFile.isEncrypted) {
                                    zipFile.setPassword(password.toCharArray())
                                    val entries = zipFile.fileHeaders.map { it.fileName }
                                    listViewFiles.adapter = ArrayAdapter(
                                        this,
                                        android.R.layout.simple_list_item_1,
                                        entries
                                    )
                                }
                            } catch (e: ZipException) {
                                Log.e(
                                    TAG,
                                    "onActivityResult Error abriendo el archivo encriptado",
                                    e
                                )
                            }

                        } else {
                            Log.i(
                                TAG,
                                "onActivityResult el archivo NO esta protegido con cotraseña"
                            )
                            val zipFile = ZipFile(tempFilePath)
                            val entries = zipFile.fileHeaders.map { it.fileName }
                            listViewFiles.adapter =
                                ArrayAdapter(this, android.R.layout.simple_list_item_1, entries)
                        }

                        tempFile.delete()
                        inputStream.close()
                    } else {
                        Log.i(TAG, "onActivityResult el inputstream es null")
                    }
                }
            } catch (e: ZipException) {
                Log.e(TAG, e.stackTrace.toString())
            }
        }
    }

    private fun isZipFilePasswordProtected(zipFilePath: String): Boolean {
        try {
            val zipFile = ZipFile(zipFilePath)
            return zipFile.isEncrypted
        } catch (e: Exception) {
            // Maneja el error aquí si ocurre uno al verificar la protección con contraseña.
        }
        return false
    }

}