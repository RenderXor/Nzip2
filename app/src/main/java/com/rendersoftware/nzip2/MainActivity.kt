package com.rendersoftware.nzip2


import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.rendersoftware.nzip2.databinding.ActivityMainBinding
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.exception.ZipException
import java.io.File
import java.io.FileOutputStream


class MainActivity : Activity() {

    private val tag = "NZip2"

    private val REQUEST_CODE_READ_EXTERNAL_STORAGE = 100
    private val REQUEST_CODE_SELECT_FILE = 101

    private lateinit var listViewFiles: ListView

    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        listViewFiles = binding.listview

        val intent = intent
        val uri = intent.data
        if(uri != null){
            openFileFromIntent(intent)
        } else {
            selectFile()
        }

    }

    private fun openFileFromIntent(intent: Intent) {
        val uri = intent.data
        if (uri != null) {
            val contentResolver = applicationContext.contentResolver
            val inputStream = contentResolver.openInputStream(uri)
            if (inputStream != null) {
                //val tempFile = File.createTempFile("temp_zip", ".zip")
                val tempFilePath = makeEmptyFile().absolutePath//tempFile.absolutePath
                //Copia el contenido del inputStream a un archivo temporal
                FileOutputStream(tempFilePath).use { output -> inputStream.copyTo(output) }
                inputStream.close()

                val zipFile = ZipFile(tempFilePath)
                mostrarArchivos(zipFile)
            }
        }
    }

    private fun checkPermissisons(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            //return Environment.isExternalStorageManager()
            true
        } else {
            ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun grantPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {/*try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(
                    this, "No se ha podido conceder el permiso para acceder al almacenamiento externo", Toast.LENGTH_SHORT
                ).show()
            }*/
        } else {
            ActivityCompat.requestPermissions(
                this, arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), REQUEST_CODE_READ_EXTERNAL_STORAGE
            )
        }
    }

    //create function for save file


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_READ_EXTERNAL_STORAGE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(
                this, "SE ha concedido el permiso para acceder al almacenamiento externo", Toast.LENGTH_SHORT
            ).show()
        } else {
            Toast.makeText(
                this, "No se ha concedido el permiso para acceder al almacenamiento externo", Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun selectFile() {
        if (checkPermissisons()) {
            val intent = Intent()
            intent.action = Intent.ACTION_OPEN_DOCUMENT
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "application/zip"

            startActivityForResult(intent, REQUEST_CODE_SELECT_FILE)
        } else {
            grantPermissions()
        }
    }

    private fun abrirArchivo(zipFile: ZipFile, filename: String, targetFile: File) {
        deleteFileIfExists(targetFile)
        zipFile.extractFile(filename, filesDir.absolutePath)
        if (targetFile.exists()) {
            try {
                val fileUri =
                    FileProvider.getUriForFile(this, applicationContext.packageName + ".provider", targetFile)
                val mime =
                    MimeTypeMap.getSingleton().getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(targetFile.absolutePath))
                val intent = Intent(Intent.ACTION_VIEW)
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                intent.setDataAndType(fileUri, mime)
                startActivity(intent)
            } catch (e: Exception) {
                Log.e(tag, "abrirArchivoEncriptado2", e)
                Toast.makeText(applicationContext, "No se encontro una aplicacion en el dispositivo para abrir este archivo", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(
                applicationContext, "No se encontro el archivo", Toast.LENGTH_SHORT
            ).show()
        }

    }


    private fun abrirArchivoEncriptado(zipFile: ZipFile, filename: String, targetFile: File) {
        deleteFileIfExists(targetFile)
        if (zipFile.isEncrypted) {
            val passwordEditText = EditText(this)
            val alertDialog =
                AlertDialog.Builder(this).setTitle("Contraseña Requerida").setMessage("Introduzca la contraseña del archivo").setView(passwordEditText).setPositiveButton("Aceptar") { _, _ ->

                    val password = passwordEditText.text.toString()
                    //TODO No hace falta la contraseña para ver los archivos del zip encriptado solo para extraerlos
                    zipFile.setPassword(password.toCharArray())

                    try {
                        zipFile.extractFile(filename, filesDir.absolutePath)
                    } catch (e: ZipException) {
                        Log.e(tag, "abrirArchivoEncriptado1", e)
                        //create Toast message
                        Toast.makeText(
                            applicationContext, "Clave incorrecta o archivo dañado", Toast.LENGTH_SHORT
                        ).show()
                        deleteFileIfExists(targetFile)
                    }
                    if (targetFile.exists()) {
                        try {
                            val fileUri =
                                FileProvider.getUriForFile(this, applicationContext.packageName + ".provider", targetFile)
                            val mime =
                                MimeTypeMap.getSingleton().getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(targetFile.absolutePath))
                            val intent = Intent(Intent.ACTION_VIEW)
                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            intent.setDataAndType(fileUri, mime)
                            startActivity(intent)
                        } catch (e: Exception) {
                            Log.e(tag, "abrirArchivoEncriptado2", e)
                            Toast.makeText(applicationContext, "No se encontro una aplicacion en el dispositivo para abrir este archivo", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(
                            applicationContext, "No se encontro el archivo", Toast.LENGTH_SHORT
                        ).show()
                    }

                }.setNegativeButton("Cancelar") { dialog, which ->

                }.create()
            alertDialog.show()
        }
    }

    //create function for make empty file on filesdir path
    private fun makeEmptyFile(): File {
        val file = File(filesDir, "test.zip")
        deleteFileIfExists(file)
        file.createNewFile()
        Log.i(tag, "makeEmptyFile")
        return file
    }

    private fun deleteFileIfExists(file: File) {
        if (file.exists()) {
            Log.i(tag, "deleteFileIfExists")
            file.delete()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_SELECT_FILE && resultCode == RESULT_OK) {
            Log.i(tag, "onActivityResult")
            try {
                val uri = data?.data
                if (uri != null) {
                    val contentResolver = applicationContext.contentResolver
                    val inputStream = contentResolver.openInputStream(uri)
                    if (inputStream != null) {
                        //val tempFile = File.createTempFile("temp_zip", ".zip")
                        val tempFilePath = makeEmptyFile().absolutePath//tempFile.absolutePath
                        //Copia el contenido del inputStream a un archivo temporal
                        FileOutputStream(tempFilePath).use { output -> inputStream.copyTo(output) }
                        inputStream.close()

                        val zipFile = ZipFile(tempFilePath)
                        mostrarArchivos(zipFile)

                        //tempFile.delete()

                    } else {
                        Log.i(tag, "onActivityResult el inputstream es null")
                    }
                }
            } catch (e: ZipException) {
                Log.e(tag, e.stackTrace.toString())
            }
        }
    }

    //TODO crear funcion para recargar la vista al pulstar el boton atras si el listview no esta vacio


    private fun mostrarArchivos(zipFile: ZipFile) {
        try {
            val entries = zipFile.fileHeaders.map { it.fileName }
            listViewFiles.adapter = ArrayAdapter(
                this, android.R.layout.simple_list_item_1, entries
            )

            listViewFiles.setOnItemClickListener { parent, view, position, id ->
                val fileName = entries[position]
                val targetFile = File(filesDir, fileName)

                if (zipFile.isEncrypted) {
                    abrirArchivoEncriptado(zipFile, fileName, targetFile)
                } else {
                    abrirArchivo(zipFile, fileName, targetFile)
                }
            }

        } catch (e: Exception) {
            Log.e(tag, "Exception mostrarArchivos", e)
        } catch (e: ZipException) {
            Log.e(tag, "ZipException mostrarArchivos", e)
        }
    }

}