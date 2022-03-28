package com.example.testtask


import android.app.Dialog
import android.os.Bundle
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController

class SaveRecordDialogFragment : DialogFragment() {
    lateinit var editText: EditText
    lateinit var navController: NavController
    var alertString: String? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val alertCode = arguments?.getInt("isAlert")
        alertCheck(alertCode)
        return activity?.let {
            val builder = AlertDialog.Builder(it, R.style.CustomAlertDialogTheme)
            val dialogAlert = layoutInflater.inflate(R.layout.save_record_name_alert, null)
            editText = dialogAlert.findViewById(R.id.editText)
            builder.setView(dialogAlert)
            if (alertString != null) {
                builder.setTitle(alertString)
            } else {
                builder.setTitle("Сохранение записи")
            }
                .setCancelable(false)
                .setPositiveButton("Сохранить") { dialog, id ->
                    navController = findNavController()
                    navController.previousBackStackEntry?.savedStateHandle?.set("key", 1)
                    navController.previousBackStackEntry?.savedStateHandle?.set(
                        "nameOfFile",
                        editText.text.toString()
                    )
                    dismiss()
                }
                .setNegativeButton("Удалить")
                { dialog, id ->
                    navController = findNavController()
                    navController.previousBackStackEntry?.savedStateHandle?.set("key", 0)
                    navController.previousBackStackEntry?.savedStateHandle?.set(
                        "nameOfFile",
                        null
                    )

                }
            builder.create().apply { setCanceledOnTouchOutside(false) }
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    private fun alertCheck(code: Int?) {
        when (code) {
            121 -> alertString = "Имя заметки некорректно\n" +
                    "Попробуйте ввести его снова"
        }
    }
}