package com.example.uammap.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.LocalDining
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Place
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.uammap.model.CategoriaPOI

object IconUtils {
    fun getIconForCategory(categoria: CategoriaPOI): ImageVector = when (categoria) {
        CategoriaPOI.BIBLIOTECA -> Icons.Default.MenuBook
        CategoriaPOI.CAJA       -> Icons.Default.AccountBalance
        CategoriaPOI.CAFETERIA  -> Icons.Default.LocalDining
        CategoriaPOI.AUDITORIO  -> Icons.Default.Mic
        CategoriaPOI.OTRO       -> Icons.Default.Place
    }
}
