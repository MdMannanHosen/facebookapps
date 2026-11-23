package com.mannanhosen.gymflow.core.util

/** Minimal CSV writer. Quotes only when a value needs it. */
object Csv {

    /** Escapes a value per RFC 4180: wrap in quotes and double inner quotes. */
    fun escape(value: String?): String {
        val text = value.orEmpty()
        val needsQuoting = text.contains(',') || text.contains('"') ||
            text.contains('\n') || text.contains('\r')
        return if (needsQuoting) "\"" + text.replace("\"", "\"\"") + "\"" else text
    }

    /** Builds one CSV line from mixed values (numbers keep their plain form). */
    fun row(vararg values: Any?): String =
        values.joinToString(separator = ",") { escape(it?.toString()) }

    /** Joins header + data rows into a complete CSV document. */
    fun document(rows: List<List<Any?>>): String =
        rows.joinToString(separator = "\n") { values -> row(*values.toTypedArray()) }
}