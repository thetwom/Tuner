package de.moekadu.tuner.instruments

//import android.content.Context
//import android.content.Intent
//import android.net.Uri
//import androidx.activity.result.contract.ActivityResultContract
//
//class InstrumentFileWriterContract : ActivityResultContract<String, String?>() {
//
//    override fun createIntent(context: Context, input: String): Intent {
//        return Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
//            addCategory(Intent.CATEGORY_OPENABLE)
//            type = "text/plain"
//            putExtra(Intent.EXTRA_TITLE, "tuner.txt")
//            // default path
//            // putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri)
//        }
//    }
//
//    override fun parseResult(resultCode: Int, intent: Intent?): String? {
//        val uri = intent?.data
//        return saveInstruments(uri)
//    }
//}
//
//private class InstrumentFileReaderContract : ActivityResultContract<String, Uri?>() {
//    override fun createIntent(context: Context, input: String): Intent {
//        return Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
//            addCategory(Intent.CATEGORY_OPENABLE)
//            type = "text/plain"
//            // default path
//            // putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri)
//        }
//    }
//
//    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
////            Log.v("Tuner", "InstrumentsArchiving.FileReaderContract.parseResult: $intent, $resultCode, ${resultCode== Activity.RESULT_OK}")
//        return intent?.data
//    }
//}
