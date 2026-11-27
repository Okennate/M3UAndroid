package com.m3u.smartphone.ui.business.guide

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.m3u.data.database.model.Programme
import com.m3u.data.service.MediaCommand
import com.m3u.i18n.R
import com.m3u.smartphone.ui.common.helper.LocalHelper
import com.m3u.smartphone.ui.common.helper.Metadata
import com.m3u.core.foundation.util.basic.title
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

@Composable
internal fun GuideRoute(
    navigateToChannel: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    viewModel: GuideViewModel = hiltViewModel(),
) {
    val title = stringResource(R.string.ui_title_guide)
    val helper = LocalHelper.current
    val scope = rememberCoroutineScope()
    val channels = viewModel.channels.collectAsLazyPagingItems()

    LifecycleResumeEffect(title) {
        Metadata.title = AnnotatedString(title.title())
        Metadata.color = Color.Unspecified
        Metadata.contentColor = Color.Unspecified
        Metadata.actions = emptyList()
        onPauseOrDispose {
            Metadata.actions = emptyList()
        }
    }

    LazyColumn(
        contentPadding = contentPadding,
        modifier = modifier.fillMaxSize(),
    ) {
        if (channels.itemCount == 0) {
            item {
                Text(
                    text = stringResource(R.string.ui_guide_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(24.dp),
                )
            }
        }

        items(
            count = channels.itemCount,
            key = channels.itemKey { it.channel.id },
        ) { index ->
            val item = channels[index] ?: return@items
            val channel = item.channel
            val programme = item.programme ?: return@items
            ListItem(
                modifier = Modifier.clickable {
                    scope.launch {
                        helper.play(MediaCommand.Common(channel.id))
                        navigateToChannel()
                    }
                },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Outlined.Tv,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                    )
                },
                headlineContent = {
                    Text(
                        text = channel.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                supportingContent = {
                    Column {
                        Text(
                            text = stringResource(R.string.ui_guide_now),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = programme.title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                trailingContent = {
                    Text(
                        text = programme.timeRangeLabel(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
            )
            HorizontalDivider()
        }
    }
}

private fun Programme.timeRangeLabel(): String {
    val formatter = DateFormat.getTimeInstance(DateFormat.SHORT)
    val startLabel = formatter.format(Date(start))
    val endLabel = formatter.format(Date(end))
    return "$startLabel – $endLabel"
}
