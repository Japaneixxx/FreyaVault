package androidx.compose.ui.res

import androidx.compose.runtime.Composable
import com.japaneixxx.freya.R

@Composable
fun stringResource(id: Int): String = R.string.of(id)
