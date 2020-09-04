package com.github.jortdebokx.flutter_barcode_scanner

import com.google.android.gms.vision.barcode.Barcode
import java.util.*

enum class BarcodeFormats(val intValue: Int) {
    ALL_FORMATS(Barcode.ALL_FORMATS), CODE_128(Barcode.CODE_128), CODE_39(Barcode.CODE_39), CODE_93(Barcode.CODE_93), CODABAR(Barcode.CODABAR), DATA_MATRIX(Barcode.DATA_MATRIX), EAN_13(Barcode.EAN_13), EAN_8(Barcode.EAN_8), ITF(Barcode.ITF), QR_CODE(Barcode.QR_CODE), UPC_A(Barcode.UPC_A), UPC_E(Barcode.UPC_E), PDF417(Barcode.PDF417), AZTEC(Barcode.AZTEC);

    companion object {
        private var formatsMap: MutableMap<String, Int>

        /**
         * Return the integer value resulting from OR-ing all of the values
         * of the supplied strings.
         *
         *
         * Note that if ALL_FORMATS is defined as well as other values, ALL_FORMATS
         * will be ignored (following how it would work with just OR-ing the ints).
         *
         * @param strings - list of strings representing the various formats
         * @return integer value corresponding to OR of all the values.
         */
        fun intFromStringList(strings: List<String?>?): Int {
            if (strings == null) return ALL_FORMATS.intValue
            var `val` = 0
            for (string in strings) {
                val asInt = formatsMap!![string]
                if (asInt != null) {
                    `val` = `val` or asInt
                }
            }
            return `val`
        }

        fun stringFromInt(format: Int): String {
            for ((key, value) in formatsMap!!) {
                if (value == format) {
                    return key
                }
            }
            return ""
        }

        init {
            val values = values()
            formatsMap = HashMap(values.size * 4 / 3)
            for (value in values) {
                formatsMap[value.name] = value.intValue
            }
        }
    }

}