package com.nintech.budgment.ui.common

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatDate(millis: Long): String =
    SimpleDateFormat("d MMM yyyy", Locale("es", "MX")).format(Date(millis))